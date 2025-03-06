package com.aliaktas.urbanscore.ui.home

import android.graphics.Color
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.graphics.ColorUtils
import androidx.core.view.ViewCompat
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.interpolator.view.animation.FastOutSlowInInterpolator
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.FragmentNavigatorExtras
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.aliaktas.urbanscore.R
import com.aliaktas.urbanscore.data.model.CategoryModel
import com.aliaktas.urbanscore.databinding.FragmentHomeBinding
import com.google.android.material.transition.Hold
import com.google.android.material.transition.MaterialContainerTransform
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch


@AndroidEntryPoint
class HomeFragment : Fragment() {

    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!

    private val viewModel: HomeViewModel by viewModels()
    private val citiesAdapter = CitiesAdapter()
    private val categoriesAdapter = CategoriesAdapter()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Çıkış animasyonu için özelleştirilmiş ayarlar (kart büyürken)
        exitTransition = Hold().apply {
            duration = 300L
        }

        sharedElementReturnTransition = MaterialContainerTransform().apply {
            duration = 450L
            interpolator = FastOutSlowInInterpolator()
            fadeMode = MaterialContainerTransform.FADE_MODE_THROUGH
            fadeProgressThresholds = MaterialContainerTransform.ProgressThresholds(0.2f, 0.9f)
            scrimColor = ColorUtils.setAlphaComponent(
                requireContext().getColor(android.R.color.black), 60
            )
            setAllContainerColors(Color.TRANSPARENT) // Transparanlık için
        }

    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentHomeBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupCategoriesRecyclerView()
        setupCitiesRecyclerView()
        setupSwipeRefresh()
        setupCitiesCardClick()
        setupLottieAnimations()
        observeViewModel()
    }

    private fun setupCitiesCardClick() {
        // View All butonuna tıklama
        binding.txtViewListFragment.setOnClickListener {
            navigateToCategoryList()
        }

        // Başlık tıklaması (isteğe bağlı)
        binding.txtTopRatedTitle.setOnClickListener {
            navigateToCategoryList()
        }
    }

    private fun navigateToCategoryList() {
        try {
            // Geçiş yaparken paylaşılacak view'i belirle
            val citiesCard = binding.cardCitiesList

            // Shared element transition için transition adını ayarla
            ViewCompat.setTransitionName(citiesCard, "cities_list_transition")

            // FragmentNavigatorExtras ile paylaşılan öğeyi tanımla
            val extras = FragmentNavigatorExtras(citiesCard to "cities_list_transition")

            // CategoryListFragment'a paylaşılan öğe geçişi ile yönlendir
            findNavController().navigate(
                R.id.action_homeFragment_to_categoryListFragment,
                null,
                null,
                extras
            )
        } catch (e: Exception) {
            Log.e("HomeFragment", "Navigation error: ${e.message}", e)
            Toast.makeText(context, "Navigation error: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    private fun setupCategoriesRecyclerView() {
        binding.recyclerViewCategories.apply {
            adapter = categoriesAdapter
            // No need to set LayoutManager as it's already set in XML
        }

        // Set categories data from the static method
        categoriesAdapter.submitList(CategoryModel.getDefaultCategories())

        categoriesAdapter.onItemClick = { category ->
            // Navigate to category detail
            Log.d("HomeFragment", "Category clicked: ${category.title}")
            // Implement navigation to category details screen
        }
    }

    private fun setupCitiesRecyclerView() {
        binding.recyclerViewCities.apply {
            adapter = citiesAdapter
            // No need to set LayoutManager as it's already set in XML
        }

        citiesAdapter.onItemClick = { city ->
            try {
                // Log for debugging
                Log.d("HomeFragment", "City clicked: ${city.cityName}")

                // Simple navigation without shared element for now
                val action = HomeFragmentDirections.actionHomeFragmentToCityDetailFragment(city.id)
                findNavController().navigate(action)

                /*
                // Once basic navigation works, you can try the shared element transition:
                val cityItem = (binding.recyclerViewCities.findViewHolderForAdapterPosition(
                    citiesAdapter.currentList.indexOf(city)
                )?.itemView) as? View

                if (cityItem != null) {
                    // Set the transition name
                    ViewCompat.setTransitionName(cityItem, "city_${city.id}")

                    // Navigate with shared element transition
                    val extras = FragmentNavigatorExtras(cityItem to "city_${city.id}")
                    val direction = HomeFragmentDirections.actionHomeFragmentToCityDetailFragment(city.id)
                    findNavController().navigate(direction, extras)
                } else {
                    // Fallback if view holder is null
                    val action = HomeFragmentDirections.actionHomeFragmentToCityDetailFragment(city.id)
                    findNavController().navigate(action)
                }
                */
            } catch (e: Exception) {
                Log.e("HomeFragment", "Navigation error: ${e.message}", e)
                Toast.makeText(context, "Navigation error: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }

    }

    private fun setupSwipeRefresh() {
        // If you're using SwipeRefreshLayout, set it up here
        // binding.swipeRefresh.setOnRefreshListener {
        //     viewModel.refreshCities()
        // }
    }


    private fun setupLottieAnimations() {
        // Setup loading animation
        binding.animationLoading.apply {
            setAnimation(R.raw.loading_animation)
            playAnimation()
        }

        // Setup error animation
        binding.animationError.apply {
            setAnimation(R.raw.error_animation)
            playAnimation()
        }

        binding.btnRetry.setOnClickListener {
            viewModel.refreshCities()
        }
    }

    private fun observeViewModel() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.state.collect { state ->
                    updateUI(state)
                }
            }
        }
    }

    private fun updateUI(state: HomeState) {
        when (state) {
            is HomeState.Initial -> {
                // Hiçbir şey yapma, UI varsayılan durumda kalsın
                binding.loadingContainer.isVisible = false
                binding.errorContainer.isVisible = false
                binding.cardCitiesList.isVisible = false
            }
            is HomeState.Loading -> {
                binding.loadingContainer.isVisible = false
                binding.errorContainer.isVisible = false
                binding.cardCitiesList.isVisible = false
            }
            is HomeState.Success -> {
                binding.loadingContainer.isVisible = false
                binding.errorContainer.isVisible = false
                binding.cardCitiesList.isVisible = true

                citiesAdapter.submitList(state.cities)

                // Debug log
                Log.d("HomeFragment", "Received ${state.cities.size} cities")
            }
            is HomeState.Error -> {
                binding.loadingContainer.isVisible = false
                binding.errorContainer.isVisible = true
                binding.cardCitiesList.isVisible = false
                binding.textError.text = state.message

                // Debug error log
                Log.e("HomeFragment", "Error: ${state.message}")
            }
        }
    }


    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}