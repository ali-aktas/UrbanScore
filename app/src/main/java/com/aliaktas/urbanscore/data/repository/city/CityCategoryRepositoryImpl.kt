package com.aliaktas.urbanscore.data.repository.city

import android.util.Log
import com.aliaktas.urbanscore.data.model.CategoryRatings
import com.aliaktas.urbanscore.data.model.CityModel
import com.aliaktas.urbanscore.data.model.PaginatedResult
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.functions.FirebaseFunctions
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
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
        private const val MIN_RATING_COUNT = 50 // Minimum oy sayısı, tek yerden yönetim için
    }

    /**
     * Kategori adını doğrulayıp alan yolunu belirler
     */
    private fun getCategoryFieldPath(categoryName: String): String {
        val validCategories = setOf("gastronomy", "aesthetics", "safety", "culture", "livability", "social", "hospitality")
        val category = if (categoryName in validCategories) categoryName else "averageRating"
        return if (category == "averageRating") "averageRating" else "ratings.$category"
    }

    /**
     * Kategorilere göre şehirleri sıralayarak getirir (Flow olarak)
     */
    override suspend fun getCitiesByCategoryRating(categoryName: String, limit: Int): Flow<List<CityModel>> = callbackFlow {
        try {
            val fieldPath = getCategoryFieldPath(categoryName)
            Log.d(TAG, "Getting cities by category: $categoryName, field: $fieldPath, limit: $limit")

            val subscription = firestore.collection(CITIES_COLLECTION)
                .whereGreaterThanOrEqualTo("ratingCount", MIN_RATING_COUNT)
                .orderBy(fieldPath, Query.Direction.DESCENDING) // Önce kategori puanına göre sırala
                .orderBy("ratingCount", Query.Direction.DESCENDING) // Sonra oy sayısına göre sırala
                .limit(limit.toLong())
                .addSnapshotListener { snapshot, error ->
                    if (error != null) {
                        Log.e(TAG, "Error fetching cities by category: ${error.message}", error)
                        close(error)
                        return@addSnapshotListener
                    }

                    val cities = snapshot?.documents?.mapNotNull { doc ->
                        try {
                            val model = doc.toObject(CityModel::class.java)
                            model?.copy(id = doc.id)
                        } catch (e: Exception) {
                            Log.e(TAG, "Error converting document: ${e.message}", e)
                            null
                        }
                    } ?: emptyList()

                    Log.d(TAG, "Fetched ${cities.size} cities for category $categoryName")

                    // Debug için sıralamayı logla
                    if (cities.isNotEmpty()) {
                        logCategorySorting(cities, categoryName)
                    }

                    trySend(cities)
                }

            awaitClose {
                Log.d(TAG, "Closing cities subscription for category: $categoryName")
                subscription.remove()
            }

        } catch (e: Exception) {
            Log.e(TAG, "Error in getCitiesByCategoryRating: ${e.message}", e)
            trySend(emptyList())
            close(e)
        }
    }

    /**
     * Kategorilere göre şehirleri tek seferde getirir (Liste olarak)
     */
    override suspend fun getCitiesByCategoryRatingOneTime(categoryName: String, limit: Int): List<CityModel> {
        val category = if (categoryName in setOf("gastronomy", "aesthetics", "safety", "culture", "livability", "social", "hospitality"))
            categoryName else "averageRating"

        Log.d(TAG, "Getting cities one-time by category: $category, limit: $limit")

        return try {
            // İlk olarak Cloud Function'ı dene
            val data = mapOf(
                "category" to category,
                "minRatings" to MIN_RATING_COUNT,
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
                Log.d(TAG, "Cloud Function returned ${citiesData.size} cities for category $category")

                val cities = citiesData.mapNotNull { cityData ->
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
                            ratings = parseRatings(cityData["ratings"])
                        )
                    } catch (e: Exception) {
                        Log.e(TAG, "Error converting city data: ${e.message}", e)
                        null
                    }
                }

                // Debug için sıralamayı logla
                if (cities.isNotEmpty()) {
                    logCategorySorting(cities, category)
                }

                cities
            } else {
                // Cloud Function hatası, direkt Firestore'a geç
                throw Exception("Cloud Function failed: ${response?.get("error") ?: "Unknown error"}")
            }
        } catch (e: Exception) {
            Log.w(TAG, "Falling back to direct Firestore query: ${e.message}")

            // Direkt Firestore sorgusu yap
            try {
                val fieldPath = getCategoryFieldPath(category)
                Log.d(TAG, "Direct query with field path: $fieldPath")

                val snapshot = firestore.collection(CITIES_COLLECTION)
                    .whereGreaterThanOrEqualTo("ratingCount", MIN_RATING_COUNT)
                    .orderBy(fieldPath, Query.Direction.DESCENDING) // Önce kategori puanına göre sırala
                    .orderBy("ratingCount", Query.Direction.DESCENDING) // Sonra oy sayısına göre sırala
                    .limit(limit.toLong())
                    .get()
                    .await()

                Log.d(TAG, "Direct Firestore query returned ${snapshot.size()} cities")

                val cities = snapshot.documents.mapNotNull { doc ->
                    try {
                        doc.toObject(CityModel::class.java)?.copy(id = doc.id)
                    } catch (e: Exception) {
                        Log.e(TAG, "Error converting document: ${e.message}", e)
                        null
                    }
                }

                // Debug için sıralamayı logla
                if (cities.isNotEmpty()) {
                    logCategorySorting(cities, category)
                }

                cities
            } catch (e2: Exception) {
                Log.e(TAG, "Backup query also failed: ${e2.message}", e2)
                emptyList() // En kötü durumda boş liste dön
            }
        }
    }

    /**
     * Kategorilere göre şehirleri sayfalandırarak getirir
     */
    override suspend fun getCitiesByCategoryPaginated(
        categoryName: String,
        limit: Int,
        lastVisible: DocumentSnapshot?
    ): Flow<PaginatedResult<CityModel>> = callbackFlow {
        try {
            val fieldPath = getCategoryFieldPath(categoryName)
            Log.d(TAG, "Getting paginated cities for category: $categoryName, field: $fieldPath, limit: $limit")

            // Temel sorguyu oluştur
            var baseQuery = firestore.collection(CITIES_COLLECTION)
                .whereGreaterThanOrEqualTo("ratingCount", MIN_RATING_COUNT)
                .orderBy(fieldPath, Query.Direction.DESCENDING) // Önce kategori puanına göre sırala
                .orderBy("ratingCount", Query.Direction.DESCENDING) // Sonra oy sayısına göre sırala

            // Sayfalandırma için son belge varsa, ondan sonrakini getir
            val query = if (lastVisible != null) {
                Log.d(TAG, "Using pagination with lastVisible document: ${lastVisible.id}")
                baseQuery.startAfter(lastVisible).limit(limit.toLong())
            } else {
                baseQuery.limit(limit.toLong())
            }

            // Sorguyu çalıştır
            val querySnapshot = query.get().await()
            Log.d(TAG, "Pagination query returned ${querySnapshot.size()} documents")

            // Sonuçları dönüştür
            val cities = querySnapshot.documents.mapNotNull { doc ->
                try {
                    doc.toObject(CityModel::class.java)?.copy(id = doc.id)
                } catch (e: Exception) {
                    Log.e(TAG, "Error converting document: ${e.message}", e)
                    null
                }
            }

            // Debug için sıralamayı logla
            if (cities.isNotEmpty()) {
                logCategorySorting(cities, categoryName)
            }

            // Son görünür belge ve daha fazla öğe olup olmadığını belirle
            val lastDocument = querySnapshot.documents.lastOrNull()
            val hasMore = cities.size >= limit

            val result = PaginatedResult(
                items = cities,
                lastVisible = lastDocument,
                hasMoreItems = hasMore
            )

            Log.d(TAG, "Sending PaginatedResult with ${cities.size} items, hasMore=$hasMore")
            trySend(result)

        } catch (e: Exception) {
            Log.e(TAG, "Error in getCitiesByCategoryPaginated: ${e.message}", e)
            // Hata durumunda boş sonuçlar gönder, ama hatayı fırlatma
            trySend(PaginatedResult(emptyList(), null, false))
        }

        awaitClose {}
    }

    /**
     * Ratings nesnesini Map'ten CategoryRatings'e dönüştürür
     */
    @Suppress("UNCHECKED_CAST")
    private fun parseRatings(ratingsData: Any?): CategoryRatings {
        if (ratingsData == null) return CategoryRatings()

        try {
            val ratingsMap = ratingsData as? Map<String, Any> ?: return CategoryRatings()

            return CategoryRatings(
                gastronomy = (ratingsMap["gastronomy"] as? Number)?.toDouble() ?: 0.0,
                aesthetics = (ratingsMap["aesthetics"] as? Number)?.toDouble() ?: 0.0,
                safety = (ratingsMap["safety"] as? Number)?.toDouble() ?: 0.0,
                culture = (ratingsMap["culture"] as? Number)?.toDouble() ?: 0.0,
                livability = (ratingsMap["livability"] as? Number)?.toDouble() ?: 0.0,
                social = (ratingsMap["social"] as? Number)?.toDouble() ?: 0.0,
                hospitality = (ratingsMap["hospitality"] as? Number)?.toDouble() ?: 0.0
            )
        } catch (e: Exception) {
            Log.e(TAG, "Error parsing ratings: ${e.message}", e)
            return CategoryRatings()
        }
    }

    /**
     * Debug için kategori sıralamasını loglar
     */
    private fun logCategorySorting(cities: List<CityModel>, categoryName: String) {
        try {
            Log.d(TAG, "---------- CATEGORY SORTING DEBUG ($categoryName) ----------")
            cities.take(5).forEachIndexed { index, city ->
                val rating = when (categoryName) {
                    "gastronomy" -> city.ratings.gastronomy
                    "aesthetics" -> city.ratings.aesthetics
                    "safety" -> city.ratings.safety
                    "culture" -> city.ratings.culture
                    "livability" -> city.ratings.livability
                    "social" -> city.ratings.social
                    "hospitality" -> city.ratings.hospitality
                    else -> city.averageRating
                }
                Log.d(TAG, "City #${index+1}: ${city.cityName}, ${city.country} - $categoryName: $rating, votes: ${city.ratingCount}")
            }
            Log.d(TAG, "----------------------------------------------------------")
        } catch (e: Exception) {
            Log.e(TAG, "Error logging category sorting: ${e.message}")
        }
    }
}