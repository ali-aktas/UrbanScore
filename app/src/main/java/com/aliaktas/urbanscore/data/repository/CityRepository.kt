package com.aliaktas.urbanscore.data.repository

import com.aliaktas.urbanscore.data.model.CategoryRatings
import com.aliaktas.urbanscore.data.model.CityModel
import com.aliaktas.urbanscore.data.model.CuratedCityItem
import com.aliaktas.urbanscore.data.model.PaginatedResult
import com.aliaktas.urbanscore.data.model.UserRatingModel
import com.google.firebase.firestore.DocumentSnapshot
import kotlinx.coroutines.flow.Flow

interface CityRepository {
    suspend fun getAllCities(): Flow<List<CityModel>>
    suspend fun getCityById(cityId: String): Flow<CityModel?>
    suspend fun rateCity(cityId: String, userId: String, ratings: CategoryRatings): Result<Unit>
    suspend fun getUserRatings(userId: String): Flow<List<UserRatingModel>>
    suspend fun getCitiesByCategoryRating(categoryName: String, limit: Int = 20): Flow<List<CityModel>>

    // Pagination için yeni metot
    suspend fun getCitiesByCategoryPaginated(
        categoryName: String,
        limit: Int = 20,
        lastVisible: DocumentSnapshot? = null
    ): Flow<PaginatedResult<CityModel>>

    suspend fun getCuratedCities(listType: String): Flow<List<CuratedCityItem>>

}