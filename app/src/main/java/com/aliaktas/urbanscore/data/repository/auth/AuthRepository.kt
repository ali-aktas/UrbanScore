package com.aliaktas.urbanscore.data.repository.auth

import com.aliaktas.urbanscore.data.model.UserModel
import kotlinx.coroutines.flow.Flow

interface AuthRepository {
    fun getCurrentUser(): Flow<UserModel?>
    suspend fun signInWithEmail(email: String, password: String): Result<UserModel>
    suspend fun signUpWithEmail(email: String, password: String, displayName: String): Result<UserModel>
    suspend fun signInWithGoogle(idToken: String): Result<UserModel>
    suspend fun signOut(): Result<Unit>
}