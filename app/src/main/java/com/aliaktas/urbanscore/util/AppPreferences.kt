package com.aliaktas.urbanscore.util

import android.content.Context
import android.content.SharedPreferences
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AppPreferences @Inject constructor(context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences(
        "app_preferences", Context.MODE_PRIVATE
    )

    companion object {
        private const val KEY_FIRST_LAUNCH = "first_launch"
        private const val KEY_LAST_VERSION = "last_version_code"
        private const val KEY_ONBOARDING_SEEN = "onboarding_seen" // Yeni eklenen
    }

    val isFirstLaunch: Boolean
        get() = prefs.getBoolean(KEY_FIRST_LAUNCH, true)

    fun setFirstLaunchComplete() {
        prefs.edit().putBoolean(KEY_FIRST_LAUNCH, false).apply()
    }

    fun getCurrentVersionCode(): Int {
        return prefs.getInt(KEY_LAST_VERSION, 0)
    }

    fun updateVersionCode(versionCode: Int) {
        prefs.edit().putInt(KEY_LAST_VERSION, versionCode).apply()
    }

    // Onboarding için yeni metodlar
    fun setOnboardingSeen(seen: Boolean) {
        prefs.edit().putBoolean(KEY_ONBOARDING_SEEN, seen).apply()
    }

    fun isOnboardingSeen(): Boolean {
        return prefs.getBoolean(KEY_ONBOARDING_SEEN, false)
    }
}