package com.aliaktas.urbanscore.util

import android.app.Application
import android.util.Log
import com.revenuecat.purchases.CustomerInfo
import com.revenuecat.purchases.Package
import com.revenuecat.purchases.PurchasesConfiguration
import com.revenuecat.purchases.PurchasesError
import com.revenuecat.purchases.Purchases
import com.revenuecat.purchases.PurchaseParams
import com.revenuecat.purchases.getCustomerInfoWith
import com.revenuecat.purchases.getOfferingsWith
import com.revenuecat.purchases.interfaces.UpdatedCustomerInfoListener
import com.revenuecat.purchases.models.StoreTransaction
import com.revenuecat.purchases.purchaseWith
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

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
            // collectDeviceIdentifiers satırını kaldırıyoruz, artık gerekli değil
            // veya varsayılan olarak zaten yapılıyor
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
     * Paket satın alma işlemini başlatır
     *
     * @param activity Satın alma işlemi için gereken aktivite
     * @param onSuccess Satın alma başarılı olduğunda çağrılacak
     * @param onError Satın alma hatası durumunda çağrılacak
     */
    fun purchaseMonthlySubscription(
        activity: android.app.Activity,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        try {
            // Default offering'deki paketi al
            Purchases.sharedInstance.getOfferingsWith(
                onError = { error ->
                    Log.e(TAG, "Error fetching offerings: ${error.message}")
                    onError("Could not load subscription options: ${error.message}")
                },
                onSuccess = { offerings ->
                    val offering = offerings.current
                    if (offering == null) {
                        Log.e(TAG, "No offerings available")
                        onError("No subscription options available")
                        return@getOfferingsWith
                    }

                    // Monthly package'ı bul
                    val monthlyPackage = offering.availablePackages.find {
                        it.identifier.contains("monthly") ||
                                it.product.id == "appsubscription" ||
                                it.product.id == "monthlysubscription" ||
                                it.product.id == "appsubscription:monthlysubscription"
                    }

                    if (monthlyPackage == null) {
                        Log.e(TAG, "Available packages: ${offering.availablePackages.map { it.identifier }}")
                        Log.e(TAG, "Available product IDs: ${offering.availablePackages.map { it.product.id }}")
                        Log.e(TAG, "Monthly package not found")
                        onError("Monthly subscription option not found")
                        return@getOfferingsWith
                    }

                    // Satın alma işlemini başlat
                    purchasePackage(activity, monthlyPackage, onSuccess, onError)
                }
            )
        } catch (e: Exception) {
            Log.e(TAG, "Error during purchase process", e)
            onError("Unexpected error: ${e.message}")
        }
        Log.d(TAG, "Starting purchase for monthly subscription")
    }

    /**
     * Belirli bir paketi satın alır
     */
    private fun purchasePackage(
        activity: android.app.Activity,
        packageToPurchase: Package,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        val purchaseParams = PurchaseParams.Builder(activity, packageToPurchase).build()

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
                if (storeTransaction != null) {
                    Log.d(TAG, "Purchase successful! Product: ${storeTransaction.productIds}")
                }
                updatePremiumStatus(customerInfo)
                onSuccess()
            }
        )
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