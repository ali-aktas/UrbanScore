package com.aliaktas.urbanscore.ui.detail

import com.aliaktas.urbanscore.data.model.CityModel

sealed class CityDetailState {
    data object Loading : CityDetailState()
    data class Success(val city: CityModel) : CityDetailState()
    data class Error(val message: String) : CityDetailState()
}