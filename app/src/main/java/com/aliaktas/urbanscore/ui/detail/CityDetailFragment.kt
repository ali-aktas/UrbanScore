package com.aliaktas.urbanscore.ui.detail

import android.app.AlertDialog
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.text.format.DateFormat
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.isVisible
import android.content.res.ColorStateList
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.launch
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
import com.google.android.material.snackbar.Snackbar
import com.google.android.material.transition.MaterialContainerTransform
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import java.net.URLEncoder
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
        ViewCompat.setTransitionName(binding.toolbar, "city_${args.cityId}")

        setupToolbar()
        setupActionButtons()
        setupExploreButtons()
        observeViewModel()
    }

    private fun setupExploreButtons() {
        binding.btnExploreYouTube.setOnClickListener {
            openYouTubeSearch()
        }

        binding.btnExploreGoogle.setOnClickListener {
            openGoogleSearch()
        }
    }

    private fun openYouTubeSearch() {
        val state = viewModel.state.value
        if (state is CityDetailState.Success) {
            val cityName = state.city.cityName
            //val countryName = state.city.country

            val searchQuery = "$cityName travel guide"
            val encodedQuery = URLEncoder.encode(searchQuery, "UTF-8")
            val youtubeUrl = "https://www.youtube.com/results?search_query=$encodedQuery"

            val intent = Intent(Intent.ACTION_VIEW).apply {
                data = Uri.parse(youtubeUrl)
            }

            startActivity(intent)
        }
    }

    private fun openGoogleSearch() {
        val state = viewModel.state.value
        if (state is CityDetailState.Success) {
            val cityName = state.city.cityName
            //val countryName = state.city.country

            val searchQuery = "$cityName 4k views"
            val encodedQuery = URLEncoder.encode(searchQuery, "UTF-8")
            val googleImagesUrl = "https://www.google.com/search?q=$encodedQuery&tbm=isch"

            val intent = Intent(Intent.ACTION_VIEW).apply {
                data = Uri.parse(googleImagesUrl)
            }

            startActivity(intent)
        }
    }


    private fun setupToolbar() {
        binding.toolbar.setOnClickListener {
            findNavController().navigateUp()
        }
    }

    private fun setupActionButtons() {
        // btnRateCity için mevcut kodu koruyoruz
        binding.btnRateCity.setOnClickListener {
            // Show the rate city bottom sheet instead of navigating
            val bottomSheet = RateCityBottomSheet.newInstance(args.cityId)
            bottomSheet.show(childFragmentManager, "RateCityBottomSheet")
        }

        // Wishlist butonunu duruma göre dinlemek için observer ekliyoruz
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.isInWishlist.collect { isInWishlist ->
                    updateWishlistButton(isInWishlist)
                }
            }
        }
    }



    private fun updateWishlistButton(isInWishlist: Boolean) {
        if (isInWishlist) {
            // Zaten wishlist'te
            binding.btnAddToWishlist.apply {
                text = "Remove from Bucket List"
                backgroundTintList = ContextCompat.getColorStateList(requireContext(), android.R.color.holo_red_light)
                setOnClickListener {
                    showRemoveFromWishlistDialog()
                }
            }
        } else {
            // Wishlist'te değil
            binding.btnAddToWishlist.apply {
                text = getString(R.string.add_to_wishlist)
                backgroundTintList = ContextCompat.getColorStateList(requireContext(), R.color.white)
                setOnClickListener {
                    viewModel.addToWishlist()
                    Snackbar.make(binding.root, "Added to bucket list", Snackbar.LENGTH_SHORT).show()
                }
            }
        }
    }

    // Yeni metod ekleyin
    private fun showRemoveFromWishlistDialog() {
        AlertDialog.Builder(requireContext())
            .setTitle("Remove from Bucket List")
            .setMessage("Are you sure you want to remove this city from your bucket list?")
            .setPositiveButton("Yes") { _, _ ->
                viewModel.removeFromWishlist()
                Snackbar.make(binding.root, "Removed from bucket list", Snackbar.LENGTH_SHORT).show()
            }
            .setNegativeButton("No", null)
            .show()
    }


    private fun observeViewModel() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                // Şehir detaylarını gözlemle
                launch {
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

                // Puanlama durumunu gözlemle
                launch {
                    viewModel.hasRated.collect { hasRated ->
                        updateRateButton(hasRated)
                    }
                }
            }
        }
    }

    // Buton görünümünü güncelleyen yeni metot
    private fun updateRateButton(hasRated: Boolean) {
        if (hasRated) {
            binding.btnRateCity.apply {
                text = "Rated"
                backgroundTintList = ContextCompat.getColorStateList(requireContext(), R.color.rating_color)
                // Seçenek 1: Butonu devre dışı bırak (tekrar puanlama yapılamaz)
                // isEnabled = false

                // Seçenek 2: Butonu aktif bırak (tekrar puanlama yapılabilir)
                setOnClickListener {
                    Snackbar.make(binding.root, "You've already rated this city. Would you like to update your rating?", Snackbar.LENGTH_LONG)
                        .setAction("Update") {
                            // Puanlama bottom sheet'i göster
                            val bottomSheet = RateCityBottomSheet.newInstance(args.cityId)
                            bottomSheet.show(childFragmentManager, "RateCityBottomSheet")
                        }
                        .show()
                }
            }
        } else {
            binding.btnRateCity.apply {
                text = getString(R.string.rate_this_city)
                backgroundTintList = ContextCompat.getColorStateList(requireContext(), R.color.gradient_center)
                isEnabled = true

                // Normal puanlama işlevini geri yükle
                setOnClickListener {
                    val bottomSheet = RateCityBottomSheet.newInstance(args.cityId)
                    bottomSheet.show(childFragmentManager, "RateCityBottomSheet")
                }
            }
        }
    }


    private fun showLoading(isLoading: Boolean) {
        // If using loading animation, control it here
        // For now, just showing/hiding content
    }

    private fun updateUI(city: CityModel) {
        // Şehir adı ve bayrak
        binding.textCityName.text = city.cityName
        binding.txtRatingCount.text = resources.getQuantityString(
            R.plurals.based_on_ratings,
            city.ratingCount,
            city.ratingCount
        )

        // Bayrak yükleme
        Glide.with(this)
            .load(city.flagUrl)
            .centerCrop()
            .into(binding.imgFlag)

        // Şehir bilgileri
        binding.txtCountry.text = city.country
        binding.txtRegion.text = city.region
        binding.txtPopulation.text = NumberFormat.getNumberInstance(Locale.getDefault())
            .format(city.population)

        // Ortalama puanlama
        binding.txtAverageRating.text = String.format("%.2f", city.averageRating)

        // Kategori puanlamaları
        with(city.ratings) {
            binding.txtEnvironmentRating.text = String.format("%.2f", environment)
            binding.txtSafetyRating.text = String.format("%.2f", safety)
            binding.txtLivabilityRating.text = String.format("%.2f", livability)
            binding.txtCostRating.text = String.format("%.2f", cost)
            binding.txtSocialRating.text = String.format("%.2f", social)
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