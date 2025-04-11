package com.aliaktas.urbanscore.data.repository.city

import android.util.Log
import com.aliaktas.urbanscore.data.model.CityModel
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CityBaseRepositoryImpl @Inject constructor(
    private val firestore: FirebaseFirestore
) : CityBaseRepository {

    companion object {
        private const val CITIES_COLLECTION = "cities"
        private const val TAG = "CityBaseRepository"
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
                        Log.e(TAG, "Error converting document", e)
                        null
                    }
                } ?: emptyList()

                trySend(cities)
            }

        awaitClose { subscription.remove() }
    }

    override suspend fun getCityById(cityId: String): Flow<CityModel?> = callbackFlow {
        try {
            val documentRef = firestore.collection(CITIES_COLLECTION).document(cityId)

            val subscription = documentRef.addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }

                if (snapshot != null && snapshot.exists()) {
                    try {
                        val city = snapshot.toObject(CityModel::class.java)?.copy(id = snapshot.id)
                        trySend(city)
                    } catch (e: Exception) {
                        Log.e(TAG, "Error converting document", e)
                        trySend(null)
                    }
                } else {
                    trySend(null)
                }
            }

            awaitClose { subscription.remove() }
        } catch (e: Exception) {
            Log.e(TAG, "Error in getCityById", e)
            close(e)
        }
    }

    override suspend fun getTopCities(limit: Int): Flow<List<CityModel>> = callbackFlow {
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
                        Log.e(TAG, "Error converting document", e)
                        null
                    }
                } ?: emptyList()

                trySend(cities)
            }

        awaitClose { subscription.remove() }
    }
}