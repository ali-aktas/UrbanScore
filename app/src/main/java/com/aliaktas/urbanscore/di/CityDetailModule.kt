package com.aliaktas.urbanscore.di

import android.content.Context
import com.aliaktas.urbanscore.ui.detail.CityDetailEventHandler
import com.aliaktas.urbanscore.ui.detail.CityDetailFormatter
import com.aliaktas.urbanscore.ui.detail.CityDetailUiControllerFactory
import com.aliaktas.urbanscore.ui.detail.RadarChartHelper
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object CityDetailModule {

    @Provides
    @Singleton
    fun provideCityDetailUiControllerFactory(
        formatter: CityDetailFormatter,
        radarChartHelper: RadarChartHelper
    ): CityDetailUiControllerFactory {
        return CityDetailUiControllerFactory(formatter, radarChartHelper)
    }

    @Provides
    @Singleton
    fun provideCityDetailEventHandler(
        @ApplicationContext context: Context
    ): CityDetailEventHandler {
        return CityDetailEventHandler(context)
    }
}