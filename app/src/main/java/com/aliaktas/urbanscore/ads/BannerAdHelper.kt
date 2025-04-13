package com.aliaktas.urbanscore.ads

import android.content.Context
import android.util.Log
import com.aliaktas.urbanscore.BuildConfig
import com.google.android.gms.ads.AdListener
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.AdSize
import com.google.android.gms.ads.AdView
import com.google.android.gms.ads.LoadAdError

/**
 * Banner reklamlar için yardımcı sınıf.
 */
class BannerAdHelper(private val context: Context) {
    private var adView: AdView? = null
    private var isAdLoaded = false

    /**
     * Banner reklamı döndürür, yoksa yükler.
     */
    fun getBannerAd(): AdView? {
        if (adView == null) {
            createAndLoadBannerAd()
        }

        return adView
    }

    /**
     * Banner reklamı oluşturur ve yükler.
     */
    private fun createAndLoadBannerAd() {
        adView = AdView(context).apply {
            // Debug modda test ID, release modda gerçek ID kullan
            adUnitId = if (BuildConfig.DEBUG) TEST_BANNER_AD_UNIT_ID else BANNER_AD_UNIT_ID
            setAdSize(AdSize.BANNER)

            adListener = object : AdListener() {
                override fun onAdLoaded() {
                    isAdLoaded = true
                    Log.d(TAG, "Banner ad loaded successfully")
                }

                override fun onAdFailedToLoad(error: LoadAdError) {
                    isAdLoaded = false
                    Log.e(TAG, "Banner ad failed to load: ${error.message}")
                }
            }

            // Reklamı yükle
            loadAd(AdRequest.Builder().build())
        }
    }

    companion object {
        private const val TAG = "BannerAdHelper"

        // Test banner reklam ID
        private const val TEST_BANNER_AD_UNIT_ID = "ca-app-pub-3940256099942544/6300978111"

        // Gerçek banner reklam ID
        private const val BANNER_AD_UNIT_ID = "ca-app-pub-5728309332567964/3263573107"
    }
}