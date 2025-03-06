package com.aliaktas.urbanscore.ui.home

import com.aliaktas.urbanscore.data.model.CityModel

sealed class HomeState {
    data object Initial : HomeState()

    // Loading durumu artık data class olarak değiştirildi ve eski verileri tutabilir
    data class Loading(val oldData: List<CityModel>? = null) : HomeState()

    data class Success(val cities: List<CityModel>) : HomeState()
    data class Error(val message: String) : HomeState()
}