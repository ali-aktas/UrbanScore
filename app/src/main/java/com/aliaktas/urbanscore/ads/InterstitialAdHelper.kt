package com.aliaktas.urbanscore.ads

import android.app.Activity
import android.content.Context
import android.util.Log
import com.aliaktas.urbanscore.BuildConfig
import com.google.android.gms.ads.AdError
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.FullScreenContentCallback
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.interstitial.InterstitialAd
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback

/**
 * Interstitial reklamlar için yardımcı sınıf.
 */
class InterstitialAdHelper(private val context: Context) {
    private var interstitialAd: InterstitialAd? = null

    /**
     * Interstitial reklamı yükler.
     */
    fun loadAd() {
        if (interstitialAd != null) return

        val adRequest = AdRequest.Builder().build()

        InterstitialAd.load(
            context,
            if (BuildConfig.DEBUG) TEST_INTERSTITIAL_AD_UNIT_ID else INTERSTITIAL_AD_UNIT_ID,
            adRequest,
            object : InterstitialAdLoadCallback() {
                override fun onAdLoaded(ad: InterstitialAd) {
                    interstitialAd = ad
                    Log.d(TAG, "Interstitial ad loaded successfully")
                }

                override fun onAdFailedToLoad(error: LoadAdError) {
                    interstitialAd = null
                    Log.e(TAG, "Interstitial ad failed to load: ${error.message}")
                }
            }
        )
    }

    /**
     * Interstitial reklamı gösterir.
     */
    fun showAd(activity: Activity, onAdClosed: () -> Unit) {
        if (interstitialAd == null) {
            loadAd()
            onAdClosed()
            return
        }

        interstitialAd?.fullScreenContentCallback = object : FullScreenContentCallback() {
            override fun onAdDismissedFullScreenContent() {
                interstitialAd = null
                loadAd()  // Bir sonraki gösterim için yeni reklam yükle
                onAdClosed()
            }

            override fun onAdFailedToShowFullScreenContent(adError: AdError) {
                interstitialAd = null
                loadAd()
                onAdClosed()
                Log.e(TAG, "Interstitial ad failed to show: ${adError.message}")
            }
        }

        interstitialAd?.show(activity)
    }

    companion object {
        private const val TAG = "InterstitialAdHelper"

        // Test interstitial reklam ID
        private const val TEST_INTERSTITIAL_AD_UNIT_ID = "ca-app-pub-3940256099942544/1033173712"

        // Gerçek interstitial reklam ID - değiştirilecek
        private const val INTERSTITIAL_AD_UNIT_ID = "ca-app-pub-XXXXXXXXXXXX/YYYYYYYYYY"
    }
}