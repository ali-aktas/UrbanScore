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
                        Log.e(TAG, "Error converting document", e)
                        null
                    }
                } ?: emptyList()

                trySend(cities)
            }

        awaitClose { subscription.remove() }
    }

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
                    Log.e(TAG, "Error converting document: ${e.message}")
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
            Log.e(TAG, "Error getting paginated cities: ${e.message}")
            close(e)
        }

        awaitClose { }
    }
}