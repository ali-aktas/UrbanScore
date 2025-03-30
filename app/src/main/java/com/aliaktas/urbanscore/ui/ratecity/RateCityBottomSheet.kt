package com.aliaktas.urbanscore.ui.ratecity

import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.aliaktas.urbanscore.data.model.CategoryRatings
import com.aliaktas.urbanscore.databinding.BottomSheetRateCityBinding
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.google.android.material.bottomsheet.BottomSheetBehavior
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

@AndroidEntryPoint
class RateCityBottomSheet : BottomSheetDialogFragment() {

    private var _binding: BottomSheetRateCityBinding? = null
    private val binding get() = _binding!!

    private val viewModel: RateCityViewModel by viewModels()

    private var cityId: String? = null
    private var isSubmitting = false

    companion object {
        const val ARG_CITY_ID = "cityId"

        fun newInstance(cityId: String): RateCityBottomSheet {
            return RateCityBottomSheet().apply {
                arguments = Bundle().apply {
                    putString(ARG_CITY_ID, cityId)
                }
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        cityId = arguments?.getString(ARG_CITY_ID)

        // Başlangıçta iptal edilebilir - kullanıcı istediği zaman kapatabilir
        isCancelable = true
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = BottomSheetRateCityBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupSliders()
        setupSubmitButton()
        observeViewModel()
    }

    private fun setupSliders() {
        // Gastronomy slider
        binding.sliderGastronomy.addOnChangeListener { _, value, _ ->
            binding.tvGastronomyRating.text = String.format("%.1f", value)
        }

        // Aesthetics slider
        binding.sliderAesthetics.addOnChangeListener { _, value, _ ->
            binding.tvAestheticsRating.text = String.format("%.1f", value)
        }

        // Safety slider
        binding.sliderSafety.addOnChangeListener { _, value, _ ->
            binding.tvSafetyRating.text = String.format("%.1f", value)
        }

        // Culture slider
        binding.sliderCulture.addOnChangeListener { _, value, _ ->
            binding.tvCultureRating.text = String.format("%.1f", value)
        }

        // Livability slider
        binding.sliderLivability.addOnChangeListener { _, value, _ ->
            binding.tvLivabilityRating.text = String.format("%.1f", value)
        }

        // Social slider
        binding.sliderSocial.addOnChangeListener { _, value, _ ->
            binding.tvSocialRating.text = String.format("%.1f", value)
        }

        // Hospitality slider
        binding.sliderHospitality.addOnChangeListener { _, value, _ ->
            binding.tvHospitalityRating.text = String.format("%.1f", value)
        }
    }

    private fun setupSubmitButton() {
        binding.btnSubmitRating.setOnClickListener {
            if (isSubmitting) return@setOnClickListener

            cityId?.let { id ->
                val ratings = CategoryRatings(
                    gastronomy = binding.sliderGastronomy.value.toDouble(),
                    aesthetics = binding.sliderAesthetics.value.toDouble(),
                    safety = binding.sliderSafety.value.toDouble(),
                    culture = binding.sliderCulture.value.toDouble(),
                    livability = binding.sliderLivability.value.toDouble(),
                    social = binding.sliderSocial.value.toDouble(),
                    hospitality = binding.sliderHospitality.value.toDouble()
                )

                // Gönderim sırasında kapatmayı engelle
                isSubmitting = true
                isCancelable = false

                binding.btnSubmitRating.isEnabled = false
                binding.btnSubmitRating.text = "Submitting..."

                // State collection'ı observeViewModel'e bırakıyoruz - burada sadece submit
                viewModel.submitRating(id, ratings)
            }
        }
    }

    private fun observeViewModel() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.ratingState.collectLatest { state ->
                    when (state) {
                        is RateCityState.Initial -> {
                            // Hiçbir şey yapma
                        }
                        is RateCityState.Loading -> {
                            binding.btnSubmitRating.isEnabled = false
                            binding.btnSubmitRating.text = "Submitting..."

                            // Progress bar yerine Lottie animasyonunu göster
                            binding.loadingOverlay.visibility = View.VISIBLE
                            binding.animationLoading.playAnimation()

                            // Gönderim sırasında iptal edilemez
                            isCancelable = false
                        }
                        is RateCityState.Success -> {
                            Log.d("RateCityBottomSheet", "Rating successful, dismissing safely")

                            // Loading overlay'ı gizle
                            binding.loadingOverlay.visibility = View.GONE
                            binding.animationLoading.pauseAnimation()

                            // Başarılı mesajını göster
                            Toast.makeText(requireContext(),
                                "Rating submitted successfully!",
                                Toast.LENGTH_SHORT).show()

                            try {
                                // Safety check - fragment still attached
                                if (!isAdded) return@collectLatest

                                // Kısa bir gecikme ile dismiss
                                Handler(Looper.getMainLooper()).postDelayed({
                                    if (isAdded && !isRemoving && !isDetached) {
                                        dismiss()
                                    }
                                }, 200)
                            } catch (e: Exception) {
                                Log.e("RateCityBottomSheet", "Error dismissing: ${e.message}", e)
                                // Alternatif yöntem
                                try {
                                    if (isAdded) dismiss()
                                } catch (e2: Exception) {
                                    Log.e("RateCityBottomSheet", "Final dismiss failed", e2)
                                }
                            }
                        }
                        is RateCityState.Error -> {
                            // Loading overlay'ı gizle
                            binding.loadingOverlay.visibility = View.GONE
                            binding.animationLoading.pauseAnimation()

                            // Hata durumunda UI'ı yeniden etkinleştir
                            isSubmitting = false
                            isCancelable = true

                            binding.btnSubmitRating.isEnabled = true
                            binding.btnSubmitRating.text = "Submit Rating"
                            Toast.makeText(requireContext(), state.message, Toast.LENGTH_LONG).show()
                        }
                    }
                }
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}