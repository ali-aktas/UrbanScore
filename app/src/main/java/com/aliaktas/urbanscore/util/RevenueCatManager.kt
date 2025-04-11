package com.aliaktas.urbanscore.util

import android.app.Activity
import android.app.Application
import android.content.Intent
import android.net.Uri
import android.util.Log
import com.aliaktas.urbanscore.BuildConfig
import com.revenuecat.purchases.CustomerInfo
import com.revenuecat.purchases.Package
import com.revenuecat.purchases.PurchasesConfiguration
import com.revenuecat.purchases.Purchases
import com.revenuecat.purchases.PurchaseParams
import com.revenuecat.purchases.getCustomerInfoWith
import com.revenuecat.purchases.getOfferingsWith
import com.revenuecat.purchases.interfaces.UpdatedCustomerInfoListener
import com.revenuecat.purchases.models.StoreProduct
import com.revenuecat.purchases.purchaseWith
import com.revenuecat.purchases.restorePurchasesWith
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.text.SimpleDateFormat
import java.util.Locale

/**
 * RevenueCat abonelik yönetimi için merkezi sınıf.
 * "identifier" kullanımına bağlı olmayan, güvenli versiyonu.
 */
class RevenueCatManager private constructor(application: Application) {

    companion object {
        private const val TAG = "RevenueCatManager"

        // API Key
        private const val API_KEY = "goog_WVsSgMhECzHIssMxfcjkQZxWEpa"

        // Premium entitlement ismi
        private const val ENTITLEMENT_PREMIUM = "Premium"

        // Plan türleri - Bunlar iç kullanım için sembolik isimler
        const val PLAN_MONTHLY = "monthly"
        const val PLAN_YEARLY = "yearly"

        @Volatile
        private var INSTANCE: RevenueCatManager? = null

        fun initialize(application: Application) {
            if (INSTANCE == null) {
                synchronized(this) {
                    if (INSTANCE == null) {
                        INSTANCE = RevenueCatManager(application)
                    }
                }
            }
        }

        fun getInstance(): RevenueCatManager {
            return INSTANCE ?: throw IllegalStateException("RevenueCatManager must be initialized first")
        }
    }

    // Abonelik durumu
    private val _isPremium = MutableStateFlow(false)
    val isPremium: StateFlow<Boolean> = _isPremium.asStateFlow()

    // Paketler
    private val _monthlyPackage = MutableStateFlow<Package?>(null)
    val monthlyPackage: StateFlow<Package?> = _monthlyPackage.asStateFlow()

    private val _yearlyPackage = MutableStateFlow<Package?>(null)
    val yearlyPackage: StateFlow<Package?> = _yearlyPackage.asStateFlow()

    // Hata ve yükleme
    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    init {
        try {
            Log.d(TAG, "RevenueCat başlatılıyor")

            // RevenueCat yapılandırma
            val configuration = PurchasesConfiguration.Builder(application, API_KEY)
                .observerMode(false)
                .build()

            Purchases.configure(configuration)
            Purchases.debugLogsEnabled = BuildConfig.DEBUG

            // Purchase listener
            setupPurchaseListener()

            // Başlangıç kontrolleri
            checkPremiumStatus()
            refreshPackages()

        } catch (e: Exception) {
            Log.e(TAG, "RevenueCat başlatma hatası", e)
            _error.value = "RevenueCat başlatılamadı: ${e.message}"
        }
    }

    /**
     * Satın alma durumundaki değişiklikleri dinler
     */
    private fun setupPurchaseListener() {
        try {
            Purchases.sharedInstance.updatedCustomerInfoListener = UpdatedCustomerInfoListener { customerInfo ->
                Log.d(TAG, "CustomerInfo güncellendi")
                updatePremiumStatus(customerInfo)
            }
            Log.d(TAG, "Satın alma listener'ı kuruldu")
        } catch (e: Exception) {
            Log.e(TAG, "Listener kurulumu hatası", e)
        }
    }

    /**
     * Premium durumunu kontrol eder
     */
    fun checkPremiumStatus() {
        _isLoading.value = true
        Log.d(TAG, "Premium durumu kontrol ediliyor")

        try {
            Purchases.sharedInstance.getCustomerInfoWith(
                onError = { error ->
                    Log.e(TAG, "CustomerInfo alınamadı: ${error.message}")
                    _error.value = "Abonelik bilgisi alınamadı: ${error.message}"
                    _isLoading.value = false
                },
                onSuccess = { customerInfo ->
                    Log.d(TAG, "CustomerInfo alındı")
                    updatePremiumStatus(customerInfo)
                    _isLoading.value = false
                }
            )
        } catch (e: Exception) {
            Log.e(TAG, "Premium kontrol hatası", e)
            _error.value = "Abonelik durumu kontrolü başarısız: ${e.message}"
            _isLoading.value = false
        }
    }

