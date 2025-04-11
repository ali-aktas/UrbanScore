package com.aliaktas.urbanscore.data.repository.city

import com.aliaktas.urbanscore.data.model.CategoryRatings
import kotlinx.coroutines.flow.Flow

interface CityRatingRepository {
    suspend fun rateCity(cityId: String, userId: String, ratings: CategoryRatings): Result<Unit>
}