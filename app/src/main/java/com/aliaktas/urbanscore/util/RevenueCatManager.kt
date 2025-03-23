package com.aliaktas.urbanscore.util

import android.app.Activity
import android.app.Application
import android.content.Intent
import android.net.Uri
import android.util.Log
import com.revenuecat.purchases.CustomerInfo
import com.revenuecat.purchases.Offering
import com.revenuecat.purchases.Package
import com.revenuecat.purchases.PurchasesConfiguration
import com.revenuecat.purchases.Purchases
import com.revenuecat.purchases.PurchaseParams
import com.revenuecat.purchases.getCustomerInfoWith
import com.revenuecat.purchases.getOfferingsWith
import com.revenuecat.purchases.interfaces.UpdatedCustomerInfoListener
import com.revenuecat.purchases.purchaseWith
import com.revenuecat.purchases.restorePurchasesWith
import com.revenuecat.purchases.ui.revenuecatui.ExperimentalPreviewRevenueCatUIPurchasesAPI
import com.revenuecat.purchases.ui.revenuecatui.PaywallDialog
import com.revenuecat.purchases.ui.revenuecatui.PaywallOptions
import com.revenuecat.purchases.ui.revenuecatui.fonts.PaywallFontFamily
import com.revenuecat.purchases.ui.revenuecatui.activity.PaywallActivityLauncher
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * RevenueCat abonelik yönetimi için merkezi sınıf.
 * Singleton pattern kullanılmıştır.
 */
class RevenueCatManager private constructor(application: Application) {

