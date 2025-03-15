package com.aliaktas.urbanscore.ui.home

import androidx.lifecycle.viewModelScope
import com.aliaktas.urbanscore.base.BaseViewModel
import com.aliaktas.urbanscore.data.model.CityModel
import com.aliaktas.urbanscore.data.repository.CityRepository
import com.aliaktas.urbanscore.util.NetworkUtil
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * ViewModel for the HomeFragment.
 * Manages city data loading and filtering by categories.
 */
@HiltViewModel
class HomeViewModel @Inject constructor(
    val cityRepository: CityRepository,
    private val networkUtil: NetworkUtil
) : BaseViewModel() {

    // State for cities data
    private val _homeState = MutableStateFlow<HomeState>(HomeState.Initial)
    val homeState: StateFlow<HomeState> = _homeState.asStateFlow()

    // Cache mechanism to avoid unnecessary reloads
    private var cachedCities: List<CityModel>? = null

    // Current selected category
    private var currentCategory: String = "averageRating"

    init {
        // Initial data load with a small delay to ensure smooth UI initialization
        launch {
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

        // Use cache if available and not forcing refresh
        if (!forceRefresh && cachedCities != null) {
            _homeState.value = HomeState.Success(cachedCities!!)
            return
        }

        // Check network connectivity first
        if (!networkUtil.isNetworkAvailable()) {
            if (cachedCities != null) {
                // We have cached data, so show it with a network warning
                _homeState.value = HomeState.Success(cachedCities!!)
                launch {
                    emitEvent(UiEvent.Error("No internet connection. Showing cached data."))
                }
            } else {
                // No cached data available
                _homeState.value = HomeState.Error("No internet connection. Please try again when you're online.")
            }
            return
        }

        // Get current cities to keep during loading
        val currentCities = if (_homeState.value is HomeState.Success) {
            (_homeState.value as HomeState.Success).cities
        } else {
            cachedCities
        }

        // Update state to loading, preserving old data
        _homeState.value = HomeState.Loading(oldData = currentCities)

        // Use launchWithLoading to handle loading state automatically
        launchWithLoading {
            try {
                // Use category-specific query for consistent behavior
                cityRepository.getCitiesByCategoryRating(currentCategory, 20)
                    .catch { e ->
                        handleError(e)
                        _homeState.value = HomeState.Error(getErrorMessage(e))
                    }
                    .collectLatest { cities ->
                        // Update cache and state
                        cachedCities = cities
                        _homeState.value = HomeState.Success(cities)
                    }
            } catch (e: Exception) {
                handleError(e)
                _homeState.value = HomeState.Error(getErrorMessage(e))
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
        val currentState = _homeState.value
        if (currentState is HomeState.Success) {
            val sortedCities = if (ascending) {
                currentState.cities.sortedBy { it.averageRating }
            } else {
                currentState.cities.sortedByDescending { it.averageRating }
            }
            cachedCities = sortedCities
            _homeState.value = HomeState.Success(sortedCities)
        }
    }

    /**
     * Sort cities by their population in ascending or descending order.
     *
     * @param ascending If true, sorts in ascending order, otherwise descending.
     */
    fun sortByPopulation(ascending: Boolean = false) {
        val currentState = _homeState.value
        if (currentState is HomeState.Success) {
            val sortedCities = if (ascending) {
                currentState.cities.sortedBy { it.population }
            } else {
                currentState.cities.sortedByDescending { it.population }
            }
            cachedCities = sortedCities
            _homeState.value = HomeState.Success(sortedCities)
        }
    }
}