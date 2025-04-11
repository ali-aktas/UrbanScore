package com.aliaktas.urbanscore.util

import android.content.Context
import android.content.SharedPreferences
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Kullanıcı tercihlerini ve durumlarını yönetir.
 */
@Singleton
class PreferenceManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val preferences: SharedPreferences by lazy {
        context.getSharedPreferences(PREFERENCE_NAME, Context.MODE_PRIVATE)
    }

    /**
     * Kullanıcının pro olup olmadığını kontrol eder.
     */
    fun isProUser(): Boolean {
        return preferences.getBoolean(KEY_IS_PRO, false)
    }

    /**
     * Kullanıcının pro durumunu kaydeder.
     */
    fun setProUser(isPro: Boolean) {
        preferences.edit().putBoolean(KEY_IS_PRO, isPro).apply()
    }

    /**
     * Pro aboneliğin bitiş zamanını kaydeder.
     */
    fun setProExpiryTime(timestamp: Long) {
        preferences.edit().putLong(KEY_PRO_EXPIRY, timestamp).apply()
    }

    /**
     * Pro aboneliğin bitiş zamanını döndürür.
     */
    fun getProExpiryTime(): Long {
        return preferences.getLong(KEY_PRO_EXPIRY, 0)
    }

    /**
     * Pro aboneliğin geçerli olup olmadığını kontrol eder.
     */
    fun isProValid(): Boolean {
        val expiryTime = getProExpiryTime()
        if (expiryTime == 0L) return false

        return System.currentTimeMillis() < expiryTime
    }

    companion object {
        private const val PREFERENCE_NAME = "urbanscore_prefs"
        private const val KEY_IS_PRO = "is_pro_user"
        private const val KEY_PRO_EXPIRY = "pro_expiry_time"
    }
}