package com.aliaktas.urbanscore.ui.detail

import com.aliaktas.urbanscore.R
import com.aliaktas.urbanscore.data.model.CityModel
import java.text.NumberFormat
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Formatter class to handle all text formatting operations
 * for city detail screen.
 */
@Singleton
class CityDetailFormatter @Inject constructor() {

    /**
     * Format population number with locale-specific separators
     */
    fun formatPopulation(population: Long): String {
        return NumberFormat.getNumberInstance(Locale.getDefault()).format(population)
    }

    /**
     * Format rating with 2 decimal places
     */
    fun formatRating(rating: Double): String {
        return String.format("%.2f", rating)
    }

    /**
     * Extract readable city name from city ID
     * Example: "istanbul-turkey" -> "Istanbul"
     */
    fun getCityNameFromId(cityId: String): String {
        return cityId.split("-").firstOrNull()?.replaceFirstChar {
            if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString()
        } ?: "Unknown City"
    }

    /**
     * Create share text for city
     */
    fun createShareText(city: CityModel): String {
        return "Check out ${city.cityName}, ${city.country} on UrbanRate! " +
                "It has an average rating of ${formatRating(city.averageRating)}/10. " +
                "Download the app to explore more cities!"
    }

    /**
     * Get plural text for rating count
     * @param resourceProvider Function to access string resources with plurals support
     */
    fun getRatingCountText(resourceProvider: (Int, Int, Int) -> String, count: Int): String {
        return resourceProvider(R.plurals.based_on_ratings, count, count)
    }

    /**
     * Format YouTube search query
     */
    fun formatYouTubeSearchQuery(cityName: String): String {
        return "$cityName 4K city tour"
    }

    /**
     * Format Google search query
     */
    fun formatGoogleSearchQuery(cityName: String): String {
        return "Best Landscapes of $cityName"
    }
}