package com.aliaktas.urbanscore.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
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

    private val _state = MutableStateFlow<HomeState>(HomeState.Loading)
    val state = _state.asStateFlow()

    init {
        getCities()
    }

    private fun getCities() {
        viewModelScope.launch {
            _state.value = HomeState.Loading
            try {
                cityRepository.getAllCities()
                    .catch { e ->
                        _state.value = HomeState.Error(e.message ?: "An error occurred")
                    }
                    .collect { cities ->
                        val sortedCities = cities.sortedByDescending { it.averageRating }
                        _state.value = HomeState.Success(sortedCities)
                    }
            } catch (e: Exception) {
                _state.value = HomeState.Error(e.message ?: "An error occurred")
            }
        }
    }

    fun refreshCities() {
        _state.value = HomeState.Loading
        getCities()
    }

    // Ordering the cities
    fun sortByRating(ascending: Boolean = false) {
        val currentState = _state.value
        if (currentState is HomeState.Success) {
            val sortedCities = if (ascending) {
                currentState.cities.sortedBy { it.averageRating }
            } else {
                currentState.cities.sortedByDescending { it.averageRating }
            }
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
            _state.value = HomeState.Success(sortedCities)
        }
    }
}