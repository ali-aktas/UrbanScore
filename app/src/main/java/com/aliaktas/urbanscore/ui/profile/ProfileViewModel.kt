package com.aliaktas.urbanscore.ui.profile

import android.content.Intent
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aliaktas.urbanscore.data.model.CityModel
import com.aliaktas.urbanscore.data.repository.CityRepository
import com.aliaktas.urbanscore.data.repository.UserRepository
import com.google.firebase.auth.FirebaseAuth
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ProfileViewModel @Inject constructor(
    private val userRepository: UserRepository,
    private val cityRepository: CityRepository,
    private val auth: FirebaseAuth
) : ViewModel() {

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
    private val cityCache = mutableMapOf<String, CityModel>()

    init {
        loadVisitedCities()
        loadWishlistCities()
    }

    private fun loadVisitedCities() {
        viewModelScope.launch {
            Log.d("ProfileViewModel", "Loading visited cities...")

            userRepository.getUserVisitedCities()
                .catch { e ->
                    Log.e("ProfileViewModel", "Error loading visited cities", e)
                }
                .collect { visitedCitiesMap ->
                    Log.d("ProfileViewModel", "Received ${visitedCitiesMap.size} visited cities")

                    if (visitedCitiesMap.isEmpty()) {
                        _visitedCities.value = emptyList()
                        return@collect
                    }

                    // Convert map to list with city details
                    val cityIds = visitedCitiesMap.keys.toList()
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

        Log.d("ProfileViewModel", "Fetching details for ${citiesWithRatings.size} cities")

        val result = mutableListOf<VisitedCitiesAdapter.VisitedCityItem>()
        var completedCount = 0

        citiesWithRatings.forEach { (cityId, rating) ->
            viewModelScope.launch {
                try {
                    // Şehir detaylarını al
                    cityRepository.getCityById(cityId)
                        .catch { e ->
                            Log.e("ProfileViewModel", "Error fetching city $cityId", e)
                            synchronized(result) {
                                completedCount++
                                checkCompletion(result, completedCount, citiesWithRatings.size)
                            }
                        }
                        .collect { city ->
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
                    Log.e("ProfileViewModel", "Error in city fetch: ${e.message}", e)
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
            Log.d("ProfileViewModel", "All cities fetched, updating UI with ${sorted.size} cities")
            _visitedCities.value = sorted
        }
    }

    private fun loadWishlistCities() {
        viewModelScope.launch {
            Log.d("ProfileViewModel", "Loading wishlist cities...")

            userRepository.getUserWishlist()
                .catch { e ->
                    Log.e("ProfileViewModel", "Error loading wishlist", e)
                }
                .collect { wishlistIds ->
                    Log.d("ProfileViewModel", "Received ${wishlistIds.size} wishlist cities")

                    if (wishlistIds.isEmpty()) {
                        _wishlistCities.value = emptyList()
                        return@collect
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

        Log.d("ProfileViewModel", "Fetching details for ${cityIds.size} wishlist cities")

        val result = mutableListOf<WishlistCitiesAdapter.WishlistCityItem>()
        var completedCount = 0

        cityIds.forEach { cityId ->
            viewModelScope.launch {
                try {
                    // Şehir detaylarını al
                    cityRepository.getCityById(cityId)
                        .catch { e ->
                            Log.e("ProfileViewModel", "Error fetching wishlist city $cityId", e)
                            synchronized(result) {
                                completedCount++
                                checkWishlistCompletion(result, completedCount, cityIds.size)
                            }
                        }
                        .collect { city ->
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
                    Log.e("ProfileViewModel", "Error in wishlist city fetch", e)
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
            Log.d("ProfileViewModel", "All wishlist cities fetched, updating UI with ${sorted.size} cities")
            _wishlistCities.value = sorted
        }
    }

    fun shareVisitedCities() {
        val cities = _visitedCities.value
        if (cities.isEmpty()) return

        val shareText = buildString {
            append("My visited cities on UrbanScore:\n\n")
            cities.forEachIndexed { index, city ->
                append("${index + 1}. ${city.name}, ${city.country} - ${String.format("%.1f", city.userRating)}/10\n")
            }
            append("\nDownload UrbanScore to rate your favorite cities!")
        }

        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_SUBJECT, "My UrbanScore Visited Cities")
            putExtra(Intent.EXTRA_TEXT, shareText)
        }

        _shareIntent.value = intent
    }

    fun shareWishlist() {
        val cities = _wishlistCities.value
        if (cities.isEmpty()) return

        val shareText = buildString {
            append("My city bucket list on UrbanScore:\n\n")
            cities.forEachIndexed { index, city ->
                append("${index + 1}. ${city.name}, ${city.country}\n")
            }
            append("\nDownload UrbanScore to create your own bucket list!")
        }

        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_SUBJECT, "My UrbanScore Bucket List")
            putExtra(Intent.EXTRA_TEXT, shareText)
        }

        _shareIntent.value = intent
    }

    fun clearShareIntent() {
        _shareIntent.value = null
    }

    fun removeFromWishlist(cityId: String) {
        viewModelScope.launch {
            Log.d("ProfileViewModel", "Removing city $cityId from wishlist")
            try {
                val result = userRepository.removeFromWishlist(cityId)
                result.fold(
                    onSuccess = {
                        Log.d("ProfileViewModel", "Successfully removed $cityId from wishlist")
                    },
                    onFailure = { e ->
                        Log.e("ProfileViewModel", "Error removing from wishlist", e)
                    }
                )
            } catch (e: Exception) {
                Log.e("ProfileViewModel", "Exception removing from wishlist", e)
            }
        }
    }
}