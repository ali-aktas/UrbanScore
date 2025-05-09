package com.aliaktas.urbanscore.ui.detail

import com.aliaktas.urbanscore.data.model.CityModel
import com.aliaktas.urbanscore.data.model.CommentModel

/**
 * Represents the state of the city detail screen.
 * Contains detailed sub-states for different UI components.
 */
sealed class CityDetailState {
    /**
     * Loading state, displayed when data is being fetched.
     */
    data object Loading : CityDetailState()

    /**
     * Success state, displayed when data is successfully loaded.
     * Contains the city model and additional UI states.
     */
    data class Success(
        val city: CityModel,
        val isInWishlist: Boolean = false,
        val hasUserRated: Boolean = false,
        val isShareInProgress: Boolean = false,
        val showRatingSheet: Boolean = false,
        val comments: List<CommentModel> = emptyList(),
        val commentsCount: Int = 0,
        val hasMoreComments: Boolean = false,
        val isLoadingComments: Boolean = false,
        val showComments: Boolean = false,
        val isPartialUpdate: Boolean = false
    ) : CityDetailState()

    /**
     * Error state, displayed when data loading fails.
     */
    data class Error(val message: String) : CityDetailState()
}

/**
 * Events from view model to fragment for transient actions
 */
sealed class CityDetailEvent {
    data class OpenUrl(val url: String) : CityDetailEvent()
    data class ShowRatingSheet(val cityId: String) : CityDetailEvent()
    data class ShowMessage(val message: String) : CityDetailEvent()
    data class ShareCity(val shareIntent: android.content.Intent) : CityDetailEvent()
    data object DismissRatingSheet : CityDetailEvent()
    data object ShowRatingSuccessAnimation : CityDetailEvent()
    data class ShowCommentBottomSheet(val cityId: String) : CityDetailEvent()
    data class LikeCommentResult(val success: Boolean, val message: String) : CityDetailEvent()
    data class AddCommentResult(val success: Boolean, val message: String) : CityDetailEvent()
}