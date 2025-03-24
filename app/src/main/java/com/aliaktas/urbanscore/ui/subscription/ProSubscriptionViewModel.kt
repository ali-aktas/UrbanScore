package com.aliaktas.urbanscore.ui.subscription

import android.app.Activity
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aliaktas.urbanscore.util.RevenueCatManager
import com.revenuecat.purchases.Package
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ProSubscriptionViewModel @Inject constructor(
    private val revenueCat: RevenueCatManager
) : ViewModel() {

    // UI State için State Flow
    private val _uiState = MutableStateFlow<ProSubscriptionState>(ProSubscriptionState.Loading)
    val uiState: StateFlow<ProSubscriptionState> = _uiState.asStateFlow()

    // Paket bilgileri için State
    data class PackageUIState(
        val monthlyPackage: Package? = null,
        val yearlyPackage: Package? = null,
        val selectedPackageId: String = RevenueCatManager.PLAN_MONTHLY // Varsayılan olarak aylık seçili
    )

    private val _packagesState = MutableStateFlow(PackageUIState())
    val packagesState: StateFlow<PackageUIState> = _packagesState.asStateFlow()

    init {
        // Pro durumunu dinle
        viewModelScope.launch {
            revenueCat.isPremium.collectLatest { isPremium ->
                _uiState.value = if (isPremium) {
                    ProSubscriptionState.SubscriptionActive
                } else {
                    ProSubscriptionState.NotSubscribed
                }
            }
        }

        // Başlangıçta abonelik durumunu kontrol et
        revenueCat.fetchSubscriptionStatus()

        // Paket bilgilerini yükle
        loadPackages()
    }

    /**
     * Paket bilgilerini yükler
     */
    private fun loadPackages() {
        revenueCat.getPackageInfo { monthlyPackage, yearlyPackage ->
            _packagesState.value = PackageUIState(
                monthlyPackage = monthlyPackage,
                yearlyPackage = yearlyPackage,
                selectedPackageId = _packagesState.value.selectedPackageId
            )
        }
    }

    /**
     * Seçilen paketi günceller
     *
     * @param packageId Paket ID ("monthly" veya "yearly")
     */
    fun selectPackage(packageId: String) {
        _packagesState.value = _packagesState.value.copy(selectedPackageId = packageId)
    }

    /**
     * Seçili planı satın alır
     *
     * @param activity Satın alma için gerekli Activity
     */
    fun purchaseSelectedPlan(activity: Activity) {
        _uiState.value = ProSubscriptionState.Processing

        val packageId = _packagesState.value.selectedPackageId

        Log.d("ProSubscriptionVM", "Purchasing plan: $packageId")

        revenueCat.purchasePackageById(
            activity = activity,
            packageId = packageId,
            onSuccess = {
                _uiState.value = ProSubscriptionState.SubscriptionActive
            },
            onError = { errorMessage ->
                Log.e("ProSubscriptionVM", "Purchase error: $errorMessage")
                _uiState.value = ProSubscriptionState.Error(errorMessage)
            }
        )
    }

    /**
     * Satın almaları geri yükleme
     */
    fun restorePurchases() {
        _uiState.value = ProSubscriptionState.Processing

        revenueCat.restorePurchases(
            onSuccess = {
                _uiState.value = if (revenueCat.isPremium.value) {
                    ProSubscriptionState.SubscriptionActive
                } else {
                    ProSubscriptionState.NotSubscribed
                }
            },
            onError = { errorMessage ->
                Log.e("ProSubscriptionVM", "Restore error: $errorMessage")
                _uiState.value = ProSubscriptionState.Error(errorMessage)
            }
        )
    }

    /**
     * Abonelik yönetimi
     */
    fun openSubscriptionManagement(activity: Activity) {
        revenueCat.openSubscriptionManagement(activity)
    }

    /**
     * Bitiş tarihini elde etme
     */
    fun getExpiryDate(callback: (String?) -> Unit) {
        revenueCat.getExpiryDateFormatted(callback)
    }

    override fun onCleared() {
        super.onCleared()
        revenueCat.cleanup()
    }
}

// UI durumlarını temsil eden sealed class
sealed class ProSubscriptionState {
    data object Loading : ProSubscriptionState()
    data object NotSubscribed : ProSubscriptionState()
    data object Processing : ProSubscriptionState()
    data object SubscriptionActive : ProSubscriptionState()
    data class Error(val message: String) : ProSubscriptionState()
}