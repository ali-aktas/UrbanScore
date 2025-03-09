package com.aliaktas.urbanscore.ui.detail

import android.util.Log
import androidx.lifecycle.SavedStateHandle
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
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class CityDetailViewModel @Inject constructor(
    private val cityRepository: CityRepository,
    private val userRepository: UserRepository,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val cityId: String = checkNotNull(savedStateHandle["cityId"])

    private val _state = MutableStateFlow<CityDetailState>(CityDetailState.Loading)
    val state: StateFlow<CityDetailState> = _state.asStateFlow()

    // Şehrin puanlanma durumunu takip etmek için
    private val _hasRated = MutableStateFlow<Boolean>(false)
    val hasRated: StateFlow<Boolean> = _hasRated.asStateFlow()

    init {
        loadCityDetails()
        checkIfRated()
    }

    private fun checkIfRated() {
        viewModelScope.launch {
            userRepository.hasUserRatedCity(cityId)
                .catch { e ->
                    Log.e("CityDetailViewModel", "Error checking if rated: ${e.message}")
                }
                .collect { rated ->
                    _hasRated.value = rated
                }
        }
    }

    private fun loadCityDetails() {
        viewModelScope.launch {
            _state.value = CityDetailState.Loading

            try {
                cityRepository.getCityById(cityId)
                    .catch { e ->
                        _state.value = CityDetailState.Error(e.message ?: "An error occurred")
                    }
                    .collect { city ->
                        if (city != null) {
                            _state.value = CityDetailState.Success(city)
                        } else {
                            _state.value = CityDetailState.Error("City not found")
                        }
                    }
            } catch (e: Exception) {
                _state.value = CityDetailState.Error(e.message ?: "An error occurred")
            }
        }
    }

    fun addToWishlist() {
        viewModelScope.launch {
            try {
                userRepository.addToWishlist(cityId)
            } catch (e: Exception) {
                Log.e("CityDetailViewModel", "Error adding to wishlist: ${e.message}")
            }
        }
    }

    fun refreshCityDetails() {
        loadCityDetails()
        checkIfRated() // Ayrıca puanlama durumunu da yenile
    }
}