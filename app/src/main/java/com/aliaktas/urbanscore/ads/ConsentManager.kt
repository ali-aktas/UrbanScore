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
    private val consentInformation: ConsentInformation = UserMessagingPlatform.getConsentInformation(context)
    private var consentForm: ConsentForm? = null

    // Kullanıcı onayı durumunu kontrol et ve gerekirse formu göster
    // ConsentManager.kt içinde updateConsent fonksiyonunu güvenli hale getir
    fun checkAndRequestConsent(activity: Activity, onConsentGathered: () -> Unit) {
        try {
            Log.d(TAG, "GDPR onay kontrolü başlıyor")

            // Debug modda test için onay durumunu sıfırla
            // SORUN OLABİLİR: Her seferinde resetleme yapmak yerine bir kontrol ekle
            if (BuildConfig.DEBUG) {
                try {
                    // Her seferinde resetleme - bu bir sorun olabilir!
                    // İsterseniz kaldırabilir veya koşula bağlayabilirsiniz
                    // resetConsent()
                    Log.d(TAG, "Debug modda resetConsent atlandı")
                } catch (e: Exception) {
                    Log.e(TAG, "resetConsent hatası: ${e.message}", e)
                }
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
                            Log.d(TAG, "Consent formu mevcut, gösteriliyor")
                            loadAndShowConsentForm(activity, onConsentGathered)
                        } else {
                            // Onay formu gerekli değil
                            Log.d(TAG, "Consent formu gerekli değil")
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

    // Onay formunu yükle ve göster
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
                                Log.e(TAG, "Consent form error: ${formError.message}")
                            } else {
                                Log.d(TAG, "Consent gathered successfully")
                            }
                            // Form gösterildikten sonra devam et
                            onConsentGathered()
                        }
                    }
                    else -> {
                        // Onay zaten alınmış veya gerekli değil
                        Log.d(TAG, "Consent already gathered: ${consentInformation.consentStatus}")
                        onConsentGathered()
                    }
                }
            },
            { error ->
                Log.e(TAG, "Consent form load failed: ${error.message}")
                onConsentGathered() // Hata durumunda devam et
            }
        )
    }

    // Onay durumunu sıfırla (sadece test için)
    fun resetConsent() {
        Log.d(TAG, "Resetting consent state")
        consentInformation.reset()
    }

    // Onay parametrelerini oluştur
    private fun buildConsentRequestParameters(): ConsentRequestParameters {
        val builder = ConsentRequestParameters.Builder()
            .setTagForUnderAgeOfConsent(false)

        // Debug modda test etmek için ek ayarlar
        if (BuildConfig.DEBUG) {
            val debugSettings = ConsentDebugSettings.Builder(context)
                .setDebugGeography(ConsentDebugSettings.DebugGeography.DEBUG_GEOGRAPHY_EEA)
                .build()
            builder.setConsentDebugSettings(debugSettings)
        }

        return builder.build()
    }

    // Reklam gösterimi için onay durumunu kontrol et
    fun canShowPersonalizedAds(): Boolean {
        return consentInformation.canRequestAds()
    }

    companion object {
        private const val TAG = "ConsentManager"
    }
}