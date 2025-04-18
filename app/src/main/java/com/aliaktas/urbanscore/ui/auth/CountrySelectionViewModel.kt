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

@HiltViewModel
class CountrySelectionViewModel @Inject constructor(
    private val userRepository: UserRepository
) : ViewModel() {

    // Ülkelerin listesi
    private val _countries = MutableStateFlow<List<CountryModel>>(emptyList())
    val countries: StateFlow<List<CountryModel>> = _countries.asStateFlow()

    // Yükleme durumu
    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    // Hata durumu
    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    // Ülke kaydedildi durumu
    private val _countrySaved = MutableStateFlow(false)
    val countrySaved: StateFlow<Boolean> = _countrySaved.asStateFlow()

    init {
        loadCountries()
    }

    // Tüm ülkeleri yükle
    private fun loadCountries() {
        _isLoading.value = true

        try {
            // CountryModel'den statik ülke listesi getir
            val countryList = CountryModel.getAll()
            _countries.value = countryList
            _isLoading.value = false
        } catch (e: Exception) {
            Log.e("CountrySelectionVM", "Error loading countries", e)
            _error.value = "Failed to load countries: ${e.message}"
            _isLoading.value = false
        }
    }

    // Ülke seçimini kaydet
    fun saveUserCountry(countryId: String) {
        viewModelScope.launch {
            _isLoading.value = true

            try {
                Log.d("CountrySelectionVM", "Saving user country: $countryId")
                val result = userRepository.saveUserCountry(countryId)

                result.fold(
                    onSuccess = {
                        _countrySaved.value = true
                        _isLoading.value = false
                    },
                    onFailure = { e ->
                        Log.e("CountrySelectionVM", "Error saving country", e)
                        _error.value = "Failed to save country: ${e.message}"
                        _isLoading.value = false
                    }
                )
            } catch (e: Exception) {
                Log.e("CountrySelectionVM", "Exception saving country", e)
                _error.value = "Error: ${e.message}"
                _isLoading.value = false
            }
        }
    }

    // Hata mesajını temizle
    fun clearError() {
        _error.value = null
    }
}