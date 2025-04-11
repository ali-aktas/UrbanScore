// ExploreViewModel.kt
package com.aliaktas.urbanscore.ui.search

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aliaktas.urbanscore.data.model.CityModel
import com.aliaktas.urbanscore.data.model.CuratedCityItem
import com.google.firebase.firestore.FirebaseFirestore
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ExploreViewModel @Inject constructor(
    private val firestore: FirebaseFirestore
) : ViewModel() {

    // Tüm şehirleri tutan stateflow
    private val _allCities = MutableStateFlow<List<CityModel>>(emptyList())
    val allCities: StateFlow<List<CityModel>> = _allCities.asStateFlow()

    private val _usersCities = MutableStateFlow<List<CuratedCityItem>>(emptyList())
    val usersCities: StateFlow<List<CuratedCityItem>> = _usersCities.asStateFlow()

    // Yükleme durumunu tutan stateflow
    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    // Şehirlerin yüklenmiş olup olmadığını izleyen değişken
    private var _allCitiesLoaded = false
    private var _usersCitiesLoaded = false

    init {
        // İlk başlatmada verileri yükle
        loadAllCities()
        loadUsersCities()
    }

    // Tüm şehirleri yükleme
    fun loadAllCities() {
        // Eğer şehirler zaten yüklenmişse, tekrar yükleme
        if (_allCitiesLoaded && _allCities.value.isNotEmpty()) {
            return
        }

        viewModelScope.launch {
            _isLoading.value = true

            try {
                firestore.collection("cities")
                    .limit(100)
                    .get()
                    .addOnSuccessListener { snapshot ->
                        val cities = snapshot.documents.mapNotNull { doc ->
                            try {
                                val model = doc.toObject(CityModel::class.java)
                                model?.copy(id = doc.id)
                            } catch (e: Exception) {
                                null
                            }
                        }

                        _allCities.value = cities
                        _allCitiesLoaded = true
                        _isLoading.value = false
                    }
                    .addOnFailureListener { e ->
                        // Yükleme hatası durumunda işaret
                        _isLoading.value = false
                    }
            } catch (e: Exception) {
                _isLoading.value = false
            }
        }
    }

    // Popüler şehirleri yükleme
    fun loadUsersCities() {
        // Eğer popüler şehirler zaten yüklenmişse, tekrar yükleme
        if (_usersCitiesLoaded && _usersCities.value.isNotEmpty()) {
            return
        }

        viewModelScope.launch {
            try {
                firestore.collection("curated_lists")
                    .whereEqualTo("listType", "editors_choice")
                    .get()
                    .addOnSuccessListener { snapshot ->
                        val cities = snapshot.documents.mapNotNull { doc ->
                            try {
                                val model = doc.toObject(CuratedCityItem::class.java)
                                model?.copy(id = doc.id)
                            } catch (e: Exception) {
                                null
                            }
                        }

                        // Position'a göre sırala
                        val sortedCities = cities.sortedBy { it.position }
                        _usersCities.value = sortedCities
                        _usersCitiesLoaded = true
                    }
                    .addOnFailureListener { e ->
                        // Hata durumunda editors_choice boş olarak kalacak
                    }
            } catch (e: Exception) {
                // Hata durumunda işlem yapma
            }
        }
    }

    // Manuel yeniden yükleme (pull-to-refresh gibi durumlar için)
    fun refresh() {
        _allCitiesLoaded = false
        _usersCitiesLoaded = false
        loadAllCities()
        loadUsersCities()
    }
}