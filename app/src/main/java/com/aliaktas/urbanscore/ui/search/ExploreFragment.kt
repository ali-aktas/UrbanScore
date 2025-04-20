package com.aliaktas.urbanscore.ui.search

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
import androidx.recyclerview.widget.RecyclerView
import com.aliaktas.urbanscore.MainActivity
import com.aliaktas.urbanscore.R
import com.aliaktas.urbanscore.data.model.CityModel
import com.aliaktas.urbanscore.data.model.CuratedCityItem
import com.aliaktas.urbanscore.databinding.FragmentExploreBinding
import com.aliaktas.urbanscore.util.NetworkUtil
import com.bumptech.glide.Glide
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import java.util.Locale
import com.aliaktas.urbanscore.ads.AdManager
import com.aliaktas.urbanscore.databinding.ItemFeaturedCityBinding
import com.google.android.gms.ads.nativead.NativeAd
import com.google.android.gms.ads.nativead.NativeAdView
import javax.inject.Inject

@AndroidEntryPoint
class ExploreFragment : Fragment() {

    private var _binding: FragmentExploreBinding? = null
    private val binding get() = _binding!!

    private val viewModel: ExploreViewModel by viewModels()
    private lateinit var cityAdapter: FeaturedCityAdapter
    private val allCities = mutableListOf<CityModel>()

    private var nativeAd: NativeAd? = null

    @Inject
    lateinit var adManager: AdManager

    @Inject
    lateinit var networkUtil: NetworkUtil

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentExploreBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // ViewPager2'nin yüksekliğini belirli bir değer olarak ayarla
        val layoutParams = binding.cityViewPager.layoutParams
        layoutParams.height = resources.getDimensionPixelSize(R.dimen.featured_card_height) +
                resources.getDimensionPixelSize(R.dimen.card_padding)
        binding.cityViewPager.layoutParams = layoutParams

        setupCityCarousel()
        setupClickListeners()
        setupAnimations()
        observeViewModel()
        observeNetworkState()
        // Banner yerine Native Ad yükle
        loadNativeAd()

