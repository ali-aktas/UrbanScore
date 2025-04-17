package com.aliaktas.urbanscore.ui.home

import android.util.Log
import androidx.lifecycle.viewModelScope
import com.aliaktas.urbanscore.base.BaseViewModel
import com.aliaktas.urbanscore.data.model.CityModel
import com.aliaktas.urbanscore.data.model.CuratedCityItem
import com.aliaktas.urbanscore.data.repository.CityRepository
import com.aliaktas.urbanscore.data.repository.stats.CityStatsRepository
import com.aliaktas.urbanscore.util.NetworkUtil
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

/**
 * ViewModel for the HomeFragment.
 * Manages data loading, caching, and business logic for the Home screen.
 */
@HiltViewModel
class HomeViewModel @Inject constructor(
    private val cityRepository: CityRepository,
    private val cityStatsRepository: CityStatsRepository,
    private val networkUtil: NetworkUtil
) : BaseViewModel() {

    // State for top rated cities
    private val _topRatedCitiesState = MutableStateFlow<HomeState>(HomeState.Initial)
    val topRatedCitiesState: StateFlow<HomeState> = _topRatedCitiesState.asStateFlow()

    // State for popular cities
    private val _popularCitiesState = MutableStateFlow<List<CuratedCityItem>>(emptyList())
    val popularCitiesState: StateFlow<List<CuratedCityItem>> = _popularCitiesState.asStateFlow()

    // Cache for top rated cities
    private var cachedTopRatedCities: List<CityModel>? = null

    private var popularCitiesLoaded = false

    private var networkObserver: Job? = null
    private var wasInErrorState = false

    fun loadTopCitiesByStats() {
        viewModelScope.launch {
            val result = cityStatsRepository.getTopCitiesByCategory()
            result.fold(
                onSuccess = { cities ->
                    // Listeyi işle
                    _topRatedCitiesState.value = HomeState.Success(cities)
                },
                onFailure = { error ->
                    // Hata yönetimi
                    _topRatedCitiesState.value = HomeState.Error(error.message ?: "Unknown error")
                }
            )
        }
    }

    init {
        // İnternet bağlantısı değişikliklerini gözlemle
        networkObserver = viewModelScope.launch {
            networkUtil.observeNetworkState()
                .collect { isConnected ->
                    Log.d("HomeViewModel", "Network state changed: connected=$isConnected, currentState=${_topRatedCitiesState.value.javaClass.simpleName}")

                    // İnternet bağlantısı geri geldiyse ve şu anda hata durumundaysak ya da daha önce hata durumundaysak
                    if (isConnected && (wasInErrorState || _topRatedCitiesState.value is HomeState.Error)) {
                        Log.d("HomeViewModel", "Internet connection restored, reloading data")
                        wasInErrorState = false
                        loadTopRatedCities(true) // Force refresh
                    }
                }
        }

        loadTopRatedCities(false)

        viewModelScope.launch {
            _topRatedCitiesState.collect { state ->
                if (state is HomeState.Success) {
                    // Top Rated Cities başarıyla yüklendiğinde Popular Cities'i yükle
                    loadPopularCities()
                }
            }
        }
    }

    // app/src/main/java/com/aliaktas/urbanscore/ui/home/HomeViewModel.kt

    fun loadTopRatedCities(forceRefresh: Boolean = false) {
        // Always use "averageRating" for HomeFragment
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
                viewModelScope.launch {
                    emitEvent(UiEvent.Error("No internet connection. Showing cached data."))
                }
            } else {
                wasInErrorState = true
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
                // CityStatsRepository'yi kullan (Cloud Function'ı çağırır)
                val result = withContext(Dispatchers.IO) {
                    cityStatsRepository.getTopCitiesByCategory(categoryToUse, 50, 20)
                }

                result.fold(
                    onSuccess = { cities ->
                        Log.d("HomeViewModel", "Loaded ${cities.size} cities from Cloud Function")
                        cachedTopRatedCities = cities
                        _topRatedCitiesState.value = HomeState.Success(cities)
                    },
                    onFailure = { error ->
                        Log.w("HomeViewModel", "Cloud Function failed, trying backup method", error)

                        // Yedek metot - doğrudan repository'den al
                        try {
                            val backupCities = withContext(Dispatchers.IO) {
                                cityRepository.getCitiesByCategoryRatingOneTime(categoryToUse, 20)
                            }

                            if (backupCities.isNotEmpty()) {
                                cachedTopRatedCities = backupCities
                                _topRatedCitiesState.value = HomeState.Success(backupCities)
                            } else {
                                _topRatedCitiesState.value = HomeState.Error("Failed to load cities")
                            }
                        } catch (e: Exception) {
                            Log.e("HomeViewModel", "Backup method also failed", e)
                            _topRatedCitiesState.value = HomeState.Error("Failed to load cities. Please try again.")
                        }
                    }
                )
            } catch (e: Exception) {
                Log.e("HomeViewModel", "Error loading top cities", e)
                handleError(e)
                wasInErrorState = true
                _topRatedCitiesState.value = HomeState.Error(getErrorMessage(e))
            }
        }
    }

    /**
     * Loads popular cities from the repository
     */
    private fun loadPopularCities() {
        // Zaten yüklendiyse tekrar yükleme
        if (popularCitiesLoaded) return

        if (!networkUtil.isNetworkAvailable()) {
            return // Silently fail, as this is not crucial content
        }

        popularCitiesLoaded = true
        viewModelScope.launch {
            try {
                // Değişiklik: Flow yerine doğrudan liste çağırıyoruz
                val cities = withContext(Dispatchers.IO) {
                    cityRepository.getCuratedCitiesOneTime("popular_cities")
                }
                _popularCitiesState.value = cities
            } catch (e: Exception) {
                Log.e("HomeViewModel", "Error loading popular cities", e)
                handleError(e)
                // Hata durumunda flag'i sıfırla ki tekrar denenebilsin
                popularCitiesLoaded = false
            }
        }
    }


    override fun onCleared() {
        super.onCleared()
        networkObserver?.cancel()
    }

    /**
     * Called when returning to HomeFragment to ensure we always have top rated cities
     * based on average rating, not category-specific ratings.
     */
    fun refreshOnReturn() {
        // No force refresh needed - will use cache if available
        loadTopRatedCities(false)

        // Popular Cities'i de yenileme mantığı eklendi
        popularCitiesLoaded = false
        loadPopularCities()
    }
}