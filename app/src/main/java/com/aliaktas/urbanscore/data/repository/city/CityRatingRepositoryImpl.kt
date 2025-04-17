package com.aliaktas.urbanscore.data.repository.city

import android.util.Log
import com.aliaktas.urbanscore.data.model.CategoryRatings
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.functions.FirebaseFunctions
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CityRatingRepositoryImpl @Inject constructor(
    private val firestore: FirebaseFirestore,
    private val functions: FirebaseFunctions
) : CityRatingRepository {

    companion object {
        private const val TAG = "CityRatingRepository"
    }

    override suspend fun rateCity(cityId: String, userId: String, ratings: CategoryRatings): Result<Unit> = try {
        // Cloud Function için veri hazırla
        val data = hashMapOf(
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

        // Cloud Function'ı çağır - Extensions.kt'deki await() metodunu kullanıyoruz
        val result = functions
            .getHttpsCallable("updateRatingOnSubmit")
            .call(data)
            .await() // import com.aliaktas.urbanscore.util.await

        val response = result.data as? Map<String, Any>
        val success = response?.get("success") as? Boolean ?: false

        if (success) {
            // Başarı mesajını logla
            val message = response?.get("message") as? String ?: "Rating submitted successfully"
            Log.d(TAG, message)
            Result.success(Unit)
        } else {
            val errorMessage = response?.get("message") as? String ?: "Unknown error"
            Log.e(TAG, "Cloud Function error: $errorMessage")
            Result.failure(Exception(errorMessage))
        }
    } catch (e: Exception) {
        Log.e(TAG, "rateCity failed", e)
        Result.failure(e)
    }
}