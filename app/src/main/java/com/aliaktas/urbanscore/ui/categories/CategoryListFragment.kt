// CategoryListFragment.kt - bu sınıfı şu şekilde güncelleyelim:
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
import androidx.navigation.fragment.navArgs
import com.aliaktas.urbanscore.databinding.FragmentCategoryListBinding
import com.aliaktas.urbanscore.ui.home.CitiesAdapter
import com.aliaktas.urbanscore.ui.home.HomeState
import com.aliaktas.urbanscore.ui.home.HomeViewModel
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class CategoryListFragment : Fragment() {

    private var _binding: FragmentCategoryListBinding? = null
    private val binding get() = _binding!!

    private val viewModel: HomeViewModel by viewModels()
    private val citiesAdapter = CitiesAdapter()

    // navArgs kullanarak kategori ID'sini alalım
    private val args: CategoryListFragmentArgs by navArgs()
    private lateinit var categoryId: String

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Kategori ID'sini argümanlardan alalım
        categoryId = args.categoryId
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

        // Kategori başlığını ayarla
        setCategoryTitle()

        setupRecyclerView()
        setupBackButton()
        observeViewModel()

        // Belirli kategoriye göre şehirleri yükle
        viewModel.refreshCities(true, categoryId)
    }

    private fun setCategoryTitle() {
        // Kategoriye göre başlığı ayarla
        val title = when (categoryId) {
            "environment" -> "Best Cities for Landscapes & Aesthetics"
            "safety" -> "Best Cities for Safety & Tranquility"
            "livability" -> "Best Cities for Livability"
            "cost" -> "Best Cities for Cost of Living"
            "social" -> "Best Cities for Social & Cultural Life"
            else -> "Top Rated Cities Overall"
        }
        binding.txtTitle.text = title
    }

    private fun setupRecyclerView() {
        binding.recyclerViewCategoryList.apply {
            adapter = citiesAdapter
        }

        citiesAdapter.onItemClick = { city ->
            // Şehir detayına git
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
                // Eğer eski veriler varsa onları göstermeye devam et
                state.oldData?.let { citiesAdapter.submitList(it) }
            }
            is HomeState.Error -> {
                binding.progressBar.isVisible = false
                // Hata durumunu göster
            }
            else -> { /* Initial state - bir şey yapma */ }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}