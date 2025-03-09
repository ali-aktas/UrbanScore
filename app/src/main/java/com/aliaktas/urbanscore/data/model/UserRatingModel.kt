package com.aliaktas.urbanscore.data.model

data class UserRatingModel(
    val id: String = "",
    val userId: String = "",
    val cityId: String = "",
    val cityName: String = "",  // Added for convenience
    val timestamp: Long = 0,
    val userAverageRating: Double = 0.0,  // Added to store user's overall rating
    val ratings: CategoryRatings = CategoryRatings()
)