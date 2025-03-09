package com.aliaktas.urbanscore.ui.ratecity

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aliaktas.urbanscore.data.model.CategoryRatings
import com.aliaktas.urbanscore.data.model.UserRatingModel
import com.aliaktas.urbanscore.data.repository.CityRepository
import com.aliaktas.urbanscore.data.repository.UserRepository
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
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

    fun submitRating(cityId: String, ratings: CategoryRatings) {
        val currentUser = auth.currentUser
        if (currentUser == null) {
            _ratingState.value = RateCityState.Error("User not logged in. Please log in to rate cities.")
            return
        }

        _ratingState.value = RateCityState.Loading

        viewModelScope.launch {
            try {
                // Calculate the average rating from all categories
                val averageRating = calculateAverageRating(ratings)

                // 1. Submit rating to city (this will update the city's average rating)
                val result = cityRepository.rateCity(cityId, currentUser.uid, ratings)

                result.fold(
                    onSuccess = {
                        viewModelScope.launch { // 🔥 İşlemleri bu scope içinde tamamla
                            saveUserRating(cityId, ratings, averageRating)
                            addCityToVisitedWithRating(cityId, averageRating)
                            _ratingState.value = RateCityState.Success // ✅ Sonra state'i güncelle
                        }
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

    private fun calculateAverageRating(ratings: CategoryRatings): Double {
        return ((ratings.environment + ratings.safety + ratings.livability +
                ratings.cost + ratings.social) / 5.0)
    }

    private suspend fun saveUserRating(cityId: String, ratings: CategoryRatings, averageRating: Double) {
        val currentUser = auth.currentUser ?: return

        try {
            // Get city name for better display in user's profile
            var cityName = ""
            cityRepository.getCityById(cityId).collect { city ->
                cityName = city?.cityName ?: cityId
            }

            // Create rating model for the user's ratings collection
            val userRating = UserRatingModel(
                userId = currentUser.uid,
                cityId = cityId,
                cityName = cityName,
                timestamp = System.currentTimeMillis(),
                userAverageRating = averageRating,
                ratings = ratings
            )

            // Save to the nested collection
            firestore.collection("users")
                .document(currentUser.uid)
                .collection("ratings")
                .document(cityId)
                .set(userRating)
                .await()

            Log.d("RateCityViewModel", "User rating saved successfully to nested collection")
        } catch (e: Exception) {
            Log.e("RateCityViewModel", "Error saving user rating: ${e.message}")
            // Don't fail the entire operation if this specific part fails
        }
    }

    private suspend fun addCityToVisitedWithRating(cityId: String, rating: Double) {
        try {
            val result = userRepository.addToVisitedCities(cityId, rating)

            result.fold(
                onSuccess = {
                    Log.d("RateCityViewModel", "City added to visited list with rating: $rating")
                },
                onFailure = { e ->
                    Log.e("RateCityViewModel", "Failed to add city to visited list: ${e.message}")
                }
            )
        } catch (e: Exception) {
            Log.e("RateCityViewModel", "Error adding city to visited list: ${e.message}")
        }
    }
}

