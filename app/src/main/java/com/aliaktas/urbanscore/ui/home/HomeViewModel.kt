package com.aliaktas.urbanscore.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aliaktas.urbanscore.data.model.CityModel
import com.aliaktas.urbanscore.data.repository.CityRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * ViewModel for the HomeFragment.
 * Manages city data loading and filtering by categories.
 */
@HiltViewModel
class HomeViewModel @Inject constructor(
    private val cityRepository: CityRepository
) : ViewModel() {

    // State flow for UI state
    private val _state = MutableStateFlow<HomeState>(HomeState.Initial)
    val state: StateFlow<HomeState> = _state.asStateFlow()

    // Cache mechanism to avoid unnecessary reloads
    private var cachedCities: List<CityModel>? = null

    // Loading state tracking
    private var isDataLoading = false

    // Current selected category
    private var currentCategory: String = "averageRating"

    init {
        // Initial data load with a small delay to ensure smooth UI initialization
        viewModelScope.launch {
            kotlinx.coroutines.delay(100)
            refreshCities(false) // Use cache if available
        }
    }

    /**
     * Refreshes city data based on current category.
     *
     * @param forceRefresh If true, ignores cache and forces new data load.
     * @param category Optional category to filter cities. If provided, updates current category.
     */
    fun refreshCities(forceRefresh: Boolean = true, category: String? = null) {
        // Update category if provided
        if (category != null) {
            currentCategory = category
        }

        // Prevent multiple simultaneous loads
        if (isDataLoading) {
            return
        }

        // Use cache if available and not forcing refresh
        if (!forceRefresh && cachedCities != null) {
            _state.value = HomeState.Success(cachedCities!!)
            return
        }

        // Get current cities to keep during loading
        val currentCities = if (_state.value is HomeState.Success) {
            (_state.value as HomeState.Success).cities
        } else {
            cachedCities
        }

        // Update state to loading, preserving old data
        _state.value = HomeState.Loading(oldData = currentCities)

        // Mark loading in progress
        isDataLoading = true

        viewModelScope.launch {
            try {
                // Use category-specific query for consistent behavior
                val citiesFlow = cityRepository.getCitiesByCategoryRating(currentCategory, 20)

                citiesFlow
                    .catch { e ->
                        isDataLoading = false
                        _state.value = HomeState.Error(e.message ?: "An error occurred")
                    }
                    .collect { cities ->
                        // Update cache and state
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

    /**
     * Switches to a different category and loads data for that category.
     *
     * @param category The category to switch to.
     */
    fun switchCategory(category: String) {
        if (category != currentCategory) {
            refreshCities(true, category)
        }
    }

    /**
     * Sort cities by their rating in ascending or descending order.
     *
     * @param ascending If true, sorts in ascending order, otherwise descending.
     */
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

    /**
     * Sort cities by their population in ascending or descending order.
     *
     * @param ascending If true, sorts in ascending order, otherwise descending.
     */
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
