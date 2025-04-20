package com.aliaktas.urbanscore.data.repository.profile

import android.net.Uri
import android.util.Log
import com.aliaktas.urbanscore.data.model.UserModel
import com.aliaktas.urbanscore.data.repository.auth.AuthRepository
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthInvalidUserException
import com.google.firebase.auth.UserProfileChangeRequest
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withTimeout
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Kullanıcı profili ile ilgili işlemleri yöneten repository implementasyonu.
 * Kullanıcı bilgilerini görüntüleme, güncelleme ve ülke seçimi gibi işlemleri gerçekleştirir.
 */
@Singleton
class ProfileRepositoryImpl @Inject constructor(
    private val auth: FirebaseAuth,
    private val firestore: FirebaseFirestore,
    private val authRepository: AuthRepository
) : ProfileRepository {

    companion object {
        private const val USERS_COLLECTION = "users"
        private const val TAG = "ProfileRepository"
        private const val FIRESTORE_TIMEOUT_MS = 10000L // 10 saniye timeout
    }

    /**
     * Mevcut kullanıcının bilgilerini bir Flow olarak döndürür.
     * Bu bilgi, kullanıcı oturumu değiştiğinde veya profil bilgileri güncellendiğinde otomatik olarak güncellenir.
     */
    override fun getCurrentUser(): Flow<UserModel?> = authRepository.getCurrentUser()

    /**
     * Kullanıcı profilini günceller.
     * Bu işlem hem Firebase Authentication hem de Firestore veritabanında yapılır.
     *
     * @param displayName Kullanıcının yeni görünen adı
     * @param photoUrl Kullanıcının yeni profil fotoğrafı URL'si (null ise değiştirilmez)
     * @return İşlem başarılıysa Result.success, değilse Result.failure
     */
    override suspend fun updateUserProfile(
        displayName: String,
        photoUrl: String?
    ): Result<Unit> {
        return try {
            // Giriş kontrolü
            if (displayName.isBlank()) {
                return Result.failure(IllegalArgumentException("Display name cannot be empty"))
            }

            // Oturum kontrolü
            val currentUser = auth.currentUser
                ?: return Result.failure(Exception("No user logged in"))

            Log.d(TAG, "Updating profile for user: ${currentUser.uid}, name: $displayName")

            try {
                // 1. Firebase Auth profilini güncelle
                val profileUpdatesBuilder = UserProfileChangeRequest.Builder()
                    .setDisplayName(displayName)

                // Profil fotoğrafını ekle (varsa ve boş değilse)
                photoUrl?.let {
                    if (it.isNotEmpty()) {
                        try {
                            profileUpdatesBuilder.setPhotoUri(Uri.parse(it))
                        } catch (e: Exception) {
                            Log.w(TAG, "Invalid photo URL: $it, skipping this update", e)
                        }
                    }
                }

                // Profili güncelle ve işlemin tamamlanmasını bekle
                withTimeout(FIRESTORE_TIMEOUT_MS) {
                    currentUser.updateProfile(profileUpdatesBuilder.build()).await()
                }
                Log.d(TAG, "Firebase Auth profile updated successfully")

                // 2. Firestore'daki kullanıcı verisini güncelle
                val updates = mutableMapOf<String, Any>(
                    "display_name" to displayName,
                    "updated_at" to FieldValue.serverTimestamp()
                )

                // Profil fotoğrafını ekle (varsa ve boş değilse)
                photoUrl?.let {
                    if (it.isNotEmpty()) {
                        updates["photo_url"] = it
                    }
                }

                // Firestore belgesini güncelle ve işlemin tamamlanmasını bekle
                withTimeout(FIRESTORE_TIMEOUT_MS) {
                    firestore.collection(USERS_COLLECTION)
                        .document(currentUser.uid)
                        .set(updates, SetOptions.merge())
                        .await()
                }
                Log.d(TAG, "Firestore profile updated successfully")

                Result.success(Unit)
            } catch (e: TimeoutCancellationException) {
                Log.e(TAG, "Profile update timed out after ${FIRESTORE_TIMEOUT_MS}ms", e)
                Result.failure(Exception("Profile update timed out. Please try again."))
            } catch (e: CancellationException) {
                // Coroutine iptal edildi, tekrar fırlatılmalı
                Log.w(TAG, "Profile update operation was cancelled", e)
                throw e
            } catch (e: FirebaseAuthInvalidUserException) {
                Log.e(TAG, "User account has been disabled or deleted", e)
                Result.failure(Exception("Your account appears to be disabled. Please contact support."))
            } catch (e: Exception) {
                Log.e(TAG, "Failed to update profile: ${e.javaClass.simpleName}", e)
                Result.failure(e)
            }
        } catch (e: Exception) {
            Log.e(TAG, "updateUserProfile failed", e)
            Result.failure(e)
        }
    }

    /**
     * Kullanıcının ülke tercihini kaydeder.
     * Seçilen ülke kodu, kullanıcının Firestore belgesinde saklanır.
     *
     * @param countryId Ülke kodu (ISO 2 karakter kodu, örn. "tr", "us")
     * @return İşlem başarılıysa Result.success, değilse Result.failure
     */
    override suspend fun saveUserCountry(countryId: String): Result<Unit> {
        return try {
            // Giriş kontrolü
            if (countryId.isBlank()) {
                return Result.failure(IllegalArgumentException("Country ID cannot be empty"))
            }

            // Oturum kontrolü
            val currentUser = auth.currentUser
                ?: return Result.failure(Exception("No user logged in"))

            val normalizedCountryId = countryId.lowercase().trim()
            Log.d(TAG, "Saving country '$normalizedCountryId' for user: ${currentUser.uid}")

            try {
                // SetOptions.merge() kullanarak, belge yoksa oluşturacak, varsa birleştirecek
                withTimeout(FIRESTORE_TIMEOUT_MS) {
                    firestore.collection(USERS_COLLECTION)
                        .document(currentUser.uid)
                        .set(
                            mapOf(
                                "country" to normalizedCountryId,
                                "updated_at" to FieldValue.serverTimestamp()
                            ),
                            SetOptions.merge() // Mevcut verileri koru, sadece belirtilen alanları güncelle
                        )
                        .await()
                }

                Log.d(TAG, "Country saved successfully: $normalizedCountryId")
                Result.success(Unit)
            } catch (e: TimeoutCancellationException) {
                Log.e(TAG, "Country save operation timed out after ${FIRESTORE_TIMEOUT_MS}ms", e)
                Result.failure(Exception("Operation timed out. Please try again."))
            } catch (e: CancellationException) {
                // Coroutine iptal edildi, tekrar fırlatılmalı
                Log.w(TAG, "Country save operation was cancelled", e)
                throw e
            } catch (e: Exception) {
                Log.e(TAG, "Error saving country: ${e.javaClass.simpleName} - ${e.message}", e)
                Result.failure(Exception("Failed to save country preference: ${e.message}"))
            }
        } catch (e: Exception) {
            Log.e(TAG, "saveUserCountry failed: ${e.javaClass.simpleName} - ${e.message}", e)
            Result.failure(e)
        }
    }
}