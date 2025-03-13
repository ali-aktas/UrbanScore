package com.aliaktas.urbanscore.ui.search

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aliaktas.urbanscore.data.repository.CityRecommendationRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class CityRecommendationViewModel @Inject constructor(
    private val repository: CityRecommendationRepository
) : ViewModel() {

    private val _submissionState = MutableLiveData<SubmissionState>()
    val submissionState: LiveData<SubmissionState> = _submissionState

    fun submitCityRecommendation(cityName: String, country: String, description: String) {
        _submissionState.value = SubmissionState.Loading
        viewModelScope.launch {
            val result = repository.submitCityRecommendation(cityName, country, description)
            result.fold(
                onSuccess = {
                    Log.d("CityRecommendationVM", "City recommendation submitted successfully")
                    _submissionState.value = SubmissionState.Success
                },
                onFailure = { e ->
                    Log.e("CityRecommendationVM", "City recommendation submission failed", e)
                    _submissionState.value = SubmissionState.Error(e.message ?: "Unknown error")
                }
            )
        }
    }

    sealed class SubmissionState {
        object Initial : SubmissionState()
        object Loading : SubmissionState()
        object Success : SubmissionState()
        data class Error(val message: String) : SubmissionState()
    }
}