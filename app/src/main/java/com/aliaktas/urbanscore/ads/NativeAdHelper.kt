package com.aliaktas.urbanscore.ads

import android.content.Context
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.widget.Button
import android.widget.ImageView
import android.widget.RatingBar
import android.widget.TextView
import com.aliaktas.urbanscore.BuildConfig
import com.aliaktas.urbanscore.R
import com.google.android.gms.ads.AdListener
import com.google.android.gms.ads.AdLoader
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.nativead.MediaView
import com.google.android.gms.ads.nativead.NativeAd
import com.google.android.gms.ads.nativead.NativeAdOptions
import com.google.android.gms.ads.nativead.NativeAdView
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Native reklamlar için yardımcı sınıf.
 */
@Singleton
class NativeAdHelper @Inject constructor(private val context: Context) {
    private val TAG = "NativeAdHelper"

    // Native Ad'in cache'lenmesi için
    private var cachedNativeAd: NativeAd? = null

    // Test Native Ad ID
    private val TEST_NATIVE_AD_UNIT_ID = "ca-app-pub-3940256099942544/2247696110"

    // Gerçek Native Ad ID
    private val NATIVE_AD_UNIT_ID = "ca-app-pub-5728309332567964/XXXXXXXX" // Gerçek ID'nizi buraya yazın

    /**
     * Native reklamı yükler ve callback ile bildirir
     */
    fun loadNativeAd(onAdLoaded: (NativeAd) -> Unit, onAdFailed: () -> Unit) {
        // Eğer cache'lenmiş reklam varsa, onu kullan
        cachedNativeAd?.let {
            Log.d(TAG, "Cache'lenmiş Native Ad kullanılıyor")
            onAdLoaded(it)
            return
        }

        // AdLoader Builder oluştur
        val adUnitId = if (BuildConfig.DEBUG) TEST_NATIVE_AD_UNIT_ID else NATIVE_AD_UNIT_ID

        val builder = AdLoader.Builder(context, adUnitId)

        // Native Ad Listener
        builder.forNativeAd { nativeAd ->
            Log.d(TAG, "Native Ad yüklendi")
            // Cache'e kaydet
            cachedNativeAd = nativeAd
            onAdLoaded(nativeAd)
        }

        // AdLoader options
        val adOptions = NativeAdOptions.Builder()
            .setAdChoicesPlacement(NativeAdOptions.ADCHOICES_TOP_RIGHT)
            .setMediaAspectRatio(NativeAdOptions.NATIVE_MEDIA_ASPECT_RATIO_LANDSCAPE)
            .build()

        builder.withNativeAdOptions(adOptions)

        // AdListener
        builder.withAdListener(object : AdListener() {
            override fun onAdFailedToLoad(error: LoadAdError) {
                Log.e(TAG, "Native Ad yüklenemedi: ${error.message}")
                onAdFailed()
            }
        })

        // AdLoader oluştur ve yükleme başlat
        val adLoader = builder.build()
        adLoader.loadAd(AdRequest.Builder().build())
        Log.d(TAG, "Native Ad yükleme başlatıldı")
    }

    /**
     * Verilen NativeAd'i NativeAdView içine yerleştirerek hazırlar
     */
    fun populateNativeAdView(nativeAd: NativeAd, adView: NativeAdView) {
        try {
            // Ad bileşenlerini bul
            val mediaView = adView.findViewById<MediaView>(R.id.ad_media)
            val headlineView = adView.findViewById<TextView>(R.id.ad_headline)
            val bodyView = adView.findViewById<TextView>(R.id.ad_body)
            val callToActionView = adView.findViewById<Button>(R.id.ad_call_to_action)
            val iconView = adView.findViewById<ImageView>(R.id.ad_app_icon)
            val starRatingView = adView.findViewById<RatingBar>(R.id.ad_stars)
            val advertiserView = adView.findViewById<TextView>(R.id.ad_advertiser)

            // MediaView ayarla (en önemli görsel öğe)
            adView.mediaView = mediaView

            // Başlık ayarla
            adView.headlineView = headlineView
            headlineView?.text = nativeAd.headline

            // İçerik ayarla
            adView.bodyView = bodyView
            bodyView?.text = nativeAd.body ?: ""

            // CTA butonu ayarla
            adView.callToActionView = callToActionView
            callToActionView?.text = nativeAd.callToAction ?: ""

            // İkon ayarla
            adView.iconView = iconView
            if (nativeAd.icon != null) {
                iconView?.visibility = View.VISIBLE
                iconView?.setImageDrawable(nativeAd.icon?.drawable)
            } else {
                iconView?.visibility = View.GONE
            }

            // Yıldız derecelendirmesi ayarla
            adView.starRatingView = starRatingView
            if (nativeAd.starRating != null && nativeAd.starRating!! > 0) {
                starRatingView?.visibility = View.VISIBLE
                starRatingView?.rating = nativeAd.starRating!!.toFloat()
            } else {
                starRatingView?.visibility = View.GONE
            }

            // Reklamveren ayarla
            adView.advertiserView = advertiserView
            if (nativeAd.advertiser != null) {
                advertiserView?.visibility = View.VISIBLE
                advertiserView?.text = nativeAd.advertiser
            } else {
                advertiserView?.visibility = View.GONE
            }

            // NativeAdView'a reklamı kaydet - bu adım çok önemli!
            adView.setNativeAd(nativeAd)

            Log.d(TAG, "Native Ad görünüme yerleştirildi")
        } catch (e: Exception) {
            Log.e(TAG, "Native Ad görünüme yerleştirilirken hata: ${e.message}", e)
        }
    }

    /**
     * Cache'lenmiş reklamı temizler
     */
    fun clearCachedAd() {
        cachedNativeAd?.destroy()
        cachedNativeAd = null
    }
}