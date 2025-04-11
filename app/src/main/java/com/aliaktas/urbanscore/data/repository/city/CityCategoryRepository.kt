package com.aliaktas.urbanscore.data.repository.city

import com.aliaktas.urbanscore.data.model.CityModel
import com.aliaktas.urbanscore.data.model.PaginatedResult
import com.google.firebase.firestore.DocumentSnapshot
import kotlinx.coroutines.flow.Flow

interface CityCategoryRepository {
    suspend fun getCitiesByCategoryRating(categoryName: String, limit: Int = 20): Flow<List<CityModel>>
    suspend fun getCitiesByCategoryPaginated(
        categoryName: String,
        limit: Int = 20,
        lastVisible: DocumentSnapshot? = null
    ): Flow<PaginatedResult<CityModel>>

    suspend fun getCitiesByCategoryRatingOneTime(categoryName: String, limit: Int): List<CityModel>

}