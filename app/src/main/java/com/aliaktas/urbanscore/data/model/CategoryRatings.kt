package com.aliaktas.urbanscore.data.model

import com.google.firebase.firestore.PropertyName

data class CategoryRatings(
    @PropertyName("environment") var environment: Double = 0.0,
    @PropertyName("safety") var safety: Double = 0.0,
    @PropertyName("livability") var livability: Double = 0.0,
    @PropertyName("cost") var cost: Double = 0.0,
    @PropertyName("social") var social: Double = 0.0
)