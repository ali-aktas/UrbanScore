package com.aliaktas.urbanscore.di

import com.aliaktas.urbanscore.ui.home.controllers.HomeControllerFactory
import com.aliaktas.urbanscore.util.ImageLoader
import com.aliaktas.urbanscore.util.NetworkUtil
import com.aliaktas.urbanscore.util.ResourceProvider
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object HomeModule {

    @Provides
    @Singleton
    fun provideHomeControllerFactory(
        networkUtil: NetworkUtil,
        resourceProvider: ResourceProvider,
        imageLoader: ImageLoader // Yeni parametre ekledik
    ): HomeControllerFactory {
        return HomeControllerFactory(networkUtil, resourceProvider, imageLoader)
    }


}