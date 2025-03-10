package com.aliaktas.urbanscore.data.model

data class UserRatingModel(
    val cityId: String = "",
    val cityName: String = "",
    val timestamp: Long = 0,
    val userAverageRating: Double = 0.0,
    val ratings: CategoryRatings = CategoryRatings()
)