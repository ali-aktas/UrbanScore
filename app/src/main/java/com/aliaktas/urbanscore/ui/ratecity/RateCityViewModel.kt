package com.aliaktas.urbanscore.ui.ratecity

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aliaktas.urbanscore.data.model.CategoryRatings
import com.aliaktas.urbanscore.data.model.UserRatingModel
import com.aliaktas.urbanscore.data.repository.CityRepository
import com.aliaktas.urbanscore.data.repository.UserRepository
import com.aliaktas.urbanscore.util.RatingEventBus
import com.aliaktas.urbanscore.util.await
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

    // RateCityViewModel.kt dosyasına eklenecek
    /**
     * Kullanıcının şehirle aynı ülkeden olup olmadığını kontrol eder
     * @param cityId Şehir ID'si
     * @param callback Sonuç (aynı ülkeden ise true)
     */
    fun checkUserCountry(cityId: String, callback: (Boolean) -> Unit) {
        viewModelScope.launch {
            try {
                // Kullanıcı bilgisini al
                val currentUser = auth.currentUser ?: return@launch callback(false)

                // Şehir belgesini al
                val cityDoc = firestore.collection("cities").document(cityId).get().await()
                if (!cityDoc.exists()) return@launch callback(false)

                // Kullanıcı belgesini al
                val userDoc = firestore.collection("users").document(currentUser.uid).get().await()
                if (!userDoc.exists()) return@launch callback(false)

                // Ülke bilgilerini karşılaştır
                val userCountry = userDoc.getString("country")?.lowercase() ?: ""
                val cityCountry = cityDoc.getString("country")?.lowercase() ?: ""

                val isSameCountry = userCountry.isNotEmpty() &&
                        cityCountry.isNotEmpty() &&
                        userCountry == cityCountry

                callback(isSameCountry)
            } catch (e: Exception) {
                Log.e("RateCityViewModel", "Error checking user country: ${e.message}", e)
                callback(false)
            }
        }
    }


    fun submitRating(cityId: String, ratings: CategoryRatings) {
        val currentUser = auth.currentUser
        if (currentUser == null) {
            _ratingState.value = RateCityState.Error("User not logged in. Please log in to rate cities.")
            return
        }

        _ratingState.value = RateCityState.Loading

        viewModelScope.launch {
            try {
                // Cloud Function tüm işi yapacak, sadece rateCity çağırıyoruz
                val result = cityRepository.rateCity(cityId, currentUser.uid, ratings)

                result.fold(
                    onSuccess = {
                        _ratingState.value = RateCityState.Success
                        // Event'i yayınla
                        RatingEventBus.emitRatingSubmitted(cityId)
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