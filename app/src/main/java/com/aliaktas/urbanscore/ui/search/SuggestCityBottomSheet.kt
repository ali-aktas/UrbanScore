package com.aliaktas.urbanscore.ui.search

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.viewModels
import com.aliaktas.urbanscore.databinding.CitySuggestionBottomSheetBinding
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class SuggestCityBottomSheet : BottomSheetDialogFragment() {

    private var _binding: CitySuggestionBottomSheetBinding? = null
    private val binding get() = _binding!!

    private val viewModel: CityRecommendationViewModel by viewModels()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = CitySuggestionBottomSheetBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupListeners()
    }

    private fun setupListeners() {
        binding.btnSubmitSuggestion.setOnClickListener {
            val cityName = binding.etCityName.text.toString().trim()
            val country = binding.etCountry.text.toString().trim()
            val description = binding.etDescription.text.toString().trim()

            when {
                cityName.isEmpty() -> {
                    binding.tilCityName.error = "City name is required"
                    return@setOnClickListener
                }
                country.isEmpty() -> {
                    binding.tilCountry.error = "Country is required"
                    return@setOnClickListener
                }
                else -> {
                    viewModel.submitCityRecommendation(cityName, country, description)
                }
            }
        }

        // Observe submission state
        viewModel.submissionState.observe(viewLifecycleOwner) { state ->
            when (state) {
                is CityRecommendationViewModel.SubmissionState.Success -> {
                    Toast.makeText(
                        requireContext(),
                        "We received your city suggestion. Thank you for helping us improve!",
                        Toast.LENGTH_LONG
                    ).show()
                    dismiss()
                }
                is CityRecommendationViewModel.SubmissionState.Error -> {
                    Toast.makeText(
                        requireContext(),
                        "Error: ${state.message}",
                        Toast.LENGTH_LONG
                    ).show()
                }
                else -> {} // Loading state, potentially show progress
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}