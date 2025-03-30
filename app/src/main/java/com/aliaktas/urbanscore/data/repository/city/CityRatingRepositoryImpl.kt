package com.aliaktas.urbanscore.data.repository.city

import android.content.ContentValues.TAG
import android.util.Log
import com.aliaktas.urbanscore.data.model.CategoryRatings
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CityRatingRepositoryImpl @Inject constructor(
    private val firestore: FirebaseFirestore
) : CityRatingRepository {

    companion object {
        private const val CITIES_COLLECTION = "cities"
        private const val TAG = "CityRatingRepository"
    }

    override suspend fun rateCity(cityId: String, userId: String, ratings: CategoryRatings): Result<Unit> = try {
        // Öndalık basamakları düzenle
        val formattedRatings = CategoryRatings(
            gastronomy = (ratings.gastronomy * 100).toInt() / 100.0,
            aesthetics = (ratings.aesthetics * 100).toInt() / 100.0,
            safety = (ratings.safety * 100).toInt() / 100.0,
            culture = (ratings.culture * 100).toInt() / 100.0,
            livability = (ratings.livability * 100).toInt() / 100.0,
            social = (ratings.social * 100).toInt() / 100.0,
            hospitality = (ratings.hospitality * 100).toInt() / 100.0
        )

        // İşlemi bir transaction içinde gerçekleştir
        firestore.runTransaction { transaction ->
            val cityRef = firestore.collection(CITIES_COLLECTION).document(cityId)
            val cityDoc = transaction.get(cityRef)

            // Şehir verilerini al
            val city = cityDoc.toObject(com.aliaktas.urbanscore.data.model.CityModel::class.java)
                ?: throw Exception("City not found")

            // Kullanıcının önceki puanını users koleksiyonundan al
            val userRef = firestore.collection("users").document(userId)
            val userDoc = transaction.get(userRef)

            @Suppress("UNCHECKED_CAST")
            val visitedCities = userDoc.get("visited_cities") as? Map<String, Double> ?: emptyMap()
            val oldRating = visitedCities[cityId]
            val wasRatedBefore = oldRating != null

            // Şehrin ortalama puanını güncelle
            if (wasRatedBefore) {
                // Bu bir güncelleme - eski puanlama etkisini kaldır
                val newRatings = CategoryRatings(
                    gastronomy = updateRatingForEdit(city.ratings.gastronomy, oldRating!!,
                        formattedRatings.gastronomy, city.ratingCount),
                    aesthetics = updateRatingForEdit(city.ratings.aesthetics, oldRating,
                        formattedRatings.aesthetics, city.ratingCount),
                    safety = updateRatingForEdit(city.ratings.safety, oldRating,
                        formattedRatings.safety, city.ratingCount),
                    culture = updateRatingForEdit(city.ratings.culture, oldRating,
                        formattedRatings.culture, city.ratingCount),
                    livability = updateRatingForEdit(city.ratings.livability, oldRating,
                        formattedRatings.livability, city.ratingCount),
                    social = updateRatingForEdit(city.ratings.social, oldRating,
                        formattedRatings.social, city.ratingCount),
                    hospitality = updateRatingForEdit(city.ratings.hospitality, oldRating,
                        formattedRatings.hospitality, city.ratingCount)
                )

                // Yeni genel ortalamayı ağırlıklı formül kullanarak hesapla
                val newAverageRating = calculateWeightedAverage(newRatings)

                // 2 ondalık basamağa yuvarla
                val formattedAverage = (newAverageRating * 100).toInt() / 100.0

                // Şehir belgesini güncelle - puanlama sayısını artırmadan
                transaction.update(cityRef, mapOf(
                    "ratings" to newRatings,
                    "averageRating" to formattedAverage
                ))
            } else {
                // Bu yeni bir puanlama
                val newRatingCount = city.ratingCount + 1
                val newRatings = CategoryRatings(
                    gastronomy = updateAverage(city.ratings.gastronomy, formattedRatings.gastronomy, city.ratingCount),
                    aesthetics = updateAverage(city.ratings.aesthetics, formattedRatings.aesthetics, city.ratingCount),
                    safety = updateAverage(city.ratings.safety, formattedRatings.safety, city.ratingCount),
                    culture = updateAverage(city.ratings.culture, formattedRatings.culture, city.ratingCount),
                    livability = updateAverage(city.ratings.livability, formattedRatings.livability, city.ratingCount),
                    social = updateAverage(city.ratings.social, formattedRatings.social, city.ratingCount),
                    hospitality = updateAverage(city.ratings.hospitality, formattedRatings.hospitality, city.ratingCount)
                )

                // Yeni genel ortalamayı ağırlıklı formül kullanarak hesapla
                val newAverageRating = calculateWeightedAverage(newRatings)

                // 2 ondalık basamağa yuvarla
                val formattedAverage = (newAverageRating * 100).toInt() / 100.0

                // Şehir belgesini güncelle ve puanlama sayısını artır
                transaction.update(cityRef, mapOf(
                    "ratings" to newRatings,
                    "averageRating" to formattedAverage,
                    "ratingCount" to newRatingCount
                ))
            }
        }.await()

        Result.success(Unit)
    } catch (e: Exception) {
        Log.e(TAG, "rateCity failed", e)
        Result.failure(e)
    }

    // Calculate weighted average based on your formula
    private fun calculateWeightedAverage(ratings: CategoryRatings): Double {
        return ((ratings.gastronomy * 1.0) +
                (ratings.aesthetics * 1.1) +
                (ratings.safety * 1.2) +
                (ratings.culture * 1.0) +
                (ratings.livability * 1.0) +
                (ratings.social * 0.9) +
                (ratings.hospitality * 0.8)) / 7.0
    }

    // Update average for a new rating
    private fun updateAverage(oldAvg: Double, newValue: Double, oldCount: Int): Double {
        val newAvg = ((oldAvg * oldCount) + newValue) / (oldCount + 1)
        // Format to 2 decimal places
        return (newAvg * 100).toInt() / 100.0
    }

    // Update average when editing an existing rating
    private fun updateRatingForEdit(currentAvg: Double, oldValue: Double, newValue: Double, totalCount: Int): Double {
        // Formula: ((currentAvg * totalCount) - oldValue + newValue) / totalCount
        val newAvg = ((currentAvg * totalCount) - oldValue + newValue) / totalCount
        // Format to 2 decimal places
        return (newAvg * 100).toInt() / 100.0
    }
}