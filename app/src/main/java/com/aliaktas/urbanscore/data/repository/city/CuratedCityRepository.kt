package com.aliaktas.urbanscore.data.repository.city

import com.aliaktas.urbanscore.data.model.CuratedCityItem
import kotlinx.coroutines.flow.Flow

interface CuratedCityRepository {
    suspend fun getCuratedCities(listType: String): Flow<List<CuratedCityItem>>
    suspend fun getCuratedCitiesOneTime(listType: String): List<CuratedCityItem>
}