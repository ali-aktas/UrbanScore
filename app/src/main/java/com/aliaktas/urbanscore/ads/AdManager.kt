package com.aliaktas.urbanscore.ads

import android.app.Activity
import android.content.Context
import android.util.Log
import com.aliaktas.urbanscore.BuildConfig
import com.aliaktas.urbanscore.util.PreferenceManager
import com.aliaktas.urbanscore.util.RevenueCatManager
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.AdView
import com.google.android.gms.ads.MobileAds
import com.google.android.gms.ads.RequestConfiguration
import com.google.android.gms.ads.nativead.NativeAd
import com.google.android.gms.ads.nativead.NativeAdView
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
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
    private val preferenceManager: PreferenceManager,
    private val revenueCatManager: RevenueCatManager  // RevenueCat enjekte ettik
) {
    private val TAG = "AdManager"

    // Banner reklam yardımcı sınıfı
    private val bannerAdHelper = BannerAdHelper(context)

    // Interstitial reklam yardımcı sınıfı
    private val interstitialAdHelper = InterstitialAdHelper(context)

    // Ödüllü reklam yardımcı sınıfı
    private val rewardedAdHelper = RewardedAdHelper(context)

    // GDPR onay yöneticisi
    private val consentManager = ConsentManager(context)

    // Native reklam yardımcı sınıfı
    private val nativeAdHelper = NativeAdHelper(context)

    // Ziyaret edilen şehir sayısı
    private var cityVisitCount = 0

    // Pro kullanıcı durumu için değişken
    private var isPro: Boolean = false

    // Coroutine scope
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    init {
        // Shared Preferences'dan Pro durumunu oku (başlangıç durumu)
        isPro = preferenceManager.isProValid()

        // RevenueCat durumunu takip et
        observePremiumStatus()
    }

    private fun observePremiumStatus() {
        scope.launch {
            revenueCatManager.isPremium.collect { premium ->
                if (isPro != premium) {
                    Log.d(TAG, "Premium durumu değişti: $premium")
                    isPro = premium

                    // Preferences'a kaydet
                    preferenceManager.setProUser(isPro)

                    if (isPro) {
                        // Cache'lenmiş reklamları temizle
                        nativeAdHelper.clearCachedAd()
                    }
                }
            }
        }
    }

    fun initialize(activity: Activity, onInitialized: () -> Unit = {}) {
        try {
            if (BuildConfig.ENABLE_LOGS) Log.d(TAG, "AdManager başlatılıyor")

            // GDPR onayını kontrol et ve sonrasında reklamları başlat
            consentManager.checkAndRequestConsent(activity) {
                initializeAds(onInitialized)
            }
        } catch (e: Exception) {
            Log.e(TAG, "AdManager.initialize hatası: ${e.message}", e)
            onInitialized() // Hata olsa bile devam et
        }
    }

    /**
     * Native reklam yükleme işlemini başlatır ve callback ile sonucu bildirir
     */
    fun loadNativeAd(
        onAdLoaded: (NativeAd) -> Unit,
        onAdFailed: () -> Unit
    ) {
        if (BuildConfig.ENABLE_LOGS) Log.d(TAG, "loadNativeAd is called, isPro: $isPro")

        if (isPro || !canShowAds()) {
            if (BuildConfig.ENABLE_LOGS) Log.d(TAG, "Pro user or no ad approval, Native Ad bypassed")
            onAdFailed()
            return
        }
        nativeAdHelper.loadNativeAd(onAdLoaded, onAdFailed)
    }

    fun populateNativeAdView(nativeAd: NativeAd, adView: NativeAdView) {
        nativeAdHelper.populateNativeAdView(nativeAd, adView)
    }

    /**
     * Reklamları başlat
     */
    private fun initializeAds(onInitialized: () -> Unit = {}) {
        try {
            if (BuildConfig.ENABLE_LOGS) Log.d(TAG, "AdManager ads are starting")

            // Debug modunda test cihazlarını kullan
            if (BuildConfig.DEBUG) {
                val testDeviceIds = listOf(
                    AdRequest.DEVICE_ID_EMULATOR,
                    "33BE2250B43518CCDA7DE426D04EE231", // Genel test ID
                    "A74E9D91458A3D4A90127DD6C96D32C3"  // Başka bir genel test ID
                )

                val configuration = RequestConfiguration.Builder()
                    .setTestDeviceIds(testDeviceIds)
                    .build()

                MobileAds.setRequestConfiguration(configuration)
            }

            MobileAds.initialize(context) { status ->
                if (BuildConfig.ENABLE_LOGS) Log.d(TAG, "AdMob started")

                // Başlangıçta reklamları yükle
                preloadAds()
                onInitialized()
            }

            // Log pro durumunu
            if (BuildConfig.ENABLE_LOGS) Log.d(TAG, "isPro value: $isPro")

        } catch (e: Exception) {
            Log.e(TAG, "AdMob start error", e)
            onInitialized()
        }
    }

    // Şehir ziyareti sayacı
    fun recordCityVisit(): Boolean {
        if (BuildConfig.ENABLE_LOGS) Log.d(TAG, "Recording a city visit, isPro: $isPro")

        if (isPro) {
            return false
        }

        cityVisitCount++
        if (cityVisitCount >= CITY_VISIT_THRESHOLD) {
            cityVisitCount = 0
            return true
        }
        return false
    }

    // Interstitial reklam gösterimi
    fun showInterstitialAd(activity: Activity, onAdClosed: () -> Unit) {
        if (isPro || !canShowAds()) {
            if (BuildConfig.ENABLE_LOGS) Log.d(TAG, "Pro user or no ad approval, skipping ad display")
            onAdClosed()
            return
        }

        interstitialAdHelper.showAd(activity, onAdClosed)
    }

    // Reklam ön yüklemesi
    private fun preloadAds() {
        if (isPro || !canShowAds()) {
            if (BuildConfig.ENABLE_LOGS) Log.d(TAG, "Pro user or no ad approval, skipping ad preload")
            return
        }

        interstitialAdHelper.loadAd()
        rewardedAdHelper.loadAd()
    }

    /**
     * Banner reklam döndürür.
     */
    fun getBannerAd(): AdView? {
        return if (!isPro && canShowAds()) bannerAdHelper.getBannerAd() else null
    }

    /**
     * Ödüllü reklamı gösterir.
     */
    fun showRewardedAd(
        activity: Activity,
        onRewarded: () -> Unit,
        onAdClosed: () -> Unit
    ) {
        if (isPro || !canShowAds()) {
            onRewarded()
            onAdClosed()
            return
        }

        rewardedAdHelper.showAd(activity, onRewarded, onAdClosed)
    }

    /**
     * Pro abonelik sayfasına gitme önerisi gösterilmeli mi?
     */
    fun shouldSuggestProSubscription(): Boolean {
        if (isPro) return false
        return cityVisitCount >= CITY_VISIT_THRESHOLD - 1
    }

    /**
     * Reklam gösterimi için GDPR onayını kontrol et
     */
    private fun canShowAds(): Boolean {
        return consentManager.canShowPersonalizedAds()
    }

    // Cleanup metodu - uygulama kapanırken çağrılabilir
    fun cleanup() {
        scope.cancel()
    }

    companion object {
        private const val CITY_VISIT_THRESHOLD = 5
    }
}