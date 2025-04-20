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
    private val TAG = "AdManager_Test"

    // Banner reklam yardımcı sınıfı
    private val bannerAdHelper = BannerAdHelper(context)

    // Interstitial reklam yardımcı sınıfı
    private val interstitialAdHelper = InterstitialAdHelper(context)

    // Ödüllü reklam yardımcı sınıfı
    private val rewardedAdHelper = RewardedAdHelper(context)

    // GDPR onay yöneticisi
    private val consentManager = ConsentManager(context)

    private val nativeAdHelper = NativeAdHelper(context)

    // Ziyaret edilen şehir sayısı
    private var cityVisitCount = 0

    // Pro kullanıcı durumu için değişken - TEST İÇİN FALSE
    private var isPro: Boolean = false

    /**
     * AdManager'ı GDPR onayı ile başlat (ana metot)
     */
    fun initialize(activity: Activity, onInitialized: () -> Unit = {}) {
        try {
            Log.d(TAG, "AdManager başlatılıyor - TEST MODU")

            // GDPR kontrolünü atlıyoruz, doğrudan reklamları başlat
            initializeAds(onInitialized)

            // Normalde bu kısım GDPR onayı sonrası çalışırdı
            // consentManager.checkAndRequestConsent(activity) {
            //    initializeAds(onInitialized)
            // }
        } catch (e: Exception) {
            Log.e(TAG, "AdManager.initialize hatası: ${e.message}", e)
            onInitialized() // Hata olsa bile devam et
        }
    }

    // AdManager.kt içine yeni metot ekle
    /**
     * Native reklam yükleme işlemini başlatır ve callback ile sonucu bildirir
     */
    fun loadNativeAd(
        onAdLoaded: (NativeAd) -> Unit,
        onAdFailed: () -> Unit
    ) {
        Log.d(TAG, "loadNativeAd çağrıldı, isPro: $isPro")

        // TEST MODU: Native Ad yükleme
        nativeAdHelper.loadNativeAd(onAdLoaded, onAdFailed)

        // Normal çalışma modunda:
        // if (isPro || !canShowAds()) {
        //     Log.d(TAG, "Pro kullanıcı veya reklam onayı yok, Native Ad atlanıyor")
        //     onAdFailed()
        //     return
        // }
        // nativeAdHelper.loadNativeAd(onAdLoaded, onAdFailed)
    }

    fun populateNativeAdView(nativeAd: NativeAd, adView: NativeAdView) {
        nativeAdHelper.populateNativeAdView(nativeAd, adView)
    }

    /**
     * Reklamları başlat
     */
    private fun initializeAds(onInitialized: () -> Unit = {}) {
        try {
            Log.d(TAG, "AdManager reklamlar başlatılıyor")

            // HER DURUMDA test cihazlarını kullan
            val testDeviceIds = listOf(
                AdRequest.DEVICE_ID_EMULATOR,
                "33BE2250B43518CCDA7DE426D04EE231", // Genel test ID
                "A74E9D91458A3D4A90127DD6C96D32C3"  // Başka bir genel test ID
            )

            val configuration = RequestConfiguration.Builder()
                .setTestDeviceIds(testDeviceIds)
                .build()

            MobileAds.setRequestConfiguration(configuration)

            MobileAds.initialize(context) { status ->
                Log.d(TAG, "AdMob başlatma tamamlandı. Durum: $status")

                // Başlangıçta reklamları yükle
                preloadAds()
                onInitialized()
            }

            // Log pro durumunu
            Log.d(TAG, "isPro değeri (test için false): $isPro")

        } catch (e: Exception) {
            Log.e(TAG, "AdMob başlatma hatası", e)
            onInitialized()
        }
    }

    // Test cihaz ID'si için yardımcı metot
    private fun logTestDeviceId() {
        try {
            val adRequest = AdRequest.Builder().build()
            Log.d(TAG, "Test Device ID için logları kontrol edin. 'Test device ID:' ifadesini arayın")
        } catch (e: Exception) {
            Log.e(TAG, "Test cihaz ID'si alınamadı", e)
        }
    }

    // Şehir ziyareti sayacı
    fun recordCityVisit(): Boolean {
        Log.d(TAG, "recordCityVisit çağrıldı, isPro: $isPro, mevcut sayaç: $cityVisitCount")
        if (isPro) {
            Log.d(TAG, "Kullanıcı Pro, şehir ziyareti kaydı atlanıyor")
            return false
        }

        cityVisitCount++
        Log.d(TAG, "Şehir ziyaret sayacı artırıldı: $cityVisitCount")
        if (cityVisitCount >= CITY_VISIT_THRESHOLD) {
            Log.d(TAG, "Şehir ziyareti eşiğine ulaşıldı, sayaç sıfırlanıyor ve true dönülüyor")
            cityVisitCount = 0
            return true
        }
        return false
    }

    // Interstitial reklam gösterimi
    fun showInterstitialAd(activity: Activity, onAdClosed: () -> Unit) {
        Log.d(TAG, "showInterstitialAd çağrıldı, isPro: $isPro")

        // Test için her zaman göster
        Log.d(TAG, "TEST MODU: Interstitial reklam gösteriliyor")
        interstitialAdHelper.showAd(activity, onAdClosed)

        // Normal durumda bu şekilde:
        // if (isPro || !canShowAds()) {
        //    Log.d(TAG, "Kullanıcı Pro veya reklam onayı yok, reklam gösterimi atlanıyor")
        //    onAdClosed()
        //    return
        // }
    }

    // Reklam ön yüklemesi
    private fun preloadAds() {
        Log.d(TAG, "preloadAds çağrıldı, isPro: $isPro")

        // Test için her durumda yükle
        Log.d(TAG, "TEST MODU: Interstitial ve ödüllü reklamlar ön yükleniyor")
        interstitialAdHelper.loadAd()
        rewardedAdHelper.loadAd()

        // Normal durumda:
        // if (isPro || !canShowAds()) {
        //    Log.d(TAG, "Kullanıcı Pro veya reklam onayı yok, reklam ön yüklemesi atlanıyor")
        //    return
        // }
    }

    /**
     * Banner reklam döndürür.
     */
    fun getBannerAd(): AdView? {
        Log.d(TAG, "getBannerAd çağrıldı (TEST MODU)")
        return bannerAdHelper.getBannerAd()

        // Normal durumda:
        // return if (!isPro && canShowAds()) bannerAdHelper.getBannerAd() else null
    }

    /**
     * Ödüllü reklamı gösterir.
     */
    fun showRewardedAd(
        activity: Activity,
        onRewarded: () -> Unit,
        onAdClosed: () -> Unit
    ) {
        Log.d(TAG, "showRewardedAd çağrıldı (TEST MODU)")
        rewardedAdHelper.showAd(activity, onRewarded, onAdClosed)

        // Normal durumda:
        // if (isPro || !canShowAds()) {
        //    onRewarded()
        //    onAdClosed()
        //    return
        // }
    }

    /**
     * Pro abonelik sayfasına gitme önerisi gösterilmeli mi?
     */
    fun shouldSuggestProSubscription(): Boolean {
        // Geçici olarak false döndür
        return false

        // Normal durumda:
        // if (isPro) return false
        // return cityVisitCount >= CITY_VISIT_THRESHOLD - 1
    }

    /**
     * Reklam gösterimi için GDPR onayını kontrol et
     */
    private fun canShowAds(): Boolean {
        // TEST İÇİN HER ZAMAN TRUE
        Log.d(TAG, "canShowAds çağrıldı - TEST MODUNDA HER ZAMAN TRUE")
        return true

        // Normal durumda:
        // return consentManager.canShowPersonalizedAds()
    }

    companion object {
        private const val CITY_VISIT_THRESHOLD = 5
    }
}