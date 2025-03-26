package com.aliaktas.urbanscore.ui.detail.controllers

import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.fragment.app.FragmentActivity
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
        val context = binding.root.context

        // Context'in hala geçerli olup olmadığını kontrol et
        val activity = context as? FragmentActivity
        if (activity == null || activity.isFinishing || activity.isDestroyed) {
            return
        }

        try {
            MaterialAlertDialogBuilder(context)
                .setTitle(R.string.error)
                .setMessage(message)
                .setPositiveButton(R.string.retry) { dialog, _ ->
                    dialog.dismiss()

                    // Retry işleminden önce loading state'i göster
                    binding.loadingOverlay.visibility = View.VISIBLE
                    binding.animationLoading.playAnimation()

                    // UI thread'de bir gecikme ile retry yaparak animasyonun gösterilmesini sağla
                    binding.root.postDelayed({
                        try {
                            onRetry()
                        } catch (e: Exception) {
                            // Retry sırasında hata olursa, kullanıcıya bildir
                            Toast.makeText(context, "Failed to load: ${e.message}", Toast.LENGTH_SHORT).show()
                            binding.loadingOverlay.visibility = View.GONE
                        }
                    }, 200)
                }
                .setNegativeButton(R.string.back) { dialog, _ ->
                    dialog.dismiss()

                    // UI thread'de bir gecikme ile back işlemini yap
                    binding.root.post {
                        try {
                            onGoBack()
                        } catch (e: Exception) {
                            // Geri dönüş sırasında hata olursa, kullanıcıya bildir
                            // Bu durumda daha fazla bir şey yapamayız, uygulamanın çökmesini engelledik
                            Toast.makeText(context, "Navigation error: ${e.message}", Toast.LENGTH_SHORT).show()
                        }
                    }
                }
                .setCancelable(false) // Kullanıcının dışarı tıklayarak dialogu kapatmasını engelle
                .show()
        } catch (e: Exception) {
            // Dialog oluşturma hatası durumunda, sessionla ilgili bir çökme olmaması için sessizce geç
            Log.e("MainStateController", "Error showing dialog: ${e.message}", e)
        }
    }
}