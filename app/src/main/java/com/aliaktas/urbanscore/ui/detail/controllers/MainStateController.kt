package com.aliaktas.urbanscore.ui.detail.controllers

import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.fragment.app.FragmentActivity
import com.aliaktas.urbanscore.R
import com.aliaktas.urbanscore.databinding.FragmentCityDetailBinding
import com.aliaktas.urbanscore.ui.detail.CityDetailState


class MainStateController(
    private val binding: FragmentCityDetailBinding,
    private val onRetry: () -> Unit,
    private val onGoBack: () -> Unit
) : UiController {

    override fun bind(view: View) {
        setupErrorHandling()
    }

    private fun setupErrorHandling() {
        // Retry butonuna tıklama
        binding.btnRetry.setOnClickListener {
            try {
                // Loading durumunu göster
                binding.errorContainer.visibility = View.GONE
                // Skeleton kullanırken Lottie loading animasyonunu gizleyebiliriz
                binding.skeletonLayout.visibility = View.VISIBLE

                // Yeniden yükleme
                onRetry()
            } catch (e: Exception) {
                Log.e("MainStateController", "Error on retry: ${e.message}", e)
                Toast.makeText(binding.root.context, "Failed to reload: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }

        // Geri dönme butonu
        binding.btnGoBack.setOnClickListener {
            try {
                binding.root.post { onGoBack() }
            } catch (e: Exception) {
                Log.e("MainStateController", "Error going back: ${e.message}", e)
                Toast.makeText(binding.root.context, "Navigation error. Please try again.", Toast.LENGTH_SHORT).show()
            }
        }
    }

    override fun update(state: CityDetailState) {
        when (state) {
            is CityDetailState.Loading -> {
                // Skeleton layout'u göster
                binding.skeletonLayout.visibility = View.VISIBLE

                // Diğer görünümleri gizle
                binding.loadingOverlay.visibility = View.GONE
                binding.errorContainer.visibility = View.GONE
                binding.topContainer.visibility = View.GONE
                binding.nestedScrollView.visibility = View.GONE
            }
            is CityDetailState.Success -> {
                if (!state.isPartialUpdate) {
                    // Fade-in animasyonu
                    binding.topContainer.alpha = 0f
                    binding.nestedScrollView.alpha = 0f

                    binding.topContainer.animate()
                        .alpha(1f)
                        .setDuration(300)
                        .start()

                    binding.nestedScrollView.animate()
                        .alpha(1f)
                        .setDuration(300)
                        .start()
                }

                binding.skeletonLayout.visibility = View.GONE
                binding.loadingOverlay.visibility = View.GONE
                binding.errorContainer.visibility = View.GONE
                binding.topContainer.visibility = View.VISIBLE
                binding.nestedScrollView.visibility = View.VISIBLE
            }
            is CityDetailState.Error -> {
                binding.skeletonLayout.visibility = View.GONE
                binding.loadingOverlay.visibility = View.GONE
                binding.animationLoading.pauseAnimation()

                binding.topContainer.visibility = View.GONE
                binding.nestedScrollView.visibility = View.GONE

                // Hata ekranını göster
                showErrorUI(state.message)
            }
        }
    }

    private fun showErrorUI(errorMessage: String) {
        // Bu metod aynı kalabilir
        val context = binding.root.context

        binding.tvErrorMessage.text = "City Could Not Be Loaded"

        val description = when {
            errorMessage.contains("not found", ignoreCase = true) ->
                "This city may not exist or have been removed"
            errorMessage.contains("internet", ignoreCase = true) ->
                "Please check your internet connection and try again"
            errorMessage.contains("permission", ignoreCase = true) ->
                "You don't have permission to view this city"
            else -> "There was a problem loading this city information"
        }
        binding.tvErrorDescription.text = description

        binding.animationError.playAnimation()
        binding.errorContainer.visibility = View.VISIBLE
    }
}