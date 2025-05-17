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
import com.aliaktas.urbanscore.MainActivity
import com.aliaktas.urbanscore.R
import com.aliaktas.urbanscore.databinding.FragmentCategoryListBinding
import com.aliaktas.urbanscore.ui.home.CitiesAdapter
import com.aliaktas.urbanscore.util.ImageLoader
import com.aliaktas.urbanscore.util.NetworkUtil
import com.google.android.material.snackbar.Snackbar
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class CategoryListFragment : Fragment() {

    private var _binding: FragmentCategoryListBinding? = null
    private val binding get() = _binding!!

    private val viewModel: CategoryListViewModel by viewModels()
    private lateinit var citiesAdapter: CitiesAdapter
    private lateinit var categoryId: String
    private var noInternetSnackbar: Snackbar? = null

    @Inject
    lateinit var imageLoader: ImageLoader
    @Inject
    lateinit var networkUtil: NetworkUtil

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // NavArgs yerine doğrudan arguments bundle'ından değeri alın
        categoryId = arguments?.getString("categoryId", "averageRating") ?: "averageRating"
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

        if (!networkUtil.isNetworkAvailable()) {
            binding.progressBar.visibility = View.GONE
            Snackbar.make(binding.root, "No internet connection", Snackbar.LENGTH_LONG).show()
            return
        }

        setCategoryTitle()
        setupRecyclerView()
        setupButtons()
        observeViewModel()
        observeNetworkState()
    }

    private fun observeNetworkState() {
        viewLifecycleOwner.lifecycleScope.launch {
            networkUtil.observeNetworkState().collect { isConnected ->
                if (isConnected) {
                    // İnternet geldiğinde snackbar'ı kapat
                    noInternetSnackbar?.dismiss()
                    noInternetSnackbar = null

                    // Yeni veri yüklenebilir
                    binding.btnLoadMore.isEnabled = true
                } else {
                    // İnternet gittiğinde snackbar göster
                    showNoInternetSnackbar()

                    // Yükleme butonunu devre dışı bırak
                    binding.btnLoadMore.isEnabled = false
                }
            }
        }
    }

    private fun showNoInternetSnackbar() {
        if (noInternetSnackbar == null || noInternetSnackbar?.isShown == false) {
            noInternetSnackbar = Snackbar.make(
                binding.root,
                R.string.no_internet_connection,
                Snackbar.LENGTH_INDEFINITE // Sürekli göster
            ).setAction(R.string.dismiss) {
                // Kullanıcı kapatabilir
                noInternetSnackbar = null
            }
            noInternetSnackbar?.show()
        }
    }

    private fun checkInternetForNavigation(): Boolean {
        val isConnected = networkUtil.isNetworkAvailable()
        if (!isConnected) {
            Toast.makeText(context, getString(R.string.no_internet_connection), Toast.LENGTH_SHORT).show()
            showNoInternetSnackbar()
        }
        return isConnected
    }

    private fun setCategoryTitle() {
        val title = when (categoryId) {
            "gastronomy" -> "Best Cities for Gastronomy & Diversity"
            "aesthetics" -> "Best Cities for City Aesthetics"
            "safety" -> "Best Cities for Safety & Peace"
            "culture" -> "Best Cities for Cultural Heritage"
            "livability" -> "Best Cities for Livability & Nature"
            "social" -> "Best Cities for Social Life & Vibrancy"
            "hospitality" -> "Best Cities for Local Hospitality"
            else -> "Top Rated Cities Overall"
        }
        binding.txtTitle.text = title
        Log.d("CategoryListFragment", "Set title to: $title")
    }

    private fun setupRecyclerView() {
        // Burada CitiesAdapter'a imageLoader parametresi ekle
        citiesAdapter = CitiesAdapter(categoryId, imageLoader)
        binding.recyclerViewCategoryList.adapter = citiesAdapter

        citiesAdapter.onItemClick = { city ->
            if (checkInternetForNavigation()) {
                Log.d("CategoryListFragment", "City clicked: ${city.cityName}")
                (requireActivity() as MainActivity).navigateToCityDetail(city.id)
            }
        }
    }

    private fun setupButtons() {
        binding.btnBack.setOnClickListener {
            (requireActivity() as MainActivity).handleBackPressed()
        }

        binding.btnLoadMore.setOnClickListener {
            if (checkInternetForNavigation()) {
                Log.d("CategoryListFragment", "Load more button clicked")
                binding.btnLoadMore.isEnabled = false
                binding.loadingMore.visibility = View.VISIBLE
                viewModel.loadMoreCities()
            }
        }
    }

    private fun observeViewModel() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.state.collect { state ->
                    Log.d("CategoryListFragment", "State changed: ${state.javaClass.simpleName}")
                    updateUI(state)

                    // State değişiminde internet durumunu kontrol et
                    if (!networkUtil.isNetworkAvailable() &&
                        state !is CategoryListState.Loading) {
                        showNoInternetSnackbar()
                        binding.btnLoadMore.isEnabled = false
                    }
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
                binding.btnLoadMore.isEnabled = networkUtil.isNetworkAvailable() // Sadece internet varsa etkinleştir
                binding.btnLoadMore.isVisible = state.hasMoreItems

                citiesAdapter.submitList(state.cities)
            }
            is CategoryListState.Error -> {
                Log.e("CategoryListFragment", "UI State: Error - ${state.message}")
                binding.progressBar.isVisible = false
                binding.loadingMore.isVisible = false
                binding.btnLoadMore.isVisible = false

                // Hata mesajını göster
                Toast.makeText(requireContext(), state.message, Toast.LENGTH_SHORT).show()
            }
        }
    }

    override fun onResume() {
        super.onResume()
        Log.d("CategoryListFragment", "onResume")

        // Internet durumunu kontrol et
        if (!networkUtil.isNetworkAvailable()) {
            showNoInternetSnackbar()
            binding.btnLoadMore.isEnabled = false
        }

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
        noInternetSnackbar?.dismiss()
        noInternetSnackbar = null
        _binding = null
    }
}