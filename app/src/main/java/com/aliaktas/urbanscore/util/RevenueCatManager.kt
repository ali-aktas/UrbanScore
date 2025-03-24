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

        // Plan ID'leri
        const val PLAN_MONTHLY = "monthly"
        const val PLAN_YEARLY = "yearly"

        // Abonelik ürün ID'si
        const val SUBSCRIPTION_PRODUCT_ID = "appsubscription"

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
     * Belirli bir paket ID'sine göre satın alma işlemi yapar.
     *
     * @param activity Satın alma için Activity referansı
     * @param packageId Paket ID'si ("monthly" veya "yearly")
     * @param onSuccess Başarılı olduğunda çalışacak callback
     * @param onError Hata durumunda çalışacak callback
     */
    fun purchasePackageById(
        activity: Activity,
        packageId: String,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        try {
            Log.d(TAG, "Starting purchase for package ID: $packageId")

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

                    // Get the package by ID
                    val selectedPackage = offering.availablePackages.find {
                        it.identifier.contains(packageId, ignoreCase = true)
                    }

                    if (selectedPackage == null) {
                        Log.e(TAG, "Package with ID $packageId not found")
                        Log.e(TAG, "Available packages: ${offering.availablePackages.map { it.identifier }}")
                        onError("Selected subscription option not found")
                        return@getOfferingsWith
                    }

                    // Purchase the package
                    val purchaseParams = PurchaseParams.Builder(activity, selectedPackage).build()

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
     * Tüm paket bilgilerini getir.
     *
     * @param callback Paket bilgileri ile çağrılacak callback
     */
    fun getPackageInfo(callback: (monthlyPackage: Package?, yearlyPackage: Package?) -> Unit) {
        Purchases.sharedInstance.getOfferingsWith(
            onError = { error ->
                Log.e(TAG, "Error fetching offerings: ${error.message}")
                callback(null, null)
            },
            onSuccess = { offerings ->
                val currentOffering = offerings.current
                if (currentOffering == null) {
                    Log.e(TAG, "No current offering found")
                    callback(null, null)
                    return@getOfferingsWith
                }

                // Aylık ve yıllık paketleri bul
                val monthlyPackage = currentOffering.availablePackages.find {
                    it.identifier.contains(PLAN_MONTHLY, ignoreCase = true)
                }

                val yearlyPackage = currentOffering.availablePackages.find {
                    it.identifier.contains(PLAN_YEARLY, ignoreCase = true)
                }

                // Logla
                Log.d(TAG, "Available packages: ${currentOffering.availablePackages.map { it.identifier }}")
                Log.d(TAG, "Monthly package: ${monthlyPackage?.identifier}, Yearly package: ${yearlyPackage?.identifier}")

                callback(monthlyPackage, yearlyPackage)
            }
        )
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
     * Abonelik bitiş tarihini döndürür
     */
    fun getExpiryDateFormatted(callback: (String?) -> Unit) {
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