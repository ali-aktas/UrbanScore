package com.aliaktas.urbanscore.ui.ratecity

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import com.aliaktas.urbanscore.data.model.CategoryRatings
import com.aliaktas.urbanscore.databinding.BottomSheetRateCityBinding
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.google.android.material.slider.Slider
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class RateCityBottomSheet : BottomSheetDialogFragment() {

    private var _binding: BottomSheetRateCityBinding? = null
    private val binding get() = _binding!!

    private val viewModel: RateCityViewModel by viewModels()

    private var cityId: String? = null

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
        // Environment slider
        binding.sliderEnvironment.addOnChangeListener { _, value, _ ->
            binding.tvEnvironmentRating.text = String.format("%.1f", value)
        }

        // Safety slider
        binding.sliderSafety.addOnChangeListener { _, value, _ ->
            binding.tvSafetyRating.text = String.format("%.1f", value)
        }

        // Livability slider
        binding.sliderLivability.addOnChangeListener { _, value, _ ->
            binding.tvLivabilityRating.text = String.format("%.1f", value)
        }

        // Cost slider
        binding.sliderCost.addOnChangeListener { _, value, _ ->
            binding.tvCostRating.text = String.format("%.1f", value)
        }

        // Social slider
        binding.sliderSocial.addOnChangeListener { _, value, _ ->
            binding.tvSocialRating.text = String.format("%.1f", value)
        }
    }

    private fun setupSubmitButton() {
        binding.btnSubmitRating.setOnClickListener {
            cityId?.let { id ->
                val ratings = CategoryRatings(
                    environment = binding.sliderEnvironment.value.toDouble(),
                    safety = binding.sliderSafety.value.toDouble(),
                    livability = binding.sliderLivability.value.toDouble(),
                    cost = binding.sliderCost.value.toDouble(),
                    social = binding.sliderSocial.value.toDouble()
                )
                viewModel.submitRating(id, ratings)
            } ?: run {
                Toast.makeText(context, "City ID not found", Toast.LENGTH_SHORT).show()
                dismiss()
            }
        }
    }

    private fun observeViewModel() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.ratingState.collect { state ->
                when (state) {
                    is RateCityState.Initial -> {
                        // Do nothing
                    }
                    is RateCityState.Loading -> {
                        binding.btnSubmitRating.isEnabled = false
                        binding.btnSubmitRating.text = "Submitting..."
                    }
                    is RateCityState.Success -> {
                        Toast.makeText(context, "Rating submitted successfully!", Toast.LENGTH_SHORT).show()
                        dismiss()
                    }
                    is RateCityState.Error -> {
                        binding.btnSubmitRating.isEnabled = true
                        binding.btnSubmitRating.text = "Submit Rating"
                        Toast.makeText(context, state.message, Toast.LENGTH_LONG).show()
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