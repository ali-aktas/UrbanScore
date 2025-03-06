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

    init {
        viewModelScope.launch {
            delay(100) // Önceki kodunuzdaki gecikmeyi koruyorum
            refreshCities(false) // false = önbellek varsa kullan
        }
    }

    fun refreshCities(forceRefresh: Boolean = true) {
        // Eğer zaten yükleme yapılıyorsa tekrar yükleme yapma
        if (isDataLoading) {
            return
        }

        // Önbellekte veri var ve zorla yenileme istenmiyorsa önbellekten al
        if (!forceRefresh && cachedCities != null) {
            _state.value = HomeState.Success(cachedCities!!)
            return
        }

        // Mevcut veriler varsa onları Loading state'inde tut
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
                cityRepository.getAllCities()
                    .catch { e ->
                        isDataLoading = false // Hata durumunda bayrağı false yap
                        _state.value = HomeState.Error(e.message ?: "An error occurred")
                    }
                    .collect { cities ->
                        val sortedCities = cities.sortedByDescending { it.averageRating }
                        // Önbelleğe kaydet
                        cachedCities = sortedCities
                        _state.value = HomeState.Success(sortedCities)
                        isDataLoading = false // Yükleme tamamlandığında bayrağı false yap
                    }
            } catch (e: Exception) {
                isDataLoading = false // Hata durumunda bayrağı false yap
                _state.value = HomeState.Error(e.message ?: "An error occurred")
            }
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