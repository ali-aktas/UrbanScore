package com.aliaktas.urbanscore.data.repository

import com.aliaktas.urbanscore.data.model.CategoryRatings
import com.aliaktas.urbanscore.data.model.CityModel
import com.aliaktas.urbanscore.data.model.CommentModel
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
    suspend fun getCitiesByCategoryRatingOneTime(categoryName: String, limit: Int): List<CityModel>
    suspend fun getCuratedCitiesOneTime(listType: String): List<CuratedCityItem>

    // Pagination için yeni metot
    suspend fun getCitiesByCategoryPaginated(
        categoryName: String,
        limit: Int = 20,
        lastVisible: DocumentSnapshot? = null
    ): Flow<PaginatedResult<CityModel>>

    // Popular and teams choice cities
    suspend fun getCuratedCities(listType: String): Flow<List<CuratedCityItem>>

    // app/src/main/java/com/aliaktas/urbanscore/data/repository/CityRepository.kt
    // Arayüze eklenecek metotlar

    suspend fun getComments(cityId: String, limit: Long, lastComment: DocumentSnapshot? = null): Flow<PaginatedResult<CommentModel>>
    suspend fun addComment(cityId: String, text: String): Result<String>
    suspend fun deleteComment(cityId: String, commentId: String): Result<Unit>
    suspend fun likeComment(cityId: String, commentId: String, like: Boolean): Result<Unit>
    suspend fun getUserLikedComments(cityId: String): Flow<List<String>>

}