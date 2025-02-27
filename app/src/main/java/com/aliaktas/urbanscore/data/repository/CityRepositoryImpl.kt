package com.aliaktas.urbanscore.data.repository

import android.util.Log
import com.aliaktas.urbanscore.data.model.CategoryRatings
import com.aliaktas.urbanscore.data.model.CityModel
import com.aliaktas.urbanscore.data.model.UserRatingModel
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import javax.inject.Inject

class CityRepositoryImpl @Inject constructor(
    private val firestore: FirebaseFirestore
) : CityRepository {

    companion object {
        private const val CITIES_COLLECTION = "cities"
        private const val USER_RATINGS_COLLECTION = "user_ratings"
    }

    override suspend fun getAllCities(): Flow<List<CityModel>> = callbackFlow {
        val subscription = firestore.collection(CITIES_COLLECTION)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }

                val cities = snapshot?.documents?.mapNotNull { doc ->
                    try {
                        val model = doc.toObject(CityModel::class.java)
                        // Assign the document ID to the model ID
                        model?.copy(id = doc.id)
                    } catch (e: Exception) {
                        Log.e("CityRepository", "Error converting document", e)
                        null
                    }
                } ?: emptyList()

                trySend(cities)
            }

        awaitClose { subscription.remove() }
    }

    override suspend fun getCityById(cityId: String): Flow<CityModel?> = callbackFlow {
        val subscription = firestore.collection(CITIES_COLLECTION)
            .document(cityId)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }

                val city = snapshot?.toObject(CityModel::class.java)?.copy(id = snapshot.id)
                trySend(city)
            }

        awaitClose { subscription.remove() }
    }

    override suspend fun getUserRatings(userId: String): Flow<List<UserRatingModel>> = callbackFlow {
        val subscription = firestore.collection(USER_RATINGS_COLLECTION)
            .whereEqualTo("userId", userId)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }

                val ratings = snapshot?.documents?.mapNotNull { doc ->
                    doc.toObject(UserRatingModel::class.java)?.copy(id = doc.id)
                } ?: emptyList()

                trySend(ratings)
            }

        awaitClose { subscription.remove() }
    }

    override suspend fun rateCity(
        cityId: String,
        userId: String,
        ratings: CategoryRatings
    ): Result<Unit> = try {
        // Perform atomic update operation using Transaction
        firestore.runTransaction { transaction ->
            val cityRef = firestore.collection(CITIES_COLLECTION).document(cityId)
            val cityDoc = transaction.get(cityRef)

            // Get available city data
            val city = cityDoc.toObject(CityModel::class.java)
                ?: throw Exception("City not found")

            // Save the new rating
            val userRatingRef = firestore.collection(USER_RATINGS_COLLECTION).document()
            val userRating = UserRatingModel(
                id = userRatingRef.id,
                userId = userId,
                cityId = cityId,
                timestamp = System.currentTimeMillis(),
                ratings = ratings
            )
            transaction.set(userRatingRef, userRating)

            // Update the average ratings of cities
            val newRatingCount = city.ratingCount + 1
            val newRatings = CategoryRatings(
                cuisine = updateAverage(city.ratings.cuisine, ratings.cuisine, city.ratingCount),
                hospitality = updateAverage(city.ratings.hospitality, ratings.hospitality, city.ratingCount),
                landscapeVibe = updateAverage(city.ratings.landscapeVibe, ratings.landscapeVibe, city.ratingCount),
                natureClimate = updateAverage(city.ratings.natureClimate, ratings.natureClimate, city.ratingCount),
                lifestyle = updateAverage(city.ratings.lifestyle, ratings.lifestyle, city.ratingCount),
                safetySerenity = updateAverage(city.ratings.safetySerenity, ratings.safetySerenity, city.ratingCount)
            )

            // Calculate the new overall average
            val newAverageRating = (newRatings.cuisine + newRatings.hospitality +
                    newRatings.landscapeVibe + newRatings.natureClimate +
                    newRatings.lifestyle + newRatings.safetySerenity) / 6

            // Update the city doc
            transaction.update(cityRef, mapOf(
                "ratings" to newRatings,
                "averageRating" to newAverageRating,
                "ratingCount" to newRatingCount
            ))
        }.await()

        Result.success(Unit)
    } catch (e: Exception) {
        Result.failure(e)
    }

    private fun updateAverage(oldAvg: Double, newValue: Double, oldCount: Int): Double {
        return ((oldAvg * oldCount) + newValue) / (oldCount + 1)
    }
}