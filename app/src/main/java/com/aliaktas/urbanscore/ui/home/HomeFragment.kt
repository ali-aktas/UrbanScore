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
import com.aliaktas.urbanscore.ads.AdManager
import com.aliaktas.urbanscore.base.BaseViewModel
import com.aliaktas.urbanscore.databinding.FragmentHomeBinding
import com.aliaktas.urbanscore.ui.home.controllers.HomeController
import com.aliaktas.urbanscore.ui.home.controllers.HomeControllerFactory
import com.aliaktas.urbanscore.ui.home.controllers.MainStateController
import com.aliaktas.urbanscore.ui.home.controllers.SwipeRefreshController
import com.aliaktas.urbanscore.ui.home.controllers.TopCitiesController
import com.google.android.gms.ads.nativead.NativeAd
import com.google.android.gms.ads.nativead.NativeAdView
import com.google.android.material.snackbar.Snackbar
import com.google.firebase.crashlytics.FirebaseCrashlytics
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

    @Inject
    lateinit var adManager: AdManager

    private val viewModel: HomeViewModel by viewModels()

    private var nativeAd: NativeAd? = null

    @Inject
    lateinit var controllerFactory: HomeControllerFactory

    // Tüm controller'ları tutan liste
    private lateinit var controllers: List<HomeController>

    private var isFirstLoad = true

    // Controller'ların başlatılıp başlatılmadığını takip etmek için flag
    private var controllersInitialized = false

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

        showSkeleton() // İlk açıldığında göster

        observeViewModel()
        createControllers()
        viewModel.loadTopRatedCities(false)

        lifecycleScope.launch {
            delay(2500) // 2.5 saniye bekleyip
            hideSkeleton() // Skeleton'u kapat
        }

        loadNativeAd()
    }

    private fun showSkeleton() {
        binding.skeletonContainer.visibility = View.VISIBLE
        binding.swipeRefreshLayout.visibility = View.INVISIBLE
        binding.loadingContainer.visibility = View.GONE
        binding.errorContainer.visibility = View.GONE
    }

    private fun hideSkeleton() {
        binding.skeletonContainer.visibility = View.GONE
        binding.swipeRefreshLayout.visibility = View.VISIBLE

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

        // Sadece veriyi güncelle ama skeleton gösterme
        if (controllersInitialized) {
            viewModel.refreshOnReturn()
        }
    }


    private fun loadNativeAd() {
        try {
            Log.d(TAG, "Native Ad yükleniyor")

            // Önceki reklamı temizle
            if (nativeAd != null) {
                nativeAd?.destroy()
                nativeAd = null
            }

            binding.homeNativeAdContainer.removeAllViews()
            binding.txtNativeAdTitle.visibility = View.GONE

            // AdManager henüz inject edilmediyse erken çık
            if (!::adManager.isInitialized) {
                Log.d(TAG, "AdManager henüz initialize edilmemiş")
                return
            }

            // Reklam yükleme
            adManager.loadNativeAd(
                onAdLoaded = { ad ->
                    // UI thread'de işlem yap
                    viewLifecycleOwner.lifecycleScope.launch(Dispatchers.Main) {
                        try {
                            nativeAd = ad

                            // Native Ad View'ı inflate et
                            val adView = layoutInflater.inflate(
                                R.layout.item_native_ad,
                                binding.homeNativeAdContainer,
                                false
                            ) as NativeAdView

                            // Native Ad'i view'a yerleştir
                            adManager.populateNativeAdView(ad, adView)

                            // Container'a ekle ve göster
                            binding.homeNativeAdContainer.removeAllViews()
                            binding.homeNativeAdContainer.addView(adView)
                            binding.homeNativeAdContainer.visibility = View.VISIBLE
                            binding.txtNativeAdTitle.visibility = View.VISIBLE

                            Log.d(TAG, "Native Ad başarıyla gösterildi")
                        } catch (e: Exception) {
                            Log.e(TAG, "Native Ad UI güncelleme hatası: ${e.message}", e)
                        }
                    }
                },
                onAdFailed = {
                    // Reklam yüklenemezse konteyneri gizle
                    binding.homeNativeAdContainer.visibility = View.GONE
                    binding.txtNativeAdTitle.visibility = View.GONE
                    Log.d(TAG, "Native Ad yüklenemedi")
                }
            )
        } catch (e: Exception) {
            Log.e(TAG, "Native Ad yükleme hatası: ${e.message}", e)
            binding.homeNativeAdContainer.visibility = View.GONE
            binding.txtNativeAdTitle.visibility = View.GONE
        }
    }

    override fun onDestroyView() {
        // Native Ad'i temizle - BU KOD PARÇASINI METHODUN BAŞINA EKLEYİN
        if (nativeAd != null) {
            nativeAd?.destroy()
            nativeAd = null
        }
        binding.homeNativeAdContainer.removeAllViews()

        // Mevcut kodlar...
        super.onDestroyView()
        Log.d(TAG, "onDestroyView")
        _binding = null
    }

    companion object {
        private const val TAG = "HomeFragment"
    }
}