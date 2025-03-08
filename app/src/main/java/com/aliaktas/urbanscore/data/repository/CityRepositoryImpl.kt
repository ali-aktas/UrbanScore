package com.aliaktas.urbanscore.data.repository

import android.util.Log
import com.aliaktas.urbanscore.data.model.CategoryRatings
import com.aliaktas.urbanscore.data.model.CityModel
import com.aliaktas.urbanscore.data.model.PaginatedResult
import com.aliaktas.urbanscore.data.model.UserRatingModel
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import com.google.firebase.firestore.Query

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

    suspend fun getTopCities(limit: Int): Flow<List<CityModel>> = callbackFlow {
        val subscription = firestore.collection(CITIES_COLLECTION)
            .orderBy("averageRating", Query.Direction.DESCENDING)
            .limit(limit.toLong())
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }

                val cities = snapshot?.documents?.mapNotNull { doc ->
                    try {
                        val model = doc.toObject(CityModel::class.java)
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

    // CityRepositoryImpl.kt içine yeni metot ekle
    override suspend fun getCitiesByCategoryPaginated(
        categoryName: String,
        limit: Int,
        lastVisible: DocumentSnapshot?
    ): Flow<PaginatedResult<CityModel>> = callbackFlow {
        // Kategori adını doğrulayalım
        val validCategories = setOf("environment", "safety", "livability", "cost", "social")
        val category = if (categoryName in validCategories) categoryName else "averageRating"

        // Kategori puanlamasına göre alanı ayarlama
        val fieldPath = if (category == "averageRating") "averageRating" else "ratings.$category"

        // Sorgu oluştur
        var query = firestore.collection(CITIES_COLLECTION)
            .orderBy(fieldPath, Query.Direction.DESCENDING)

        // Eğer son görünen belge varsa, ondan sonrasını al
        if (lastVisible != null) {
            query = query.startAfter(lastVisible)
        }

        // Limit uygula
        query = query.limit(limit.toLong())

        try {
            // Verileri al
            val querySnapshot = query.get().await()

            // Şehir listesini dönüştür
            val cities = querySnapshot.documents.mapNotNull { document ->
                try {
                    val city = document.toObject(CityModel::class.java)
                    city?.copy(id = document.id)
                } catch (e: Exception) {
                    Log.e("CityRepository", "Error converting document: ${e.message}")
                    null
                }
            }

            // Son belgeyi al
            val lastDoc = querySnapshot.documents.lastOrNull()

            // Daha fazla öğe olup olmadığını kontrol et
            val hasMore = cities.size == limit && cities.isNotEmpty()

            // Sonucu gönder
            trySend(PaginatedResult(cities, lastDoc, hasMore))
        } catch (e: Exception) {
            Log.e("CityRepository", "Error getting paginated cities: ${e.message}")
            close(e)
        }

        awaitClose { }
    }

    override suspend fun getCitiesByCategoryRating(categoryName: String, limit: Int): Flow<List<CityModel>> = callbackFlow {
        // Kategori adını doğrulayalım
        val validCategories = setOf("environment", "safety", "livability", "cost", "social")
        val category = if (categoryName in validCategories) categoryName else "averageRating"

        // Kategori puanlamasına göre alanı ayarlama
        val fieldPath = if (category == "averageRating") "averageRating" else "ratings.$category"

        val subscription = firestore.collection(CITIES_COLLECTION)
            .orderBy(fieldPath, Query.Direction.DESCENDING)
            .limit(limit.toLong())
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }

                val cities = snapshot?.documents?.mapNotNull { doc ->
                    try {
                        val model = doc.toObject(CityModel::class.java)
                        // Döküman ID'sini modele atayalım
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

    override suspend fun rateCity(
        cityId: String,
        userId: String,
        ratings: CategoryRatings
    ): Result<Unit> = try {
        // Önce ondalık basamakları düzenle
        val formattedRatings = CategoryRatings(
            environment = (ratings.environment * 100).toInt() / 100.0,
            safety = (ratings.safety * 100).toInt() / 100.0,
            livability = (ratings.livability * 100).toInt() / 100.0,
            cost = (ratings.cost * 100).toInt() / 100.0,
            social = (ratings.social * 100).toInt() / 100.0
        )

        // Perform atomic update operation using Transaction
        firestore.runTransaction { transaction ->
            val cityRef = firestore.collection(CITIES_COLLECTION).document(cityId)
            val cityDoc = transaction.get(cityRef)

            // Get available city data
            val city = cityDoc.toObject(CityModel::class.java)
                ?: throw Exception("City not found")

            // Create a unique ID for user rating
            val userRatingId = "${userId}_${cityId}"
            val userRatingRef = firestore.collection(USER_RATINGS_COLLECTION).document(userRatingId)

            // Check if the user has already rated this city
            val userRatingDoc = transaction.get(userRatingRef)
            val oldRating = userRatingDoc.toObject(UserRatingModel::class.java)

            // Create user rating model
            val userRating = UserRatingModel(
                userId = userId,
                cityId = cityId,
                timestamp = System.currentTimeMillis(),
                ratings = formattedRatings
            )

            // Set user rating (will update if already exists)
            transaction.set(userRatingRef, userRating)

            // Update the average ratings of city based on whether this is a new rating or an update
            if (oldRating != null) {
                // This is an update - need to remove old rating impact
                val newRatings = CategoryRatings(
                    environment = updateRatingForEdit(city.ratings.environment, oldRating.ratings.environment,
                        formattedRatings.environment, city.ratingCount),
                    safety = updateRatingForEdit(city.ratings.safety, oldRating.ratings.safety,
                        formattedRatings.safety, city.ratingCount),
                    livability = updateRatingForEdit(city.ratings.livability, oldRating.ratings.livability,
                        formattedRatings.livability, city.ratingCount),
                    cost = updateRatingForEdit(city.ratings.cost, oldRating.ratings.cost,
                        formattedRatings.cost, city.ratingCount),
                    social = updateRatingForEdit(city.ratings.social, oldRating.ratings.social,
                        formattedRatings.social, city.ratingCount)
                )

                // Recalculate the new overall average using weighted formula
                val newAverageRating = calculateWeightedAverage(newRatings)

                // Format to 2 decimal places
                val formattedAverage = (newAverageRating * 100).toInt() / 100.0

                // Update the city doc - without incrementing rating count since it's an update
                transaction.update(cityRef, mapOf(
                    "ratings" to newRatings,
                    "averageRating" to formattedAverage
                ))
            } else {
                // This is a new rating
                val newRatingCount = city.ratingCount + 1
                val newRatings = CategoryRatings(
                    environment = updateAverage(city.ratings.environment, formattedRatings.environment, city.ratingCount),
                    safety = updateAverage(city.ratings.safety, formattedRatings.safety, city.ratingCount),
                    livability = updateAverage(city.ratings.livability, formattedRatings.livability, city.ratingCount),
                    cost = updateAverage(city.ratings.cost, formattedRatings.cost, city.ratingCount),
                    social = updateAverage(city.ratings.social, formattedRatings.social, city.ratingCount)
                )

                // Calculate the new overall average using weighted formula
                val newAverageRating = calculateWeightedAverage(newRatings)

                // Format to 2 decimal places
                val formattedAverage = (newAverageRating * 100).toInt() / 100.0

                // Update the city doc and increment rating count
                transaction.update(cityRef, mapOf(
                    "ratings" to newRatings,
                    "averageRating" to formattedAverage,
                    "ratingCount" to newRatingCount
                ))
            }
        }.await()

        Result.success(Unit)
    } catch (e: Exception) {
        Result.failure(e)
    }

    // Calculate weighted average based on your formula
    private fun calculateWeightedAverage(ratings: CategoryRatings): Double {
        return ((ratings.environment * 1.3) +
                (ratings.safety * 1.1) +
                (ratings.livability * 1.0) +
                (ratings.cost * 1.0) +
                (ratings.social * 1.2)) / 5.6
    }

    // Update average for a new rating
    private fun updateAverage(oldAvg: Double, newValue: Double, oldCount: Int): Double {
        val newAvg = ((oldAvg * oldCount) + newValue) / (oldCount + 1)
        // Format to 2 decimal places
        return (newAvg * 100).toInt() / 100.0
    }

    // Update average when editing an existing rating
    private fun updateRatingForEdit(currentAvg: Double, oldValue: Double, newValue: Double, totalCount: Int): Double {
        // Formula: ((currentAvg * totalCount) - oldValue + newValue) / totalCount
        val newAvg = ((currentAvg * totalCount) - oldValue + newValue) / totalCount
        // Format to 2 decimal places
        return (newAvg * 100).toInt() / 100.0
    }

}