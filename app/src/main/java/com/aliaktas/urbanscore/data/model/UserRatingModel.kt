package com.aliaktas.urbanscore.data.model

data class UserRatingModel(
    val id: String = "",
    val userId: String = "",
    val cityId: String = "",
    val timestamp: Long = 0,
    val ratings: CategoryRatings = CategoryRatings()
)