package com.aliaktas.urbanscore.data.repository.city

import com.aliaktas.urbanscore.data.model.CityModel
import kotlinx.coroutines.flow.Flow

interface CityBaseRepository {
    suspend fun getAllCities(): Flow<List<CityModel>>
    suspend fun getCityById(cityId: String): Flow<CityModel?>
    suspend fun getTopCities(limit: Int): Flow<List<CityModel>>
}