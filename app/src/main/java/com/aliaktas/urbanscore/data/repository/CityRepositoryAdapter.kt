package com.aliaktas.urbanscore.data.repository

import com.aliaktas.urbanscore.data.model.CategoryRatings
import com.aliaktas.urbanscore.data.model.CityModel
import com.aliaktas.urbanscore.data.model.CommentModel
import com.aliaktas.urbanscore.data.model.CuratedCityItem
import com.aliaktas.urbanscore.data.model.PaginatedResult
import com.aliaktas.urbanscore.data.model.UserRatingModel
import com.aliaktas.urbanscore.data.repository.city.CityBaseRepository
import com.aliaktas.urbanscore.data.repository.city.CityCategoryRepository
import com.aliaktas.urbanscore.data.repository.city.CityCommentRepository
import com.aliaktas.urbanscore.data.repository.city.CityRatingRepository
import com.aliaktas.urbanscore.data.repository.city.CuratedCityRepository
import com.google.firebase.firestore.DocumentSnapshot
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CityRepositoryAdapter @Inject constructor(
    private val cityBaseRepository: CityBaseRepository,
    private val cityCategoryRepository: CityCategoryRepository,
    private val cityRatingRepository: CityRatingRepository,
    private val cityCommentRepository: CityCommentRepository,
    private val curatedCityRepository: CuratedCityRepository
) : CityRepository {

    override suspend fun getAllCities(): Flow<List<CityModel>> =
        cityBaseRepository.getAllCities()

    override suspend fun getCityById(cityId: String): Flow<CityModel?> =
        cityBaseRepository.getCityById(cityId)

    override suspend fun rateCity(cityId: String, userId: String, ratings: CategoryRatings): Result<Unit> =
        cityRatingRepository.rateCity(cityId, userId, ratings)

    override suspend fun getUserRatings(userId: String): Flow<List<UserRatingModel>> {
        // Bu metod aslında artık kullanılmıyor, boş liste dönebiliriz
        return kotlinx.coroutines.flow.flowOf(emptyList())
    }

    // Mevcut metotları koruyun, altına yeni metodu ekleyin
    override suspend fun getCuratedCitiesOneTime(listType: String): List<CuratedCityItem> =
        curatedCityRepository.getCuratedCitiesOneTime(listType)

    // CityRepositoryAdapter.kt dosyasındaki sınıfa ekleyin
    override suspend fun getCitiesByCategoryRatingOneTime(categoryName: String, limit: Int): List<CityModel> =
        cityCategoryRepository.getCitiesByCategoryRatingOneTime(categoryName, limit)

    override suspend fun getCitiesByCategoryRating(categoryName: String, limit: Int): Flow<List<CityModel>> =
        cityCategoryRepository.getCitiesByCategoryRating(categoryName, limit)

    override suspend fun getCitiesByCategoryPaginated(
        categoryName: String,
        limit: Int,
        lastVisible: DocumentSnapshot?
    ): Flow<PaginatedResult<CityModel>> =
        cityCategoryRepository.getCitiesByCategoryPaginated(categoryName, limit, lastVisible)

    override suspend fun getCuratedCities(listType: String): Flow<List<CuratedCityItem>> =
        curatedCityRepository.getCuratedCities(listType)

    override suspend fun getComments(
        cityId: String,
        limit: Long,
        lastComment: DocumentSnapshot?
    ): Flow<PaginatedResult<CommentModel>> =
        cityCommentRepository.getComments(cityId, limit, lastComment)

    override suspend fun addComment(cityId: String, text: String): Result<String> =
        cityCommentRepository.addComment(cityId, text)

    override suspend fun deleteComment(cityId: String, commentId: String): Result<Unit> =
        cityCommentRepository.deleteComment(cityId, commentId)

    override suspend fun likeComment(cityId: String, commentId: String, like: Boolean): Result<Unit> =
        cityCommentRepository.likeComment(cityId, commentId, like)

    override suspend fun getUserLikedComments(cityId: String): Flow<List<String>> =
        cityCommentRepository.getUserLikedComments(cityId)
}