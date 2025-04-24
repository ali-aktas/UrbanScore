package com.aliaktas.urbanscore.data.model

import com.google.firebase.firestore.PropertyName
import com.google.firebase.firestore.IgnoreExtraProperties

@IgnoreExtraProperties
data class CuratedCityItem(
    @get:PropertyName("id") @set:PropertyName("id")
    var id: String = "",

    @get:PropertyName("cityId") @set:PropertyName("cityId")
    var cityId: String = "",

    @get:PropertyName("description") @set:PropertyName("description")
    var description: String = "",

    @get:PropertyName("imageUrl") @set:PropertyName("imageUrl")
    var imageUrl: String = "",

    @get:PropertyName("listType") @set:PropertyName("listType")
    var listType: String = "",

    @get:PropertyName("position") @set:PropertyName("position")
    var position: Int = 0
) {
    // ÖNEMLİ: Firebase boş konstruktor gerektirir
    constructor() : this("", "", "", "", "", 0)

    companion object {
        const val TYPE_EDITORS_CHOICE = "editors_choice"
        const val TYPE_POPULAR_CITIES = "popular_cities"
    }
}