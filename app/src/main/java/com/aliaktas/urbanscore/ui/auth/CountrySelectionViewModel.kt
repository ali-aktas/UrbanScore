package com.aliaktas.urbanscore.ui.auth

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aliaktas.urbanscore.data.model.CountryModel
import com.aliaktas.urbanscore.data.repository.UserRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Ülke seçim ekranı için ViewModel
 */
@HiltViewModel
class CountrySelectionViewModel @Inject constructor(
    private val userRepository: UserRepository
) : ViewModel() {

    // Ülke listesi
    private val _countries = MutableStateFlow<List<CountryModel>>(emptyList())
    val countries: StateFlow<List<CountryModel>> = _countries.asStateFlow()

    // Yükleme durumu
    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    // Hata durumu
    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    // Ülkenin kaydedilme durumu
    private val _countrySaved = MutableStateFlow(false)
    val countrySaved: StateFlow<Boolean> = _countrySaved.asStateFlow()

    init {
        loadCountries()
    }

    /**
     * Ülke listesini yükler
     */
    private fun loadCountries() {
        viewModelScope.launch {
            _isLoading.value = true

            try {
                // CountryModel sınıfındaki statik getAll() metodunu kullanarak ülkeleri al
                val countryList = CountryModel.getAll()
                _countries.value = countryList
                Log.d("CountrySelectionVM", "Successfully loaded ${countryList.size} countries")
            } catch (e: Exception) {
                Log.e("CountrySelectionVM", "Error loading countries", e)
                _error.value = "Failed to load country list: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    /**
     * Seçilen ülkeyi kullanıcı profiline kaydeder
     */
    fun saveUserCountry(countryId: String) {
        viewModelScope.launch {
            try {
                _isLoading.value = true
                Log.d("CountrySelectionVM", "Saving user country: $countryId")

                val result = userRepository.saveUserCountry(countryId)

                result.fold(
                    onSuccess = {
                        Log.d("CountrySelectionVM", "Country saved successfully")
                        _countrySaved.value = true
                    },
                    onFailure = { e ->
                        Log.e("CountrySelectionVM", "Error saving country", e)
                        _error.value = "Could not save country: ${e.message}"
                    }
                )
            } catch (e: Exception) {
                Log.e("CountrySelectionVM", "Exception saving country", e)
                _error.value = "Error: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    /**
     * Hata mesajını temizler
     */
    fun clearError() {
        _error.value = null
    }
}