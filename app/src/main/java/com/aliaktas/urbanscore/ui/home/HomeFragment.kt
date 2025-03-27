package com.aliaktas.urbanscore.ui.home

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.aliaktas.urbanscore.MainActivity
import com.aliaktas.urbanscore.R
import com.aliaktas.urbanscore.base.BaseViewModel
import com.aliaktas.urbanscore.databinding.FragmentHomeBinding
import com.aliaktas.urbanscore.ui.home.controllers.HomeController
import com.aliaktas.urbanscore.ui.home.controllers.HomeControllerFactory
import com.google.android.material.snackbar.Snackbar
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class HomeFragment : Fragment() {

    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!

    private val viewModel: HomeViewModel by viewModels()

    @Inject
    lateinit var controllerFactory: HomeControllerFactory

    // Tüm controller'ları tutan liste
    private lateinit var controllers: List<HomeController>

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
        Log.d(TAG, "onViewCreated")

        // Controller'ları oluştur ve başlat
        initializeControllers()

        // ViewModel'i gözlemle
        observeViewModel()
    }

    private fun initializeControllers() {
        // Controller'ları oluştur
        controllers = controllerFactory.createControllers(
            binding = binding,
            viewModel = viewModel,
            lifecycleOwner = viewLifecycleOwner,
            onCityClick = this::navigateToCityDetail,
            onCategoryClick = this::navigateToCategoryList,
            onViewAllClick = this::navigateToAllCategories,
            onRetry = this::retryLoading
        )

        // Her controller'ı başlat
        controllers.forEach { controller ->
            controller.bind(binding.root)
        }
    }

    private fun observeViewModel() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                // Ana state akışını gözlemle
                launch {
                    viewModel.topRatedCitiesState.collect { state ->
                        Log.d(TAG, "State update: ${state.javaClass.simpleName}")
                        // Tüm controller'ları güncelle
                        controllers.forEach { it.update(state) }
                    }
                }

                // Event'leri gözlemle
                launch {
                    viewModel.events.collect { event ->
                        handleEvent(event)
                    }
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

    // Navigasyon yardımcı metodları
    private fun navigateToCategoryList(category: com.aliaktas.urbanscore.data.model.CategoryModel) {
        try {
            Log.d(TAG, "Navigating to category: ${category.id}")
            (requireActivity() as MainActivity).navigateToCategoryList(category.id)
        } catch (e: Exception) {
            Log.e(TAG, "Navigation error: ${e.message}", e)
            Toast.makeText(context, getString(R.string.msg_generic_error), Toast.LENGTH_SHORT).show()
        }
    }

    private fun navigateToAllCategories() {
        try {
            Log.d(TAG, "Navigating to all categories (average rating)")
            (requireActivity() as MainActivity).navigateToCategoryList("averageRating")
        } catch (e: Exception) {
            Log.e(TAG, "Navigation error: ${e.message}", e)
            Toast.makeText(context, getString(R.string.msg_generic_error), Toast.LENGTH_SHORT).show()
        }
    }

    private fun navigateToCityDetail(cityId: String) {
        try {
            Log.d(TAG, "Navigating to city detail: $cityId")
            (requireActivity() as MainActivity).navigateToCityDetail(cityId)
        } catch (e: Exception) {
            Log.e(TAG, "Navigation error: ${e.message}", e)
            Toast.makeText(context, getString(R.string.msg_generic_error), Toast.LENGTH_SHORT).show()
        }
    }

    private fun retryLoading() {
        Log.d(TAG, "Retry loading triggered")
        viewModel.loadTopRatedCities(true)
    }

    override fun onResume() {
        super.onResume()
        Log.d(TAG, "onResume")

        // Her ana sayfaya dönüşte verileri yenile
        viewModel.refreshOnReturn()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        Log.d(TAG, "onDestroyView")
        _binding = null
    }

    companion object {
        private const val TAG = "HomeFragment"
    }
}