package com.aliaktas.urbanscore.ui.detail

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
import androidx.navigation.fragment.navArgs
import com.aliaktas.urbanscore.MainActivity
import com.aliaktas.urbanscore.R
import com.aliaktas.urbanscore.ads.AdManager
import com.aliaktas.urbanscore.databinding.FragmentCityDetailBinding
import com.aliaktas.urbanscore.ui.detail.controllers.UiController
import com.aliaktas.urbanscore.ui.ratecity.RateCityBottomSheet
import com.google.android.material.snackbar.Snackbar
import com.google.android.material.transition.MaterialContainerTransform
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import javax.inject.Inject

private const val TAG = "CityDetailFragment"

/**
 * Şehir detay ekranı.
 * Yeniden yapılandırılmış versiyonda Fragment sadece koordinasyon
 * görevlerinden sorumludur ve ayrıntılı UI işlemleri controllerlara
 * delege edilmiştir.
 */
@AndroidEntryPoint
class CityDetailFragment : Fragment() {

    @Inject lateinit var adManager: AdManager
    @Inject lateinit var uiControllerFactory: CityDetailUiControllerFactory
    @Inject lateinit var eventHandler: CityDetailEventHandler
    private var isFirstInteraction = true

    private var _binding: FragmentCityDetailBinding? = null
    private val binding get() = _binding!!
    private val viewModel: CityDetailViewModel by viewModels()
    private lateinit var cityId: String

    private lateinit var uiControllers: List<UiController>
    private var ratingBottomSheet: RateCityBottomSheet? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // NavArgs yerine doğrudan arguments bundle'ından değeri alın
        cityId = arguments?.getString("cityId") ?: throw IllegalArgumentException("City ID is required")
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


        // Toolbar geçiş adını ayarla (paylaşılan element geçişi için)
        binding.toolbar.transitionName = "city_${cityId}"

        // Reklamları göster
        setupAds()

        // UI controller'ları başlat
        initializeUiControllers()

        // ViewModel'i gözlemle
        observeViewModel()

        // Geri navigasyonu
        binding.toolbar.setOnClickListener {
            (requireActivity() as MainActivity).handleBackPressed()
        }
    }

    private fun setupAds() {
        val shouldShowAd = adManager.recordCityVisit()
        if (shouldShowAd) {
            adManager.showInterstitialAd(requireActivity()) {
                // Reklam kapatıldığında callback (gerekirse)
                Log.d(TAG, "Interstitial ad closed")
            }
        }

        if (adManager.shouldSuggestProSubscription()) {
            showProSubscriptionSuggestion()
        }
    }

    private fun showProSubscriptionSuggestion() {
        val snackbar = Snackbar.make(
            binding.root,
            "Want to remove ads? Try Pro subscription!",
            Snackbar.LENGTH_LONG
        )
        snackbar.setAction("Upgrade") {
            (requireActivity() as MainActivity).navigateToProSubscription()
        }
        snackbar.show()
    }

    private fun initializeUiControllers() {
        uiControllers = uiControllerFactory.createControllers(
            binding,
            viewLifecycleOwner,
            viewModel,
            this  // Fragment'i parametre olarak geçir
        )
        uiControllers.forEach { it.bind(binding.root) }
    }

    private fun observeViewModel() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                // State akışını gözlemle
                launch {
                    viewModel.detailState.collectLatest { state ->
                        uiControllers.forEach { it.update(state) }
                    }
                }

                // Event akışını gözlemle
                launch {
                    viewModel.detailEvents.collectLatest { event ->
                        eventHandler.handleEvent(event, this@CityDetailFragment)
                    }
                }
            }
        }
    }

    // CityDetailFragment.kt içine ekleyin
    fun navigateBack() {
        try {
            (requireActivity() as? MainActivity)?.handleBackPressed() ?:
            requireActivity().onBackPressedDispatcher.onBackPressed()
        } catch (e: Exception) {
            Log.e(TAG, "Error navigating back: ${e.message}", e)
            // Fallback yöntem: Fragment Manager'ı kullanarak geri dönmeyi dene
            try {
                parentFragmentManager.popBackStack()
            } catch (e2: Exception) {
                Log.e(TAG, "Error popping back stack: ${e2.message}", e2)
                Toast.makeText(requireContext(), "Could not navigate back. Please try again.", Toast.LENGTH_SHORT).show()
            }
        }
    }

    fun setRatingBottomSheet(bottomSheet: RateCityBottomSheet) {
        ratingBottomSheet = bottomSheet
    }

    override fun onResume() {
        super.onResume()

    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
        ratingBottomSheet = null
    }
}