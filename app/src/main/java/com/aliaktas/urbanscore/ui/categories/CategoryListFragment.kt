package com.aliaktas.urbanscore.ui.categories

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
import com.aliaktas.urbanscore.MainActivity
import com.aliaktas.urbanscore.databinding.FragmentCategoryListBinding
import com.aliaktas.urbanscore.ui.home.CitiesAdapter
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class CategoryListFragment : Fragment() {

    private var _binding: FragmentCategoryListBinding? = null
    private val binding get() = _binding!!

    private val viewModel: CategoryListViewModel by viewModels()
    private val citiesAdapter = CitiesAdapter()
    private val args: CategoryListFragmentArgs by navArgs()
    private lateinit var categoryId: String

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        categoryId = args.categoryId
        Log.d("CategoryListFragment", "Created with category ID: $categoryId")
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentCategoryListBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        Log.d("CategoryListFragment", "onViewCreated")

        setCategoryTitle()
        setupRecyclerView()
        setupButtons()
        observeViewModel()
    }

    private fun setCategoryTitle() {
        val title = when (categoryId) {
            "environment" -> "Best Cities for Landscapes & Aesthetics"
            "safety" -> "Best Cities for Safety & Tranquility"
            "livability" -> "Best Cities for Livability"
            "cost" -> "Best Cities for Cost of Living"
            "social" -> "Best Cities for Social & Cultural Life"
            else -> "Top Rated Cities Overall"
        }
        binding.txtTitle.text = title
        Log.d("CategoryListFragment", "Set title to: $title")
    }

    private fun setupRecyclerView() {
        binding.recyclerViewCategoryList.adapter = citiesAdapter

        citiesAdapter.onItemClick = { city ->
            Log.d("CategoryListFragment", "City clicked: ${city.cityName}")
            (requireActivity() as MainActivity).navigateToCityDetail(city.id)
        }
    }

    private fun setupButtons() {
        binding.btnBack.setOnClickListener {
            requireActivity().onBackPressedDispatcher.onBackPressed()
        }

        binding.btnLoadMore.setOnClickListener {
            Log.d("CategoryListFragment", "Load more button clicked")
            binding.btnLoadMore.isEnabled = false
            binding.loadingMore.visibility = View.VISIBLE
            viewModel.loadMoreCities()
        }
    }

    private fun observeViewModel() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.state.collect { state ->
                    Log.d("CategoryListFragment", "State changed: ${state.javaClass.simpleName}")
                    updateUI(state)
                }
            }
        }
    }

    private fun updateUI(state: CategoryListState) {
        when (state) {
            is CategoryListState.Initial -> {
                Log.d("CategoryListFragment", "UI State: Initial")
                binding.progressBar.isVisible = false
                binding.loadingMore.isVisible = false
                binding.btnLoadMore.isVisible = false
            }
            is CategoryListState.Loading -> {
                if (state.isLoadingMore) {
                    Log.d("CategoryListFragment", "UI State: Loading More")
                    binding.progressBar.isVisible = false
                    binding.loadingMore.isVisible = true
                    binding.btnLoadMore.isVisible = false
                    state.oldData?.let { citiesAdapter.submitList(it) }
                } else {
                    Log.d("CategoryListFragment", "UI State: Initial Loading")
                    binding.progressBar.isVisible = true
                    binding.loadingMore.isVisible = false
                    binding.btnLoadMore.isVisible = false
                }
            }
            is CategoryListState.Success -> {
                Log.d("CategoryListFragment", "UI State: Success - ${state.cities.size} cities, hasMore=${state.hasMoreItems}")
                binding.progressBar.isVisible = false
                binding.loadingMore.isVisible = false
                binding.btnLoadMore.isEnabled = true
                binding.btnLoadMore.isVisible = state.hasMoreItems

                citiesAdapter.submitList(state.cities)
            }
            is CategoryListState.Error -> {
                Log.e("CategoryListFragment", "UI State: Error - ${state.message}")
                binding.progressBar.isVisible = false
                binding.loadingMore.isVisible = false
                binding.btnLoadMore.isVisible = false

                Toast.makeText(requireContext(), state.message, Toast.LENGTH_SHORT).show()
            }
        }
    }

    override fun onResume() {
        super.onResume()
        Log.d("CategoryListFragment", "onResume")

        // Force refresh list when returning to this fragment
        val currentState = viewModel.state.value
        if (currentState is CategoryListState.Success) {
            citiesAdapter.submitList(null)
            citiesAdapter.submitList(currentState.cities)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        Log.d("CategoryListFragment", "onDestroyView")
        _binding = null
    }
}