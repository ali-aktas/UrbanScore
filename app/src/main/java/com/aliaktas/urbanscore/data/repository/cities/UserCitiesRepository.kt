package com.aliaktas.urbanscore.data.repository.cities

import kotlinx.coroutines.flow.Flow

interface UserCitiesRepository {
    // Bucket list (wishlist) işlemleri
    suspend fun addToWishlist(cityId: String): Result<Unit>
    suspend fun removeFromWishlist(cityId: String): Result<Unit>
    suspend fun getUserWishlist(): Flow<List<String>>

    // Ziyaret edilen şehirler işlemleri
    suspend fun addToVisitedCities(cityId: String, rating: Double): Result<Unit>
    suspend fun removeFromVisitedCities(cityId: String): Result<Unit>
    suspend fun getUserVisitedCities(): Flow<Map<String, Double>>

    // Şehir puanlama kontrolü
    fun hasUserRatedCity(cityId: String): Flow<Boolean>
}