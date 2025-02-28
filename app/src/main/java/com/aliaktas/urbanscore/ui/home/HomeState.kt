package com.aliaktas.urbanscore.ui.home

import com.aliaktas.urbanscore.data.model.CityModel

sealed class HomeState {
    data object Initial : HomeState()  // Yeni durum
    data object Loading : HomeState()
    data class Success(val cities: List<CityModel>) : HomeState()
    data class Error(val message: String) : HomeState()
}