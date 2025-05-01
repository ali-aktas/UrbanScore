package com.aliaktas.urbanscore.data.model

/**
 * Model class representing a city rating category
 * Each category corresponds to a specific rating type in the CategoryRatings class
 */
data class CategoryModel(
    val id: String,
    val title: String,
    val description: String = "",
    val imageUrl: String = "",
    val ratingType: String = ""
) {
    companion object {
        // Yeni kategorilere göre listeyi güncelle
        fun getDefaultCategories(): List<CategoryModel> = listOf(
            CategoryModel(
                id = "aesthetics",
                title = "City Aesthetics",
                description = "Cityscape beauty, architectural design, scenic viewpoints, photogenic locations, and overall visual appeal of the urban environment.",
                imageUrl = "https://example.com/aesthetics.jpg",
                ratingType = "aesthetics"
            ),
            CategoryModel(
                id = "gastronomy",
                title = "Gastronomy & Diversity",
                description = "Quality and diversity of local cuisine, street food, restaurants, cafes, and dining options. Representation of both authentic local dishes and international options.",
                imageUrl = "https://example.com/gastronomy.jpg",
                ratingType = "gastronomy"
            ),
            CategoryModel(
                id = "safety",
                title = "Safety & Peace",
                description = "Day and night safety levels, personal security feeling, peaceful atmosphere, and overall sense of comfort while exploring the city.",
                imageUrl = "https://example.com/safety.jpg",
                ratingType = "safety"
            ),
            CategoryModel(
                id = "culture",
                title = "Cultural Heritage",
                description = "Museums, historical sites, architectural landmarks, cultural diversity, traditions, and the preservation of the city's historical identity.",
                imageUrl = "https://example.com/culture.jpg",
                ratingType = "culture"
            ),
            CategoryModel(
                id = "livability",
                title = "Livability & Nature",
                description = "Public transportation efficiency, walkability, bike-friendliness, cleanliness of streets and public spaces, green areas, parks, and overall urban accessibility.",
                imageUrl = "https://example.com/livability.jpg",
                ratingType = "livability"
            ),
            CategoryModel(
                id = "social",
                title = "Social Life & Vibrancy",
                description = "Entertainment options, festivals, events, bars/clubs, social gathering places, and the value-for-money of these activities compared to other cities.",
                imageUrl = "https://example.com/social.jpg",
                ratingType = "social"
            ),
            CategoryModel(
                id = "hospitality",
                title = "Local People",
                description = "Friendliness of locals toward visitors, language accessibility, helpfulness, opportunities for meaningful social interactions, and the overall welcoming atmosphere.",
                imageUrl = "https://example.com/hospitality.jpg",
                ratingType = "hospitality"
            )
        )
    }
}