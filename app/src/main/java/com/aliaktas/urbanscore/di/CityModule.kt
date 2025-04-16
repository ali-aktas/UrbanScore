package com.aliaktas.urbanscore.di

import com.aliaktas.urbanscore.data.repository.CityRepository
import com.aliaktas.urbanscore.data.repository.CityRepositoryAdapter
import com.aliaktas.urbanscore.data.repository.city.CityBaseRepository
import com.aliaktas.urbanscore.data.repository.city.CityBaseRepositoryImpl
import com.aliaktas.urbanscore.data.repository.city.CityCategoryRepository
import com.aliaktas.urbanscore.data.repository.city.CityCategoryRepositoryImpl
import com.aliaktas.urbanscore.data.repository.city.CityCommentRepository
import com.aliaktas.urbanscore.data.repository.city.CityCommentRepositoryImpl
import com.aliaktas.urbanscore.data.repository.city.CityRatingRepository
import com.aliaktas.urbanscore.data.repository.city.CityRatingRepositoryImpl
import com.aliaktas.urbanscore.data.repository.city.CuratedCityRepository
import com.aliaktas.urbanscore.data.repository.city.CuratedCityRepositoryImpl
import com.aliaktas.urbanscore.data.repository.stats.CityStatsRepository
import com.aliaktas.urbanscore.data.repository.stats.CityStatsRepositoryImpl
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class CityModule {

    @Binds
    @Singleton
    abstract fun bindCityBaseRepository(
        cityBaseRepositoryImpl: CityBaseRepositoryImpl
    ): CityBaseRepository

    @Binds
    @Singleton
    abstract fun bindCityCategoryRepository(
        cityCategoryRepositoryImpl: CityCategoryRepositoryImpl
    ): CityCategoryRepository

    @Binds
    @Singleton
    abstract fun bindCityRatingRepository(
        cityRatingRepositoryImpl: CityRatingRepositoryImpl
    ): CityRatingRepository

    @Binds
    @Singleton
    abstract fun bindCityCommentRepository(
        cityCommentRepositoryImpl: CityCommentRepositoryImpl
    ): CityCommentRepository

    @Binds
    @Singleton
    abstract fun bindCuratedCityRepository(
        curatedCityRepositoryImpl: CuratedCityRepositoryImpl
    ): CuratedCityRepository

    @Binds
    @Singleton
    abstract fun bindCityRepository(
        cityRepositoryAdapter: CityRepositoryAdapter
    ): CityRepository

    @Module
    @InstallIn(SingletonComponent::class)
    abstract class CityStatsModule {
        @Binds
        @Singleton
        abstract fun bindCityStatsRepository(
            cityStatsRepositoryImpl: CityStatsRepositoryImpl
        ): CityStatsRepository
    }
}