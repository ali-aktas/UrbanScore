package com.aliaktas.urbanscore.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aliaktas.urbanscore.data.model.CityModel
import com.aliaktas.urbanscore.data.repository.CityRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val cityRepository: CityRepository
) : ViewModel() {

    private val _state = MutableStateFlow<HomeState>(HomeState.Initial)
    val state = _state.asStateFlow()

    // Önbellek değişkeni
    private var cachedCities: List<CityModel>? = null

    // Veri yükleniyor mu kontrolü için flag
    private var isDataLoading = false

    private var currentCategory: String = "averageRating"

    init {
        viewModelScope.launch {
            delay(100) // Önceki kodunuzdaki gecikmeyi koruyorum
            refreshCities(false) // false = önbellek varsa kullan
        }
    }

    fun refreshCities(forceRefresh: Boolean = true, category: String? = null) {
        // Eğer kategori sağlanmışsa güncelle
        if (category != null) {
            currentCategory = category
        }

        // Zaten yükleme yapılıyorsa, tekrar etme
        if (isDataLoading) {
            return
        }

        // Önbellekte veri var ve zorla yenileme istenmiyorsa önbellekten al
        if (!forceRefresh && cachedCities != null) {
            _state.value = HomeState.Success(cachedCities!!)
            return
        }

        // Mevcut verileri yükleme sırasında korumak için al
        val currentCities = if (_state.value is HomeState.Success) {
            (_state.value as HomeState.Success).cities
        } else {
            cachedCities
        }

        // Yükleme durumuna geç, eski verileri koruyarak
        _state.value = HomeState.Loading(oldData = currentCities)

        // Veri yükleme bayrağını true yap
        isDataLoading = true

        viewModelScope.launch {
            try {
                // Kategori spesifik sorgu kullan
                val citiesFlow = cityRepository.getCitiesByCategoryRating(currentCategory)

                citiesFlow
                    .catch { e ->
                        isDataLoading = false
                        _state.value = HomeState.Error(e.message ?: "An error occurred")
                    }
                    .collect { cities ->
                        // Burada sıralama yapmaya gerek yok, sorgu zaten seçilen kategoriye göre sıralanmış geliyor
                        cachedCities = cities
                        _state.value = HomeState.Success(cities)
                        isDataLoading = false
                    }
            } catch (e: Exception) {
                isDataLoading = false
                _state.value = HomeState.Error(e.message ?: "An error occurred")
            }
        }
    }

    // Kategoriyi açıkça değiştirmek için yeni bir metot
    fun switchCategory(category: String) {
        if (category != currentCategory) {
            refreshCities(true, category)
        }
    }

    fun sortByRating(ascending: Boolean = false) {
        val currentState = _state.value
        if (currentState is HomeState.Success) {
            val sortedCities = if (ascending) {
                currentState.cities.sortedBy { it.averageRating }
            } else {
                currentState.cities.sortedByDescending { it.averageRating }
            }
            cachedCities = sortedCities
            _state.value = HomeState.Success(sortedCities)
        }
    }

    fun sortByPopulation(ascending: Boolean = false) {
        val currentState = _state.value
        if (currentState is HomeState.Success) {
            val sortedCities = if (ascending) {
                currentState.cities.sortedBy { it.population }
            } else {
                currentState.cities.sortedByDescending { it.population }
            }
            cachedCities = sortedCities
            _state.value = HomeState.Success(sortedCities)
        }
    }
}