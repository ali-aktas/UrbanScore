package com.aliaktas.urbanscore.ui.profile

import android.content.Intent
import android.graphics.Bitmap
import com.aliaktas.urbanscore.base.BaseViewModel
import com.aliaktas.urbanscore.data.repository.CityRepository
import com.aliaktas.urbanscore.data.repository.UserRepository
import com.aliaktas.urbanscore.util.ImageUtils
import com.aliaktas.urbanscore.util.NetworkUtil
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.collectLatest
import javax.inject.Inject

@HiltViewModel
class ProfileViewModel @Inject constructor(
    private val userRepository: UserRepository,
    private val cityRepository: CityRepository,
    private val networkUtil: NetworkUtil,
    private val imageUtils: ImageUtils
) : BaseViewModel() {

    // State for visited cities
    private val _visitedCities = MutableStateFlow<List<VisitedCitiesAdapter.VisitedCityItem>>(emptyList())
    val visitedCities: StateFlow<List<VisitedCitiesAdapter.VisitedCityItem>> = _visitedCities.asStateFlow()

    // State for wishlist cities
    private val _wishlistCities = MutableStateFlow<List<WishlistCitiesAdapter.WishlistCityItem>>(emptyList())
    val wishlistCities: StateFlow<List<WishlistCitiesAdapter.WishlistCityItem>> = _wishlistCities.asStateFlow()

    // State for share intent
    private val _shareIntent = MutableStateFlow<Intent?>(null)
    val shareIntent: StateFlow<Intent?> = _shareIntent.asStateFlow()

    // Map to cache city details
    private val cityCache = mutableMapOf<String, com.aliaktas.urbanscore.data.model.CityModel>()

    init {
        loadVisitedCities()
        loadWishlistCities()
    }

    private fun loadVisitedCities() {
        launch {
            if (!networkUtil.isNetworkAvailable()) {
                emitEvent(UiEvent.Error("No internet connection. Some data may not be available."))
                return@launch
            }

            userRepository.getUserVisitedCities()
                .catch { e ->
                    handleError(e)
                }
                .collectLatest { visitedCitiesMap ->
                    if (visitedCitiesMap.isEmpty()) {
                        _visitedCities.value = emptyList()
                        return@collectLatest
                    }

                    // Convert map to list with city details
                    val visitedWithRatings = mutableListOf<Pair<String, Double>>()
                    for ((cityId, rating) in visitedCitiesMap) {
                        visitedWithRatings.add(cityId to rating)
                    }

                    fetchCityDetails(visitedWithRatings)
                }
        }
    }

    private fun fetchCityDetails(citiesWithRatings: List<Pair<String, Double>>) {
        if (citiesWithRatings.isEmpty()) {
            _visitedCities.value = emptyList()
            return
        }

        val result = mutableListOf<VisitedCitiesAdapter.VisitedCityItem>()
        var completedCount = 0

        citiesWithRatings.forEach { (cityId, rating) ->
            launch {
                try {
                    // Şehir detaylarını al
                    cityRepository.getCityById(cityId)
                        .catch { e ->
                            handleError(e)
                            synchronized(result) {
                                completedCount++
                                checkCompletion(result, completedCount, citiesWithRatings.size)
                            }
                        }
                        .collectLatest { city ->
                            city?.let {
                                synchronized(result) {
                                    // Cache şehir bilgisini
                                    cityCache[cityId] = it

                                    // VisitedCityItem oluştur
                                    result.add(
                                        VisitedCitiesAdapter.VisitedCityItem(
                                            id = it.id,
                                            name = it.cityName,
                                            country = it.country,
                                            flagUrl = it.flagUrl,
                                            userRating = rating
                                        )
                                    )

                                    completedCount++
                                    checkCompletion(result, completedCount, citiesWithRatings.size)
                                }
                            } ?: run {
                                synchronized(result) {
                                    completedCount++
                                    checkCompletion(result, completedCount, citiesWithRatings.size)
                                }
                            }
                        }
                } catch (e: Exception) {
                    handleError(e)
                    synchronized(result) {
                        completedCount++
                        checkCompletion(result, completedCount, citiesWithRatings.size)
                    }
                }
            }
        }
    }

    private fun checkCompletion(cities: List<VisitedCitiesAdapter.VisitedCityItem>, completed: Int, total: Int) {
        if (completed >= total) {
            val sorted = cities.sortedByDescending { it.userRating }
            _visitedCities.value = sorted
        }
    }

    private fun loadWishlistCities() {
        launch {
            if (!networkUtil.isNetworkAvailable()) {
                // Already shown error in loadVisitedCities, no need to show again
                return@launch
            }

            userRepository.getUserWishlist()
                .catch { e ->
                    handleError(e)
                }
                .collectLatest { wishlistIds ->
                    if (wishlistIds.isEmpty()) {
                        _wishlistCities.value = emptyList()
                        return@collectLatest
                    }

                    fetchWishlistCityDetails(wishlistIds)
                }
        }
    }

    private fun fetchWishlistCityDetails(cityIds: List<String>) {
        if (cityIds.isEmpty()) {
            _wishlistCities.value = emptyList()
            return
        }

        val result = mutableListOf<WishlistCitiesAdapter.WishlistCityItem>()
        var completedCount = 0

        cityIds.forEach { cityId ->
            launch {
                try {
                    // Şehir detaylarını al
                    cityRepository.getCityById(cityId)
                        .catch { e ->
                            handleError(e)
                            synchronized(result) {
                                completedCount++
                                checkWishlistCompletion(result, completedCount, cityIds.size)
                            }
                        }
                        .collectLatest { city ->
                            city?.let {
                                synchronized(result) {
                                    // Cache şehir bilgisini
                                    cityCache[cityId] = it

                                    // WishlistCityItem oluştur
                                    result.add(
                                        WishlistCitiesAdapter.WishlistCityItem(
                                            id = it.id,
                                            name = it.cityName,
                                            country = it.country,
                                            flagUrl = it.flagUrl
                                        )
                                    )

                                    completedCount++
                                    checkWishlistCompletion(result, completedCount, cityIds.size)
                                }
                            } ?: run {
                                synchronized(result) {
                                    completedCount++
                                    checkWishlistCompletion(result, completedCount, cityIds.size)
                                }
                            }
                        }
                } catch (e: Exception) {
                    handleError(e)
                    synchronized(result) {
                        completedCount++
                        checkWishlistCompletion(result, completedCount, cityIds.size)
                    }
                }
            }
        }
    }

    private fun checkWishlistCompletion(cities: List<WishlistCitiesAdapter.WishlistCityItem>, completed: Int, total: Int) {
        if (completed >= total) {
            val sorted = cities.sortedBy { it.name }
            _wishlistCities.value = sorted
        }
    }

    fun shareVisitedCities() {
        launch {
            val cities = _visitedCities.value
            if (cities.isEmpty()) {
                emitEvent(UiEvent.Error("You don't have any visited cities to share"))
                return@launch
            }

            try {
                // Bitmap oluşturma işlemi artık ImageUtils sınıfına taşındı
                val bitmap = imageUtils.createVisitedCitiesImage(cities, cities.size)
                val intent = imageUtils.createShareImageIntent(bitmap)
                _shareIntent.value = intent
            } catch (e: Exception) {
                handleError(e)
                emitEvent(UiEvent.Error("Error creating image: ${e.message}"))
            }
        }
    }

    fun shareWishlist() {
        val cities = _wishlistCities.value
        if (cities.isEmpty()) {
            launch { emitEvent(UiEvent.Error("You don't have any cities in your bucket list to share")) }
            return
        }

        val shareText = buildString {
            append("My city bucket list on UrbanRate:\n\n")
            cities.forEachIndexed { index, city ->
                append("${index + 1}. ${city.name}, ${city.country}\n")
            }
            append("\nDownload UrbanRate to create your own bucket list!")
        }

        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_SUBJECT, "My UrbanRate Bucket List")
            putExtra(Intent.EXTRA_TEXT, shareText)
        }

        _shareIntent.value = intent
    }

    fun clearShareIntent() {
        _shareIntent.value = null
    }

    fun removeFromWishlist(cityId: String) {
        launchWithLoading {
            try {
                val result = userRepository.removeFromWishlist(cityId)
                result.fold(
                    onSuccess = {
                        emitEvent(UiEvent.Success("City removed from bucket list"))
                    },
                    onFailure = { e ->
                        handleError(e)
                    }
                )
            } catch (e: Exception) {
                handleError(e)
            }
        }
    }

    fun refreshData() {
        loadVisitedCities()
        loadWishlistCities()
    }
}