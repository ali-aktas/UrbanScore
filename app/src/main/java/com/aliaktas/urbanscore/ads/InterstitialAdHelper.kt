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
        if (interstitialAd != null) {
            Log.d(TAG, "Interstitial reklam zaten yüklü, yükleme atlanıyor")
            return
        }

        Log.d(TAG, "Interstitial reklam yükleniyor")
        val adRequest = AdRequest.Builder().build()

        InterstitialAd.load(
            context,
            // ÖNEMLİ DEĞİŞİKLİK: Her zaman test ID'sini kullan
            TEST_INTERSTITIAL_AD_UNIT_ID,
            adRequest,
            object : InterstitialAdLoadCallback() {
                override fun onAdLoaded(ad: InterstitialAd) {
                    Log.d(TAG, "Interstitial reklam başarıyla yüklendi")
                    interstitialAd = ad
                }

                override fun onAdFailedToLoad(error: LoadAdError) {
                    Log.e(TAG, "Interstitial reklam yüklenemedi: ${error.message}")
                    interstitialAd = null
                }
            }
        )
    }

    /**
     * Interstitial reklamı gösterir.
     */
    fun showAd(activity: Activity, onAdClosed: () -> Unit) {
        Log.d(TAG, "showAd çağrıldı, interstitialAd ${if (interstitialAd != null) "yüklü" else "yüklü değil"}")

        if (interstitialAd == null) {
            Log.d(TAG, "Gösterilecek interstitial reklam yok, yeni reklam yükleniyor ve onAdClosed çağrılıyor")
            loadAd()
            onAdClosed()
            return
        }

        // Reklam gösterilmeden önce callback'i ayarla
        interstitialAd?.fullScreenContentCallback = object : FullScreenContentCallback() {
            override fun onAdDismissedFullScreenContent() {
                Log.d(TAG, "Interstitial reklam kapatıldı")
                interstitialAd = null
                loadAd()  // Sonraki kullanım için yeni reklam yükle
                onAdClosed()
            }

            override fun onAdFailedToShowFullScreenContent(adError: AdError) {
                Log.e(TAG, "Interstitial reklam gösterilemedi: ${adError.message}")
                interstitialAd = null
                loadAd()  // Başka bir reklam yüklemeyi dene
                onAdClosed()
            }

            override fun onAdShowedFullScreenContent() {
                Log.d(TAG, "Interstitial reklam başarıyla gösterildi")
            }
        }

        Log.d(TAG, "Interstitial reklam gösterme girişimi")
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