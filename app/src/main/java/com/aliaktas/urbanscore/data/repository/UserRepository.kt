package com.aliaktas.urbanscore.data.repository

import com.aliaktas.urbanscore.data.model.UserModel
import kotlinx.coroutines.flow.Flow

interface UserRepository {
    fun getCurrentUser(): Flow<UserModel?>
    suspend fun signInWithEmail(email: String, password: String): Result<UserModel>
    suspend fun signUpWithEmail(email: String, password: String, displayName: String): Result<UserModel>
    suspend fun signInWithGoogle(idToken: String): Result<UserModel>
    suspend fun signOut(): Result<Unit>
    suspend fun updateUserProfile(displayName: String, photoUrl: String? = null): Result<Unit>
    suspend fun addToWishlist(cityId: String): Result<Unit>
    suspend fun removeFromWishlist(cityId: String): Result<Unit>
    suspend fun addToVisitedCities(cityId: String): Result<Unit>
    suspend fun removeFromVisitedCities(cityId: String): Result<Unit>
    suspend fun getUserWishlist(): Flow<List<String>>
    suspend fun getUserVisitedCities(): Flow<List<String>>
}