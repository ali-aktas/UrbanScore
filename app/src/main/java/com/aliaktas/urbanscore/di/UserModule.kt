package com.aliaktas.urbanscore.di

import com.aliaktas.urbanscore.data.repository.UserRepository
import com.aliaktas.urbanscore.data.repository.UserRepositoryAdapter
import com.aliaktas.urbanscore.data.repository.auth.AuthRepository
import com.aliaktas.urbanscore.data.repository.auth.AuthRepositoryImpl
import com.aliaktas.urbanscore.data.repository.cities.UserCitiesRepository
import com.aliaktas.urbanscore.data.repository.cities.UserCitiesRepositoryImpl
import com.aliaktas.urbanscore.data.repository.profile.ProfileRepository
import com.aliaktas.urbanscore.data.repository.profile.ProfileRepositoryImpl
import com.aliaktas.urbanscore.data.repository.rating.UserRatingRepository
import com.aliaktas.urbanscore.data.repository.rating.UserRatingRepositoryImpl
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class UserModule {

    @Binds
    @Singleton
    abstract fun bindAuthRepository(
        authRepositoryImpl: AuthRepositoryImpl
    ): AuthRepository

    @Binds
    @Singleton
    abstract fun bindProfileRepository(
        profileRepositoryImpl: ProfileRepositoryImpl
    ): ProfileRepository

    @Binds
    @Singleton
    abstract fun bindUserCitiesRepository(
        userCitiesRepositoryImpl: UserCitiesRepositoryImpl
    ): UserCitiesRepository

    @Binds
    @Singleton
    abstract fun bindUserRatingRepository(
        userRatingRepositoryImpl: UserRatingRepositoryImpl
    ): UserRatingRepository

    @Binds
    @Singleton
    abstract fun bindUserRepository(
        userRepositoryAdapter: UserRepositoryAdapter
    ): UserRepository
}