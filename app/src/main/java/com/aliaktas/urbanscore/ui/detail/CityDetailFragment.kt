package com.aliaktas.urbanscore.ui.detail

import android.content.Intent
import android.os.Bundle
import android.text.format.DateFormat
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
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
import com.bumptech.glide.Glide
import com.google.android.material.snackbar.Snackbar
import com.google.android.material.transition.MaterialContainerTransform
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import java.text.NumberFormat
import java.util.Locale

@AndroidEntryPoint
class CityDetailFragment : Fragment() {

    private var _binding: FragmentCityDetailBinding? = null
    private val binding get() = _binding!!

    private val viewModel: CityDetailViewModel by viewModels()
    private val args: CityDetailFragmentArgs by navArgs()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Setup shared element transition animation
        sharedElementEnterTransition = MaterialContainerTransform().apply {
            duration = 300L
            scrimColor = ContextCompat.getColor(requireContext(), R.color.gradient_center)
            fadeMode = MaterialContainerTransform.FADE_MODE_THROUGH
        }
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

        // Set the transition name for shared element transition
        ViewCompat.setTransitionName(binding.appBarLayout, "city_${args.cityId}")

        setupToolbar()
        setupActionButtons()
        observeViewModel()
    }

    private fun setupToolbar() {
        binding.toolbar.setNavigationOnClickListener {
            findNavController().navigateUp()
        }
    }

    private fun setupActionButtons() {
        binding.btnAddToWishlist.setOnClickListener {
            // Implement wishlist logic
            Snackbar.make(binding.root, "Added to wishlist", Snackbar.LENGTH_SHORT).show()
            viewModel.addToWishlist()
        }

        binding.btnRateCity.setOnClickListener {
            // Navigate to rating screen
            // findNavController().navigate(CityDetailFragmentDirections.actionDetailToRate(args.cityId))
            Snackbar.make(binding.root, "Rate city functionality coming soon", Snackbar.LENGTH_SHORT).show()
        }

        binding.fabShare.setOnClickListener {
            shareCity()
        }
    }

    private fun observeViewModel() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.state.collect { state ->
                    when (state) {
                        is CityDetailState.Loading -> showLoading(true)
                        is CityDetailState.Success -> {
                            showLoading(false)
                            updateUI(state.city)
                        }
                        is CityDetailState.Error -> {
                            showLoading(false)
                            Snackbar.make(binding.root, state.message, Snackbar.LENGTH_LONG).show()
                        }
                    }
                }
            }
        }
    }

    private fun showLoading(isLoading: Boolean) {
        // If using loading animation, control it here
        // For now, just showing/hiding content
        binding.appBarLayout.isVisible = !isLoading
        binding.fabShare.isVisible = !isLoading
    }

    private fun updateUI(city: CityModel) {
        // Set the collapsing toolbar title
        binding.collapsingToolbar.title = "${city.cityName}, ${city.country}"

        // Load the city background image (could be a map or skyline)
        // Using a placeholder for now
        Glide.with(this)
            .load(R.drawable.default_city_background) // Later replace with city.imageUrl
            .centerCrop()
            .into(binding.imgCityBackground)

        // Load the flag
        Glide.with(this)
            .load(city.flagUrl)
            .centerCrop()
            .into(binding.imgFlag)

        // Format population with commas
        val formattedPopulation = NumberFormat.getNumberInstance(Locale.getDefault())
            .format(city.population)
        binding.txtPopulation.text = getString(R.string.population_format, formattedPopulation)

        // Set rating count
        binding.txtRatingCount.text = resources.getQuantityString(
            R.plurals.based_on_ratings,
            city.ratingCount,
            city.ratingCount
        )

        // Set average rating
        binding.txtAverageRating.text = String.format("%.1f", city.averageRating)

        // Set individual category ratings
        with(city.ratings) {
            binding.txtSafetyRating.text = String.format("%.1f", safetySerenity)
            binding.txtLandscapeRating.text = String.format("%.1f", landscapeVibe)

            // Set other category ratings similarly
            // Example: binding.txtCuisineRating.text = String.format("%.1f", cuisine)
        }
    }

    private fun shareCity() {
        val state = viewModel.state.value
        if (state is CityDetailState.Success) {
            val city = state.city
            val shareText = getString(
                R.string.share_city_format,
                city.cityName,
                city.country,
                city.averageRating
            )

            val intent = Intent(Intent.ACTION_SEND).apply {
                type = "text/plain"
                putExtra(Intent.EXTRA_SUBJECT, "Check out ${city.cityName} on UrbanScore")
                putExtra(Intent.EXTRA_TEXT, shareText)
            }

            startActivity(Intent.createChooser(intent, "Share via"))
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}