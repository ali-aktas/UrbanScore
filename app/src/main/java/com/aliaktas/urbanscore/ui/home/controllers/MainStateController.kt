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
        // Lottie animasyonlarını artık kullanmıyoruz
    }

    private fun setupErrorUI() {
        binding.btnRetry.setOnClickListener {
            Log.d(TAG, "Retry button clicked")
            onRetry()
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
                // Animasyon olmadan doğrudan içeriği göster
                binding.swipeRefreshLayout.isVisible = true
                binding.loadingContainer.isVisible = false
                binding.errorContainer.isVisible = false
                binding.nestedLayout.isVisible = true
                binding.cardCitiesList.isVisible = true
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
            // Loading durumunda skeleton gösterdiğimiz için burada hiçbir şey yapmıyoruz
            // Tüm loading gösterimi HomeFragment içinde yapılıyor
        }
    }

    fun showErrorUI(errorMessage: String) {
        binding.errorContainer.visibility = View.VISIBLE
        binding.textError.text = errorMessage
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