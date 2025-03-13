package com.aliaktas.urbanscore.data.model

import com.google.firebase.firestore.ServerTimestamp
import java.util.Date

data class CityRecommendationModel(
    val cityName: String = "",
    val country: String = "",
    val userDescription: String = "",
    val userId: String = "",
    val userName: String = "",
    val processed: Boolean = false,
    @ServerTimestamp val timestamp: Date? = null
)