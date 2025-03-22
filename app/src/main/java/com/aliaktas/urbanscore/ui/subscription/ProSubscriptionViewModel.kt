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

    //private val revenueCat = RevenueCatManager.getInstance()

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

    fun purchaseMonthlySubscription(activity: Activity) {
        _uiState.value = ProSubscriptionState.Processing

        revenueCat.purchaseMonthlySubscription(
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