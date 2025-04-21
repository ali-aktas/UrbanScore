package com.aliaktas.urbanscore.ads

import android.app.Activity
import android.content.Context
import android.util.Log
import com.aliaktas.urbanscore.BuildConfig
import com.google.android.ump.ConsentDebugSettings
import com.google.android.ump.ConsentForm
import com.google.android.ump.ConsentInformation
import com.google.android.ump.ConsentRequestParameters
import com.google.android.ump.UserMessagingPlatform

/**
 * GDPR onayını yöneten sınıf
 */
class ConsentManager(private val context: Context) {
    private val TAG = "ConsentManager"
    private val consentInformation: ConsentInformation = UserMessagingPlatform.getConsentInformation(context)
    private var consentForm: ConsentForm? = null

    /**
     * Kullanıcı onayı durumunu kontrol et ve gerekirse formu göster
     */
    fun checkAndRequestConsent(activity: Activity, onConsentGathered: () -> Unit) {
        try {
            if (BuildConfig.ENABLE_LOGS) Log.d(TAG, "GDPR onay kontrolü başlıyor")

            // Debug modunda uygulamayı yeniden başlattığınızda test için consent formunu sıfırlayalım
            if (BuildConfig.DEBUG) {
                Log.d(TAG, "Debug modunda - Test için consent durumu sıfırlanıyor")
                consentInformation.reset()
            }

            // Onay parametrelerini oluştur
            val params = buildConsentRequestParameters()

            // Onay durumunu güncelle
            consentInformation.requestConsentInfoUpdate(
                activity,
                params,
                { // Güncelleme başarılı
                    try {
                        if (consentInformation.isConsentFormAvailable) {
                            if (BuildConfig.ENABLE_LOGS) Log.d(TAG, "Consent formu mevcut, gösteriliyor")
                            loadAndShowConsentForm(activity, onConsentGathered)
                        } else {
                            // Onay formu gerekli değil
                            if (BuildConfig.ENABLE_LOGS) Log.d(TAG, "Consent formu gerekli değil")
                            onConsentGathered()
                        }
                    } catch (e: Exception) {
                        Log.e(TAG, "Consent form işleme hatası: ${e.message}", e)
                        onConsentGathered() // Hata olsa bile devam et
                    }
                },
                { error -> // Güncelleme başarısız
                    Log.e(TAG, "Consent bilgisi güncellenemedi: ${error.message}")
                    onConsentGathered() // Hata olsa bile devam et
                }
            )
        } catch (e: Exception) {
            Log.e(TAG, "GDPR onay kontrolünde genel hata: ${e.message}", e)
            onConsentGathered() // Hata olsa bile devam et
        }
    }

    /**
     * Onay formunu yükle ve göster
     */
    private fun loadAndShowConsentForm(activity: Activity, onConsentGathered: () -> Unit) {
        UserMessagingPlatform.loadConsentForm(
            context,
            { form ->
                consentForm = form

                when (consentInformation.consentStatus) {
                    ConsentInformation.ConsentStatus.REQUIRED -> {
                        // Onay gerekli, formu göster
                        form.show(activity) { formError ->
                            if (formError != null) {
                                Log.e(TAG, "Consent form hatası: ${formError.message}")
                            } else {
                                if (BuildConfig.ENABLE_LOGS) Log.d(TAG, "Consent başarıyla alındı")
                            }
                            // Form gösterildikten sonra devam et
                            onConsentGathered()
                        }
                    }
                    else -> {
                        // Onay zaten alınmış veya gerekli değil
                        if (BuildConfig.ENABLE_LOGS) Log.d(TAG, "Consent zaten alınmış: ${consentInformation.consentStatus}")
                        onConsentGathered()
                    }
                }
            },
            { error ->
                Log.e(TAG, "Consent form yüklenemedi: ${error.message}")
                onConsentGathered() // Hata durumunda devam et
            }
        )
    }

    /**
     * Onay durumunu sıfırla (sadece test için)
     * Bu metodu doğrudan çağırmayın, geliştirme sırasında kullanılır.
     */
    fun resetConsent() {
        if (BuildConfig.DEBUG) {
            if (BuildConfig.ENABLE_LOGS) Log.d(TAG, "Consent durumu sıfırlanıyor")
            consentInformation.reset()
        }
    }

    /**
     * Onay parametrelerini oluştur
     */
    private fun buildConsentRequestParameters(): ConsentRequestParameters {
        val builder = ConsentRequestParameters.Builder()
            .setTagForUnderAgeOfConsent(false)

        // Debug modda test etmek için ek ayarlar
        if (BuildConfig.DEBUG) {
            val debugSettings = ConsentDebugSettings.Builder(context)
                .setDebugGeography(ConsentDebugSettings.DebugGeography.DEBUG_GEOGRAPHY_EEA) // Avrupa bölgesi simüle et
                .addTestDeviceHashedId("B61A84F9F07EDF07D5D6F290DD880708") // Test cihaz ID'sini ekleyin
                .build()
            builder.setConsentDebugSettings(debugSettings)
        }

        return builder.build()
    }

    // Bu metodu önceki haline getirin
    fun canShowPersonalizedAds(): Boolean {
        // Debug modunda her zaman true döndür
        if (BuildConfig.DEBUG) {
            Log.d(TAG, "canShowPersonalizedAds çağrıldı - TEST MODU, TRUE DÖNÜYORUZ")
            return true;
        }
        // Release modunda gerçek kontrolü yap
        return consentInformation.canRequestAds()
    }
}