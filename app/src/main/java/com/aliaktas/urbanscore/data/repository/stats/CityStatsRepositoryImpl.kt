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

@Singleton
class CityStatsRepositoryImpl @Inject constructor() : CityStatsRepository {
    override suspend fun getTopCitiesByCategory(
        category: String,
        minRatings: Int,
        limit: Int
    ): Result<List<CityModel>> = withContext(Dispatchers.IO) {
        try {
            val functions = FirebaseFunctions.getInstance()
            val result = functions
                .getHttpsCallable("getTopCitiesByMinRatings")
                .call(mapOf(
                    "category" to category,
                    "minRatings" to minRatings,
                    "limit" to limit
                ))
                .await()

            val response = result.data as? Map<String, Any>
            if (response?.get("success") == true) {
                val cities = (response["cities"] as? List<Map<String, Any>>)?.map { cityData ->
                    CityModel(
                        id = cityData["id"] as? String ?: "",
                        cityName = cityData["cityName"] as? String ?: "",
                        country = cityData["country"] as? String ?: "",
                        averageRating = (cityData["averageRating"] as? Number)?.toDouble() ?: 0.0,
                        population = (cityData["population"] as? Number)?.toLong() ?: 0,
                        flagUrl = cityData["flagUrl"] as? String ?: "",
                        ratings = cityData["ratings"] as? CategoryRatings ?: CategoryRatings()
                    )
                } ?: emptyList()
                Result.success(cities)
            } else {
                Result.failure(Exception(response?.get("error") as? String ?: "Unknown error"))
            }
        } catch (e: Exception) {
            Log.e("CityStatsRepositoryImpl", "Error fetching top cities", e)
            Result.failure(e)
        }
    }
}