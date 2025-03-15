package com.aliaktas.urbanscore.ui.detail

import androidx.lifecycle.SavedStateHandle
import com.aliaktas.urbanscore.base.BaseViewModel
import com.aliaktas.urbanscore.data.model.CityModel
import com.aliaktas.urbanscore.data.repository.CityRepository
import com.aliaktas.urbanscore.data.repository.UserRepository
import com.aliaktas.urbanscore.util.NetworkUtil
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.collectLatest
import javax.inject.Inject

@HiltViewModel
class CityDetailViewModel @Inject constructor(
    private val cityRepository: CityRepository,
    private val userRepository: UserRepository,
    private val networkUtil: NetworkUtil,
    savedStateHandle: SavedStateHandle
) : BaseViewModel() {

    private val cityId: String = checkNotNull(savedStateHandle["cityId"])

    private val _detailState = MutableStateFlow<CityDetailState>(CityDetailState.Loading)
    val detailState: StateFlow<CityDetailState> = _detailState.asStateFlow()

    private val _isInWishlist = MutableStateFlow<Boolean>(false)
    val isInWishlist: StateFlow<Boolean> = _isInWishlist.asStateFlow()

    // Şehrin puanlanma durumunu takip etmek için
    private val _hasRated = MutableStateFlow<Boolean>(false)
    val hasRated: StateFlow<Boolean> = _hasRated.asStateFlow()

    init {
        loadCityDetails()
        checkIfRated()
        checkIfInWishlist()
    }

    private fun checkIfInWishlist() {
        launch {
            userRepository.getUserWishlist()
                .catch { e ->
                    handleError(e)
                }
                .collectLatest { wishlist ->
                    _isInWishlist.value = wishlist.contains(cityId)
                }
        }
    }

    fun removeFromWishlist() {
        launchWithLoading {
            try {
                val result = userRepository.removeFromWishlist(cityId)
                result.fold(
                    onSuccess = {
                        emitEvent(UiEvent.Success("Removed from bucket list"))
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

    private fun checkIfRated() {
        launch {
            userRepository.hasUserRatedCity(cityId)
                .catch { e ->
                    handleError(e)
                }
                .collectLatest { rated ->
                    _hasRated.value = rated
                }
        }
    }

    private fun loadCityDetails() {
        // Check network connectivity
        if (!networkUtil.isNetworkAvailable()) {
            _detailState.value = CityDetailState.Error("No internet connection. Please check your connection and try again.")
            launch { emitEvent(UiEvent.Error("No internet connection")) }
            return
        }

        _detailState.value = CityDetailState.Loading

        launchWithLoading {
            try {
                cityRepository.getCityById(cityId)
                    .catch { e ->
                        handleError(e)
                        _detailState.value = CityDetailState.Error(getErrorMessage(e))
                    }
                    .collectLatest { city ->
                        if (city != null) {
                            _detailState.value = CityDetailState.Success(city)
                        } else {
                            _detailState.value = CityDetailState.Error("City not found")
                            emitEvent(UiEvent.Error("City not found"))
                        }
                    }
            } catch (e: Exception) {
                handleError(e)
                _detailState.value = CityDetailState.Error(getErrorMessage(e))
            }
        }
    }

    fun addToWishlist() {
        launchWithLoading {
            try {
                val result = userRepository.addToWishlist(cityId)
                result.fold(
                    onSuccess = {
                        emitEvent(UiEvent.Success("Added to bucket list"))
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

    fun refreshCityDetails() {
        loadCityDetails()
        checkIfRated()
    }
}