package com.aliaktas.urbanscore.ui.ratecity

sealed class RateCityState {
    data object Initial : RateCityState()
    data object Loading : RateCityState()
    data object Success : RateCityState()
    data class Error(val message: String) : RateCityState()
}
