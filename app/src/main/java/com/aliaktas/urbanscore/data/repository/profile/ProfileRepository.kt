package com.aliaktas.urbanscore.data.repository.profile

import com.aliaktas.urbanscore.data.model.UserModel
import kotlinx.coroutines.flow.Flow

interface ProfileRepository {
    fun getCurrentUser(): Flow<UserModel?>
    suspend fun updateUserProfile(displayName: String, photoUrl: String? = null): Result<Unit>
    suspend fun saveUserCountry(countryId: String): Result<Unit>
}