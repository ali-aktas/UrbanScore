package com.aliaktas.urbanscore.ui.categories

import android.util.Log
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aliaktas.urbanscore.data.model.CityModel
import com.aliaktas.urbanscore.data.repository.CityRepository
import com.google.firebase.firestore.DocumentSnapshot
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class CategoryListViewModel @Inject constructor(
    private val cityRepository: CityRepository,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val _state = MutableStateFlow<CategoryListState>(CategoryListState.Initial)
    val state: StateFlow<CategoryListState> = _state.asStateFlow()

    private val categoryId: String = savedStateHandle["categoryId"] ?: "averageRating"
    private var lastVisibleDocument: DocumentSnapshot? = null
    private var currentCities = mutableListOf<CityModel>()
    private var isDataLoading = false

    init {
        loadInitialCities()
    }

    fun loadInitialCities() {
        if (isDataLoading) return

        isDataLoading = true
        _state.value = CategoryListState.Loading()

        viewModelScope.launch {
            try {
                Log.d("CategoryListViewModel", "Loading initial cities for category: $categoryId")
                cityRepository.getCitiesByCategoryPaginated(categoryId, 20)
                    .collect { result ->
                        Log.d("CategoryListViewModel", "Loaded ${result.items.size} initial cities")
                        currentCities = result.items.toMutableList()
                        lastVisibleDocument = result.lastVisible
                        _state.value = CategoryListState.Success(
                            currentCities,
                            result.hasMoreItems
                        )
                        isDataLoading = false
                    }
            } catch (e: Exception) {
                Log.e("CategoryListViewModel", "Error loading initial cities: ${e.message}")
                _state.value = CategoryListState.Error(e.message ?: "Failed to load cities")
                isDataLoading = false
            }
        }
    }

    fun loadMoreCities() {
        Log.d("CategoryListViewModel", "loadMoreCities() called")

        val currentState = _state.value
        if (isDataLoading ||
            currentState !is CategoryListState.Success ||
            !currentState.hasMoreItems) {
            Log.d("CategoryListViewModel", "Cannot load more: isLoading=$isDataLoading, state=${currentState.javaClass.simpleName}, hasMore=${if (currentState is CategoryListState.Success) currentState.hasMoreItems else false}")
            return
        }

        isDataLoading = true
        _state.value = CategoryListState.Loading(
            oldData = currentCities,
            isLoadingMore = true
        )

        viewModelScope.launch {
            try {
                Log.d("CategoryListViewModel", "Loading more cities for category: $categoryId, after: ${lastVisibleDocument?.id}")
                cityRepository.getCitiesByCategoryPaginated(
                    categoryId,
                    20,
                    lastVisibleDocument
                ).collect { result ->
                    Log.d("CategoryListViewModel", "Loaded ${result.items.size} more cities")
                    currentCities.addAll(result.items)
                    lastVisibleDocument = result.lastVisible

                    // Determine if there are more items
                    val hasMore = result.items.isNotEmpty() && currentCities.size < 100
                    Log.d("CategoryListViewModel", "Has more items: $hasMore")

                    _state.value = CategoryListState.Success(
                        currentCities.toList(),
                        hasMore
                    )
                    isDataLoading = false
                }
            } catch (e: Exception) {
                Log.e("CategoryListViewModel", "Error loading more cities: ${e.message}")
                _state.value = CategoryListState.Error(e.message ?: "Failed to load more cities")
                isDataLoading = false
            }
        }
    }
}

// State Management
sealed class CategoryListState {
    data object Initial : CategoryListState()
    data class Loading(
        val oldData: List<CityModel>? = null,
        val isLoadingMore: Boolean = false
    ) : CategoryListState()
    data class Success(
        val cities: List<CityModel>,
        val hasMoreItems: Boolean = false
    ) : CategoryListState()
    data class Error(val message: String) : CategoryListState()
}