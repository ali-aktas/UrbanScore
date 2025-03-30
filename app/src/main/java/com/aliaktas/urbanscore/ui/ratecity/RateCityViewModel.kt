package com.aliaktas.urbanscore.ui.ratecity

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aliaktas.urbanscore.data.model.CategoryRatings
import com.aliaktas.urbanscore.data.model.UserRatingModel
import com.aliaktas.urbanscore.data.repository.CityRepository
import com.aliaktas.urbanscore.data.repository.UserRepository
import com.aliaktas.urbanscore.util.RatingEventBus
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class RateCityViewModel @Inject constructor(
    private val cityRepository: CityRepository,
    private val userRepository: UserRepository,
    private val auth: FirebaseAuth,
    private val firestore: FirebaseFirestore
) : ViewModel() {

    private val _ratingState = MutableStateFlow<RateCityState>(RateCityState.Initial)
    val ratingState: StateFlow<RateCityState> = _ratingState.asStateFlow()

    // RateCityViewModel.kt içindeki submitRating fonksiyonunun tam hali:
    fun submitRating(cityId: String, ratings: CategoryRatings) {
        val currentUser = auth.currentUser
        if (currentUser == null) {
            _ratingState.value = RateCityState.Error("User not logged in. Please log in to rate cities.")
            return
        }

        _ratingState.value = RateCityState.Loading

        // TEST için log
        Log.d("RateCityViewModel", "Starting rating for cityId: $cityId, userId: ${currentUser.uid}")

        viewModelScope.launch {
            try {
                // 1. Ortalama puanı hesapla
                val averageRating = calculateAverageRating(ratings)
                Log.d("RateCityViewModel", "Calculated average rating: $averageRating")

                // 2. Şehre puanlamayı gönder
                val result = cityRepository.rateCity(cityId, currentUser.uid, ratings)

                result.fold(
                    onSuccess = {
                        Log.d("RateCityViewModel", "CityRepository.rateCity successful")
                        viewModelScope.launch {
                            try {
                                // 3. Kullanıcının ziyaret ettiği şehirler listesine ekle
                                val visitedResult = userRepository.addToVisitedCities(cityId, averageRating)

                                visitedResult.fold(
                                    onSuccess = {
                                        Log.d("RateCityViewModel", "SUCCESSFULLY added to visited_cities")
                                    },
                                    onFailure = { e ->
                                        Log.e("RateCityViewModel", "FAILED to add to visited_cities: ${e.message}", e)
                                    }
                                )

                                // 4. Başarılı state'i ayarla
                                _ratingState.value = RateCityState.Success

                                // 5. Event'i yayınla (YENİ EKLENDİ)
                                RatingEventBus.emitRatingSubmitted(cityId)

                            } catch (e: Exception) {
                                Log.e("RateCityViewModel", "Error in adding to visited cities: ${e.message}", e)
                                _ratingState.value = RateCityState.Error("Error updating profile: ${e.message}")
                            }
                        }
                    },
                    onFailure = { exception ->
                        Log.e("RateCityViewModel", "CityRepository.rateCity failed: ${exception.message}", exception)
                        _ratingState.value = RateCityState.Error(exception.message ?: "Failed to submit rating")
                    }
                )
            } catch (e: Exception) {
                Log.e("RateCityViewModel", "Unexpected error in submitRating: ${e.message}", e)
                _ratingState.value = RateCityState.Error(e.message ?: "An unexpected error occurred")
            }
        }
    }

    private fun calculateAverageRating(ratings: CategoryRatings): Double {
        // Weighted average calculation
        val sum = (ratings.gastronomy * 1.0 +
                ratings.aesthetics * 1.1 +
                ratings.safety * 1.2 +
                ratings.culture * 1.0 +
                ratings.livability * 1.0 +
                ratings.social * 0.9 +
                ratings.hospitality * 0.8)
        return (sum / 7.0).let { Math.round(it * 100) / 100.0 } // Round to 2 decimal places
    }
}