    /**
     * Premium durumunu günceller
     */
    private fun updatePremiumStatus(customerInfo: CustomerInfo) {
        try {
            val entitlement = customerInfo.entitlements[ENTITLEMENT_PREMIUM]
            val isActive = entitlement?.isActive == true

            if (_isPremium.value != isActive) {
                Log.d(TAG, "Premium durumu değişti: $isActive")
                _isPremium.value = isActive
            }

            if (isActive) {
                val expirationDate = entitlement?.expirationDate
                Log.d(TAG, "Premium aktif, bitiş: ${expirationDate ?: "belirsiz"}")
            } else {
                Log.d(TAG, "Premium aktif değil")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Premium durum güncelleme hatası", e)
        }
    }

    /**
     * Paketleri yeniler
     */
    fun refreshPackages() {
        _isLoading.value = true
        Log.d(TAG, "Paketler yükleniyor")

        try {
            Purchases.sharedInstance.getOfferingsWith(
                onError = { error ->
                    Log.e(TAG, "Offerings alınamadı: ${error.message}")
                    _error.value = "Abonelik seçenekleri yüklenemedi: ${error.message}"
                    _isLoading.value = false
                },
                onSuccess = { offerings ->
                    Log.d(TAG, "Offerings başarıyla alındı")

                    // Şu anda kullanılan offering
                    val currentOffering = offerings.current

                    if (currentOffering == null) {
                        Log.e(TAG, "Hiçbir offering bulunamadı!")
                        _error.value = "Abonelik paketi bulunamadı"
                        _isLoading.value = false
                        return@getOfferingsWith
                    }

                    // Paketleri işle
                    val allPackages = currentOffering.availablePackages
                    Log.d(TAG, "Paket sayısı: ${allPackages.size}")

                    // Her paketi logla - identifier kullanımı yok!
                    allPackages.forEachIndexed { index, pkg ->
                        try {
                            val price = pkg.product.price.formatted
                            val packageType = pkg.packageType.toString()
                            Log.d(TAG, "Paket $index: Tür=$packageType, Fiyat=$price")
                        } catch (e: Exception) {
                            Log.e(TAG, "Paket bilgisi loglama hatası", e)
                        }
                    }

                    // Aylık ve yıllık paketleri bul
                    var foundMonthly = false
                    var foundYearly = false

                    // İçerik tabanlı arama - identifier'a dayanmıyor
                    for (pkg in allPackages) {
                        try {
                            val packageType = pkg.packageType.toString().lowercase()

                            if (!foundMonthly && packageType.contains("month")) {
                                _monthlyPackage.value = pkg
                                foundMonthly = true
                                Log.d(TAG, "Aylık paket bulundu: Tür=$packageType")
                            }

                            if (!foundYearly && (packageType.contains("year") ||
                                        packageType.contains("annual"))) {
                                _yearlyPackage.value = pkg
                                foundYearly = true
                                Log.d(TAG, "Yıllık paket bulundu: Tür=$packageType")
                            }
                        } catch (e: Exception) {
                            Log.e(TAG, "Paket inceleme hatası", e)
                        }
                    }

                    // Hala bulunamadıysa ilk paketleri kullan
                    if (!foundMonthly && allPackages.isNotEmpty()) {
                        _monthlyPackage.value = allPackages[0]
                        Log.d(TAG, "Aylık paket için ilk paket kullanılıyor")
                    }

                    if (!foundYearly && allPackages.size > 1) {
                        _yearlyPackage.value = allPackages[1]
                        Log.d(TAG, "Yıllık paket için ikinci paket kullanılıyor")
                    }

                    // Sonuç
                    Log.d(TAG, "Aylık paket bulundu: ${_monthlyPackage.value != null}")
                    Log.d(TAG, "Yıllık paket bulundu: ${_yearlyPackage.value != null}")

                    _isLoading.value = false
                }
            )
        } catch (e: Exception) {
            Log.e(TAG, "Paket yenileme hatası", e)
            _error.value = "Abonelik seçenekleri yüklenirken hata: ${e.message}"
            _isLoading.value = false
        }
    }

    /**
     * Planı satın alır
     */
    fun purchasePlan(
        activity: Activity,
        planId: String,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        Log.d(TAG, "Satın alma başlatılıyor: $planId")
        _isLoading.value = true

        try {
            // Paket bulma
            val packageToUse = when (planId) {
                PLAN_MONTHLY -> _monthlyPackage.value
                PLAN_YEARLY -> _yearlyPackage.value
                else -> null
            }

            if (packageToUse == null) {
                val errorMsg = "Seçilen paket bulunamadı: $planId"
                Log.e(TAG, errorMsg)
                _error.value = errorMsg
                onError(errorMsg)
                _isLoading.value = false
                return
            }

            Log.d(TAG, "Satın alınacak paket: ${packageToUse.packageType}")

            // Satın alma işlemi
            val purchaseParams = PurchaseParams.Builder(activity, packageToUse).build()

            Purchases.sharedInstance.purchaseWith(
                purchaseParams = purchaseParams,
                onError = { error, userCancelled ->
                    if (userCancelled) {
                        val msg = "Satın alma iptal edildi"
                        Log.d(TAG, msg)
                        onError(msg)
                    } else {
                        val errorMsg = "Satın alma hatası: ${error.message}"
                        Log.e(TAG, errorMsg)
                        _error.value = errorMsg
                        onError(errorMsg)
                    }
                    _isLoading.value = false
                },
                onSuccess = { storeTransaction, customerInfo ->
                    val successMsg = "Satın alma başarılı!"
                    Log.d(TAG, successMsg)
                    updatePremiumStatus(customerInfo)
                    onSuccess()
                    _isLoading.value = false
                }
            )
        } catch (e: Exception) {
            val errorMsg = "Satın alma başlatılamadı: ${e.message}"
            Log.e(TAG, errorMsg, e)
            _error.value = errorMsg
            onError(errorMsg)
            _isLoading.value = false
        }
    }

    /**
     * Satın almaları geri yükler
     */
    fun restorePurchases(
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        Log.d(TAG, "Satın almaları geri yükleme")
        _isLoading.value = true

        try {
            Purchases.sharedInstance.restorePurchasesWith(
                onError = { error ->
                    val errorMsg = "Geri yükleme hatası: ${error.message}"
                    Log.e(TAG, errorMsg)
                    _error.value = errorMsg
                    onError(errorMsg)
                    _isLoading.value = false
                },
                onSuccess = { customerInfo ->
                    val successMsg = "Satın almalar başarıyla geri yüklendi"
                    Log.d(TAG, successMsg)
                    updatePremiumStatus(customerInfo)
                    onSuccess()
                    _isLoading.value = false
                }
            )
        } catch (e: Exception) {
            val errorMsg = "Geri yükleme başlatılamadı: ${e.message}"
            Log.e(TAG, errorMsg, e)
            _error.value = errorMsg
            onError(errorMsg)
            _isLoading.value = false
        }
    }

    /**
     * Google Play abonelik yönetimi sayfasını açar
     */
    fun openSubscriptionManagement(activity: Activity) {
        try {
            val intent = Intent(Intent.ACTION_VIEW).apply {
                data = Uri.parse("https://play.google.com/store/account/subscriptions")
                setPackage("com.android.vending") // Google Play Store package
            }
            activity.startActivity(intent)
            Log.d(TAG, "Abonelik yönetimi sayfası açıldı")
        } catch (e: Exception) {
            Log.e(TAG, "Abonelik yönetimi sayfası açılamadı", e)
            _error.value = "Abonelik yönetimi açılamadı: ${e.message}"
        }
    }

    /**
     * Bitiş tarihini döndürür
     */
    fun getExpiryDateFormatted(callback: (String?) -> Unit) {
        try {
            Purchases.sharedInstance.getCustomerInfoWith(
                onError = { error ->
                    Log.e(TAG, "Bitiş tarihi alma hatası: ${error.message}")
                    callback(null)
                },
                onSuccess = { customerInfo ->
                    val entitlement = customerInfo.entitlements[ENTITLEMENT_PREMIUM]
                    if (entitlement?.isActive == true) {
                        val expirationDate = entitlement.expirationDate
                        if (expirationDate != null) {
                            // Format: "15 Apr 2025"
                            val dateFormat = SimpleDateFormat("dd MMM yyyy", Locale.getDefault())
                            val formatted = dateFormat.format(expirationDate)
                            Log.d(TAG, "Bitiş tarihi: $formatted")
                            callback(formatted)
                        } else {
                            Log.d(TAG, "Bitiş tarihi bulunamadı")
                            callback(null)
                        }
                    } else {
                        Log.d(TAG, "Entitlement aktif değil")
                        callback(null)
                    }
                }
            )
        } catch (e: Exception) {
            Log.e(TAG, "Bitiş tarihi alma hatası", e)
            callback(null)
        }
    }

    /**
     * RevenueCat listener'ını temizler
     */
    fun cleanup() {
        try {
            Log.d(TAG, "RevenueCat listener'ı temizleniyor")
            Purchases.sharedInstance.updatedCustomerInfoListener = null
        } catch (e: Exception) {
            Log.e(TAG, "Cleanup hatası", e)
        }
    }
}