        // İlk açılışta internet kontrolü yap
        checkInternetConnection()

    }

    private fun setupAnimations() {
        binding.animationError.apply {
            setAnimation(R.raw.error_animation)
        }
    }

    private fun observeNetworkState() {
        viewLifecycleOwner.lifecycleScope.launch {
            networkUtil.observeNetworkState().collect { isConnected ->
                if (isConnected) {
                    hideErrorUI()
                    viewModel.refresh()
                } else {
                    showErrorUI(getString(R.string.no_internet_connection))
                }
            }
        }
    }

    private fun checkInternetConnection(): Boolean {
        val isConnected = networkUtil.isNetworkAvailable()
        if (!isConnected) {
            showErrorUI(getString(R.string.no_internet_connection))
        }
        return isConnected
    }

    private fun showErrorUI(errorMessage: String) {
        binding.errorContainer.visibility = View.VISIBLE
        binding.textError.text = errorMessage
        binding.animationError.playAnimation()

        // Ana içeriği gizle
        binding.contentContainer.visibility = View.GONE
    }

    private fun hideErrorUI() {
        binding.errorContainer.visibility = View.GONE

        // Ana içeriği göster
        binding.contentContainer.visibility = View.VISIBLE
    }

    private fun loadNativeAd() {
        try {
            Log.d("ExploreFragment", "Native Ad yükleniyor")

            // Önceki reklamı temizle
            if (nativeAd != null) {
                nativeAd?.destroy()
                nativeAd = null
            }

            binding.nativeAdContainer.removeAllViews()

            // Reklam yükleme
            adManager.loadNativeAd(
                onAdLoaded = { ad ->
                    nativeAd = ad

                    // Native Ad View'ı inflate et
                    val adView = layoutInflater.inflate(
                        R.layout.item_native_ad,
                        binding.nativeAdContainer,
                        false
                    ) as NativeAdView

                    // Native Ad'i view'a yerleştir
                    adManager.populateNativeAdView(ad, adView)

                    // Container'a ekle ve göster
                    binding.nativeAdContainer.removeAllViews()
                    binding.nativeAdContainer.addView(adView)
                    binding.nativeAdContainer.visibility = View.VISIBLE

                    Log.d("ExploreFragment", "Native Ad başarıyla gösterildi")
                },
                onAdFailed = {
                    binding.nativeAdContainer.visibility = View.GONE
                    Log.d("ExploreFragment", "Native Ad yüklenemedi")
                }
            )
        } catch (e: Exception) {
            Log.e("ExploreFragment", "Native Ad yükleme hatası: ${e.message}", e)
            binding.nativeAdContainer.visibility = View.GONE
        }
    }

    private fun setupCityCarousel() {
        // Boş liste ile adaptörü başlat, veriler gelince güncellenecek
        cityAdapter = FeaturedCityAdapter(emptyList())

        binding.cityViewPager.apply {
            adapter = cityAdapter
            offscreenPageLimit = 3
            setPageTransformer(CityCardTransformer())

            // Clip özelliklerini ayarla
            clipToPadding = false
            clipChildren = false

            // NestedScrollingEnabled özelliğini false yap (iç içe kaydırma sorunlarını önler)
            getChildAt(0).apply {
                overScrollMode = View.OVER_SCROLL_NEVER
            }

            val pageMargin = resources.getDimensionPixelOffset(R.dimen.card_padding) / 2
            val pageOffset = resources.getDimensionPixelOffset(R.dimen.card_padding)

            setPageTransformer { page, position ->
                val myOffset = position * -(2 * pageOffset + pageMargin)
                if (position < -1) {
                    page.translationX = -myOffset
                } else if (position <= 1) {
                    val scaleFactor = 0.80f.coerceAtLeast(1 - Math.abs(position * 0.45f))
                    page.translationX = myOffset
                    page.scaleY = scaleFactor
                    page.scaleX = scaleFactor
                    page.alpha = 0.5f + (1 - Math.abs(position)) * 0.5f
                } else {
                    page.translationX = -myOffset
                }
            }
        }
    }

    private fun observeViewModel() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch {
                    viewModel.allCities.collect { cities ->
                        allCities.clear()
                        allCities.addAll(cities)
                    }
                }

                launch {
                    viewModel.usersCities.collect { cities ->
                        if (cities.isNotEmpty()) {
                            updateCities(cities)
                        } else if (cityAdapter.itemCount == 0) {
                            showMockCities()
                        }
                    }
                }

                launch {
                    viewModel.isLoading.collect { isLoading ->
                        binding.progressBar.visibility = if (isLoading) View.VISIBLE else View.GONE
                    }
                }
            }
        }
    }

    private fun setupClickListeners() {
        binding.cardSearch.setOnClickListener {
            if (checkInternetConnection()) {
                showSearchBottomSheet()
            } else {
                Toast.makeText(context, getString(R.string.no_internet_connection), Toast.LENGTH_SHORT).show()
            }
        }

        binding.btnSuggestCity.setOnClickListener {
            if (checkInternetConnection()) {
                val suggestCityBottomSheet = SuggestCityBottomSheet()
                suggestCityBottomSheet.show(childFragmentManager, "SuggestCityBottomSheet")
            } else {
                Toast.makeText(context, getString(R.string.no_internet_connection), Toast.LENGTH_SHORT).show()
            }
        }

        binding.btnRetry.setOnClickListener {
            if (networkUtil.isNetworkAvailable()) {
                hideErrorUI()
                viewModel.refresh()
            } else {
                // Bağlantı hala yoksa animasyonu yeniden oynat
                binding.animationError.playAnimation()
            }
        }
    }

    private fun showSearchBottomSheet() {
        if (allCities.isEmpty()) {
            binding.progressBar.visibility = View.VISIBLE

            viewModel.loadAllCities()
            lifecycleScope.launch {
                kotlinx.coroutines.delay(2000) // 2 saniye bekleyelim
                if (allCities.isNotEmpty()) {
                    showBottomSheetWithCities()
                }
            }
        } else {
            showBottomSheetWithCities()
        }
    }

    private fun showBottomSheetWithCities() {
        val searchBottomSheet = SearchBottomSheetFragment.newInstance(
            cities = allCities,
            onCitySelected = { cityId ->
                navigateToCityDetail(cityId)
            }
        )
        searchBottomSheet.show(childFragmentManager, "SearchBottomSheet")
    }

    private fun updateCities(cities: List<CuratedCityItem>) {
        if (cities.isNotEmpty()) {
            Log.d("ExploreFragment", "Updating UI with ${cities.size} cities")
            cityAdapter = FeaturedCityAdapter(cities)
            binding.cityViewPager.adapter = cityAdapter
        } else {
            Log.d("ExploreFragment", "No popular cities found, showing mock data")
            showMockCities()
        }
    }

    private fun showMockCities() {
        val mockCities = listOf(
            MockCity("1", "Loading..", "https://example.com/istanbul.jpg"),
            MockCity("2", "Loading..", "https://example.com/barcelona.jpg"),
            MockCity("3", "Loading..", "https://example.com/paris.jpg"),
            MockCity("3", "Loading..", "https://example.com/paris.jpg"),
        )

        val curatedItems = mockCities.map {
            CuratedCityItem(
                id = it.id,
                cityId = "${it.name.lowercase()}-country",
                imageUrl = it.imageUrl
            )
        }

        updateCities(curatedItems)
    }

    private fun navigateToCityDetail(cityId: String) {
        try {
            if (checkInternetConnection()) {
                (requireActivity() as MainActivity).navigateToCityDetail(cityId)
            } else {
                Toast.makeText(context, getString(R.string.no_internet_connection), Toast.LENGTH_SHORT).show()
            }
        } catch (e: Exception) {
            Log.e("ExploreFragment", "Navigation error: ${e.message}", e)
            val errorMessage = getString(R.string.msg_generic_error)
            Toast.makeText(context, errorMessage, Toast.LENGTH_SHORT).show()
        }
    }

    fun scrollToTop() {
        // Root view'ı en başa kaydır
        binding.root.scrollTo(0, 0)
    }

    override fun onDestroyView() {
        // Native Ad'i temizle
        if (nativeAd != null) {
            nativeAd?.destroy()
            nativeAd = null
        }

        binding.nativeAdContainer.removeAllViews()
        // binding.adContainerView referansını kaldırdık
        super.onDestroyView()
        _binding = null
    }

    data class MockCity(
        val id: String,
        val name: String,
        val imageUrl: String
    )

    inner class FeaturedCityAdapter(private val cities: List<CuratedCityItem>) :
        RecyclerView.Adapter<FeaturedCityAdapter.CityViewHolder>() {

        inner class CityViewHolder(val binding: ItemFeaturedCityBinding) :
            RecyclerView.ViewHolder(binding.root)

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CityViewHolder {
            val binding = ItemFeaturedCityBinding.inflate(
                LayoutInflater.from(parent.context), parent, false)
            return CityViewHolder(binding)
        }

        override fun onBindViewHolder(holder: CityViewHolder, position: Int) {
            val city = cities[position]

            val cityName = city.cityId.split("-").firstOrNull()?.replaceFirstChar {
                if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString()
            } ?: "Unknown City"

            with(holder.binding) {
                txtCityName.text = cityName

                if (city.imageUrl.isNotEmpty()) {
                    Glide.with(root.context)
                        .load(city.imageUrl)
                        .error(R.drawable.city_placeholder) // Yükleme hatası durumunda
                        .placeholder(R.drawable.city_placeholder) // Yükleme sırasında
                        .centerCrop()
                        .into(imgCity)
                } else {
                    Glide.with(root.context)
                        .load(R.drawable.city_placeholder)
                        .centerCrop()
                        .into(imgCity)
                }
            }

            holder.itemView.setOnClickListener {
                if (checkInternetConnection()) {
                    try {
                        (requireActivity() as MainActivity).navigateToCityDetail(city.cityId)
                    } catch (e: Exception) {
                        Log.e("ExploreFragment", "Navigation error: ${e.message}", e)
                        Toast.makeText(context, "Navigation error", Toast.LENGTH_SHORT).show()
                    }
                } else {
                    Toast.makeText(context, getString(R.string.no_internet_connection), Toast.LENGTH_SHORT).show()
                }
            }
        }

        override fun getItemCount() = cities.size
    }
}