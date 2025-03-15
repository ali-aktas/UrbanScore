package com.aliaktas.urbanscore.ui.detail

import android.content.Intent
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import com.aliaktas.urbanscore.base.BaseViewModel
import com.aliaktas.urbanscore.data.model.CityModel
import com.aliaktas.urbanscore.data.repository.CityRepository
import com.aliaktas.urbanscore.data.repository.UserRepository
import com.aliaktas.urbanscore.util.NetworkUtil
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import java.net.URLEncoder
import javax.inject.Inject

/**
 * ViewModel for CityDetailFragment.
 * Handles all business logic for city detail screen.
 */
@HiltViewModel
class CityDetailViewModel @Inject constructor(
    private val cityRepository: CityRepository,
    private val userRepository: UserRepository,
    private val networkUtil: NetworkUtil,
    savedStateHandle: SavedStateHandle
) : BaseViewModel() {

    // The city ID obtained from navigation args
    private val cityId: String = checkNotNull(savedStateHandle["cityId"])

    // State for UI
    private val _detailState = MutableStateFlow<CityDetailState>(CityDetailState.Loading)
    val detailState: StateFlow<CityDetailState> = _detailState.asStateFlow()

    // Events for one-time actions (navigation, dialogs, etc.)
    private val _detailEvents = MutableSharedFlow<CityDetailEvent>()
    val detailEvents: SharedFlow<CityDetailEvent> = _detailEvents.asSharedFlow()

    // City data cache
    private var currentCity: CityModel? = null

    init {
        loadCityDetails()
    }

    /**
     * Load city details from repository.
     * Also checks if city is in wishlist and if user has rated it.
     */
    fun loadCityDetails() {
        if (!networkUtil.isNetworkAvailable()) {
            _detailState.value = CityDetailState.Error("No internet connection. Please check your connection and try again.")
            viewModelScope.launch {
                emitEvent(UiEvent.Error("No internet connection"))
            }
            return
        }

        _detailState.value = CityDetailState.Loading

        viewModelScope.launch {
            try {
                // Load city details, wishlist status and rating status in parallel
                val cityDeferred = launch {
                    cityRepository.getCityById(cityId)
                        .catch { e ->
                            _detailState.value = CityDetailState.Error(getErrorMessage(e))
                        }
                        .collectLatest { city ->
                            if (city != null) {
                                currentCity = city
                                updateSuccessState { copy(city = city) }
                            } else {
                                _detailState.value = CityDetailState.Error("City not found")
                            }
                        }
                }

                val wishlistDeferred = launch {
                    userRepository.getUserWishlist()
                        .catch { /* Silent fail, not critical */ }
                        .collectLatest { wishlist ->
                            updateSuccessState { copy(isInWishlist = wishlist.contains(cityId)) }
                        }
                }

                val ratingDeferred = launch {
                    userRepository.hasUserRatedCity(cityId)
                        .catch { /* Silent fail, not critical */ }
                        .collectLatest { hasRated ->
                            updateSuccessState { copy(hasUserRated = hasRated) }
                        }
                }

                // Wait for all data to load
                cityDeferred.join()
                wishlistDeferred.join()
                ratingDeferred.join()

            } catch (e: Exception) {
                handleError(e)
                _detailState.value = CityDetailState.Error(getErrorMessage(e))
            }
        }
    }

    /**
     * Helper method to update success state only if current state is Success
     */
    private fun updateSuccessState(update: CityDetailState.Success.() -> CityDetailState.Success) {
        val currentState = _detailState.value
        if (currentState is CityDetailState.Success) {
            _detailState.value = currentState.update()
        } else if (currentState is CityDetailState.Loading) {
            // Eğer mevcut durum Loading ise, başlangıç değerleriyle bir Success nesnesi oluştur
            val city = currentCity ?: return // City null ise çık
            _detailState.value = CityDetailState.Success(
                city = city,
                isInWishlist = false,
                hasUserRated = false
            ).update()
        }
    }

    /**
     * Toggle wishlist status for current city
     */
    fun toggleWishlist() {
        val currentState = _detailState.value
        if (currentState !is CityDetailState.Success) return

        val isCurrentlyInWishlist = currentState.isInWishlist

        viewModelScope.launch {
            try {
                // Optimistic update
                updateSuccessState { copy(isInWishlist = !isInWishlist) }

                val result = if (isCurrentlyInWishlist) {
                    userRepository.removeFromWishlist(cityId)
                } else {
                    userRepository.addToWishlist(cityId)
                }

                result.fold(
                    onSuccess = {
                        val message = if (isCurrentlyInWishlist) {
                            "Removed from bucket list"
                        } else {
                            "Added to bucket list"
                        }
                        _detailEvents.emit(CityDetailEvent.ShowMessage(message))
                    },
                    onFailure = { e ->
                        // Revert optimistic update
                        updateSuccessState { copy(isInWishlist = isCurrentlyInWishlist) }
                        _detailEvents.emit(CityDetailEvent.ShowMessage("Error: ${e.message}"))
                    }
                )
            } catch (e: Exception) {
                // Revert optimistic update
                updateSuccessState { copy(isInWishlist = isCurrentlyInWishlist) }
                handleError(e)
            }
        }
    }

    /**
     * Show rating dialog to rate the city
     */
    fun showRatingSheet() {
        viewModelScope.launch {
            _detailEvents.emit(CityDetailEvent.ShowRatingSheet(cityId))
        }
    }

    /**
     * Open YouTube search for the current city
     */
    fun openYouTubeSearch() {
        val city = currentCity ?: return

        viewModelScope.launch {
            try {
                val searchQuery = "${city.cityName} 4K"
                val encodedQuery = URLEncoder.encode(searchQuery, "UTF-8")
                val youtubeUrl = "https://www.youtube.com/results?search_query=$encodedQuery"

                _detailEvents.emit(CityDetailEvent.OpenUrl(youtubeUrl))
            } catch (e: Exception) {
                _detailEvents.emit(CityDetailEvent.ShowMessage("Failed to open YouTube search"))
            }
        }
    }

    /**
     * Open Google image search for the current city
     */
    fun openGoogleSearch() {
        val city = currentCity ?: return

        viewModelScope.launch {
            try {
                val searchQuery = "Best Landscapes of ${city.cityName}"
                val encodedQuery = URLEncoder.encode(searchQuery, "UTF-8")
                val googleImagesUrl = "https://www.google.com/search?q=$encodedQuery&tbm=isch"

                _detailEvents.emit(CityDetailEvent.OpenUrl(googleImagesUrl))
            } catch (e: Exception) {
                _detailEvents.emit(CityDetailEvent.ShowMessage("Failed to open Google search"))
            }
        }
    }

    /**
     * Share city info with others
     */
    fun shareCity() {
        val city = currentCity ?: return

        viewModelScope.launch {
            // Mark share as in progress
            updateSuccessState { copy(isShareInProgress = true) }

            try {
                val shareText = "Check out ${city.cityName}, ${city.country} on UrbanRate! " +
                        "It has an average rating of ${String.format("%.1f", city.averageRating)}/10. " +
                        "Download the app to explore more cities!"

                val intent = Intent(Intent.ACTION_SEND).apply {
                    type = "text/plain"
                    putExtra(Intent.EXTRA_SUBJECT, "${city.cityName} on UrbanRate")
                    putExtra(Intent.EXTRA_TEXT, shareText)
                }

                _detailEvents.emit(CityDetailEvent.ShareCity(intent))
            } catch (e: Exception) {
                _detailEvents.emit(CityDetailEvent.ShowMessage("Failed to share city"))
            } finally {
                // Mark share as completed
                updateSuccessState { copy(isShareInProgress = false) }
            }
        }
    }

    /**
     * Called when returning from RatingBottomSheet
     * Refreshes city data to get updated ratings
     */
    fun refreshAfterRating() {
        loadCityDetails()
    }
}