package com.aliaktas.urbanscore.ui.home.controllers

import android.util.Log
import android.view.View
import androidx.core.view.isVisible
import com.aliaktas.urbanscore.R
import com.aliaktas.urbanscore.databinding.FragmentHomeBinding
import com.aliaktas.urbanscore.ui.home.HomeState

/**
 * Ana durumu (loading, error, success) yöneten controller
 */
class MainStateController(
    private val binding: FragmentHomeBinding,
    private val onRetry: () -> Unit
) : HomeController {

    override fun bind(view: View) {
        setupErrorUI()
        setupAnimations()
    }

    private fun setupErrorUI() {
        binding.btnRetry.setOnClickListener {
            Log.d(TAG, "Retry button clicked")
            onRetry()
        }
    }

    private fun setupAnimations() {
        binding.animationLoading.apply {
            setAnimation(R.raw.loading_animation)
        }
        binding.animationError.apply {
            setAnimation(R.raw.error_animation)
        }
    }

    override fun update(state: HomeState) {
        Log.d(TAG, "Updating UI state: ${state.javaClass.simpleName}")

        when (state) {
            is HomeState.Initial -> {
                binding.swipeRefreshLayout.isVisible = true
                binding.loadingContainer.isVisible = false
                binding.errorContainer.isVisible = false
                binding.nestedLayout.isVisible = true
                binding.cardCitiesList.isVisible = false
            }
            is HomeState.Loading -> {
                updateLoadingState(state)
            }
            is HomeState.Success -> {
                // Yükleme animasyonunu biraz daha göster, sonra içeriği göster
                binding.loadingContainer.isVisible = true
                binding.animationLoading.isVisible = true
                binding.animationLoading.playAnimation()

                // 2.2 saniye sonra içeriği göster
                binding.root.postDelayed({
                    if (binding.root.isAttachedToWindow) {  // Fragment hala aktif mi kontrol et
                        binding.swipeRefreshLayout.isVisible = true
                        binding.loadingContainer.isVisible = false
                        binding.errorContainer.isVisible = false
                        binding.nestedLayout.isVisible = true
                        binding.cardCitiesList.isVisible = true
                    }
                }, 3000)  // 3 saniye gecikme
            }
            is HomeState.Error -> {
                // Eğer halihazırda veri gösteriliyorsa ve internet varsa, içeriği tutmaya devam et
                if (binding.cardCitiesList.isVisible && binding.nestedLayout.isVisible) {
                    // Snackbar gösterilecek, içerik kalacak
                } else {
                    // Tam hata ekranını göster
                    binding.swipeRefreshLayout.isVisible = false
                    binding.loadingContainer.isVisible = false
                    binding.errorContainer.isVisible = true
                    binding.textError.text = state.message
                    binding.animationError.playAnimation()
                    binding.nestedLayout.isVisible = false
                }
            }
        }
    }

    private fun updateLoadingState(state: HomeState.Loading) {
        if (state.oldData != null && state.oldData.isNotEmpty()) {
            // Var olan veriyi göstermeye devam et, arka planda yükle
            binding.swipeRefreshLayout.isVisible = true
            binding.loadingContainer.isVisible = false
            binding.errorContainer.isVisible = false
            binding.nestedLayout.isVisible = true
            binding.cardCitiesList.isVisible = true
        } else {
            // Tam loading ekranını göster
            binding.swipeRefreshLayout.isVisible = true
            binding.loadingContainer.isVisible = true
            binding.animationLoading.isVisible = true
            binding.animationLoading.playAnimation()
            binding.errorContainer.isVisible = false
            binding.nestedLayout.isVisible = true
            binding.cardCitiesList.isVisible = false
        }
    }

    fun showErrorUI(errorMessage: String) {
        binding.errorContainer.visibility = View.VISIBLE
        binding.textError.text = errorMessage
        binding.animationError.playAnimation()
        binding.nestedLayout.visibility = View.GONE
        binding.swipeRefreshLayout.isEnabled = false
    }

    fun hideErrorUI() {
        binding.errorContainer.visibility = View.GONE
        binding.nestedLayout.visibility = View.VISIBLE
        binding.swipeRefreshLayout.isEnabled = true
    }

    companion object {
        private const val TAG = "MainStateController"
    }
}