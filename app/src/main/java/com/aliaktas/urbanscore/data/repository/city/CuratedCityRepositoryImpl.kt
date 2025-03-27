package com.aliaktas.urbanscore.data.repository.city

import android.util.Log
import com.aliaktas.urbanscore.data.model.CuratedCityItem
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
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
}