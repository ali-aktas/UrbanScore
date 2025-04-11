package com.aliaktas.urbanscore.ads

import android.app.Activity
import android.content.Context
import android.util.Log
import com.aliaktas.urbanscore.BuildConfig
import com.google.android.gms.ads.AdError
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.FullScreenContentCallback
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.rewarded.RewardedAd
import com.google.android.gms.ads.rewarded.RewardedAdLoadCallback

/**
 * Ödüllü reklamlar için yardımcı sınıf.
 */
class RewardedAdHelper(private val context: Context) {
    private var rewardedAd: RewardedAd? = null

    /**
     * Ödüllü reklamı yükler.
     */
    fun loadAd() {
        if (rewardedAd != null) return

        val adRequest = AdRequest.Builder().build()

        RewardedAd.load(
            context,
            // ÖNEMLİ DEĞİŞİKLİK: Her zaman test ID'sini kullan
            TEST_REWARDED_AD_UNIT_ID,
            adRequest,
            object : RewardedAdLoadCallback() {
                override fun onAdLoaded(ad: RewardedAd) {
                    rewardedAd = ad
                    Log.d(TAG, "Rewarded ad loaded successfully")
                }

                override fun onAdFailedToLoad(error: LoadAdError) {
                    rewardedAd = null
                    Log.e(TAG, "Rewarded ad failed to load: ${error.message}")
                }
            }
        )
    }

    /**
     * Ödüllü reklamı gösterir.
     */
    fun showAd(activity: Activity, onRewarded: () -> Unit, onAdClosed: () -> Unit) {
        if (rewardedAd == null) {
            loadAd()
            // Reklam yoksa ödülü direkt ver - kötü deneyim olmasın
            onRewarded()
            onAdClosed()
            return
        }

        rewardedAd?.fullScreenContentCallback = object : FullScreenContentCallback() {
            override fun onAdDismissedFullScreenContent() {
                rewardedAd = null
                loadAd()  // Bir sonraki gösterim için yeni reklam yükle
                onAdClosed()
            }

            override fun onAdFailedToShowFullScreenContent(adError: AdError) {
                rewardedAd = null
                loadAd()
                // Reklam gösterimi başarısız olduğunda ödülü yine de ver
                onRewarded()
                onAdClosed()
                Log.e(TAG, "Rewarded ad failed to show: ${adError.message}")
            }
        }

        rewardedAd?.show(activity) { rewardItem ->
            Log.d(TAG, "User rewarded with ${rewardItem.amount} ${rewardItem.type}")
            onRewarded()
        }
    }

    companion object {
        private const val TAG = "RewardedAdHelper"

        // Test ödüllü reklam ID
        private const val TEST_REWARDED_AD_UNIT_ID = "ca-app-pub-3940256099942544/5224354917"

        // Gerçek ödüllü reklam ID - değiştirilecek
        private const val REWARDED_AD_UNIT_ID = "ca-app-pub-XXXXXXXXXXXX/YYYYYYYYYY"
    }
}