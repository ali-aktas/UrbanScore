package com.aliaktas.urbanscore.ui.ratecity

import android.content.ContentValues.TAG
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
import com.google.firebase.functions.FirebaseFunctions
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


    // app/src/main/java/com/aliaktas/urbanscore/ui/ratecity/RateCityViewModel.kt

    fun submitRating(cityId: String, ratings: CategoryRatings) {
        val currentUser = auth.currentUser
        if (currentUser == null) {
            _ratingState.value = RateCityState.Error("User not logged in. Please log in to rate cities.")
            return
        }

        _ratingState.value = RateCityState.Loading

        viewModelScope.launch {
            try {
                // Cloud Function için veri hazırla
                val data = mapOf(
                    "cityId" to cityId,
                    "ratings" to mapOf(
                        "gastronomy" to ratings.gastronomy,
                        "aesthetics" to ratings.aesthetics,
                        "safety" to ratings.safety,
                        "culture" to ratings.culture,
                        "livability" to ratings.livability,
                        "social" to ratings.social,
                        "hospitality" to ratings.hospitality
                    )
                )

                Log.d(TAG, "Submitting rating for city: $cityId")

                // Cloud Function çağrısı
                val functions = FirebaseFunctions.getInstance()
                val result = functions
                    .getHttpsCallable("updateRatingOnSubmit")
                    .call(data)
                    .await()

                // Sonucu işle
                val response = result.data as? Map<String, Any>
                val success = response?.get("success") as? Boolean ?: false

                if (success) {
                    Log.d(TAG, "Rating submitted successfully: ${response?.get("message")}")
                    _ratingState.value = RateCityState.Success
                    RatingEventBus.emitRatingSubmitted(cityId)
                } else {
                    val errorMessage = response?.get("message") as? String ?: "Failed to submit rating"
                    Log.e(TAG, "Rating submission failed: $errorMessage")
                    _ratingState.value = RateCityState.Error(errorMessage)
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error submitting rating", e)
                _ratingState.value = RateCityState.Error(e.message ?: "An unexpected error occurred")
            }
        }
    }

}