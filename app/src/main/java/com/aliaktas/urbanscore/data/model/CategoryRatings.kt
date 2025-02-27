package com.aliaktas.urbanscore.data.model

import com.google.firebase.firestore.PropertyName

data class CategoryRatings(
    @PropertyName("cuisine") var cuisine: Double = 0.0,
    @PropertyName("hospitality") var hospitality: Double = 0.0,
    @PropertyName("landscape_vibe") var landscapeVibe: Double = 0.0,
    @PropertyName("nature_climate") var natureClimate: Double = 0.0,
    @PropertyName("lifestyle") var lifestyle: Double = 0.0,
    @PropertyName("safety_serenity") var safetySerenity: Double = 0.0,
)