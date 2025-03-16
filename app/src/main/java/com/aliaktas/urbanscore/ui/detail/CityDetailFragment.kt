package com.aliaktas.urbanscore.ui.detail

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import com.aliaktas.urbanscore.R
import com.aliaktas.urbanscore.data.model.CityModel
import com.aliaktas.urbanscore.databinding.FragmentCityDetailBinding
import com.aliaktas.urbanscore.ui.ratecity.RateCityBottomSheet
import com.bumptech.glide.Glide
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.Snackbar
import com.google.android.material.transition.MaterialContainerTransform
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import java.text.NumberFormat
import java.util.Locale

@AndroidEntryPoint
class CityDetailFragment : Fragment() {

    private var _binding: FragmentCityDetailBinding? = null
    private val binding get() = _binding!!

    private val viewModel: CityDetailViewModel by viewModels()
    private val args: CityDetailFragmentArgs by navArgs()

    private lateinit var radarChartHelper: RadarChartHelper

    // Reference to rating bottom sheet for lifecycle management
    private var ratingBottomSheet: RateCityBottomSheet? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Setup shared element transition
        sharedElementEnterTransition = MaterialContainerTransform().apply {
            duration = 300L
            fadeMode = MaterialContainerTransform.FADE_MODE_THROUGH
        }

        // Initialize chart helper
        radarChartHelper = RadarChartHelper(requireContext())
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentCityDetailBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Setup transition name for shared element transition
        binding.toolbar.transitionName = "city_${args.cityId}"

        setupClickListeners()
        setupRadarChart()
        observeViewModel()
    }

    private fun setupRadarChart() {
        // Launch coroutine to setup chart (UI thread work)
        lifecycleScope.launch {
            radarChartHelper.setupRadarChart(binding.radarChart)
        }
    }

    private fun setupClickListeners() {
        // Back button
        binding.toolbar.setOnClickListener {
            requireActivity().onBackPressedDispatcher.onBackPressed()
        }

        // Action buttons
        binding.btnRateCity.setOnClickListener {
            viewModel.showRatingSheet()
        }

        binding.btnAddToWishlist.setOnClickListener {
            viewModel.toggleWishlist()
        }

        // Explore buttons
        binding.btnExploreYouTube.setOnClickListener {
            viewModel.openYouTubeSearch()
        }

        binding.btnExploreGoogle.setOnClickListener {
            viewModel.openGoogleSearch()
        }
    }

    private fun observeViewModel() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                // Observe state
                launch {
                    viewModel.detailState.collectLatest { state ->
                        updateUI(state)
                    }
                }

                // Observe events
                launch {
                    viewModel.detailEvents.collectLatest { event ->
                        handleEvent(event)
                    }
                }
            }
        }
    }

    private fun handleEvent(event: CityDetailEvent) {
        when (event) {
            is CityDetailEvent.OpenUrl -> {
                openUrl(event.url)
            }
            is CityDetailEvent.ShowRatingSheet -> {
                showRatingBottomSheet(event.cityId)
            }
            is CityDetailEvent.ShowMessage -> {
                showMessage(event.message)
            }
            is CityDetailEvent.ShareCity -> {
                startActivity(Intent.createChooser(event.shareIntent, "Share via"))
            }
            is CityDetailEvent.DismissRatingSheet -> {
                ratingBottomSheet?.dismiss()
                ratingBottomSheet = null
            }
        }
    }

    private fun openUrl(url: String) {
        try {
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
            startActivity(intent)
        } catch (e: Exception) {
            showMessage("Could not open link: ${e.message}")
        }
    }

    private fun showRatingBottomSheet(cityId: String) {
        try {
            Log.d("CityDetailFragment", "Creating rating sheet for cityId: $cityId")
            val bottomSheet = RateCityBottomSheet.newInstance(cityId)

            Log.d("CityDetailFragment", "About to show rating sheet")
            bottomSheet.show(parentFragmentManager, "RateCityBottomSheet")

            Log.d("CityDetailFragment", "Rating sheet shown")
            ratingBottomSheet = bottomSheet
        } catch (e: Exception) {
            Log.e("CityDetailFragment", "Error showing rating sheet: ${e.message}", e)
            e.printStackTrace()
            Toast.makeText(requireContext(), "Could not open rating screen: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    private fun showMessage(message: String) {
        Snackbar.make(binding.root, message, Snackbar.LENGTH_SHORT).show()
    }

    private fun updateUI(state: CityDetailState) {
        when (state) {
            is CityDetailState.Loading -> {
                binding.progressBar.isVisible = true
                binding.cityInfoLayout.isVisible = false
            }
            is CityDetailState.Success -> {
                binding.progressBar.isVisible = false
                binding.cityInfoLayout.isVisible = true

                updateCityInfo(state.city)
                updateRatingData(state.city)
                updateWishlistButton(state.isInWishlist)
                updateRateButton(state.hasUserRated)
            }
            is CityDetailState.Error -> {
                binding.progressBar.isVisible = false
                showErrorDialog(state.message)
            }
        }
    }

    private fun updateCityInfo(city: CityModel) {
        // City name and flag
        binding.textCityName.text = city.cityName
        binding.txtRatingCount.text = resources.getQuantityString(
            R.plurals.based_on_ratings,
            city.ratingCount,
            city.ratingCount
        )

        // Load flag
        Glide.with(this)
            .load(city.flagUrl)
            .centerCrop()
            .into(binding.imgFlag)

        // City information
        binding.txtCountry.text = city.country
        binding.txtRegion.text = city.region
        binding.txtPopulation.text = NumberFormat.getNumberInstance(Locale.getDefault())
            .format(city.population)

        // Average rating
        binding.txtAverageRating.text = String.format("%.2f", city.averageRating)
    }

    private fun updateRatingData(city: CityModel) {
        // Update category ratings
        with(city.ratings) {
            binding.txtEnvironmentRating.text = String.format("%.2f", environment)
            binding.txtSafetyRating.text = String.format("%.2f", safety)
            binding.txtLivabilityRating.text = String.format("%.2f", livability)
            binding.txtCostRating.text = String.format("%.2f", cost)
            binding.txtSocialRating.text = String.format("%.2f", social)
        }

        // Update radar chart in background
        lifecycleScope.launch {
            radarChartHelper.updateChartData(binding.radarChart, city.ratings)
        }
    }

    private fun updateWishlistButton(isInWishlist: Boolean) {
        binding.btnAddToWishlist.apply {
            if (isInWishlist) {
                text = "In your list"
                setBackgroundColor(resources.getColor(android.R.color.holo_red_light, null))
            } else {
                text = getString(R.string.add_to_wishlist)
                setBackgroundColor(resources.getColor(R.color.white, null))
            }
        }
    }

    private fun updateRateButton(hasRated: Boolean) {
        binding.btnRateCity.setOnClickListener {
            try {
                viewModel.showRatingSheet()
            } catch (e: Exception) {
                Log.e("CityDetailFragment", "Error in rate button click: ${e.message}", e)
                Toast.makeText(requireContext(), "Error: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun showErrorDialog(message: String) {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Error")
            .setMessage(message)
            .setPositiveButton("Retry") { _, _ -> viewModel.loadCityDetails() }
            .setNegativeButton("Go Back") { _, _ -> findNavController().navigateUp() }
            .show()
    }

    override fun onResume() {
        super.onResume()
        // Refresh data if returning from rating bottom sheet
        if (ratingBottomSheet?.isAdded == true && !ratingBottomSheet?.isVisible!!) {
            viewModel.refreshAfterRating()
            ratingBottomSheet = null
        }
    }


    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
        ratingBottomSheet = null
    }
}