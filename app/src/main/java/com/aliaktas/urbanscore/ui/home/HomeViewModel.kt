package com.aliaktas.urbanscore.ui.home

import androidx.lifecycle.viewModelScope
import com.aliaktas.urbanscore.base.BaseViewModel
import com.aliaktas.urbanscore.data.model.CityModel
import com.aliaktas.urbanscore.data.model.CuratedCityItem
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
 * Manages data loading, caching, and business logic for the Home screen.
 */
@HiltViewModel
class HomeViewModel @Inject constructor(
    private val cityRepository: CityRepository,
    private val networkUtil: NetworkUtil
) : BaseViewModel() {

    // State for top rated cities
    private val _topRatedCitiesState = MutableStateFlow<HomeState>(HomeState.Initial)
    val topRatedCitiesState: StateFlow<HomeState> = _topRatedCitiesState.asStateFlow()

    // State for editors' choice cities
    private val _editorsChoiceState = MutableStateFlow<List<CuratedCityItem>>(emptyList())
    val editorsChoiceState: StateFlow<List<CuratedCityItem>> = _editorsChoiceState.asStateFlow()

    // Cache for top rated cities
    private var cachedTopRatedCities: List<CityModel>? = null

    init {
        loadTopRatedCities(false)
        loadEditorsChoiceCities()
    }

    /**
     * Loads the top rated cities based on average rating.
     * Always uses averageRating for HomeFragment, regardless of any category selection.
     *
     * @param forceRefresh If true, ignores cache and forces a new network request
     */
    fun loadTopRatedCities(forceRefresh: Boolean = false) {
        // Always use "averageRating" for HomeFragment
        // This ensures top rated cities list is consistent
        val categoryToUse = "averageRating"

        // Use cache if available and not forcing refresh
        if (!forceRefresh && cachedTopRatedCities != null) {
            _topRatedCitiesState.value = HomeState.Success(cachedTopRatedCities!!)
            return
        }

        // Check network connectivity
        if (!networkUtil.isNetworkAvailable()) {
            if (cachedTopRatedCities != null) {
                _topRatedCitiesState.value = HomeState.Success(cachedTopRatedCities!!)
                // Coroutine scope içinde emitEvent çağrısı
                viewModelScope.launch {
                    emitEvent(UiEvent.Error("No internet connection. Showing cached data."))
                }
            } else {
                _topRatedCitiesState.value = HomeState.Error("No internet connection. Please try again later.")
            }
            return
        }

        // Preserve old data during loading if available
        val currentCities = if (_topRatedCitiesState.value is HomeState.Success) {
            (_topRatedCitiesState.value as HomeState.Success).cities
        } else {
            cachedTopRatedCities
        }

        _topRatedCitiesState.value = HomeState.Loading(oldData = currentCities)

        viewModelScope.launch {
            try {
                cityRepository.getCitiesByCategoryRating(categoryToUse, 20)
                    .catch { e ->
                        handleError(e)
                        _topRatedCitiesState.value = HomeState.Error(getErrorMessage(e))
                    }
                    .collectLatest { cities ->
                        cachedTopRatedCities = cities
                        _topRatedCitiesState.value = HomeState.Success(cities)
                    }
            } catch (e: Exception) {
                handleError(e)
                _topRatedCitiesState.value = HomeState.Error(getErrorMessage(e))
            }
        }
    }

    /**
     * Loads editor's choice cities from the repository
     */
    private fun loadEditorsChoiceCities() {
        if (!networkUtil.isNetworkAvailable()) {
            return // Silently fail, as this is not crucial content
        }

        viewModelScope.launch {
            try {
                cityRepository.getCuratedCities("editors_choice")
                    .catch { e ->
                        handleError(e)
                    }
                    .collectLatest { cities ->
                        _editorsChoiceState.value = cities
                    }
            } catch (e: Exception) {
                handleError(e)
            }
        }
    }

    /**
     * Called when returning to HomeFragment to ensure we always have top rated cities
     * based on average rating, not category-specific ratings.
     */
    fun refreshOnReturn() {
        // No force refresh needed - will use cache if available
        loadTopRatedCities(false)
    }
}