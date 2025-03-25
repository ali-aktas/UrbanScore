package com.aliaktas.urbanscore.ui.subscription

import android.app.Activity
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aliaktas.urbanscore.util.RevenueCatManager
import com.revenuecat.purchases.Package
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Pro abonelik ekranı için ViewModel
 * Reactive programlama yaklaşımı ile yeniden tasarlandı.
 */
@HiltViewModel
class ProSubscriptionViewModel @Inject constructor(
    private val revenueCatManager: RevenueCatManager
) : ViewModel() {

    private val TAG = "ProSubscriptionVM"

    // Kullanıcı mesajları için tek seferlik event akışı
    private val _userMessage = MutableSharedFlow<String>()
    val userMessage: SharedFlow<String> = _userMessage.asSharedFlow()

    // Seçilen paket ID'si
    private val _selectedPackageId = MutableStateFlow(RevenueCatManager.PLAN_MONTHLY)
    val selectedPackageId: StateFlow<String> = _selectedPackageId.asStateFlow()

    // ViewModel'in çeşitli diğer state'leri RevenueCatManager'dan alınıyor
    val isPremium = revenueCatManager.isPremium
    val isLoading = revenueCatManager.isLoading
    val monthlyPackage = revenueCatManager.monthlyPackage
    val yearlyPackage = revenueCatManager.yearlyPackage

    // Birleştirilmiş UI durumu
    val uiState: StateFlow<SubscriptionUIState> = combine(
        isPremium,
        isLoading,
        monthlyPackage,
        yearlyPackage,
        _selectedPackageId
    ) { isPremium, isLoading, monthlyPackage, yearlyPackage, selectedPackageId ->
        when {
            isLoading -> SubscriptionUIState.Loading
            isPremium -> SubscriptionUIState.PremiumActive
            else -> {
                val hasPackages = monthlyPackage != null || yearlyPackage != null
                if (hasPackages) {
                    SubscriptionUIState.ReadyForPurchase(
                        monthlyPackage = monthlyPackage,
                        yearlyPackage = yearlyPackage,
                        selectedPackageId = selectedPackageId
                    )
                } else {
                    SubscriptionUIState.PackagesUnavailable
                }
            }
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = SubscriptionUIState.Loading
    )

    init {
        // Başlangıçta gerekli yüklemeleri yap
        Log.d(TAG, "ProSubscriptionViewModel başlatılıyor")
        refreshSubscriptionData()

        // RevenueCatManager'dan hata izleme
        viewModelScope.launch {
            revenueCatManager.error.collect { errorMsg ->
                errorMsg?.let {
                    Log.e(TAG, "RevenueCatManager hatası: $it")
                    emitUserMessage(it)
                }
            }
        }
    }

    /**
     * Abonelik verilerini yenile
     */
    fun refreshSubscriptionData() {
        Log.d(TAG, "Abonelik verilerini yenileme")
        revenueCatManager.checkPremiumStatus()
        revenueCatManager.refreshPackages()
    }

    /**
     * Seçilen paketi güncelle
     */
    fun selectPackage(packageId: String) {
        Log.d(TAG, "Plan seçildi: $packageId")

        // Geçerli bir paket mi kontrol et
        if (packageId !in setOf(RevenueCatManager.PLAN_MONTHLY, RevenueCatManager.PLAN_YEARLY)) {
            Log.e(TAG, "Geçersiz paket ID: $packageId")
            return
        }

        _selectedPackageId.value = packageId
    }

    /**
     * Seçili planı satın al
     */
    fun purchaseSelectedPlan(activity: Activity) {
        val packageId = _selectedPackageId.value

        Log.d(TAG, "Satın alma başlatılıyor: $packageId")
        Log.d(TAG, "UI State: ${uiState.value.javaClass.simpleName}")

        val currentState = uiState.value
        if (currentState !is SubscriptionUIState.ReadyForPurchase) {
            Log.e(TAG, "Satın alma yapılamaz, durum uygun değil: ${currentState.javaClass.simpleName}")
            emitUserMessage("Satın alma işlemi şu anda başlatılamıyor. Lütfen tekrar deneyin.")
            return
        }

        // Aylık paket seçiliyse ama null ise, kullanıcıyı uyar
        if (packageId == RevenueCatManager.PLAN_MONTHLY && currentState.monthlyPackage == null) {
            Log.e(TAG, "Aylık paket seçili ama bulunamadı")
            emitUserMessage("Aylık abonelik şu anda kullanılamıyor. Lütfen yıllık aboneliği deneyin veya daha sonra tekrar kontrol edin.")
            return
        }

        // Yıllık paket seçiliyse ama null ise, kullanıcıyı uyar
        if (packageId == RevenueCatManager.PLAN_YEARLY && currentState.yearlyPackage == null) {
            Log.e(TAG, "Yıllık paket seçili ama bulunamadı")
            emitUserMessage("Yıllık abonelik şu anda kullanılamıyor. Lütfen aylık aboneliği deneyin veya daha sonra tekrar kontrol edin.")
            return
        }

        // Satın alma işlemini başlat
        revenueCatManager.purchasePlan(
            activity = activity,
            planId = packageId,
            onSuccess = {
                Log.d(TAG, "Satın alma başarılı")
                emitUserMessage("Tebrikler! Pro aboneliğiniz aktif.")
            },
            onError = { errorMsg ->
                Log.e(TAG, "Satın alma hatası: $errorMsg")
                emitUserMessage(errorMsg)
            }
        )
    }

    /**
     * Satın almaları geri yükle
     */
    fun restorePurchases() {
        Log.d(TAG, "Satın almaları geri yükleme")

        if (isLoading.value) {
            Log.d(TAG, "Yükleme devam ediyor, işlem ertelendi")
            emitUserMessage("Lütfen bekleyin...")
            return
        }

        revenueCatManager.restorePurchases(
            onSuccess = {
                Log.d(TAG, "Geri yükleme başarılı")
                if (isPremium.value) {
                    emitUserMessage("Aboneliğiniz başarıyla geri yüklendi!")
                } else {
                    emitUserMessage("Geri yükleme tamamlandı, ancak aktif abonelik bulunamadı.")
                }
            },
            onError = { errorMsg ->
                Log.e(TAG, "Geri yükleme hatası: $errorMsg")
                emitUserMessage(errorMsg)
            }
        )
    }

    /**
     * Abonelik yönetimi
     */
    fun openSubscriptionManagement(activity: Activity) {
        Log.d(TAG, "Abonelik yönetimi açılıyor")
        revenueCatManager.openSubscriptionManagement(activity)
    }

    /**
     * Bitiş tarihini al
     */
    fun getExpiryDate(callback: (String?) -> Unit) {
        Log.d(TAG, "Bitiş tarihi alınıyor")
        revenueCatManager.getExpiryDateFormatted(callback)
    }

    /**
     * Kullanıcı mesajı gönder
     */
    private fun emitUserMessage(message: String) {
        viewModelScope.launch {
            _userMessage.emit(message)
        }
    }

    /**
     * Hesaplanan tasarruf yüzdesini döndürür
     */
    fun calculateSavingsPercentage(): Int {
        val monthly = monthlyPackage.value
        val yearly = yearlyPackage.value

        if (monthly == null || yearly == null) {
            return 0
        }

        try {
            val monthlyPrice = monthly.product.price.amountMicros
            val yearlyPrice = yearly.product.price.amountMicros

            if (monthlyPrice <= 0) return 0

            val monthlyPricePerYear = monthlyPrice * 12

            return ((monthlyPricePerYear - yearlyPrice) * 100.0 / monthlyPricePerYear).toInt()
                .coerceAtLeast(0) // Negatif değer olmamalı
        } catch (e: Exception) {
            Log.e(TAG, "Tasarruf hesaplama hatası", e)
            return 0
        }
    }

    override fun onCleared() {
        super.onCleared()
        Log.d(TAG, "ViewModel temizleniyor")
    }
}

/**
 * Pro abonelik ekranının state'leri
 */
sealed class SubscriptionUIState {
    data object Loading : SubscriptionUIState()
    data object PremiumActive : SubscriptionUIState()
    data object PackagesUnavailable : SubscriptionUIState()

    data class ReadyForPurchase(
        val monthlyPackage: Package?,
        val yearlyPackage: Package?,
        val selectedPackageId: String
    ) : SubscriptionUIState()
}