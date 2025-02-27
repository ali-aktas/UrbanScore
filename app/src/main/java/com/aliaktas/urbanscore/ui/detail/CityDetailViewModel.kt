package com.aliaktas.urbanscore.ui.detail

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aliaktas.urbanscore.data.repository.CityRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class CityDetailViewModel @Inject constructor(
    private val cityRepository: CityRepository,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val cityId: String = checkNotNull(savedStateHandle["cityId"])

    private val _state = MutableStateFlow<CityDetailState>(CityDetailState.Loading)
    val state = _state.asStateFlow()

    init {
        loadCityDetails()
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
        // Implementation will depend on your user repository
        // For example:
        // viewModelScope.launch {
        //     userRepository.addToWishlist(userId, cityId)
        // }
    }

    fun refreshCityDetails() {
        loadCityDetails()
    }
}