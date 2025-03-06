package com.aliaktas.urbanscore.ui.categories

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import com.aliaktas.urbanscore.databinding.FragmentCategoryListBinding
import com.aliaktas.urbanscore.ui.home.CitiesAdapter
import com.aliaktas.urbanscore.ui.home.HomeState
import com.aliaktas.urbanscore.ui.home.HomeViewModel
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

/**
 * Category List Fragment showing all cities in a category
 * Uses simple navigation without complex transitions
 */
@AndroidEntryPoint
class CategoryListFragment : Fragment() {

    private var _binding: FragmentCategoryListBinding? = null
    private val binding get() = _binding!!

    private val viewModel: HomeViewModel by viewModels()
    private val citiesAdapter = CitiesAdapter()

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

        setupRecyclerView()
        setupBackButton()
        observeViewModel()
    }

    private fun setupRecyclerView() {
        binding.recyclerViewCategoryList.apply {
            adapter = citiesAdapter
        }

        citiesAdapter.onItemClick = { city ->
            // Navigate to city detail
            val action = CategoryListFragmentDirections.actionCategoryListFragmentToCityDetailFragment(city.id)
            findNavController().navigate(action)
        }
    }

    private fun setupBackButton() {
        binding.btnBack.setOnClickListener {
            findNavController().navigateUp()
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
            is HomeState.Success -> {
                binding.progressBar.isVisible = false
                citiesAdapter.submitList(state.cities)
            }
            is HomeState.Loading -> {
                binding.progressBar.isVisible = true
            }
            is HomeState.Error -> {
                binding.progressBar.isVisible = false
                // Show error state
            }
            else -> { /* Initial state - do nothing */ }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}