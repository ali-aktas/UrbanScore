package com.aliaktas.urbanscore.ui.profile

import android.content.Intent
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aliaktas.urbanscore.data.model.CityModel
import com.aliaktas.urbanscore.data.repository.CityRepository
import com.aliaktas.urbanscore.data.repository.UserRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ProfileViewModel @Inject constructor(
    private val userRepository: UserRepository,
    private val cityRepository: CityRepository
) : ViewModel() {

    // State for visited cities
    private val _visitedCities = MutableStateFlow<List<VisitedCitiesAdapter.VisitedCityItem>>(emptyList())
    val visitedCities: StateFlow<List<VisitedCitiesAdapter.VisitedCityItem>> = _visitedCities.asStateFlow()

    // State for wishlist cities
    private val _wishlistCities = MutableStateFlow<List<WishlistCitiesAdapter.WishlistCityItem>>(emptyList())
    val wishlistCities: StateFlow<List<WishlistCitiesAdapter.WishlistCityItem>> = _wishlistCities.asStateFlow()

    // Map to cache city details (to avoid fetching the same city multiple times)
    private val cityCache = mutableMapOf<String, CityModel>()

    init {
        loadVisitedCities()
        loadWishlistCities()
    }

    private fun loadVisitedCities() {
        viewModelScope.launch {
            try {
                userRepository.getUserVisitedCities()
                    .catch { e ->
                        Log.e("ProfileViewModel", "Error loading visited cities", e)
                    }
                    .collect { visitedCitiesMap ->
                        // Bu haritadan şehir kimliklerini al
                        val cityIds = visitedCitiesMap.keys.toList()

                        // Eğer harita boşsa, boş liste göster ve çık
                        if (cityIds.isEmpty()) {
                            _visitedCities.value = emptyList()
                            return@collect
                        }

                        // Tüm şehirleri paralel olarak yükle ve sonuçları birleştir
                        val allCities = mutableListOf<VisitedCitiesAdapter.VisitedCityItem>()

                        // Her şehir için detayları al ve listeye ekle
                        cityIds.forEach { cityId ->
                            val rating = visitedCitiesMap[cityId] ?: 0.0

                            // Önce cache'den kontrol et
                            if (cityCache.containsKey(cityId)) {
                                val city = cityCache[cityId]!!
                                allCities.add(createVisitedCityItem(city, rating))
                            } else {
                                // Değilse Repository'den al
                                launch {
                                    cityRepository.getCityById(cityId)
                                        .catch { e ->
                                            Log.e("ProfileViewModel", "Error fetching city $cityId", e)
                                        }
                                        .collect { city ->
                                            city?.let {
                                                cityCache[cityId] = it
                                                allCities.add(createVisitedCityItem(it, rating))

                                                // Listeyi rating'e göre sırala ve güncelle
                                                val sortedCities = allCities.sortedByDescending { it.userRating }
                                                _visitedCities.value = sortedCities
                                            }
                                        }
                                }
                            }
                        }

                        // Eğer tüm şehirler cache'den yüklendiyse, şimdi güncelle
                        if (allCities.size == cityIds.size) {
                            val sortedCities = allCities.sortedByDescending { it.userRating }
                            _visitedCities.value = sortedCities
                        }
                    }
            } catch (e: Exception) {
                Log.e("ProfileViewModel", "Error in loadVisitedCities", e)
            }
        }
    }

    // Yardımcı metot - Şehir modelinden VisitedCityItem oluşturur
    private fun createVisitedCityItem(city: CityModel, rating: Double): VisitedCitiesAdapter.VisitedCityItem {
        return VisitedCitiesAdapter.VisitedCityItem(
            id = city.id,
            name = city.cityName,
            country = city.country,
            flagUrl = city.flagUrl,
            userRating = rating
        )
    }

    private fun loadWishlistCities() {
        viewModelScope.launch {
            userRepository.getUserWishlist()
                .catch { e ->
                    Log.e("ProfileViewModel", "Error loading wishlist", e)
                }
                .collect { wishlistIds ->
                    // For each city ID, fetch the city details
                    val cityItems = mutableListOf<WishlistCitiesAdapter.WishlistCityItem>()

                    wishlistIds.forEach { cityId ->
                        if (cityCache.containsKey(cityId)) {
                            // Use cached city data
                            val city = cityCache[cityId]!!
                            cityItems.add(
                                WishlistCitiesAdapter.WishlistCityItem(
                                    id = city.id,
                                    name = city.cityName,
                                    country = city.country,
                                    flagUrl = city.flagUrl
                                )
                            )
                        } else {
                            // Fetch city data from repository
                            cityRepository.getCityById(cityId)
                                .catch { e ->
                                    Log.e("ProfileViewModel", "Error fetching city $cityId", e)
                                }
                                .collect { city ->
                                    city?.let {
                                        // Cache the city data
                                        cityCache[cityId] = it

                                        cityItems.add(
                                            WishlistCitiesAdapter.WishlistCityItem(
                                                id = it.id,
                                                name = it.cityName,
                                                country = it.country,
                                                flagUrl = it.flagUrl
                                            )
                                        )

                                        // Sort alphabetically by name and update the state
                                        _wishlistCities.value = cityItems.sortedBy { item -> item.name }
                                    }
                                }
                        }
                    }

                    // If no cities or all cities were loaded from cache
                    if (wishlistIds.isEmpty() || cityItems.size == wishlistIds.size) {
                        _wishlistCities.value = cityItems.sortedBy { it.name }
                    }
                }
        }
    }

    // Method to share visited cities
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

        // This intent needs to be started from an Activity, so we'll need to handle this in the Fragment
        _shareIntent.value = intent
    }

    // Method to share wishlist
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

        // This intent needs to be started from an Activity, so we'll need to handle this in the Fragment
        _shareIntent.value = intent
    }

    // State for share intent
    private val _shareIntent = MutableStateFlow<Intent?>(null)
    val shareIntent: StateFlow<Intent?> = _shareIntent.asStateFlow()

    // Intent kullanıldıktan sonra temizle
    fun clearShareIntent() {
        _shareIntent.value = null
    }

    // İstek listesinden şehir kaldır
    fun removeFromWishlist(cityId: String) {
        viewModelScope.launch {
            try {
                val result = userRepository.removeFromWishlist(cityId)
                result.fold(
                    onSuccess = {
                        // Başarılı, flow otomatik olarak güncellenecek
                        Log.d("ProfileViewModel", "Şehir istek listesinden kaldırıldı: $cityId")
                    },
                    onFailure = { e ->
                        Log.e("ProfileViewModel", "Şehir kaldırılırken hata: ${e.message}")
                    }
                )
            } catch (e: Exception) {
                Log.e("ProfileViewModel", "İstek listesinden kaldırma hatası", e)
            }
        }
    }
}