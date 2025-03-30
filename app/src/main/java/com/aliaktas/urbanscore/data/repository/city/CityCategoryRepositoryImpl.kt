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

    // CityCategoryRepositoryImpl.kt içinde
    override suspend fun getCitiesByCategoryRating(categoryName: String, limit: Int): Flow<List<CityModel>> = callbackFlow {
        // Kategori adını doğrulayalım
        val validCategories = setOf("environment", "safety", "livability", "cost", "social")
        val category = if (categoryName in validCategories) categoryName else "averageRating"

        // Kategori puanlamasına göre alanı ayarlama
        val fieldPath = if (category == "averageRating") "averageRating" else "ratings.$category"

        // Daha fazla veri çek, sonra istemci tarafında filtreleyeceğiz
        val realLimit = limit * 3 // Daha fazla veri çek, sonra filtreleyeceğiz

        val subscription = firestore.collection(CITIES_COLLECTION)
            .orderBy(fieldPath, Query.Direction.DESCENDING)
            .limit(realLimit.toLong())
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }

                val allCities = snapshot?.documents?.mapNotNull { doc ->
                    try {
                        val model = doc.toObject(CityModel::class.java)
                        model?.copy(id = doc.id)
                    } catch (e: Exception) {
                        Log.e(TAG, "Error converting document", e)
                        null
                    }
                } ?: emptyList()

                // İSTEMCİ TARAFINDA FİLTRELEME
                val filteredCities = allCities.filter { city ->
                    city.ratingCount >= 20
                }.take(limit) // Orijinal limit kadar göster

                trySend(filteredCities)
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

        // Daha fazla veri çek
        val realLimit = limit * 3

        // Sorgu oluştur
        var query = firestore.collection(CITIES_COLLECTION)
            .orderBy(fieldPath, Query.Direction.DESCENDING)

        // Eğer son görünen belge varsa, ondan sonrasını al
        if (lastVisible != null) {
            query = query.startAfter(lastVisible)
        }

        // Limit uygula
        query = query.limit(realLimit.toLong())

        try {
            // Verileri al
            val querySnapshot = query.get().await()

            // Şehir listesini dönüştür
            val allCities = querySnapshot.documents.mapNotNull { document ->
                try {
                    val city = document.toObject(CityModel::class.java)
                    city?.copy(id = document.id)
                } catch (e: Exception) {
                    Log.e(TAG, "Error converting document: ${e.message}")
                    null
                }
            }

            // İSTEMCİ TARAFINDA FİLTRELEME
            val filteredCities = allCities.filter { city ->
                city.ratingCount >= 20
            }.take(limit)

            // Son belgeyi al - filtrelenmiş listedeki son eleman
            val lastDoc = if (filteredCities.isNotEmpty()) {
                val lastCity = filteredCities.last()
                querySnapshot.documents.find { it.id == lastCity.id }
            } else {
                null
            }

            // Daha fazla öğe olup olmadığını kontrol et
            val hasMore = allCities.size > filteredCities.size || querySnapshot.documents.size == realLimit

            // Sonucu gönder
            trySend(PaginatedResult(filteredCities, lastDoc, hasMore))
        } catch (e: Exception) {
            Log.e(TAG, "Error getting paginated cities: ${e.message}")
            close(e)
        }

        awaitClose { }
    }
}