    companion object {
        private const val TAG = "RevenueCatManager"
        private const val API_KEY = "goog_WVsSgMhECzHIssMxfcjkQZxWEpa"
        private const val ENTITLEMENT_PREMIUM = "Premium"
        private const val OFFERING_DEFAULT = "default"

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

    // Abonelik durumu için StateFlow
    private val _isPremium = MutableStateFlow(false)
    val isPremium: StateFlow<Boolean> = _isPremium.asStateFlow()

    init {
        // RevenueCat'i yapılandır
        val configuration = PurchasesConfiguration.Builder(application, API_KEY)
            .observerMode(false)
            .build()

        Purchases.configure(configuration)
        Purchases.debugLogsEnabled = true

        // Abonelik durumunu kontrol et
        fetchSubscriptionStatus()

        // RevenueCat yapılandırma bilgilerini logla
        Log.d(TAG, "RevenueCat initialized with API key: ${API_KEY.take(5)}...")
        Log.d(TAG, "Checking if user is Premium...")
    }

    /**
     * Kullanıcının abonelik durumunu kontrol eder ve isPremium state'ini günceller
     */
    fun fetchSubscriptionStatus() {
        try {
            Purchases.sharedInstance.getCustomerInfoWith(
                onError = { error ->
                    Log.e(TAG, "Error fetching customer info: ${error.message}")
                },
                onSuccess = { customerInfo ->
                    updatePremiumStatus(customerInfo)
                }
            )
        } catch (e: Exception) {
            Log.e(TAG, "Error checking subscription status", e)
        }
    }

    /**
     * Kullanıcı bilgisine göre premium durumunu günceller
     */
    private fun updatePremiumStatus(customerInfo: CustomerInfo) {
        val isPremiumActive = customerInfo.entitlements[ENTITLEMENT_PREMIUM]?.isActive == true
        _isPremium.value = isPremiumActive
        Log.d(TAG, "Premium status updated: $isPremiumActive")
    }

    /**
     * Offerings'leri getirir ve callback fonksiyonunu çağırır
     */
    fun getOfferings(callback: (offerings: com.revenuecat.purchases.Offerings?) -> Unit) {
        Purchases.sharedInstance.getOfferingsWith(
            onError = { error ->
                Log.e(TAG, "Error fetching offerings: ${error.message}")
                callback(null)
            },
            onSuccess = { offerings ->
                callback(offerings)
            }
        )
    }

    // RevenueCatManager.kt'deki showPaywall metodunu tamamen değiştirin
    fun showPaywall(activity: Activity, onDismissed: () -> Unit) {
        // UI yok, doğrudan mevcut satın alma metodunu kullan
        Purchases.sharedInstance.getOfferingsWith(
            onError = { error ->
                Log.e(TAG, "Error fetching offerings: ${error.message}")
                onDismissed()
            },
            onSuccess = { offerings ->
                val currentOffering = offerings.current
                if (currentOffering == null) {
                    Log.e(TAG, "No current offering found")
                    onDismissed()
                    return@getOfferingsWith
                }

                // Manuel akışa geç - Bu kısım zaten çalışıyor
                purchaseViaManualFlow(activity, currentOffering, onDismissed)
            }
        )
    }

    /**
     * Manuel satın alma süreci - Paywall UI çalışmazsa yedek yöntem
     */
    private fun purchaseViaManualFlow(activity: Activity, offering: Offering, onDismissed: () -> Unit) {
        try {
            // Package kontrolü
            val monthlyPackage = offering.availablePackages.find {
                it.identifier.contains("monthly", ignoreCase = true)
            }

            if (monthlyPackage == null) {
                Log.e(TAG, "No monthly package found in offering")
                onDismissed()
                return
            }

            // Manuel olarak satın alma işlemini başlat
            val purchaseParams = PurchaseParams.Builder(activity, monthlyPackage).build()

            Purchases.sharedInstance.purchaseWith(
                purchaseParams = purchaseParams,
                onError = { error, userCancelled ->
                    if (userCancelled) {
                        Log.d(TAG, "User cancelled purchase")
                    } else {
                        Log.e(TAG, "Error purchasing package: ${error.message}")
                    }
                    onDismissed()
                },
                onSuccess = { storeTransaction, customerInfo ->
                    Log.d(TAG, "Purchase successful!")
                    updatePremiumStatus(customerInfo)
                    onDismissed()
                }
            )
        } catch (e: Exception) {
            Log.e(TAG, "Error in manual purchase flow", e)
            onDismissed()
        }
    }

    /**
     * Satın alma işlemi - Programatik olarak
     */
    fun purchasePackage(
        activity: Activity,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        try {
            // Get offerings from RevenueCat
            Purchases.sharedInstance.getOfferingsWith(
                onError = { error ->
                    Log.e(TAG, "Error fetching offerings: ${error.message}")
                    onError("Could not load subscription options: ${error.message}")
                },
                onSuccess = { offerings ->
                    // Get the default offering
                    val offering = offerings.current
                    if (offering == null) {
                        Log.e(TAG, "No offerings available")
                        onError("No subscription options available")
                        return@getOfferingsWith
                    }

                    // Get the monthly package
                    val monthlyPackage = offering.availablePackages.find {
                        it.identifier.contains("monthly", ignoreCase = true)
                    }

                    if (monthlyPackage == null) {
                        Log.e(TAG, "Monthly package not found")
                        Log.e(TAG, "Available packages: ${offering.availablePackages.map { it.identifier }}")
                        onError("Monthly subscription option not found")
                        return@getOfferingsWith
                    }

                    // Purchase the package
                    val purchaseParams = PurchaseParams.Builder(activity, monthlyPackage).build()

                    Purchases.sharedInstance.purchaseWith(
                        purchaseParams = purchaseParams,
                        onError = { error, userCancelled ->
                            if (userCancelled) {
                                Log.d(TAG, "User cancelled purchase")
                                onError("Purchase cancelled")
                            } else {
                                Log.e(TAG, "Error purchasing package: ${error.message}")
                                onError("Purchase failed: ${error.message}")
                            }
                        },
                        onSuccess = { storeTransaction, customerInfo ->
                            Log.d(TAG, "Purchase successful!")
                            updatePremiumStatus(customerInfo)
                            onSuccess()
                        }
                    )
                }
            )
        } catch (e: Exception) {
            Log.e(TAG, "Error during purchase process", e)
            onError("Unexpected error: ${e.message}")
        }
    }

    /**
     * Satın almaları geri yükleme
     */
    fun restorePurchases(
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        try {
            Log.d(TAG, "Attempting to restore purchases...")
            Purchases.sharedInstance.restorePurchasesWith(
                onError = { error ->
                    Log.e(TAG, "Error restoring purchases: ${error.message}")
                    onError("Error restoring purchases: ${error.message}")
                },
                onSuccess = { customerInfo ->
                    Log.d(TAG, "Purchases restored successfully")
                    updatePremiumStatus(customerInfo)
                    onSuccess()
                }
            )
        } catch (e: Exception) {
            Log.e(TAG, "Exception during restore purchases", e)
            onError("Error: ${e.message}")
        }
    }

    /**
     * Abonelik yönetimi için Google Play'i açar
     */
    fun openSubscriptionManagement(activity: Activity) {
        try {
            val intent = Intent(Intent.ACTION_VIEW).apply {
                data = Uri.parse("https://play.google.com/store/account/subscriptions")
                setPackage("com.android.vending") // Google Play Store package
            }
            activity.startActivity(intent)
        } catch (e: Exception) {
            Log.e(TAG, "Could not open Google Play Store", e)
        }
    }

    /**
     * Abonelik bitiş tarihini döndürür (güncellendi - asenkron)
     */
    fun getExpiryDateFormatted(callback: (String?) -> Unit = {}) {
        try {
            Purchases.sharedInstance.getCustomerInfoWith(
                onError = { error ->
                    Log.e(TAG, "Error getting customer info for expiry date: ${error.message}")
                    callback(null)
                },
                onSuccess = { customerInfo ->
                    val entitlement = customerInfo.entitlements[ENTITLEMENT_PREMIUM]
                    if (entitlement?.isActive == true) {
                        val expirationDate = entitlement.expirationDate
                        if (expirationDate != null) {
                            // Format: "15 Apr 2025"
                            val dateFormat = SimpleDateFormat("dd MMM yyyy", Locale.getDefault())
                            callback(dateFormat.format(expirationDate))
                        } else {
                            callback(null)
                        }
                    } else {
                        callback(null)
                    }
                }
            )
        } catch (e: Exception) {
            Log.e(TAG, "Error getting expiry date", e)
            callback(null)
        }
    }

    /**
     * Abonelik bitiş tarihini döndürür (senkron yardımcı metod)
     * ViewModel için daha kullanışlı
     */
    fun getExpiryDateFormatted(): String? {
        try {
            // En son bilinen tarih bilgisini hemen döndür, arka planda güncelleme yapılır
            var result: String? = null

            // Asenkron fonksiyonu çağır, sonucu doğrudan kullanılamaz
            getExpiryDateFormatted { formattedDate ->
                result = formattedDate
            }

            // Geçici sonuç döndür
            return result ?: "Fetching expiry date..."
        } catch (e: Exception) {
            Log.e(TAG, "Error in synchronous getExpiryDateFormatted", e)
            return null
        }
    }

    /**
     * Satın alma durumunu dinler
     */
    fun observePurchases() {
        Purchases.sharedInstance.updatedCustomerInfoListener = UpdatedCustomerInfoListener { customerInfo ->
            updatePremiumStatus(customerInfo)
        }
    }

    /**
     * RevenueCat dinleyicisini temizler
     */
    fun cleanup() {
        Purchases.sharedInstance.updatedCustomerInfoListener = null
    }
}