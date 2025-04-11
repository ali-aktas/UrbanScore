package com.aliaktas.urbanscore.data.repository.profile

import android.util.Log
import com.aliaktas.urbanscore.data.model.UserModel
import com.aliaktas.urbanscore.data.repository.auth.AuthRepository
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.UserProfileChangeRequest
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ProfileRepositoryImpl @Inject constructor(
    private val auth: FirebaseAuth,
    private val firestore: FirebaseFirestore,
    private val authRepository: AuthRepository
) : ProfileRepository {

    companion object {
        private const val USERS_COLLECTION = "users"
        private const val TAG = "ProfileRepository"
    }

    override fun getCurrentUser(): Flow<UserModel?> = authRepository.getCurrentUser()

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
}