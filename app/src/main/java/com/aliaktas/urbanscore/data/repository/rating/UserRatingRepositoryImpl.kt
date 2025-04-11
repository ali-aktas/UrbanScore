package com.aliaktas.urbanscore.data.repository.rating

import android.util.Log
import com.aliaktas.urbanscore.data.model.UserRatingModel
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class UserRatingRepositoryImpl @Inject constructor(
    private val auth: FirebaseAuth,
    private val firestore: FirebaseFirestore
) : UserRatingRepository {

    companion object {
        private const val USERS_COLLECTION = "users"
        private const val TAG = "UserRatingRepository"
    }

    override suspend fun saveDetailedRating(cityId: String, cityName: String, rating: UserRatingModel): Result<Unit> = try {
        val currentUser = auth.currentUser ?: throw Exception("No user logged in")

        Log.d(TAG, "Saving detailed rating for $cityId ($cityName)")

        // Alt koleksiyona kaydet
        firestore.collection(USERS_COLLECTION)
            .document(currentUser.uid)
            .collection("ratings")
            .document(cityId)
            .set(mapOf(
                "cityId" to cityId,
                "cityName" to cityName,
                "timestamp" to rating.timestamp,
                "userAverageRating" to rating.userAverageRating,
                "ratings" to mapOf(
                    "aesthetics" to rating.ratings.aesthetics,
                    "safety" to rating.ratings.safety,
                    "livability" to rating.ratings.livability,
                    "culture" to rating.ratings.culture,
                    "gastronomy" to rating.ratings.gastronomy,
                    "social" to rating.ratings.social,
                    "hospitality" to rating.ratings.hospitality
                )
            )).await()

        Result.success(Unit)
    } catch (e: Exception) {
        Log.e(TAG, "saveDetailedRating failed", e)
        Result.failure(e)
    }

    override suspend fun getDetailedRating(cityId: String): Flow<UserRatingModel?> = callbackFlow {
        val currentUser = auth.currentUser

        if (currentUser == null) {
            trySend(null)
            close()
            return@callbackFlow
        }

        val listener = firestore.collection(USERS_COLLECTION)
            .document(currentUser.uid)
            .collection("ratings")
            .document(cityId)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.e(TAG, "Error getting detailed rating", error)
                    trySend(null)
                    return@addSnapshotListener
                }

                if (snapshot == null || !snapshot.exists()) {
                    trySend(null)
                    return@addSnapshotListener
                }

                try {
                    val data = snapshot.data ?: return@addSnapshotListener

                    @Suppress("UNCHECKED_CAST")
                    val ratingsMap = data["ratings"] as? Map<String, Double> ?: mapOf()

                    val rating = UserRatingModel(
                        cityId = data["cityId"] as? String ?: "",
                        cityName = data["cityName"] as? String ?: "",
                        timestamp = (data["timestamp"] as? Long) ?: 0L,
                        userAverageRating = (data["userAverageRating"] as? Double) ?: 0.0,
                        ratings = com.aliaktas.urbanscore.data.model.CategoryRatings(
                            aesthetics = ratingsMap["aesthetics"] ?: 0.0,
                            safety = ratingsMap["safety"] ?: 0.0,
                            livability = ratingsMap["livability"] ?: 0.0,
                            culture = ratingsMap["culture"] ?: 0.0,
                            gastronomy = ratingsMap["gastronomy"] ?: 0.0,
                            hospitality = ratingsMap["hospitality"] ?: 0.0,
                            social = ratingsMap["social"] ?: 0.0
                        )
                    )

                    trySend(rating)
                } catch (e: Exception) {
                    Log.e(TAG, "Error parsing detailed rating", e)
                    trySend(null)
                }
            }

        awaitClose { listener.remove() }
    }
}