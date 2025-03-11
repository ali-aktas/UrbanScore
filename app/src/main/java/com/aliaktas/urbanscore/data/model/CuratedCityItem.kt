package com.aliaktas.urbanscore.data.model

data class CuratedCityItem(
    val id: String = "", // Firestore doküman ID'si
    val cityId: String = "", // cities koleksiyonundaki şehir ID'si (örn. "istanbul-turkey")
    val description: String = "", // Açıklama
    val imageUrl: String = "", // Görsel URL'si
    val listType: String = "", // "editors_choice" veya "popular_cities"
    val position: Int = 0 // Sıralama pozisyonu
) {
    companion object {
        const val TYPE_EDITORS_CHOICE = "editors_choice"
        const val TYPE_POPULAR_CITIES = "popular_cities"
    }
}