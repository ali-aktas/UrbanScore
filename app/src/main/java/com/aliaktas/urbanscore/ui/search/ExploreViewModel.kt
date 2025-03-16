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

    // Popüler şehirleri tutan stateflow
    private val _popularCities = MutableStateFlow<List<CuratedCityItem>>(emptyList())
    val popularCities: StateFlow<List<CuratedCityItem>> = _popularCities.asStateFlow()

    // Yükleme durumunu tutan stateflow
    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    // Şehirlerin yüklenmiş olup olmadığını izleyen değişken
    private var _allCitiesLoaded = false
    private var _popularCitiesLoaded = false

    init {
        // İlk başlatmada verileri yükle
        loadAllCities()
        loadPopularCities()
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
    fun loadPopularCities() {
        // Eğer popüler şehirler zaten yüklenmişse, tekrar yükleme
        if (_popularCitiesLoaded && _popularCities.value.isNotEmpty()) {
            return
        }

        viewModelScope.launch {
            try {
                firestore.collection("curated_lists")
                    .whereEqualTo("listType", "popular_cities")
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
                        _popularCities.value = sortedCities
                        _popularCitiesLoaded = true
                    }
                    .addOnFailureListener { e ->
                        // Hata durumunda _popularCities boş olarak kalacak
                    }
            } catch (e: Exception) {
                // Hata durumunda işlem yapma
            }
        }
    }

    // Manuel yeniden yükleme (pull-to-refresh gibi durumlar için)
    fun refresh() {
        _allCitiesLoaded = false
        _popularCitiesLoaded = false
        loadAllCities()
        loadPopularCities()
    }
}