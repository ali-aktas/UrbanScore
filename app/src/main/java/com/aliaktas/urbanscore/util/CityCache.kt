package com.aliaktas.urbanscore.util

import android.content.Context
import com.aliaktas.urbanscore.data.model.CityModel
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CityCache @Inject constructor(@ApplicationContext private val context: Context) {
    private val prefs = context.getSharedPreferences("city_cache", Context.MODE_PRIVATE)
    private val gson = Gson()

    fun getCities(): List<CityModel>? {
        val json = prefs.getString("cities", null) ?: return null
        return try {
            gson.fromJson(json, object : TypeToken<List<CityModel>>() {}.type)
        } catch (e: Exception) {
            null
        }
    }

    fun saveCities(cities: List<CityModel>) {
        val json = gson.toJson(cities)
        prefs.edit().putString("cities", json).apply()
    }

    fun getLastUpdateTime(): Long = prefs.getLong("last_update", 0)

    fun setLastUpdateTime(time: Long) {
        prefs.edit().putLong("last_update", time).apply()
    }

    fun needsUpdate(): Boolean {
        val lastUpdate = getLastUpdateTime()
        // 7 günde bir güncelleme yap
        return System.currentTimeMillis() - lastUpdate > 7 * 24 * 60 * 60 * 1000
    }
}