package com.aliaktas.urbanscore.data.repository.stats

import android.util.Log
import com.aliaktas.urbanscore.data.model.CategoryRatings
import com.aliaktas.urbanscore.data.model.CityModel
import com.google.firebase.functions.FirebaseFunctions
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

// app/src/main/java/com/aliaktas/urbanscore/data/repository/stats/CityStatsRepositoryImpl.kt

@Singleton
class CityStatsRepositoryImpl @Inject constructor() : CityStatsRepository {
    override suspend fun getTopCitiesByCategory(
        category: String,
        minRatings: Int,
        limit: Int
    ): Result<List<CityModel>> = withContext(Dispatchers.IO) {
        try {
            val data = mapOf(
                "category" to category,
                "minRatings" to minRatings,
                "limit" to limit
            )

            val functions = FirebaseFunctions.getInstance()
            val result = functions
                .getHttpsCallable("getTopCitiesByCategory")
                .call(data)
                .await()

            val response = result.data as? Map<String, Any>
            val success = response?.get("success") as? Boolean ?: false

            if (success) {
                @Suppress("UNCHECKED_CAST")
                val citiesData = response?.get("cities") as? List<Map<String, Any>> ?: emptyList()

                val cities = citiesData.mapNotNull { cityData ->
                    try {
                        CityModel(
                            id = cityData["id"] as? String ?: "",
                            cityName = cityData["cityName"] as? String ?: "",
                            country = cityData["country"] as? String ?: "",
                            flagUrl = cityData["flagUrl"] as? String ?: "",
                            region = cityData["region"] as? String ?: "",
                            population = (cityData["population"] as? Number)?.toLong() ?: 0,
                            averageRating = (cityData["averageRating"] as? Number)?.toDouble() ?: 0.0,
                            ratingCount = (cityData["ratingCount"] as? Number)?.toInt() ?: 0,
                            ratings = cityData["ratings"] as? CategoryRatings ?: CategoryRatings()
                        )
                    } catch (e: Exception) {
                        Log.e("CityStatsRepositoryImpl", "Error converting city data", e)
                        null
                    }
                }

                Result.success(cities)
            } else {
                Result.failure(Exception("Cloud Function failed: ${response?.get("error") ?: "Unknown error"}"))
            }
        } catch (e: Exception) {
            Log.e("CityStatsRepositoryImpl", "Error fetching top cities", e)
            Result.failure(e)
        }
    }
}