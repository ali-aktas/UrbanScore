package com.aliaktas.urbanscore.data.repository

import android.util.Log
import com.aliaktas.urbanscore.data.model.CityRecommendationModel
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

interface CityRecommendationRepository {
    suspend fun submitCityRecommendation(
        cityName: String,
        country: String,
        description: String
    ): Result<Unit>
}

@Singleton
class CityRecommendationRepositoryImpl @Inject constructor(
    private val firestore: FirebaseFirestore,
    private val auth: FirebaseAuth
) : CityRecommendationRepository {

    override suspend fun submitCityRecommendation(
        cityName: String,
        country: String,
        description: String
    ): Result<Unit> = try {
        val currentUser = auth.currentUser ?: throw Exception("User not logged in")

        val recommendation = CityRecommendationModel(
            cityName = cityName,
            country = country,
            userDescription = description,
            userId = currentUser.uid,
            userName = currentUser.displayName ?: "Anonymous"
        )

        firestore.collection("city_suggestions")
            .add(recommendation)
            .await()

        Result.success(Unit)
    } catch (e: Exception) {
        Log.e("CityRecommendationRepo", "Error submitting recommendation", e)
        Result.failure(e)
    }
}