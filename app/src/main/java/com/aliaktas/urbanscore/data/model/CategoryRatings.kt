package com.aliaktas.urbanscore.data.model

import com.google.firebase.firestore.PropertyName

data class CategoryRatings(
    @PropertyName("gastronomy") var gastronomy: Double = 0.0,
    @PropertyName("aesthetics") var aesthetics: Double = 0.0,
    @PropertyName("safety") var safety: Double = 0.0,
    @PropertyName("culture") var culture: Double = 0.0,
    @PropertyName("livability") var livability: Double = 0.0,
    @PropertyName("social") var social: Double = 0.0,
    @PropertyName("hospitality") var hospitality: Double = 0.0
)