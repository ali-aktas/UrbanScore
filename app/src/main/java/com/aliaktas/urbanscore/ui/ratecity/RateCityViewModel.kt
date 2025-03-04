package com.aliaktas.urbanscore.ui.ratecity

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aliaktas.urbanscore.data.model.CategoryRatings
import com.aliaktas.urbanscore.data.repository.CityRepository
import com.aliaktas.urbanscore.data.repository.UserRepository
import com.google.firebase.auth.FirebaseAuth
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class RateCityViewModel @Inject constructor(
    private val cityRepository: CityRepository,
    private val userRepository: UserRepository,
    private val auth: FirebaseAuth
) : ViewModel() {

    private val _ratingState = MutableStateFlow<RateCityState>(RateCityState.Initial)
    val ratingState: StateFlow<RateCityState> = _ratingState.asStateFlow()

    fun submitRating(cityId: String, ratings: CategoryRatings) {
        val currentUser = auth.currentUser
        if (currentUser == null) {
            _ratingState.value = RateCityState.Error("User not logged in. Please log in to rate cities.")
            return
        }

        _ratingState.value = RateCityState.Loading

        viewModelScope.launch {
            try {
                val result = cityRepository.rateCity(cityId, currentUser.uid, ratings)

                result.fold(
                    onSuccess = {
                        addCityToVisited(cityId)
                        _ratingState.value = RateCityState.Success
                    },
                    onFailure = { exception ->
                        _ratingState.value = RateCityState.Error(exception.message ?: "Failed to submit rating")
                    }
                )
            } catch (e: Exception) {
                _ratingState.value = RateCityState.Error(e.message ?: "An unexpected error occurred")
            }
        }
    }

    private fun addCityToVisited(cityId: String) {
        val currentUser = auth.currentUser ?: return

        viewModelScope.launch {
            try {
                userRepository.addToVisitedCities(cityId)
            } catch (e: Exception) {
                // Burada listeleme hatasını sessizce geçiyoruz çünkü puanlama başarılı oldu
                // Sadece loglama yapabiliriz
                Log.e("RateCityViewModel", "Failed to add city to visited list: ${e.message}")
            }
        }
    }

}


sealed class RateCityState {
    data object Initial : RateCityState()
    data object Loading : RateCityState()
    data object Success : RateCityState()
    data class Error(val message: String) : RateCityState()
}