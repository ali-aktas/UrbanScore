package com.aliaktas.urbanscore.data.model

data class ContinentModel(
    val id: String,
    val title: String,
    val imageUrl: String = "",
    val ratingType: String = "" // Ekledik
) {
    companion object {
        fun getAll(): List<ContinentModel> = listOf(
            ContinentModel(
                id = "global",
                title = "Global",
                imageUrl = "https://example.com/global.jpg"
            ),
            ContinentModel(
                id = "europe",
                title = "Europe",
                imageUrl = "https://example.com/europe.jpg"
            ),
            ContinentModel(
                id = "asia",
                title = "Asia",
                imageUrl = "https://example.com/asia.jpg"
            ),
            ContinentModel(
                id = "americas",
                title = "Americas",
                imageUrl = "https://example.com/americas.jpg"
            ),
            ContinentModel(
                id = "africa",
                title = "Africa",
                imageUrl = "https://example.com/africa.jpg"
            ),
            ContinentModel(
                id = "oceania",
                title = "Oceania",
                imageUrl = "https://example.com/oceania.jpg"
            )
        )
    }
}