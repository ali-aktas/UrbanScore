package com.aliaktas.urbanscore.data.model

data class ShareCityItem(
    val id: String,
    val name: String,
    val country: String,
    val flagUrl: String,
    val rating: Double,
    val position: Int
)