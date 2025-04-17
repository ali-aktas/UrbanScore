package com.aliaktas.urbanscore.data.repository.city

import android.util.Log
import com.aliaktas.urbanscore.data.model.CityModel
import com.aliaktas.urbanscore.data.model.PaginatedResult
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import com.google.firebase.firestore.Filter
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CityCategoryRepositoryImpl @Inject constructor(
    private val firestore: FirebaseFirestore
) : CityCategoryRepository {

    companion object {
        private const val CITIES_COLLECTION = "cities"
        private const val TAG = "CityCategoryRepository"
    }

    override suspend fun getCitiesByCategoryRating(categoryName: String, limit: Int): Flow<List<CityModel>> = callbackFlow {
        // Kategori adını doğrulayalım
        val validCategories = setOf("gastronomy", "aesthetics", "safety", "culture", "livability", "social", "hospitality")
        val category = if (categoryName in validCategories) categoryName else "averageRating"

        // Kategori puanlamasına göre alanı ayarlama
        val fieldPath = if (category == "averageRating") "averageRating" else "ratings.$category"
        Log.d(TAG, "Kategori field path: $fieldPath, kategori adı: $category")

        // Değişiklik burada: minRating 20'den 50'ye çıkarılıyor
        val subscription = firestore.collection(CITIES_COLLECTION)
            .whereGreaterThanOrEqualTo("ratingCount", 50) // 20'den 50'ye değişti
            .orderBy(fieldPath, Query.Direction.DESCENDING) // rating sayısına göre sıralamayı kaldırdık
            .limit(limit.toLong())
            .addSnapshotListener { snapshot, error ->
                // Diğer kod aynı kalacak
                // ...
            }

        awaitClose {
            Log.d(TAG, "Closing cities by category subscription")
            subscription.remove()
        }
    }

    override suspend fun getCitiesByCategoryPaginated(
        categoryName: String,
        limit: Int,
        lastVisible: DocumentSnapshot?
    ): Flow<PaginatedResult<CityModel>> = callbackFlow {
        // Kategori adını doğrulayalım
        val validCategories = setOf("gastronomy", "aesthetics", "safety", "culture", "livability", "social", "hospitality")
        val category = if (categoryName in validCategories) categoryName else "averageRating"

        // Kategori puanlamasına göre alanı ayarlama
        val fieldPath = if (category == "averageRating") "averageRating" else "ratings.$category"
        Log.d(TAG, "Kategori field path: $fieldPath, kategori adı: $category")

        try {
            // Değişiklik burada: minRating 20'den 50'ye çıkarılıyor ve sıralama sadece puana göre
            var query = firestore.collection(CITIES_COLLECTION)
                .whereGreaterThanOrEqualTo("ratingCount", 50) // 20'den 50'ye değişti
                .orderBy(fieldPath, Query.Direction.DESCENDING) // Sadece puana göre sıralama

            // Diğer kod aynı kalacak
            // ...
        } catch (e: Exception) {
            Log.e(TAG, "Error getting paginated cities: ${e.message}")
            close(e)
        }

        awaitClose { }
    }

    override suspend fun getCitiesByCategoryRatingOneTime(categoryName: String, limit: Int): List<CityModel> {
        // Kategori adını doğrulayalım
        val validCategories = setOf("gastronomy", "aesthetics", "safety", "culture", "livability", "social", "hospitality")
        val category = if (categoryName in validCategories) categoryName else "averageRating"

        // Kategori puanlamasına göre alanı ayarlama
        val fieldPath = if (category == "averageRating") "averageRating" else "ratings.$category"
        Log.d(TAG, "One-time query - Kategori: $category, Alan: $fieldPath")

        try {
            // Değişiklik burada: minRating 20'den 50'ye çıkarılıyor
            val snapshot = firestore.collection(CITIES_COLLECTION)
                .whereGreaterThanOrEqualTo("ratingCount", 50) // 20'den 50'ye değişti
                .orderBy(fieldPath, Query.Direction.DESCENDING) // Sadece puana göre sıralama
                .limit(limit.toLong())
                .get()
                .await()

            // Diğer kod aynı kalacak
            // ...
        } catch (e: Exception) {
            Log.e(TAG, "Error in one-time query: ${e.message}", e)
            throw e
        }
        return TODO("Provide the return value")
    }
}