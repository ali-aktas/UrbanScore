package com.aliaktas.urbanscore.ui.detail

import android.content.Intent
import android.util.Log
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import com.aliaktas.urbanscore.base.BaseViewModel
import com.aliaktas.urbanscore.data.model.CityModel
import com.aliaktas.urbanscore.data.repository.CityRepository
import com.aliaktas.urbanscore.data.repository.UserRepository
import com.aliaktas.urbanscore.util.NetworkUtil
import com.aliaktas.urbanscore.util.RatingEventBus
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.async
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
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
    private val firestore: FirebaseFirestore,
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

    // LastComment referansı için değişken
    private var lastCommentDoc: DocumentSnapshot? = null

    // Yorum gösterme durum değişkeni
    private val _showComments = MutableStateFlow(false)


    init {
        loadCityDetails()

        // Rating event'lerini dinle
        viewModelScope.launch {
            RatingEventBus.events.collect { event ->
                if (event.cityId == cityId) {
                    Log.d("CityDetailViewModel", "Rating event received, refreshing data silently: ${event.silentRefresh}")

                    // Başarılı puanlama animasyonunu göster
                    _detailEvents.emit(CityDetailEvent.ShowRatingSuccessAnimation)

                    if (event.silentRefresh) {
                        // Sessiz yenileme yap (Loading state göstermeden)
                        loadCityDetailsSilently()
                    } else {
                        // Normal yenileme yap (Loading state göstererek)
                        loadCityDetails()
                    }
                }
            }
        }
    }

    private fun loadCityDetailsSilently() {
        viewModelScope.launch {
            try {
                // Loading state olmadan doğrudan city repository'ye erişim
                cityRepository.getCityById(cityId)
                    .catch { e ->
                        handleError(e)
                    }
                    .collectLatest { city ->
                        if (city != null) {
                            currentCity = city

                            // Başarılı state'i al, ancak isPartialUpdate=true ile
                            val currentState = _detailState.value
                            if (currentState is CityDetailState.Success) {
                                _detailState.value = currentState.copy(
                                    city = city,
                                    isPartialUpdate = true // Önemli: animasyon yok, sadece veri güncelleniyor
                                )
                            } else {
                                _detailState.value = CityDetailState.Success(
                                    city = city,
                                    isPartialUpdate = true // Önemli: animasyon yok, sadece veri güncelleniyor
                                )
                            }
                        }
                    }
            } catch (e: Exception) {
                handleError(e)
            }
        }
    }


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
                // Başlangıç zamanını kaydet
                val startTime = System.currentTimeMillis()

                // Şehir bilgilerini al
                val cityFlow = cityRepository.getCityById(cityId)
                    .catch { e ->
                        _detailState.value = CityDetailState.Error("Error loading city: ${getErrorMessage(e)}")
                    }
                    .first()

                if (cityFlow == null) {
                    _detailState.value = CityDetailState.Error("City not found. It may have been removed or the ID is incorrect.")
                    return@launch
                }

                if (cityFlow.cityName.isBlank() || cityFlow.country.isBlank()) {
                    _detailState.value = CityDetailState.Error("City data is incomplete. Essential information is missing.")
                    return@launch
                }

                // Diğer verileri al
                val wishlistDeferred = async {
                    try {
                        userRepository.getUserWishlist().first().contains(cityId)
                    } catch (e: Exception) {
                        Log.e("CityDetailViewModel", "Error checking wishlist status", e)
                        false
                    }
                }

                val ratingDeferred = async {
                    try {
                        userRepository.hasUserRatedCity(cityId).first()
                    } catch (e: Exception) {
                        Log.e("CityDetailViewModel", "Error checking user rating status", e)
                        false
                    }
                }

                val commentsCountDeferred = async {
                    try {
                        val snapshot = firestore.collection("cities")
                            .document(cityId)
                            .collection("comments")
                            .get()
                            .await()
                        snapshot.size()
                    } catch (e: Exception) {
                        Log.e("CityDetailViewModel", "Error getting comments count", e)
                        0
                    }
                }

                // Sonuçları derle
                val isInWishlist = wishlistDeferred.await()
                val hasUserRated = ratingDeferred.await()
                val commentsCount = commentsCountDeferred.await()

                // Geçen süreyi hesapla
                val elapsedTime = System.currentTimeMillis() - startTime

                // Minimum 1.5 saniye yükleme süresi uygula
                val minimumLoadingTime = 1500L
                if (elapsedTime < minimumLoadingTime) {
                    delay(minimumLoadingTime - elapsedTime)
                }

                // Şehir verilerini güncelle ve başarı durumunu göster
                currentCity = cityFlow
                _detailState.value = CityDetailState.Success(
                    city = cityFlow,
                    isInWishlist = isInWishlist,
                    hasUserRated = hasUserRated,
                    commentsCount = commentsCount
                )

            } catch (e: Exception) {
                Log.e("CityDetailViewModel", "Unexpected error loading city details", e)
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

    fun toggleWishlist() {
        val currentState = _detailState.value
        if (currentState !is CityDetailState.Success) return

        val isCurrentlyInWishlist = currentState.isInWishlist

        viewModelScope.launch {
            try {
                // Optimistic update
                updateSuccessState { copy(isInWishlist = !isInWishlist, isPartialUpdate = true) }

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
                        updateSuccessState { copy(isInWishlist = isCurrentlyInWishlist, isPartialUpdate = true) }

                        // Sadece mesaj göster, fragment'a etki etmesini engelle
                        _detailEvents.emit(CityDetailEvent.ShowMessage("Error: ${e.message}"))

                        // BaseViewModel'ın handleError metodunu ÇAĞIRMA - bu navigasyonu bozabilir
                        // handleError(e) -> Bu satırı kaldır/yorum satırına çevir

                        // Log ekleyelim
                        Log.e("CityDetailViewModel", "Wishlist toggle error: ${e.message}", e)
                    }
                )
            } catch (e: Exception) {
                // Revert optimistic update
                updateSuccessState { copy(isInWishlist = isCurrentlyInWishlist, isPartialUpdate = true) }

                // Sadece mesaj göster, fragment'a etki etmesini engelle
                _detailEvents.emit(CityDetailEvent.ShowMessage("Error: ${e.message}"))

                // BaseViewModel'ın handleError metodunu ÇAĞIRMA - bu navigasyonu bozabilir
                // handleError(e) -> Bu satırı kaldır/yorum satırına çevir

                // Log ekleyelim
                Log.e("CityDetailViewModel", "Wishlist toggle unexpected error: ${e.message}", e)
            }
        }
    }

    fun showRatingSheet() {
        viewModelScope.launch {
            try {
                _detailEvents.emit(CityDetailEvent.ShowRatingSheet(cityId))
            } catch (e: Exception) {
                // Sadece log, fragment'ı etkileme
                Log.e("CityDetailViewModel", "Error showing rating sheet: ${e.message}", e)
                _detailEvents.emit(CityDetailEvent.ShowMessage("Could not open rating sheet: ${e.message}"))
            }
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

    // Yorumları yükle
    fun loadComments(forceRefresh: Boolean = false) {
        val currentState = _detailState.value
        if (currentState !is CityDetailState.Success) return

        // Eğer force refresh istenmişse lastComment'i sıfırla
        if (forceRefresh) {
            lastCommentDoc = null
            // Eski yorumları göstermeye devam ederken yeni yorumları yükle
            updateSuccessState { copy(isLoadingComments = true, hasMoreComments = true) }
        } else if (currentState.isLoadingComments) {
            // Zaten yükleme yapılıyorsa tekrar yükleme
            return
        } else {
            updateSuccessState { copy(isLoadingComments = true) }
        }

        viewModelScope.launch {
            try {
                cityRepository.getComments(cityId, 5, lastCommentDoc)
                    .catch { e ->
                        updateSuccessState { copy(isLoadingComments = false) }
                        _detailEvents.emit(CityDetailEvent.ShowMessage("Error loading comments: ${e.message}"))
                    }
                    .collectLatest { result ->
                        val currentComments = if (forceRefresh || lastCommentDoc == null) {
                            result.items
                        } else {
                            currentState.comments + result.items
                        }

                        lastCommentDoc = result.lastVisible

                        updateSuccessState {
                            copy(
                                comments = currentComments,
                                hasMoreComments = result.hasMoreItems,
                                isLoadingComments = false,
                                showComments = true,
                                isPartialUpdate = true
                            )
                        }
                    }
            } catch (e: Exception) {
                updateSuccessState { copy(isLoadingComments = false) }
                handleError(e)
            }
        }
    }

    // Daha fazla yorum yükle
    fun loadMoreComments() {
        val currentState = _detailState.value
        if (currentState !is CityDetailState.Success ||
            !currentState.hasMoreComments ||
            currentState.isLoadingComments) {
            return
        }

        loadComments(false)
    }

    // Yorum ekle
    fun addComment(text: String) {
        if (text.isBlank()) {
            viewModelScope.launch {
                _detailEvents.emit(CityDetailEvent.ShowMessage("Comment cannot be empty"))
            }
            return
        }

        viewModelScope.launch {
            try {
                val result = cityRepository.addComment(cityId, text)

                result.fold(
                    onSuccess = {
                        _detailEvents.emit(CityDetailEvent.AddCommentResult(true, "Comment added successfully"))
                        // Yorumları yeniden yükle
                        loadComments(true)
                    },
                    onFailure = { e ->
                        _detailEvents.emit(CityDetailEvent.AddCommentResult(false, e.message ?: "Failed to add comment"))
                    }
                )
            } catch (e: Exception) {
                handleError(e)
                _detailEvents.emit(CityDetailEvent.AddCommentResult(false, e.message ?: "Failed to add comment"))
            }
        }
    }

    // Yorumu beğen
    fun likeComment(commentId: String, like: Boolean) {
        viewModelScope.launch {
            try {
                val result = cityRepository.likeComment(cityId, commentId, like)

                result.fold(
                    onSuccess = {
                        val message = if (like) "Comment liked" else "Like removed"
                        _detailEvents.emit(CityDetailEvent.LikeCommentResult(true, message))
                    },
                    onFailure = { e ->
                        _detailEvents.emit(CityDetailEvent.LikeCommentResult(false, e.message ?: "Failed to like comment"))
                    }
                )
            } catch (e: Exception) {
                handleError(e)
                _detailEvents.emit(CityDetailEvent.LikeCommentResult(false, e.message ?: "Failed to like comment"))
            }
        }
    }

    // Yorumu sil
    fun deleteComment(commentId: String) {
        viewModelScope.launch {
            try {
                val result = cityRepository.deleteComment(cityId, commentId)

                result.fold(
                    onSuccess = {
                        _detailEvents.emit(CityDetailEvent.ShowMessage("Comment deleted"))
                        // Yorumları yeniden yükle
                        loadComments(true)
                    },
                    onFailure = { e ->
                        _detailEvents.emit(CityDetailEvent.ShowMessage("Failed to delete comment: ${e.message}"))
                    }
                )
            } catch (e: Exception) {
                handleError(e)
                _detailEvents.emit(CityDetailEvent.ShowMessage("Failed to delete comment: ${e.message}"))
            }
        }
    }

    fun toggleComments() {
        val currentState = _detailState.value
        if (currentState !is CityDetailState.Success) return

        val newShowComments = !currentState.showComments
        updateSuccessState { copy(showComments = newShowComments, isPartialUpdate = true) }

        if (newShowComments && currentState.comments.isEmpty()) {
            loadComments(true)
        }
    }

    // Yorum ekleme bottom sheet'i göster
    fun showCommentBottomSheet() {
        viewModelScope.launch {
            _detailEvents.emit(CityDetailEvent.ShowCommentBottomSheet(cityId))
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