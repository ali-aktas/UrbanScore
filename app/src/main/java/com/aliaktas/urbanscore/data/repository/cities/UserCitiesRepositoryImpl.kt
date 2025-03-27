package com.aliaktas.urbanscore.data.repository.cities

import android.util.Log
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class UserCitiesRepositoryImpl @Inject constructor(
    private val auth: FirebaseAuth,
    private val firestore: FirebaseFirestore
) : UserCitiesRepository {

    companion object {
        private const val USERS_COLLECTION = "users"
        private const val TAG = "UserCitiesRepository"
    }

    override suspend fun addToWishlist(cityId: String): Result<Unit> = try {
        val currentUser = auth.currentUser ?: throw Exception("No user logged in")

        Log.d(TAG, "Adding city $cityId to wishlist")
        firestore.collection(USERS_COLLECTION).document(currentUser.uid)
            .update("wishlist_cities", FieldValue.arrayUnion(cityId)).await()

        Result.success(Unit)
    } catch (e: Exception) {
        Log.e(TAG, "addToWishlist failed", e)
        Result.failure(e)
    }

    override suspend fun removeFromWishlist(cityId: String): Result<Unit> = try {
        val currentUser = auth.currentUser ?: throw Exception("No user logged in")

        Log.d(TAG, "Removing city $cityId from wishlist")
        firestore.collection(USERS_COLLECTION).document(currentUser.uid)
            .update("wishlist_cities", FieldValue.arrayRemove(cityId)).await()

        Result.success(Unit)
    } catch (e: Exception) {
        Log.e(TAG, "removeFromWishlist failed", e)
        Result.failure(e)
    }

    override suspend fun getUserWishlist(): Flow<List<String>> = callbackFlow {
        val currentUser = auth.currentUser

        if (currentUser == null) {
            trySend(emptyList())
            close()
            return@callbackFlow
        }

        val listener = firestore.collection(USERS_COLLECTION).document(currentUser.uid)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.e(TAG, "Error listening to wishlist", error)
                    trySend(emptyList())
                    return@addSnapshotListener
                }

                if (snapshot == null || !snapshot.exists()) {
                    trySend(emptyList())
                    return@addSnapshotListener
                }

                try {
                    @Suppress("UNCHECKED_CAST")
                    val wishlist = snapshot.get("wishlist_cities") as? List<String> ?: emptyList()
                    Log.d(TAG, "Fetched wishlist with ${wishlist.size} cities")
                    trySend(wishlist)
                } catch (e: Exception) {
                    Log.e(TAG, "Error parsing wishlist", e)
                    trySend(emptyList())
                }
            }

        awaitClose { listener.remove() }
    }

    override suspend fun addToVisitedCities(cityId: String, rating: Double): Result<Unit> {
        try {
            val currentUser = auth.currentUser ?: throw Exception("No user logged in")

            Log.d(TAG, "Adding city $cityId with rating $rating to visited_cities")
            Log.d(TAG, "Current user: ${currentUser.uid}")

            val userDocRef = firestore.collection(USERS_COLLECTION).document(currentUser.uid)

            // Önce kullanıcı belgesini al ve kontrol et
            val userDoc = userDocRef.get().await()
            if (!userDoc.exists()) {
                Log.e(TAG, "User document does not exist! Creating new document...")
                // Kullanıcı belgesi yoksa oluştur
                val userData = mapOf(
                    "display_name" to (currentUser.displayName ?: ""),
                    "email" to (currentUser.email ?: ""),
                    "photo_url" to (currentUser.photoUrl?.toString() ?: ""),
                    "visited_cities" to mapOf(cityId to rating),
                    "wishlist_cities" to listOf<String>()
                )

                userDocRef.set(userData).await()
                Log.d(TAG, "Created new user document with visited_cities")
                return Result.success(Unit)
            }

            // Güncel visited_cities map'ini al
            @Suppress("UNCHECKED_CAST")
            val visitedCities = userDoc.get("visited_cities") as? Map<String, Double> ?: mapOf()
            Log.d(TAG, "Current visited_cities: $visitedCities")

            // Yeni map oluştur ve şehri ekle/güncelle
            val updatedMap = visitedCities.toMutableMap()
            updatedMap[cityId] = rating

            // Manual update
            userDocRef.update("visited_cities", updatedMap).await()
            Log.d(TAG, "Updated visited_cities manually: $updatedMap")

            return Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "addToVisitedCities failed: ${e.message}", e)
            return Result.failure(e)
        }
    }

    override suspend fun removeFromVisitedCities(cityId: String): Result<Unit> {
        return try {
            val currentUser = auth.currentUser ?: throw Exception("No user logged in")

            // Önce kullanıcı belgesini al
            val userDocRef = firestore.collection(USERS_COLLECTION).document(currentUser.uid)
            val userDoc = userDocRef.get().await()

            if (!userDoc.exists()) {
                return Result.failure(Exception("User document not found"))
            }

            // Mevcut visited_cities map'ini al
            @Suppress("UNCHECKED_CAST")
            val visitedCities = userDoc.get("visited_cities") as? Map<String, Double> ?: emptyMap()

            // Yeni bir map oluştur, belirtilen şehir hariç
            val updatedMap = visitedCities.filterKeys { it != cityId }

            // Map'i güncelle
            userDocRef.update("visited_cities", updatedMap).await()

            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "removeFromVisitedCities failed", e)
            Result.failure(e)
        }
    }

    override suspend fun getUserVisitedCities(): Flow<Map<String, Double>> = callbackFlow {
        val currentUser = auth.currentUser

        if (currentUser == null) {
            trySend(emptyMap())
            close()
            return@callbackFlow
        }

        val listener = firestore.collection(USERS_COLLECTION).document(currentUser.uid)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.e(TAG, "Error listening to visited cities", error)
                    trySend(emptyMap())
                    return@addSnapshotListener
                }

                if (snapshot == null || !snapshot.exists()) {
                    trySend(emptyMap())
                    return@addSnapshotListener
                }

                try {
                    @Suppress("UNCHECKED_CAST")
                    val visitedCities = snapshot.get("visited_cities") as? Map<String, Double> ?: emptyMap()
                    Log.d(TAG, "Fetched ${visitedCities.size} visited cities")
                    trySend(visitedCities)
                } catch (e: Exception) {
                    Log.e(TAG, "Error parsing visited cities", e)
                    trySend(emptyMap())
                }
            }

        awaitClose { listener.remove() }
    }

    override fun hasUserRatedCity(cityId: String): Flow<Boolean> = callbackFlow {
        val currentUser = auth.currentUser

        if (currentUser == null) {
            trySend(false)
            close()
            return@callbackFlow
        }

        val listener = firestore.collection(USERS_COLLECTION)
            .document(currentUser.uid)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.e(TAG, "Error checking if user rated city", error)
                    trySend(false)
                    return@addSnapshotListener
                }

                if (snapshot == null || !snapshot.exists()) {
                    trySend(false)
                    return@addSnapshotListener
                }

                try {
                    @Suppress("UNCHECKED_CAST")
                    val visitedCities = snapshot.get("visited_cities") as? Map<String, Double> ?: emptyMap()
                    val hasRated = visitedCities.containsKey(cityId)
                    Log.d(TAG, "City $cityId rated status: $hasRated")
                    trySend(hasRated)
                } catch (e: Exception) {
                    Log.e(TAG, "Error checking if user rated city", e)
                    trySend(false)
                }
            }

        awaitClose { listener.remove() }
    }
}