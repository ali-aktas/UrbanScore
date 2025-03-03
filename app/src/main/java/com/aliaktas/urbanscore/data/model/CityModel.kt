package com.aliaktas.urbanscore.data.model

import com.google.firebase.firestore.PropertyName

data class CityModel(
    // We use the Firestore document ID
    var id: String = "",
    @PropertyName("city_name") var cityName: String = "",
    @PropertyName("country") var country: String = "",
    @PropertyName("flag_url") var flagUrl: String = "",
    @PropertyName("region") var region: String = "",
    @PropertyName("population") var population: Long = 0,
    @PropertyName("average_rating") var averageRating: Double = 0.0,
    @PropertyName("rating_count") var ratingCount: Int = 0,
    @PropertyName("ratings") var ratings: CategoryRatings = CategoryRatings(),
)