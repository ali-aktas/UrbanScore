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
    val ratingType: String
) {
    companion object {
        // Create predefined category list
        fun getDefaultCategories(): List<CategoryModel> = listOf(
            CategoryModel(
                id = "safety_serenity",
                title = "The Most Safe & Peaceful Cities",
                description = "Cities with highest safety and serenity scores",
                imageUrl = "https://example.com/safety.jpg", // Replace with actual URLs
                ratingType = "safety_serenity"
            ),
            CategoryModel(
                id = "landscape_vibe",
                title = "Most Beautiful Urban Landscapes",
                description = "Cities with stunning urban landscapes and atmosphere",
                imageUrl = "https://example.com/landscape.jpg",
                ratingType = "landscape_vibe"
            ),
            CategoryModel(
                id = "cuisine",
                title = "Best Cities for Food Lovers",
                description = "Cities with the highest rated cuisine",
                imageUrl = "https://example.com/cuisine.jpg",
                ratingType = "cuisine"
            ),
            CategoryModel(
                id = "hospitality",
                title = "Most Welcoming Cities",
                description = "Cities with the friendliest locals and best hospitality",
                imageUrl = "https://example.com/hospitality.jpg",
                ratingType = "hospitality"
            ),
            CategoryModel(
                id = "nature_climate",
                title = "Best Climate & Nature",
                description = "Cities with the best natural environments and climate",
                imageUrl = "https://example.com/nature.jpg",
                ratingType = "nature_climate"
            ),
            CategoryModel(
                id = "lifestyle",
                title = "Best Cities for Lifestyle",
                description = "Cities with the highest quality of life",
                imageUrl = "https://example.com/lifestyle.jpg",
                ratingType = "lifestyle"
            )
        )
    }
}