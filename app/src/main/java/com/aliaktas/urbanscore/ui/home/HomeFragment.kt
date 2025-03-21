package com.aliaktas.urbanscore.ui.home

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
import androidx.recyclerview.widget.LinearLayoutManager
import com.aliaktas.urbanscore.MainActivity
import com.aliaktas.urbanscore.R
import com.aliaktas.urbanscore.base.BaseViewModel
import com.aliaktas.urbanscore.data.model.CategoryModel
import com.aliaktas.urbanscore.data.model.CuratedCityItem
import com.aliaktas.urbanscore.databinding.FragmentHomeBinding
import com.aliaktas.urbanscore.ui.common.EditorsChoiceAdapter
import com.google.android.material.snackbar.Snackbar
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

@AndroidEntryPoint
class HomeFragment : Fragment() {

    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!

    private val viewModel: HomeViewModel by viewModels()

    private val citiesAdapter = CitiesAdapter()
    private val categoriesAdapter = CategoriesAdapter()
    private val editorsChoiceAdapter = EditorsChoiceAdapter() // Yeni adaptör

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

        setupRecyclerViews()
        setupClickListeners()
        setupAnimations()
        setupSwipeRefresh() // SwipeRefresh setup'ı gözlemcilerden önce yapılmalı
        observeViewModel()
    }

    override fun onResume() {
        super.onResume()
        // Her zaman HomeFragment'a döndüğümüzde top rated cities listesini yeniliyoruz
        // Bu, kategori seçiminden sonra Ana sayfaya döndüğümüzde listenin etkilenmemesini sağlar
        viewModel.refreshOnReturn()
    }

    private fun setupSwipeRefresh() {
        binding.swipeRefreshLayout.setOnRefreshListener {
            viewModel.loadTopRatedCities(true) // Force refresh
        }

        binding.swipeRefreshLayout.setColorSchemeResources(
            R.color.auth_accent,
            R.color.auth_primary
        )
    }

    private fun setupRecyclerViews() {
        // Categories RecyclerView
        binding.recyclerViewCategories.apply {
            adapter = categoriesAdapter
            setHasFixedSize(true)
            itemAnimator = null
        }

        // Kategorileri göster (değişmez veriler)
        categoriesAdapter.submitList(CategoryModel.getDefaultCategories())
        categoriesAdapter.onItemClick = { category ->
            navigateToCategoryList(category.ratingType)
        }

        // Top Rated Cities RecyclerView
        binding.recyclerViewCities.apply {
            adapter = citiesAdapter
            setHasFixedSize(false)
            recycledViewPool.setMaxRecycledViews(0, 15)
        }
        citiesAdapter.onItemClick = { city ->
            navigateToCityDetail(city.id)
        }

        // Editors' Choice RecyclerView (önceki grid layoutu yerine)
        binding.recyclerViewEditorsChoice.apply {
            adapter = editorsChoiceAdapter
            layoutManager = LinearLayoutManager(context, LinearLayoutManager.HORIZONTAL, false)
            setHasFixedSize(false)
        }
        editorsChoiceAdapter.onItemClick = { cityId ->
            navigateToCityDetail(cityId)
        }
    }

    private fun setupClickListeners() {
        // "View All" button click - simple navigation to CategoryList
        binding.txtViewListFragment.setOnClickListener {
            navigateToCategoryList()
        }
        binding.cardCitiesList.setOnClickListener {
            navigateToCategoryList()
        }
        binding.btnRetry.setOnClickListener {
            viewModel.loadTopRatedCities(true)
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

    private fun observeViewModel() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                // Observe top rated cities state
                launch {
                    viewModel.topRatedCitiesState.collect { state ->
                        Log.d("HomeFragment", "State update: ${state.javaClass.simpleName}")
                        updateTopRatedCitiesUI(state)

                        // SwipeRefreshLayout durumunu güncelle
                        binding.swipeRefreshLayout.isRefreshing = state is HomeState.Loading &&
                                state.oldData != null
                    }
                }

                // Observe editors' choice cities
                launch {
                    viewModel.editorsChoiceState.collectLatest { cities ->
                        editorsChoiceAdapter.submitList(cities)
                    }
                }

                // Observe loading state
                launch {
                    viewModel.isLoading.collect { isLoading ->
                        binding.animationLoading.isVisible = isLoading &&
                                (viewModel.topRatedCitiesState.value is HomeState.Initial)
                    }
                }

                // Observe events
                launch {
                    viewModel.events.collect { event ->
                        handleEvent(event)
                    }
                }
            }
        }
    }

    private fun updateTopRatedCitiesUI(state: HomeState) {
        when (state) {
            is HomeState.Initial -> {
                binding.swipeRefreshLayout.isVisible = true
                binding.loadingContainer.isVisible = false
                binding.errorContainer.isVisible = false
                binding.nestedLayout.isVisible = true
                binding.cardCitiesList.isVisible = false
                Log.d("HomeFragment", "UI State: Initial")
            }
            is HomeState.Loading -> {
                if (state.oldData != null && state.oldData.isNotEmpty()) {
                    binding.swipeRefreshLayout.isVisible = true
                    binding.loadingContainer.isVisible = false
                    binding.errorContainer.isVisible = false
                    binding.nestedLayout.isVisible = true
                    binding.cardCitiesList.isVisible = true
                    Log.d("HomeFragment", "UI State: Loading with existing data")
                } else {
                    binding.swipeRefreshLayout.isVisible = true
                    binding.loadingContainer.isVisible = true
                    binding.animationLoading.isVisible = true
                    binding.animationLoading.playAnimation()
                    binding.errorContainer.isVisible = false
                    binding.nestedLayout.isVisible = true
                    binding.cardCitiesList.isVisible = false
                    Log.d("HomeFragment", "UI State: Loading new data")
                }
            }
            is HomeState.Success -> {
                binding.swipeRefreshLayout.isVisible = true
                binding.loadingContainer.isVisible = false
                binding.errorContainer.isVisible = false
                binding.nestedLayout.isVisible = true
                binding.cardCitiesList.isVisible = true
                citiesAdapter.submitList(state.cities)
                Log.d("HomeFragment", "UI State: Success with ${state.cities.size} cities")
            }
            is HomeState.Error -> {
                if (citiesAdapter.itemCount > 0) {
                    // Eğer daha önce yüklenmiş şehirler varsa, onları göstermeye devam et
                    binding.swipeRefreshLayout.isVisible = true
                    binding.loadingContainer.isVisible = false
                    binding.errorContainer.isVisible = false
                    binding.nestedLayout.isVisible = true
                    Toast.makeText(context, state.message, Toast.LENGTH_SHORT).show()
                    Log.d("HomeFragment", "UI State: Error with existing content")
                } else {
                    // Hiç içerik yoksa, hata ekranını göster, içeriği gizle
                    binding.swipeRefreshLayout.isVisible = false  // SwipeRefresh'i gizle
                    binding.loadingContainer.isVisible = false
                    binding.errorContainer.isVisible = true
                    binding.textError.text = state.message
                    binding.animationError.playAnimation()
                    binding.nestedLayout.isVisible = false  // Ana içeriği gizle
                    Log.d("HomeFragment", "UI State: Error with no content")
                }
            }
        }
    }

    private fun handleEvent(event: BaseViewModel.UiEvent) {
        when (event) {
            is BaseViewModel.UiEvent.Error -> {
                Snackbar.make(binding.root, event.message, Snackbar.LENGTH_LONG).show()
            }
            is BaseViewModel.UiEvent.Success -> {
                Toast.makeText(context, event.message, Toast.LENGTH_SHORT).show()
            }
            else -> {
                // Handle other events
            }
        }
    }

    private fun navigateToCategoryList(categoryId: String? = null) {
        try {
            // Navigation Component yerine MainActivity API'sini kullan
            (requireActivity() as MainActivity).navigateToCategoryList(categoryId ?: "averageRating")
        } catch (e: Exception) {
            Log.e("HomeFragment", "Navigation error: ${e.message}", e)
            Toast.makeText(context, "Navigation error", Toast.LENGTH_SHORT).show()
        }
    }

    private fun navigateToCityDetail(cityId: String) {
        try {
            (requireActivity() as MainActivity).navigateToCityDetail(cityId)
        } catch (e: Exception) {
            Log.e("HomeFragment", "Navigation error: ${e.message}", e)
            Toast.makeText(context, "Navigation error", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}