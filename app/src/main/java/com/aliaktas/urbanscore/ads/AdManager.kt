package com.aliaktas.urbanscore.ads

import android.content.Context
import android.util.Log
import com.aliaktas.urbanscore.util.PreferenceManager
import com.aliaktas.urbanscore.util.RevenueCatManager
import com.google.android.gms.ads.MobileAds
import com.google.android.gms.ads.RequestConfiguration
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Merkezi reklam yönetim sınıfı.
 * Tüm reklam türlerini yönetir ve koordine eder.
 */
@Singleton
class AdManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val preferenceManager: PreferenceManager
) {
    // Banner reklam yardımcı sınıfı
    private val bannerAdHelper = BannerAdHelper(context)

    // Interstitial reklam yardımcı sınıfı
    private val interstitialAdHelper = InterstitialAdHelper(context)

    // Ödüllü reklam yardımcı sınıfı
    private val rewardedAdHelper = RewardedAdHelper(context)

    // Ziyaret edilen şehir sayısı
    private var cityVisitCount = 0

    // RevenueCat manager'a referans
    private val revenueCatManager by lazy { RevenueCatManager.getInstance() }

    // Pro kullanıcı durumu için değişken - başlangıçta false
    private var isPro: Boolean = false

    /**
     * AdMob'u başlatır ve temel yapılandırmayı yapar.
     */
    fun initialize() {
        try {
            val testDeviceIds = listOf("ABCDEF012345", "123456ABCDEF")
            val configuration = RequestConfiguration.Builder()
                .setTestDeviceIds(testDeviceIds)
                .build()

            MobileAds.setRequestConfiguration(configuration)
            MobileAds.initialize(context) { status ->
                Log.d(TAG, "AdMob initialization completed. Status: $status")

                // Önceden reklamları yükle
                preloadAds()
            }

            // Premium durumunu dinle
            CoroutineScope(Dispatchers.Main).launch {
                revenueCatManager.isPremium.collect { premiumStatus ->
                    isPro = premiumStatus
                    Log.d(TAG, "Premium status updated: $isPro")
                }
            }

        } catch (e: Exception) {
            Log.e(TAG, "AdMob initialization failed", e)
        }
    }

    /**
     * Banner reklam döndürür.
     */
    fun getBannerAd() = if (!isPro) bannerAdHelper.getBannerAd() else null

    /**
     * Interstitial reklamı gösterir.
     */
    fun showInterstitialAd(activity: android.app.Activity, onAdClosed: () -> Unit) {
        if (isPro) {
            onAdClosed()
            return
        }

        interstitialAdHelper.showAd(activity, onAdClosed)
    }

    /**
     * Ödüllü reklamı gösterir.
     */
    fun showRewardedAd(
        activity: android.app.Activity,
        onRewarded: () -> Unit,
        onAdClosed: () -> Unit
    ) {
        if (isPro) {
            onRewarded()
            onAdClosed()
            return
        }

        rewardedAdHelper.showAd(activity, onRewarded, onAdClosed)
    }

    /**
     * Şehir ziyaretini kaydeder ve belirli sayıya ulaşıldığında true döner.
     */
    fun recordCityVisit(): Boolean {
        if (isPro) return false

        cityVisitCount++
        if (cityVisitCount >= CITY_VISIT_THRESHOLD) {
            cityVisitCount = 0
            return true
        }
        return false
    }

    /**
     * Reklamları önceden yükler.
     */
    private fun preloadAds() {
        if (isPro) return

        interstitialAdHelper.loadAd()
        rewardedAdHelper.loadAd()
    }

    /**
     * Pro abonelik sayfasına gitme önerisi gösterilmeli mi?
     * Eğer kullanıcı Pro değilse ve gerekli şartlar sağlanıyorsa true döner.
     */
    fun shouldSuggestProSubscription(): Boolean {
        // Kullanıcı zaten Pro ise öneri gösterme
        if (isPro) return false

        // Gerekli koşulları kontrol et (örneğin: belirli bir sayıda reklam görüntülendi mi?)
        return cityVisitCount >= CITY_VISIT_THRESHOLD - 1
    }

    companion object {
        private const val TAG = "AdManager"
        private const val CITY_VISIT_THRESHOLD = 5
    }
}