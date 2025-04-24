package com.aliaktas.urbanscore.ui.search

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aliaktas.urbanscore.data.model.CityModel
import com.aliaktas.urbanscore.data.model.CuratedCityItem
import com.google.firebase.firestore.FirebaseFirestore
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import javax.inject.Inject

@HiltViewModel
class ExploreViewModel @Inject constructor(
    private val firestore: FirebaseFirestore
) : ViewModel() {
    private val TAG = "ExploreViewModel"

    // Sadece Curated şehirler için State Flow
    private val _usersCities = MutableStateFlow<List<CuratedCityItem>>(emptyList())
    val usersCities: StateFlow<List<CuratedCityItem>> = _usersCities.asStateFlow()

    // Yükleme durumunu tutan stateflow
    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    // Şehirlerin yüklenmiş olup olmadığını izleyen değişken
    private var _usersCitiesLoaded = false

    init {
        // İlk başlatmada verileri yükle
        loadUsersCities()
    }

    // Popüler şehirleri yükleme
    fun loadUsersCities() {
        // Eğer popüler şehirler zaten yüklenmişse, tekrar yükleme
        if (_usersCitiesLoaded && _usersCities.value.isNotEmpty()) {
            return
        }

        viewModelScope.launch {
            try {
                _isLoading.value = true

                // Editors choice - öne çıkan şehirleri yükle
                val snapshot = withContext(Dispatchers.IO) {
                    firestore.collection("curated_lists")
                        .whereEqualTo("listType", "editors_choice")
                        .get()
                        .await()
                }

                val cities = snapshot.documents.mapNotNull { doc ->
                    try {
                        val model = doc.toObject(CuratedCityItem::class.java)
                        model?.copy(id = doc.id)
                    } catch (e: Exception) {
                        Log.e(TAG, "Error converting document: ${e.message}")
                        null
                    }
                }

                // Position'a göre sırala
                val sortedCities = cities.sortedBy { it.position }
                _usersCities.value = sortedCities
                _usersCitiesLoaded = true
                _isLoading.value = false

                Log.d(TAG, "Loaded ${sortedCities.size} curated cities")
            } catch (e: Exception) {
                Log.e(TAG, "Error loading curated cities: ${e.message}", e)
                _isLoading.value = false
                // Hata durumunda flag'i sıfırla ki tekrar denenebilsin
                _usersCitiesLoaded = false
            }
        }
    }

    // Manuel yeniden yükleme (pull-to-refresh gibi durumlar için)
    fun refresh() {
        _usersCitiesLoaded = false
        loadUsersCities()
    }
}