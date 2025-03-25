package com.aliaktas.urbanscore.ui.detail.controllers

import android.view.View
import com.aliaktas.urbanscore.R
import com.aliaktas.urbanscore.databinding.FragmentCityDetailBinding
import com.aliaktas.urbanscore.ui.detail.CityDetailState
import com.google.android.material.dialog.MaterialAlertDialogBuilder

/**
 * Ana UI durumunu (yükleniyor, başarılı, hata) yöneten controller
 */
class MainStateController(
    private val binding: FragmentCityDetailBinding,
    private val onRetry: () -> Unit,
    private val onGoBack: () -> Unit
) : UiController {

    override fun bind(view: View) {
        // İlk yapılandırma gerekirse buraya eklenecek
    }

    override fun update(state: CityDetailState) {
        when (state) {
            is CityDetailState.Loading -> {
                // Lottie animasyonunu göster
                binding.loadingOverlay.visibility = View.VISIBLE
                binding.animationLoading.playAnimation()

                binding.cityInfoLayout.visibility = View.GONE
                binding.nestedScrollView.visibility = View.GONE
            }
            is CityDetailState.Success -> {
                // isPartialUpdate kontrolü
                if (!state.isPartialUpdate) {
                    // Normal update - animasyonu gizle
                    binding.loadingOverlay.visibility = View.GONE
                    binding.animationLoading.pauseAnimation()
                }
                // isPartialUpdate=true ise animasyonun durumunu değiştirme

                binding.cityInfoLayout.visibility = View.VISIBLE
                binding.nestedScrollView.visibility = View.VISIBLE
            }
            is CityDetailState.Error -> {
                binding.loadingOverlay.visibility = View.GONE
                binding.animationLoading.pauseAnimation()

                binding.cityInfoLayout.visibility = View.GONE
                binding.nestedScrollView.visibility = View.GONE

                showErrorDialog(state.message)
            }
        }
    }

    private fun showErrorDialog(message: String) {
        MaterialAlertDialogBuilder(binding.root.context)
            .setTitle(R.string.error)
            .setMessage(message)
            .setPositiveButton(R.string.retry) { _, _ -> onRetry() }
            .setNegativeButton(R.string.cancel) { _, _ -> onGoBack() }
            .show()
    }
}