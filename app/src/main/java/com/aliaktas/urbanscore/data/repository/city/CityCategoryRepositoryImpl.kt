package com.aliaktas.urbanscore.data.repository.city

import android.util.Log
import com.aliaktas.urbanscore.data.model.CategoryRatings
import com.aliaktas.urbanscore.data.model.CityModel
import com.aliaktas.urbanscore.data.model.PaginatedResult
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import com.google.firebase.firestore.Filter
import com.google.firebase.functions.FirebaseFunctions
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

    // app/src/main/java/com/aliaktas/urbanscore/data/repository/city/CityCategoryRepositoryImpl.kt

    override suspend fun getCitiesByCategoryRatingOneTime(categoryName: String, limit: Int): List<CityModel> {
        // Kategori adını doğrulayalım
        val validCategories = setOf("gastronomy", "aesthetics", "safety", "culture", "livability", "social", "hospitality")
        val category = if (categoryName in validCategories) categoryName else "averageRating"

        Log.d(TAG, "Getting cities by category: $category, limit: $limit")

        return try {
            // Önce Cloud Function ile deneyelim
            val data = mapOf(
                "category" to category,
                "minRatings" to 50,
                "limit" to limit
            )

            val functions = FirebaseFunctions.getInstance()
            val result = functions
                .getHttpsCallable("getTopCitiesByCategory")
                .call(data)
                .await()

            val response = result.data as? Map<String, Any>
            val success = response?.get("success") as? Boolean ?: false

            if (success) {
                // Cloud Function başarılı, sonuçları dönüştür
                @Suppress("UNCHECKED_CAST")
                val citiesData = response?.get("cities") as? List<Map<String, Any>> ?: emptyList()

                citiesData.mapNotNull { cityData ->
                    try {
                        CityModel(
                            id = cityData["id"] as? String ?: "",
                            cityName = cityData["cityName"] as? String ?: "",
                            country = cityData["country"] as? String ?: "",
                            flagUrl = cityData["flagUrl"] as? String ?: "",
                            region = cityData["region"] as? String ?: "",
                            population = (cityData["population"] as? Number)?.toLong() ?: 0,
                            averageRating = (cityData["averageRating"] as? Number)?.toDouble() ?: 0.0,
                            ratingCount = (cityData["ratingCount"] as? Number)?.toInt() ?: 0,
                            ratings = cityData["ratings"] as? CategoryRatings ?: CategoryRatings()
                        )
                    } catch (e: Exception) {
                        Log.e(TAG, "Error converting city data", e)
                        null
                    }
                }
            } else {
                // Hata durumunda yedek yöntemi kullan
                throw Exception("Cloud Function failed: ${response?.get("error") ?: "Unknown error"}")
            }
        } catch (e: Exception) {
            Log.w(TAG, "Cloud Function failed, falling back to direct Firestore query", e)

            // Yedek yöntem - direkt Firestore sorgusu
            try {
                // Kategori puanlamasına göre alanı ayarlama
                val fieldPath = if (category == "averageRating") "averageRating" else "ratings.$category"

                val snapshot = firestore.collection(CITIES_COLLECTION)
                    .whereGreaterThanOrEqualTo("ratingCount", 50)
                    .orderBy("ratingCount") // Önce filtreleme alanını sırala
                    .orderBy(fieldPath, Query.Direction.DESCENDING)
                    .limit(limit.toLong())
                    .get()
                    .await()

                snapshot.documents.mapNotNull { doc ->
                    try {
                        doc.toObject(CityModel::class.java)?.copy(id = doc.id)
                    } catch (e: Exception) {
                        Log.e(TAG, "Error converting document", e)
                        null
                    }
                }
            } catch (e2: Exception) {
                Log.e(TAG, "Backup query also failed", e2)
                emptyList() // En kötü durumda boş liste dön
            }
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


}