package com.aliaktas.urbanscore.data.repository

import android.util.Log
import com.aliaktas.urbanscore.data.model.UserModel
import com.aliaktas.urbanscore.data.model.UserRatingModel
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.auth.UserProfileChangeRequest
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class UserRepositoryImpl @Inject constructor(
    private val auth: FirebaseAuth,
    private val firestore: FirebaseFirestore
) : UserRepository {

    companion object {
        private const val USERS_COLLECTION = "users"
        private const val TAG = "UserRepository"
    }

    override fun getCurrentUser(): Flow<UserModel?> = callbackFlow {
        val authStateListener = FirebaseAuth.AuthStateListener { auth ->
            if (auth.currentUser != null) {
                // Kullanıcı giriş yapmış, Firestore'dan detaylı bilgileri al
                firestore.collection(USERS_COLLECTION).document(auth.currentUser!!.uid)
                    .get()
                    .addOnSuccessListener { documentSnapshot ->
                        if (documentSnapshot.exists()) {
                            val user = auth.currentUser!!
                            val data = documentSnapshot.data ?: mapOf<String, Any>()

                            // visited_cities ve wishlist_cities alanlarını dikkatlice al
                            @Suppress("UNCHECKED_CAST")
                            val visitedCities = data["visited_cities"] as? Map<String, Double> ?: emptyMap()

                            @Suppress("UNCHECKED_CAST")
                            val wishlistCities = data["wishlist_cities"] as? List<String> ?: emptyList()

                            Log.d(TAG, "Loaded user with ${visitedCities.size} visited cities and ${wishlistCities.size} wishlist cities")

                            val userModel = UserModel(
                                id = user.uid,
                                email = user.email ?: "",
                                displayName = user.displayName ?: data["display_name"] as? String ?: "",
                                photoUrl = user.photoUrl?.toString() ?: data["photo_url"] as? String ?: "",
                                visited_cities = visitedCities,
                                wishlist_cities = wishlistCities
                            )
                            trySend(userModel)
                        } else {
                            // Firestore'da kullanıcı verisi yok, temel auth bilgilerini kullan
                            val user = auth.currentUser!!
                            val userModel = UserModel(
                                id = user.uid,
                                email = user.email ?: "",
                                displayName = user.displayName ?: "",
                                photoUrl = user.photoUrl?.toString() ?: ""
                            )
                            trySend(userModel)
                        }
                    }
                    .addOnFailureListener { e ->
                        Log.e(TAG, "Error fetching user data", e)
                        // Hata durumunda temel auth bilgilerini kullan
                        val user = auth.currentUser!!
                        trySend(
                            UserModel(
                                id = user.uid,
                                email = user.email ?: "",
                                displayName = user.displayName ?: "",
                                photoUrl = user.photoUrl?.toString() ?: ""
                            )
                        )
                    }
            } else {
                // Kullanıcı çıkış yapmış
                trySend(null)
            }
        }

        auth.addAuthStateListener(authStateListener)

        awaitClose {
            auth.removeAuthStateListener(authStateListener)
        }
    }

    override suspend fun signInWithEmail(email: String, password: String): Result<UserModel> = try {
        val authResult = auth.signInWithEmailAndPassword(email, password).await()
        val user = authResult.user ?: throw Exception("Authentication failed")

        val userModel = UserModel(
            id = user.uid,
            email = user.email ?: "",
            displayName = user.displayName ?: "",
            photoUrl = user.photoUrl?.toString() ?: ""
        )

        Result.success(userModel)
    } catch (e: Exception) {
        Log.e(TAG, "signInWithEmail failed", e)
        Result.failure(e)
    }

    override suspend fun signUpWithEmail(
        email: String,
        password: String,
        displayName: String
    ): Result<UserModel> = try {
        Log.d(TAG, "Attempting to create user with email: $email")
        val authResult = auth.createUserWithEmailAndPassword(email, password).await()
        val user = authResult.user ?: throw Exception("User creation failed")

        Log.d(TAG, "User created successfully, updating profile...")
        // Firebase Auth profilini güncelle
        val profileUpdates = UserProfileChangeRequest.Builder()
            .setDisplayName(displayName)
            .build()

        user.updateProfile(profileUpdates).await()

        Log.d(TAG, "Profile updated, saving to Firestore...")
        // Firestore'a kullanıcı verisini kaydet
        val userData = mapOf(
            "display_name" to displayName,
            "email" to email,
            "photo_url" to "",
            "visited_cities" to emptyMap<String, Double>(),
            "wishlist_cities" to emptyList<String>()
        )

        try {
            Log.d(TAG, "Saving user data to Firestore...")
            firestore.collection(USERS_COLLECTION).document(user.uid)
                .set(userData).await()
            Log.d(TAG, "User data saved to Firestore successfully")
        } catch (e: Exception) {
            Log.e(TAG, "Error saving user data to Firestore", e)
        }

        val userModel = UserModel(
            id = user.uid,
            email = email,
            displayName = displayName,
            photoUrl = ""
        )

        Result.success(userModel)
    } catch (e: Exception) {
        Log.e(TAG, "signUpWithEmail failed", e)
        Result.failure(e)
    }

    override suspend fun signInWithGoogle(idToken: String): Result<UserModel> = try {
        val credential = GoogleAuthProvider.getCredential(idToken, null)
        val authResult = auth.signInWithCredential(credential).await()
        val user = authResult.user ?: throw Exception("Google authentication failed")

        // Kullanıcı Firestore'da var mı kontrol et
        val userDoc = firestore.collection(USERS_COLLECTION).document(user.uid).get().await()

        if (!userDoc.exists()) {
            // Yeni kullanıcı, Firestore'a kaydet
            val userData = mapOf(
                "display_name" to (user.displayName ?: ""),
                "email" to (user.email ?: ""),
                "photo_url" to (user.photoUrl?.toString() ?: ""),
                "visited_cities" to emptyMap<String, Double>(),
                "wishlist_cities" to emptyList<String>()
            )

            firestore.collection(USERS_COLLECTION).document(user.uid).set(userData).await()
        }

        // Güncel verileri al
        @Suppress("UNCHECKED_CAST")
        val visitedCities = userDoc.get("visited_cities") as? Map<String, Double> ?: emptyMap()

        @Suppress("UNCHECKED_CAST")
        val wishlistCities = userDoc.get("wishlist_cities") as? List<String> ?: emptyList()

        val userModel = UserModel(
            id = user.uid,
            email = user.email ?: "",
            displayName = user.displayName ?: "",
            photoUrl = user.photoUrl?.toString() ?: "",
            visited_cities = visitedCities,
            wishlist_cities = wishlistCities
        )

        Result.success(userModel)
    } catch (e: Exception) {
        Log.e(TAG, "signInWithGoogle failed", e)
        Result.failure(e)
    }

    override suspend fun signOut(): Result<Unit> = try {
        auth.signOut()
        Result.success(Unit)
    } catch (e: Exception) {
        Log.e(TAG, "signOut failed", e)
        Result.failure(e)
    }

    override suspend fun updateUserProfile(
        displayName: String,
        photoUrl: String?
    ): Result<Unit> = try {
        val currentUser = auth.currentUser ?: throw Exception("No user logged in")

        // Firebase Auth profilini güncelle
        val profileUpdatesBuilder = UserProfileChangeRequest.Builder()
            .setDisplayName(displayName)

        photoUrl?.let {
            if (it.isNotEmpty()) {
                profileUpdatesBuilder.setPhotoUri(android.net.Uri.parse(it))
            }
        }

        currentUser.updateProfile(profileUpdatesBuilder.build()).await()

        // Firestore'daki kullanıcı verisini güncelle
        val updates = mutableMapOf<String, Any>(
            "display_name" to displayName
        )

        photoUrl?.let {
            if (it.isNotEmpty()) {
                updates["photo_url"] = it
            }
        }

        firestore.collection(USERS_COLLECTION).document(currentUser.uid)
            .update(updates).await()

        Result.success(Unit)
    } catch (e: Exception) {
        Log.e(TAG, "updateUserProfile failed", e)
        Result.failure(e)
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

    override suspend fun saveDetailedRating(cityId: String, cityName: String, rating: UserRatingModel): Result<Unit> = try {
        val currentUser = auth.currentUser ?: throw Exception("No user logged in")

        Log.d(TAG, "Saving detailed rating for $cityId ($cityName)")

        // Alt koleksiyona kaydet
        firestore.collection(USERS_COLLECTION)
            .document(currentUser.uid)
            .collection("ratings")
            .document(cityId)
            .set(mapOf(
                "cityId" to cityId,
                "cityName" to cityName,
                "timestamp" to rating.timestamp,
                "userAverageRating" to rating.userAverageRating,
                "ratings" to mapOf(
                    "environment" to rating.ratings.environment,
                    "safety" to rating.ratings.safety,
                    "livability" to rating.ratings.livability,
                    "cost" to rating.ratings.cost,
                    "social" to rating.ratings.social
                )
            )).await()

        Result.success(Unit)
    } catch (e: Exception) {
        Log.e(TAG, "saveDetailedRating failed", e)
        Result.failure(e)
    }

    override suspend fun getDetailedRating(cityId: String): Flow<UserRatingModel?> = callbackFlow {
        val currentUser = auth.currentUser

        if (currentUser == null) {
            trySend(null)
            close()
            return@callbackFlow
        }

        val listener = firestore.collection(USERS_COLLECTION)
            .document(currentUser.uid)
            .collection("ratings")
            .document(cityId)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.e(TAG, "Error getting detailed rating", error)
                    trySend(null)
                    return@addSnapshotListener
                }

                if (snapshot == null || !snapshot.exists()) {
                    trySend(null)
                    return@addSnapshotListener
                }

                try {
                    val data = snapshot.data ?: return@addSnapshotListener

                    @Suppress("UNCHECKED_CAST")
                    val ratingsMap = data["ratings"] as? Map<String, Double> ?: mapOf()

                    val rating = UserRatingModel(
                        cityId = data["cityId"] as? String ?: "",
                        cityName = data["cityName"] as? String ?: "",
                        timestamp = (data["timestamp"] as? Long) ?: 0L,
                        userAverageRating = (data["userAverageRating"] as? Double) ?: 0.0,
                        ratings = com.aliaktas.urbanscore.data.model.CategoryRatings(
                            environment = ratingsMap["environment"] ?: 0.0,
                            safety = ratingsMap["safety"] ?: 0.0,
                            livability = ratingsMap["livability"] ?: 0.0,
                            cost = ratingsMap["cost"] ?: 0.0,
                            social = ratingsMap["social"] ?: 0.0
                        )
                    )

                    trySend(rating)
                } catch (e: Exception) {
                    Log.e(TAG, "Error parsing detailed rating", e)
                    trySend(null)
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