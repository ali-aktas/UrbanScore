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
                id = "environment",
                title = "Environment & Aesthetics",
                description = "City architecture, green spaces, cleanliness, and overall beauty",
                imageUrl = "https://example.com/environment.jpg",
                ratingType = "environment"
            ),
            CategoryModel(
                id = "safety",
                title = "Safety & Tranquility",
                description = "Crime rates, security, traffic safety, and general feeling of peace",
                imageUrl = "https://example.com/safety.jpg",
                ratingType = "safety"
            ),
            CategoryModel(
                id = "livability",
                title = "Livability",
                description = "Public transportation, walkability, infrastructure quality, and ease of daily life",
                imageUrl = "https://example.com/livability.jpg",
                ratingType = "livability"
            ),
            CategoryModel(
                id = "cost",
                title = "Cost of Living",
                description = "Housing prices, food costs, transportation expenses, and overall affordability",
                imageUrl = "https://example.com/cost.jpg",
                ratingType = "cost"
            ),
            CategoryModel(
                id = "social",
                title = "Social & Cultural Life",
                description = "Museums, galleries, nightlife, events, local hospitality, and social activities",
                imageUrl = "https://example.com/social.jpg",
                ratingType = "social"
            )
        )
    }
}