package com.aliaktas.urbanscore.ads

import android.content.Context
import android.util.Log
import com.aliaktas.urbanscore.BuildConfig
import com.aliaktas.urbanscore.util.PreferenceManager
import com.aliaktas.urbanscore.util.RevenueCatManager
import com.google.android.gms.ads.AdRequest
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


    fun initialize() {
        try {
            Log.d(TAG, "AdManager is starting")

            // Debug modda test cihazlarını kullan, release modda kullanma
            if (BuildConfig.DEBUG) {
                // Sadece debug build'de test cihazlarını ekle ve logla
                logTestDeviceId()
                val testDeviceIds = listOf("B61A84F9F07EDF07D5D6F290DD880708", "123456ABCDEF")
                val configuration = RequestConfiguration.Builder()
                    .setTestDeviceIds(testDeviceIds)
                    .build()
                MobileAds.setRequestConfiguration(configuration)
            } else {
                // Release modda test cihazları kullanma
                val configuration = RequestConfiguration.Builder().build()
                MobileAds.setRequestConfiguration(configuration)
            }

            MobileAds.initialize(context) { status ->
                // Debug modda log kaydı tut
                if (BuildConfig.DEBUG) {
                    Log.d(TAG, "AdMob başlatma tamamlandı. Durum: $status")
                }

                // Başlangıçta reklamları yükle
                preloadAds()
            }

            // Başlangıç Pro durumunu logla (sadece debug modda)
            if (BuildConfig.DEBUG) {
                Log.d(TAG, "Başlangıç isPro değeri: $isPro")
            }

            // Premium durumunu dinle
            CoroutineScope(Dispatchers.Main).launch {
                if (BuildConfig.DEBUG) {
                    Log.d(TAG, "isPremium akışı dinleniyor")
                }

                try {
                    revenueCatManager.isPremium.collect { premiumStatus ->
                        if (BuildConfig.DEBUG) {
                            Log.d(TAG, "Premium durum güncellendi: $premiumStatus")
                        }

                        isPro = premiumStatus

                        if (BuildConfig.DEBUG) {
                            Log.d(TAG, "AdManager'da premium durum güncellendi: $isPro")
                        }

                        // Eğer Pro durumu false ise reklamları yükle
                        if (!isPro) {
                            preloadAds()
                        }
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Premium durum dinlenirken hata", e)
                }
            }

        } catch (e: Exception) {
            Log.e(TAG, "AdMob başlatma hatası", e)
        }
    }

    private fun logTestDeviceId() {
        try {
            // Bu kısmı mevcut initialize() metodunuza ekleyin
            val adRequest = AdRequest.Builder().build()
            Log.d(TAG, "Test Device ID için logları kontrol edin. 'Test device ID:' ifadesini arayın")

            // Bu satır, cihazınızın test ID'sini loglar
            MobileAds.setRequestConfiguration(
                RequestConfiguration.Builder()
                    .setTestDeviceIds(listOf(AdRequest.DEVICE_ID_EMULATOR))
                    .build()
            )
        } catch (e: Exception) {
            Log.e(TAG, "Test cihaz ID'si alınamadı", e)
        }
    }

    // recordCityVisit metodunu güncelleyelim
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

    // showInterstitialAd metodunu güncelleyelim
    fun showInterstitialAd(activity: android.app.Activity, onAdClosed: () -> Unit) {
        Log.d(TAG, "showInterstitialAd çağrıldı, isPro: $isPro")
        if (isPro) {
            Log.d(TAG, "Kullanıcı Pro, reklam gösterimi atlanıyor ve onAdClosed çağrılıyor")
            onAdClosed()
            return
        }

        Log.d(TAG, "Interstitial reklam gösteriliyor")
        interstitialAdHelper.showAd(activity, onAdClosed)
    }

    // preloadAds metodunu güncelleyelim
    private fun preloadAds() {
        Log.d(TAG, "preloadAds çağrıldı, isPro: $isPro")
        if (isPro) {
            Log.d(TAG, "Kullanıcı Pro, reklam ön yüklemesi atlanıyor")
            return
        }

        Log.d(TAG, "Interstitial ve ödüllü reklamlar ön yükleniyor")
        interstitialAdHelper.loadAd()
        rewardedAdHelper.loadAd()
    }


    /**
     * Banner reklam döndürür.
     */
    fun getBannerAd() = if (!isPro) bannerAdHelper.getBannerAd() else null

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