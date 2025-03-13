package com.aliaktas.urbanscore.di

import com.aliaktas.urbanscore.data.repository.CityRecommendationRepository
import com.aliaktas.urbanscore.data.repository.CityRecommendationRepositoryImpl
import com.aliaktas.urbanscore.data.repository.CityRepository
import com.aliaktas.urbanscore.data.repository.CityRepositoryImpl
import com.aliaktas.urbanscore.data.repository.UserRepository
import com.aliaktas.urbanscore.data.repository.UserRepositoryImpl
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {
    @Provides
    @Singleton
    fun provideFirebaseFirestore(): FirebaseFirestore {
        return FirebaseFirestore.getInstance()
    }

    @Provides
    @Singleton
    fun provideFirebaseAuth(): FirebaseAuth {
        return FirebaseAuth.getInstance()
    }

    @Provides
    @Singleton
    fun provideCityRepository(
        firestore: FirebaseFirestore
    ): CityRepository {
        return CityRepositoryImpl(firestore)
    }

    @Provides
    @Singleton
    fun provideUserRepository(
        auth: FirebaseAuth,
        firestore: FirebaseFirestore
    ): UserRepository {
        return UserRepositoryImpl(auth, firestore)
    }

    @Provides
    @Singleton
    fun provideCityRecommendationRepository(
        firestore: FirebaseFirestore,
        auth: FirebaseAuth
    ): CityRecommendationRepository {
        return CityRecommendationRepositoryImpl(firestore, auth)
    }

}