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

        // Sunucu tarafında filtreleme yapan sorgu
        val subscription = firestore.collection(CITIES_COLLECTION)
            .whereGreaterThanOrEqualTo("ratingCount", 20) // İstemci yerine sunucuda filtrele
            .orderBy("ratingCount", Query.Direction.ASCENDING) // İndeks sıralamasıyla uyumlu olmalı
            .orderBy(fieldPath, Query.Direction.DESCENDING)
            .limit(limit.toLong()) // İhtiyacımız olan kadarını direkt çek
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.e(TAG, "Error getting cities by category: ${error.message}", error)
                    close(error)
                    return@addSnapshotListener
                }

                val cities = snapshot?.documents?.mapNotNull { doc ->
                    try {
                        val model = doc.toObject(CityModel::class.java)
                        model?.copy(id = doc.id)
                    } catch (e: Exception) {
                        Log.e(TAG, "Error converting document", e)
                        null
                    }
                } ?: emptyList()

                // İstemci tarafında filtreleme YOK, direkt sunucudan gelen listeyi kullan
                trySend(cities)
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
            // SORGU YAPISI DEĞİŞİKLİĞİ - ARTIK SADECE KATEGORI ALANINA GÖRE SIRALAMA YAPACAĞIZ
            var query = firestore.collection(CITIES_COLLECTION)
                .orderBy(fieldPath, Query.Direction.DESCENDING)

            // Pagination için son belge
            if (lastVisible != null) {
                query = query.startAfter(lastVisible)
            }

            // Limit uygula
            query = query.limit(limit.toLong())

            // Verileri al
            val querySnapshot = query.get().await()

            // Şehir listesini dönüştür
            val cities = querySnapshot.documents.mapNotNull { document ->
                try {
                    val city = document.toObject(CityModel::class.java)
                    city?.copy(id = document.id)
                } catch (e: Exception) {
                    Log.e(TAG, "Error converting document: ${e.message}")
                    null
                }
            }

            // Son belgeyi al
            val lastDoc = if (cities.isNotEmpty()) {
                querySnapshot.documents.last()
            } else {
                null
            }

            // Daha fazla öğe olup olmadığını kontrol et
            val hasMore = querySnapshot.size() >= limit

            // Sonucu gönder
            trySend(PaginatedResult(cities, lastDoc, hasMore))
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
            // SORGU YAPISI DEĞİŞİKLİĞİ - SADECE KATEGORI ALANINA GÖRE SIRALAMA YAPACAĞIZ
            val snapshot = firestore.collection(CITIES_COLLECTION)
                .orderBy(fieldPath, Query.Direction.DESCENDING)
                .limit(limit.toLong())
                .get()
                .await()

            return snapshot.documents.mapNotNull { doc ->
                try {
                    val model = doc.toObject(CityModel::class.java)
                    model?.copy(id = doc.id)
                } catch (e: Exception) {
                    Log.e(TAG, "Error converting document", e)
                    null
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error in one-time query: ${e.message}", e)
            throw e
        }
    }

}