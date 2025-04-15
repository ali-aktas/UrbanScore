package com.aliaktas.urbanscore.data.repository

import com.aliaktas.urbanscore.data.model.UserModel
import com.aliaktas.urbanscore.data.model.UserRatingModel
import com.aliaktas.urbanscore.data.repository.auth.AuthRepository
import com.aliaktas.urbanscore.data.repository.cities.UserCitiesRepository
import com.aliaktas.urbanscore.data.repository.profile.ProfileRepository
import com.aliaktas.urbanscore.data.repository.rating.UserRatingRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Adapter class to provide backward compatibility with the original UserRepository interface.
 * This class delegates all method calls to the appropriate new repositories.
 */
@Singleton
class UserRepositoryAdapter @Inject constructor(
    private val authRepository: AuthRepository,
    private val profileRepository: ProfileRepository,
    private val userCitiesRepository: UserCitiesRepository,
    private val userRatingRepository: UserRatingRepository
) : UserRepository {

    override fun getCurrentUser(): Flow<UserModel?> = authRepository.getCurrentUser()

    override suspend fun signInWithEmail(email: String, password: String): Result<UserModel> =
        authRepository.signInWithEmail(email, password)

    override suspend fun signUpWithEmail(email: String, password: String, displayName: String): Result<UserModel> =
        authRepository.signUpWithEmail(email, password, displayName)

    override suspend fun signInWithGoogle(idToken: String): Result<UserModel> =
        authRepository.signInWithGoogle(idToken)

    override suspend fun signOut(): Result<Unit> =
        authRepository.signOut()

    override suspend fun updateUserProfile(displayName: String, photoUrl: String?): Result<Unit> =
        profileRepository.updateUserProfile(displayName, photoUrl)

    override suspend fun addToWishlist(cityId: String): Result<Unit> =
        userCitiesRepository.addToWishlist(cityId)

    override suspend fun removeFromWishlist(cityId: String): Result<Unit> =
        userCitiesRepository.removeFromWishlist(cityId)

    override suspend fun getUserWishlist(): Flow<List<String>> =
        userCitiesRepository.getUserWishlist()

    override suspend fun addToVisitedCities(cityId: String, rating: Double): Result<Unit> =
        userCitiesRepository.addToVisitedCities(cityId, rating)

    override suspend fun removeFromVisitedCities(cityId: String): Result<Unit> =
        userCitiesRepository.removeFromVisitedCities(cityId)

    override suspend fun getUserVisitedCities(): Flow<Map<String, Double>> =
        userCitiesRepository.getUserVisitedCities()

    override suspend fun saveDetailedRating(cityId: String, cityName: String, rating: UserRatingModel): Result<Unit> =
        userRatingRepository.saveDetailedRating(cityId, cityName, rating)

    override suspend fun getDetailedRating(cityId: String): Flow<UserRatingModel?> =
        userRatingRepository.getDetailedRating(cityId)

    override fun hasUserRatedCity(cityId: String): Flow<Boolean> =
        userCitiesRepository.hasUserRatedCity(cityId)

    override suspend fun requestAccountDeletion(reason: String?): Result<Unit> =
        authRepository.requestAccountDeletion(reason)

    override suspend fun saveUserCountry(countryId: String): Result<Unit> =
        profileRepository.saveUserCountry(countryId)

}