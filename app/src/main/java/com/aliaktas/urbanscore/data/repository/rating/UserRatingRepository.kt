package com.aliaktas.urbanscore.data.repository.rating

import com.aliaktas.urbanscore.data.model.UserRatingModel
import kotlinx.coroutines.flow.Flow

interface UserRatingRepository {
    // Puanlama detayları işlemleri
    suspend fun saveDetailedRating(cityId: String, cityName: String, rating: UserRatingModel): Result<Unit>
    suspend fun getDetailedRating(cityId: String): Flow<UserRatingModel?>
}