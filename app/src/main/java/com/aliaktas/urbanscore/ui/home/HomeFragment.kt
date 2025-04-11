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
import com.aliaktas.urbanscore.ui.home.controllers.MainStateController
import com.aliaktas.urbanscore.ui.home.controllers.SwipeRefreshController
import com.aliaktas.urbanscore.ui.home.controllers.TopCitiesController
import com.google.android.material.snackbar.Snackbar
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

@AndroidEntryPoint
class HomeFragment : Fragment() {

    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!

    // Skeleton View referansı
    private var skeletonView: View? = null

    private val viewModel: HomeViewModel by viewModels()

    @Inject
    lateinit var controllerFactory: HomeControllerFactory

    // Tüm controller'ları tutan liste
    private lateinit var controllers: List<HomeController>

    // Controller'ların başlatılıp başlatılmadığını takip etmek için flag
    private var controllersInitialized = false

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentHomeBinding.inflate(inflater, container, false)

        // Skeleton view'ı inflate et
        skeletonView = inflater.inflate(R.layout.layout_home_skeleton, container, false)

        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        Log.d(TAG, "onViewCreated")

        // Önce skeleton göster ve içeriği gizle
        showSkeleton()

        // ViewModel'i gözlemle
        observeViewModel()

        // Controller'ları oluştur
        createControllers()

        // Verileri hemen yükle (arka planda)
        viewModel.loadTopRatedCities(false)

        // 3 saniye sonra gerçek içeriği göster
        lifecycleScope.launch {
            delay(4000)
            hideSkeleton()
        }
    }

    private fun showSkeleton() {
        // Ana içeriği gizle
        binding.swipeRefreshLayout.visibility = View.INVISIBLE
        binding.loadingContainer.visibility = View.GONE
        binding.errorContainer.visibility = View.GONE

        // Skeleton'ı göster
        (binding.root as ViewGroup).addView(skeletonView)
    }

    private fun hideSkeleton() {
        // Skeleton'ı kaldır
        skeletonView?.let {
            (binding.root as ViewGroup).removeView(it)
        }

        // Ana içeriği göster
        binding.swipeRefreshLayout.visibility = View.VISIBLE

        // Skeleton kaldırıldıktan sonra controller'ları başlat
        if (!controllersInitialized) {
            initializeControllers()
        }
    }

    private fun createControllers() {
        // Sadece controller'ları oluştur ama başlatma
        controllers = controllerFactory.createControllers(
            binding = binding,
            viewModel = viewModel,
            lifecycleOwner = viewLifecycleOwner,
            onCityClick = this::navigateToCityDetail,
            onCategoryClick = this::navigateToCategoryList,
            onViewAllClick = this::navigateToAllCategories,
            onRetry = this::retryLoading
        )
    }

    private fun initializeControllers() {
        Log.d(TAG, "Initializing controllers")
        // Controller'ları başlat
        controllers.forEach { controller ->
            controller.bind(binding.root)
        }

        // Son state'i controller'lara zorla uygula
        val currentState = viewModel.topRatedCitiesState.value
        controllers.forEach { controller ->
            controller.update(currentState)
        }

        controllersInitialized = true
    }

    private fun observeViewModel() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                // Ana state akışını gözlemle
                launch {
                    var previousStateType: Class<out HomeState>? = null

                    viewModel.topRatedCitiesState.collect { state ->
                        // Controller'lar başlatılmış mı kontrol et
                        if (!controllersInitialized) {
                            // Controller'lar başlatılmamışsa state değişikliklerini görmezden gel
                            return@collect
                        }

                        // Veri tipi değişince sadece MainStateController güncellenir
                        val stateType = state.javaClass
                        val typeChanged = previousStateType != stateType
                        previousStateType = stateType

                        // Önce ana state controller'ı güncelle
                        controllers.firstOrNull { it is MainStateController }?.update(state)

                        // Sonra diğer ilgili controller'ları güncelle
                        when (state) {
                            is HomeState.Success -> {
                                // TopCitiesController ve CategoriesController güncelle
                                controllers.forEach { controller ->
                                    when (controller) {
                                        is TopCitiesController,
                                        is SwipeRefreshController -> controller.update(state)
                                        // Diğer controller'ları güncelleme
                                        else -> if (typeChanged) controller.update(state)
                                    }
                                }
                            }
                            is HomeState.Loading -> {
                                // Sadece SwipeRefreshController'ı güncelle
                                controllers.forEach { controller ->
                                    if (controller is SwipeRefreshController) {
                                        controller.update(state)
                                    }
                                }
                            }
                            else -> if (typeChanged) {
                                // State tipi değiştiyse tüm controller'ları güncelle
                                controllers.forEach { controller ->
                                    controller.update(state)
                                }
                            }
                        }
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

        // Controller'lar başlatılmışsa sıfırla ve yeniden skeletonu göster
        if (controllersInitialized) {
            controllersInitialized = false
            showSkeleton()

            // Verileri yeniden yükle
            viewModel.refreshOnReturn()

            // 4 saniye sonra gerçek içeriği göster
            lifecycleScope.launch {
                delay(4000)
                hideSkeleton()
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        Log.d(TAG, "onDestroyView")
        skeletonView = null
        _binding = null
    }

    companion object {
        private const val TAG = "HomeFragment"
    }
}