package com.aliaktas.urbanscore.data.repository

import android.util.Log
import com.aliaktas.urbanscore.data.model.UserModel
import com.aliaktas.urbanscore.data.model.UserRatingModel
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.auth.UserProfileChangeRequest
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
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
                fetchUserData(auth.currentUser!!.uid)
                    .addOnSuccessListener { documentSnapshot ->
                        val userModel = if (documentSnapshot.exists()) {
                            val user = auth.currentUser!!
                            val data = documentSnapshot.data
                            UserModel(
                                id = user.uid,
                                email = user.email ?: "",
                                displayName = user.displayName ?: data?.get("display_name") as? String ?: "",
                                photoUrl = user.photoUrl?.toString() ?: data?.get("photo_url") as? String ?: "",
                                visitedCities = (data?.get("visited_cities") as? List<String>) ?: emptyList(),
                                wishlistCities = (data?.get("wishlist_cities") as? List<String>) ?: emptyList()
                            )
                        } else {
                            // Firestore'da kullanıcı verisi yok, temel auth bilgilerini kullan
                            val user = auth.currentUser!!
                            UserModel(
                                id = user.uid,
                                email = user.email ?: "",
                                displayName = user.displayName ?: "",
                                photoUrl = user.photoUrl?.toString() ?: ""
                            )
                        }
                        trySend(userModel)
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

    private fun fetchUserData(userId: String) =
        firestore.collection(USERS_COLLECTION).document(userId).get()

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
        val userModel = UserModel(
            id = user.uid,
            email = email,
            displayName = displayName,
            photoUrl = ""
        )

        try {
            Log.d(TAG, "Saving user data to Firestore...")
            firestore.collection(USERS_COLLECTION).document(user.uid)
                .set(mapOf(
                    "display_name" to displayName,
                    "email" to email,
                    "photo_url" to "",
                    "visited_cities" to emptyList<String>(),
                    "wishlist_cities" to emptyList<String>()
                )).await()
            Log.d(TAG, "User data saved to Firestore successfully")
        } catch (e: Exception) {
            Log.e(TAG, "Error saving user data to Firestore", e)
            // Firestore kaydetme hatası olsa bile, kullanıcı kaydı başarılı oldu
            // Bu durumda veritabanı verilerini daha sonra senkronize edebiliriz
        }

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
            val userData = hashMapOf(
                "display_name" to (user.displayName ?: ""),
                "email" to (user.email ?: ""),
                "photo_url" to (user.photoUrl?.toString() ?: ""),
                "visited_cities" to emptyList<String>(),
                "wishlist_cities" to emptyList<String>()
            )

            firestore.collection(USERS_COLLECTION).document(user.uid).set(userData).await()
        }

        val userModel = UserModel(
            id = user.uid,
            email = user.email ?: "",
            displayName = user.displayName ?: "",
            photoUrl = user.photoUrl?.toString() ?: "",
            visitedCities = if (userDoc.exists())
                userDoc.get("visited_cities") as? List<String> ?: emptyList()
            else
                emptyList(),
            wishlistCities = if (userDoc.exists())
                userDoc.get("wishlist_cities") as? List<String> ?: emptyList()
            else
                emptyList()
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

        firestore.collection(USERS_COLLECTION).document(currentUser.uid)
            .update("wishlist_cities", FieldValue.arrayUnion(cityId)).await()

        Result.success(Unit)
    } catch (e: Exception) {
        Log.e(TAG, "addToWishlist failed", e)
        Result.failure(e)
    }

    override suspend fun removeFromWishlist(cityId: String): Result<Unit> = try {
        val currentUser = auth.currentUser ?: throw Exception("No user logged in")

        firestore.collection(USERS_COLLECTION).document(currentUser.uid)
            .update("wishlist_cities", FieldValue.arrayRemove(cityId)).await()

        Result.success(Unit)
    } catch (e: Exception) {
        Log.e(TAG, "removeFromWishlist failed", e)
        Result.failure(e)
    }

    override suspend fun addToVisitedCities(cityId: String, rating: Double): Result<Unit> = try {
        val currentUser = auth.currentUser ?: throw Exception("No user logged in")

        // Instead of array update, we're setting a map field
        firestore.collection(USERS_COLLECTION).document(currentUser.uid)
            .update("visitedCities.$cityId", rating).await()

        Result.success(Unit)
    } catch (e: Exception) {
        Log.e(TAG, "addToVisitedCities failed", e)
        Result.failure(e)
    }

    override suspend fun removeFromVisitedCities(cityId: String): Result<Unit> = try {
        val currentUser = auth.currentUser ?: throw Exception("No user logged in")

        // Remove the specific field from the map
        firestore.collection(USERS_COLLECTION).document(currentUser.uid)
            .update("visitedCities.$cityId", FieldValue.delete()).await()

        Result.success(Unit)
    } catch (e: Exception) {
        Log.e(TAG, "removeFromVisitedCities failed", e)
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
                    close(error)
                    return@addSnapshotListener
                }

                val wishlist = snapshot?.get("wishlist_cities") as? List<String> ?: emptyList()
                trySend(wishlist)
            }

        awaitClose { listener.remove() }
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
                    close(error)
                    return@addSnapshotListener
                }

                // Get the map from Firestore
                @Suppress("UNCHECKED_CAST")
                val visitedCities = snapshot?.get("visitedCities") as? Map<String, Double> ?: emptyMap()
                trySend(visitedCities)
            }

        awaitClose { listener.remove() }
    }

    override suspend fun getUserRatingsForCity(cityId: String): Flow<UserRatingModel?> = callbackFlow {
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
                    close(error)
                    return@addSnapshotListener
                }

                val rating = if (snapshot != null && snapshot.exists()) {
                    try {
                        snapshot.toObject(UserRatingModel::class.java)
                    } catch (e: Exception) {
                        Log.e(TAG, "Error converting rating document", e)
                        null
                    }
                } else null

                trySend(rating)
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

        // Kullanıcının visitedCities haritasını kontrol et
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

                val data = snapshot.data
                if (data != null) {
                    @Suppress("UNCHECKED_CAST")
                    val visitedCities = data["visitedCities"] as? Map<String, Any> ?: emptyMap()
                    val hasRated = visitedCities.containsKey(cityId)
                    trySend(hasRated)
                } else {
                    trySend(false)
                }
            }

        awaitClose { listener.remove() }
    }

}