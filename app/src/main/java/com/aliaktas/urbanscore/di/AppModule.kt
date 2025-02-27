package com.aliaktas.urbanscore.di

import com.aliaktas.urbanscore.data.repository.CityRepository
import com.aliaktas.urbanscore.data.repository.CityRepositoryImpl
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
    fun provideCityRepository(
        firestore: FirebaseFirestore
    ): CityRepository {
        return CityRepositoryImpl(firestore)
    }
}