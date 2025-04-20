package com.aliaktas.urbanscore.data.repository.auth

import android.util.Log
import com.aliaktas.urbanscore.data.model.UserModel
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.auth.UserProfileChangeRequest
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AuthRepositoryImpl @Inject constructor(
    private val auth: FirebaseAuth,
    private val firestore: FirebaseFirestore
) : AuthRepository {

    companion object {
        private const val USERS_COLLECTION = "users"
        private const val TAG = "AuthRepository"
    }

    override fun getCurrentUser(): Flow<UserModel?> = callbackFlow {
        val authStateListener = FirebaseAuth.AuthStateListener { auth ->
            if (auth.currentUser != null) {
                firestore.collection(USERS_COLLECTION).document(auth.currentUser!!.uid)
                    .addSnapshotListener { documentSnapshot, error ->
                        if (error != null) {
                            Log.e(TAG, "Error listening to user document", error)
                            trySend(null)
                            return@addSnapshotListener
                        }

                        if (documentSnapshot != null && documentSnapshot.exists()) {
                            val user = auth.currentUser!!
                            val data = documentSnapshot.data ?: mapOf<String, Any>()

                            @Suppress("UNCHECKED_CAST")
                            val visitedCities = data["visited_cities"] as? Map<String, Double> ?: emptyMap()

                            @Suppress("UNCHECKED_CAST")
                            val wishlistCities = data["wishlist_cities"] as? List<String> ?: emptyList()

                            // country alanını al
                            val country = data["country"] as? String ?: ""

                            Log.d(TAG, "User data loaded: ${user.uid}, country=$country, has ${visitedCities.size} visited cities")

                            val userModel = UserModel(
                                id = user.uid,
                                email = user.email ?: "",
                                displayName = user.displayName ?: data["display_name"] as? String ?: "",
                                photoUrl = user.photoUrl?.toString() ?: data["photo_url"] as? String ?: "",
                                visited_cities = visitedCities,
                                wishlist_cities = wishlistCities,
                                country = country
                            )
                            trySend(userModel)
                        } else {
                            trySend(null)
                        }
                    }
            } else {
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
            // country alanını eklemeyin; cloud function bunu otomatik ekleyecek
        )

        try {
            Log.d(TAG, "Saving user data to Firestore...")
            // withTimeout ile süre sınırı ekleyelim
            kotlinx.coroutines.withTimeout(5000) {
                firestore.collection(USERS_COLLECTION).document(user.uid)
                    .set(userData).await()
            }
            Log.d(TAG, "User data saved to Firestore successfully")
        } catch (e: Exception) {
            // Hata detaylarını kaydet
            Log.e(TAG, "Error saving user data to Firestore: ${e.javaClass.simpleName} - ${e.message}")
            // Ama hatayı tekrar fırlatma - işlemi tamamlamaya çalışalım
        }

        val userModel = UserModel(
            id = user.uid,
            email = email,
            displayName = displayName,
            photoUrl = "",
            // country alanını boş bırakarak başlatalım
            country = ""
        )

        Result.success(userModel)
    } catch (e: Exception) {
        Log.e(TAG, "signUpWithEmail failed: ${e.javaClass.simpleName} - ${e.message}")
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
            wishlist_cities = wishlistCities,
            country = ""
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

    override suspend fun requestAccountDeletion(reason: String?): Result<Unit> = try {
        val currentUser = auth.currentUser ?: throw Exception("No user logged in")

        val requestData = hashMapOf(
            "userId" to currentUser.uid,
            "email" to (currentUser.email ?: ""),
            "displayName" to (currentUser.displayName ?: ""),
            "timestamp" to com.google.firebase.Timestamp.now(),
            "reason" to (reason ?: "No reason provided"),
            "status" to "pending" // pending, approved, rejected
        )

        firestore.collection("deletion_requests").document(currentUser.uid)
            .set(requestData).await()

        Result.success(Unit)
    } catch (e: Exception) {
        Log.e(TAG, "requestAccountDeletion failed", e)
        Result.failure(e)
    }
}