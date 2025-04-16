package com.aliaktas.urbanscore.data.repository.stats

import com.aliaktas.urbanscore.data.model.CityModel
import kotlinx.coroutines.flow.Flow

interface CityStatsRepository {
    suspend fun getTopCitiesByCategory(
        category: String = "averageRating",
        minRatings: Int = 50,
        limit: Int = 20
    ): Result<List<CityModel>>
}