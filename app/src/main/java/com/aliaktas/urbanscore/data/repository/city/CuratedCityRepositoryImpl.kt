package com.aliaktas.urbanscore.data.repository.city

import android.util.Log
import com.aliaktas.urbanscore.data.model.CuratedCityItem
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CuratedCityRepositoryImpl @Inject constructor(
    private val firestore: FirebaseFirestore
) : CuratedCityRepository {

    companion object {
        private const val CURATED_LISTS_COLLECTION = "curated_lists"
        private const val TAG = "CuratedCityRepository"
    }

    override suspend fun getCuratedCities(listType: String): Flow<List<CuratedCityItem>> = callbackFlow {
        Log.d(TAG, "Loading curated cities for type: $listType")

        val subscription = firestore.collection(CURATED_LISTS_COLLECTION)
            .whereEqualTo("listType", listType)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.e(TAG, "Error getting curated cities", error)
                    trySend(emptyList())
                    return@addSnapshotListener
                }

                val cities = snapshot?.documents?.mapNotNull { doc ->
                    try {
                        Log.d(TAG, "Found curated document: ${doc.id}")
                        val model = doc.toObject(CuratedCityItem::class.java)
                        model?.copy(id = doc.id)
                    } catch (e: Exception) {
                        Log.e(TAG, "Error converting document", e)
                        null
                    }
                } ?: emptyList()

                // Position'a göre manuel sıralama yapıyoruz
                val sortedCities = cities.sortedBy { it.position }

                Log.d(TAG, "Loaded ${sortedCities.size} curated cities")
                trySend(sortedCities)
            }

        awaitClose { subscription.remove() }
    }

    // Mevcut getCuratedCities metodunu KORUYUN, altına yeni metodu ekleyin
    override suspend fun getCuratedCitiesOneTime(listType: String): List<CuratedCityItem> {
        Log.d(TAG, "Loading curated cities one-time for type: $listType")

        try {
            val snapshot = firestore.collection(CURATED_LISTS_COLLECTION)
                .whereEqualTo("listType", listType)
                .get()
                .await()

            val cities = snapshot.documents.mapNotNull { doc ->
                try {
                    Log.d(TAG, "Found curated document: ${doc.id}")
                    val model = doc.toObject(CuratedCityItem::class.java)
                    model?.copy(id = doc.id)
                } catch (e: Exception) {
                    Log.e(TAG, "Error converting document", e)
                    null
                }
            }

            // Position'a göre manuel sıralama yapıyoruz
            return cities.sortedBy { it.position }

        } catch (e: Exception) {
            Log.e(TAG, "Error in one-time curated cities query: ${e.message}", e)
            throw e
        }
    }

}