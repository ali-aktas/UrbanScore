package com.aliaktas.urbanscore.ui.detail

import android.content.Context
import androidx.core.view.isVisible
import androidx.lifecycle.LifecycleCoroutineScope
import com.aliaktas.urbanscore.R
import com.aliaktas.urbanscore.data.model.CityModel
import com.aliaktas.urbanscore.databinding.FragmentCityDetailBinding
import com.bumptech.glide.Glide
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * UI state manager for CityDetailFragment.
 * Handles all UI updates based on state changes.
 */
class CityDetailUiStateManager @Inject constructor(
    private val context: Context,
    private val binding: FragmentCityDetailBinding,
    private val lifecycleScope: LifecycleCoroutineScope,
    private val formatter: CityDetailFormatter,
    private val radarChartHelper: RadarChartHelper,
    private val onRetry: () -> Unit,
    private val onGoBack: () -> Unit,
    private val onRateButtonClick: () -> Unit
) {

    /**
     * Update UI based on state
     */
    fun updateUI(state: CityDetailState) {
        when (state) {
            is CityDetailState.Loading -> showLoading()
            is CityDetailState.Success -> showContent(state)
            is CityDetailState.Error -> showError(state.message)
        }
    }

    /**
     * Show loading state
     */
    private fun showLoading() {
        binding.progressBar.isVisible = true
        binding.cityInfoLayout.isVisible = false
    }

    /**
     * Show content with city data
     */
    private fun showContent(state: CityDetailState.Success) {
        binding.progressBar.isVisible = false
        binding.cityInfoLayout.isVisible = true

        updateCityInfo(state.city)
        updateRatingData(state.city)
        updateWishlistButton(state.isInWishlist)
        updateRateButton(state.hasUserRated)
    }

    /**
     * Show error state with message
     */
    fun showError(message: String) {
        binding.progressBar.isVisible = false

        MaterialAlertDialogBuilder(context)
            .setTitle(R.string.error)
            .setMessage(message)
            .setPositiveButton(R.string.retry) { _, _ -> onRetry() }
            .setNegativeButton(R.string.cancel) { _, _ -> onGoBack() }
            .show()
    }

    /**
     * Update city information UI elements
     */
    private fun updateCityInfo(city: CityModel) {
        // City name and flag
        binding.textCityName.text = city.cityName
        binding.txtRatingCount.text = formatter.getRatingCountText(
            { id, quantity, arg -> context.resources.getQuantityString(id, quantity, arg) },
            city.ratingCount
        )

        // Load flag
        Glide.with(context)
            .load(city.flagUrl)
            .centerCrop()
            .into(binding.imgFlag)

        // City information
        binding.txtCountry.text = city.country
        binding.txtRegion.text = city.region
        binding.txtPopulation.text = formatter.formatPopulation(city.population)

        // Average rating
        binding.txtAverageRating.text = formatter.formatRating(city.averageRating)
    }

    /**
     * Update rating data UI elements and radar chart
     */
    private fun updateRatingData(city: CityModel) {
        // Update category ratings text
        with(city.ratings) {
            binding.txtEnvironmentRating.text = formatter.formatRating(environment)
            binding.txtSafetyRating.text = formatter.formatRating(safety)
            binding.txtLivabilityRating.text = formatter.formatRating(livability)
            binding.txtCostRating.text = formatter.formatRating(cost)
            binding.txtSocialRating.text = formatter.formatRating(social)
        }

        // Update radar chart in background
        lifecycleScope.launch {
            radarChartHelper.updateChartData(binding.radarChart, city.ratings)
        }
    }

    /**
     * Update wishlist button state
     */
    private fun updateWishlistButton(isInWishlist: Boolean) {
        binding.btnAddToWishlist.apply {
            if (isInWishlist) {
                text = context.getString(R.string.in_your_list)
                setBackgroundColor(context.getColor(android.R.color.holo_red_light))
            } else {
                text = context.getString(R.string.add_to_wishlist)
                setBackgroundColor(context.getColor(R.color.white))
            }
        }
    }

    /**
     * Update rate button state
     */
    private fun updateRateButton(hasRated: Boolean) {
        binding.btnRateCity.apply {
            text = if (hasRated) {
                context.getString(R.string.update_rating)
            } else {
                context.getString(R.string.rate_this_city)
            }
            setOnClickListener { onRateButtonClick() }
        }
    }
}