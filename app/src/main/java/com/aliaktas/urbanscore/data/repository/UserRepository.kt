package com.aliaktas.urbanscore.data.repository

import com.aliaktas.urbanscore.data.model.UserModel
import com.aliaktas.urbanscore.data.model.UserRatingModel
import kotlinx.coroutines.flow.Flow

interface UserRepository {
    fun getCurrentUser(): Flow<UserModel?>
    suspend fun signInWithEmail(email: String, password: String): Result<UserModel>
    suspend fun signUpWithEmail(email: String, password: String, displayName: String): Result<UserModel>
    suspend fun signInWithGoogle(idToken: String): Result<UserModel>
    suspend fun signOut(): Result<Unit>
    suspend fun updateUserProfile(displayName: String, photoUrl: String? = null): Result<Unit>

    // Bucket list (wishlist) işlemleri
    suspend fun addToWishlist(cityId: String): Result<Unit>
    suspend fun removeFromWishlist(cityId: String): Result<Unit>
    suspend fun getUserWishlist(): Flow<List<String>>

    // Ziyaret edilen şehirler işlemleri
    suspend fun addToVisitedCities(cityId: String, rating: Double): Result<Unit>
    suspend fun removeFromVisitedCities(cityId: String): Result<Unit>
    suspend fun getUserVisitedCities(): Flow<Map<String, Double>>

    // Puanlama detayları işlemleri
    suspend fun saveDetailedRating(cityId: String, cityName: String, rating: UserRatingModel): Result<Unit>
    suspend fun getDetailedRating(cityId: String): Flow<UserRatingModel?>

    // Şehir puanlama kontrolü
    fun hasUserRatedCity(cityId: String): Flow<Boolean>
}