package com.aliaktas.urbanscore.ui.subscription

import android.app.Activity
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aliaktas.urbanscore.util.RevenueCatManager
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

    private val _uiState = MutableStateFlow<ProSubscriptionState>(ProSubscriptionState.Loading)
    val uiState: StateFlow<ProSubscriptionState> = _uiState.asStateFlow()

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
    }

    /**
     * RevenueCat Paywall UI'ını gösterir
     */
    fun showPaywall(activity: Activity) {
        _uiState.value = ProSubscriptionState.Processing

        try {
            revenueCat.showPaywall(activity) {
                // Paywall kapatıldığında premium durumu kontrol et
                refreshSubscriptionStatus()
            }
        } catch (e: Exception) {
            Log.e("ProSubscriptionVM", "Error showing paywall", e)
            _uiState.value = ProSubscriptionState.Error("Could not show subscription options: ${e.message}")
        }
    }

    /**
     * Programatik satın alma
     */
    fun purchaseSubscription(activity: Activity) {
        _uiState.value = ProSubscriptionState.Processing

        revenueCat.purchasePackage(
            activity = activity,
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
    fun getExpiryDate(): String {
        return revenueCat.getExpiryDateFormatted() ?: "Unknown expiry date"
    }

    /**
     * Abonelik durumunu yenileme
     */
    private fun refreshSubscriptionStatus() {
        viewModelScope.launch {
            try {
                revenueCat.fetchSubscriptionStatus()
                // Abonelik durumunu kontrol et
                _uiState.value = if (revenueCat.isPremium.value) {
                    ProSubscriptionState.SubscriptionActive
                } else {
                    ProSubscriptionState.NotSubscribed
                }
            } catch (e: Exception) {
                Log.e("ProSubscriptionVM", "Error refreshing premium status", e)
            }
        }
    }

    /**
     * RevenueCat Offerings'leri elde etme (Paywall Fragment ile kullanım için)
     */
    fun getOfferings(callback: (offerings: com.revenuecat.purchases.Offerings?) -> Unit) {
        revenueCat.getOfferings(callback)
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