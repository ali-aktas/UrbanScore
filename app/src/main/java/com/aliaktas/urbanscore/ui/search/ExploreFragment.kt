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
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.RecyclerView
import androidx.viewpager2.widget.ViewPager2
import com.aliaktas.urbanscore.MainActivity
import com.aliaktas.urbanscore.R
import com.aliaktas.urbanscore.data.model.CityModel
import com.aliaktas.urbanscore.data.model.CuratedCityItem
import com.aliaktas.urbanscore.databinding.FragmentExploreBinding
import com.aliaktas.urbanscore.databinding.ItemFeaturedCityBinding
import com.bumptech.glide.Glide
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import java.util.Locale

@AndroidEntryPoint
class ExploreFragment : Fragment() {

    private var _binding: FragmentExploreBinding? = null
    private val binding get() = _binding!!

    private val viewModel: ExploreViewModel by viewModels()
    private lateinit var cityAdapter: FeaturedCityAdapter
    private val allCities = mutableListOf<CityModel>() // Tüm şehirleri saklamak için

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
        observeViewModel()
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
                // AllCities'i gözlemle
                launch {
                    viewModel.allCities.collect { cities ->
                        allCities.clear()
                        allCities.addAll(cities)
                    }
                }

                // PopularCities'i gözlemle
                launch {
                    viewModel.popularCities.collect { cities ->
                        if (cities.isNotEmpty()) {
                            updateCities(cities)
                        } else if (cityAdapter.itemCount == 0) {
                            // Eğer boşsa ve henüz bir şey gösterilmiyorsa mock verileri göster
                            showMockCities()
                        }
                    }
                }

                // Loading durumunu gözlemle
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
            showSearchBottomSheet()
        }

        binding.btnSuggestCity.setOnClickListener {
            val suggestCityBottomSheet = SuggestCityBottomSheet()
            suggestCityBottomSheet.show(childFragmentManager, "SuggestCityBottomSheet")
        }
    }

    private fun showSearchBottomSheet() {
        if (allCities.isEmpty()) {
            binding.progressBar.visibility = View.VISIBLE

            // ViewModel aracılığıyla şehirleri yükle
            viewModel.loadAllCities()

            // Şehirler hazır olduğunda observable içinde zaten fark edeceğiz
            // ve bottom sheet'i o zaman göstereceğiz

            // Direkt bir timeout ile kontrol edelim
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
            MockCity("1", "Istanbul", "https://example.com/istanbul.jpg"),
            MockCity("2", "Barcelona", "https://example.com/barcelona.jpg"),
            MockCity("3", "Paris", "https://example.com/paris.jpg")
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
            (requireActivity() as MainActivity).navigateToCityDetail(cityId)
        } catch (e: Exception) {
            Log.e("ExploreFragment", "Navigation error: ${e.message}", e)
            Toast.makeText(context, "Navigation error", Toast.LENGTH_SHORT).show()
        }
    }

    // Scroll to top fonksiyonu - MainActivity'den çağrılabilir
    fun scrollToTop() {
        // Root view'ı en başa kaydır
        binding.root.scrollTo(0, 0)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    // Yardımcı Mock sınıfı (eski yapıyı desteklemek için)
    data class MockCity(
        val id: String,
        val name: String,
        val imageUrl: String
    )

    // Adapter sınıfını FeaturedCityAdapter adı altında tutup içeriğini güncelliyoruz
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

            // cityId'den şehir adını çıkar (örneğin "istanbul-turkey" -> "Istanbul")
            val cityName = city.cityId.split("-").firstOrNull()?.replaceFirstChar {
                if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString()
            } ?: "Unknown City"

            with(holder.binding) {
                txtCityName.text = cityName

                // Resim yükleme - önce custom URL, yoksa varsayılan resim
                if (city.imageUrl.isNotEmpty()) {
                    Glide.with(root.context)
                        .load(city.imageUrl)
                        .centerCrop()
                        .into(imgCity)
                } else {
                    // Varsayılan PNG dosyasını yükle
                    Glide.with(root.context)
                        .load(R.drawable.istanbulphoto)
                        .centerCrop()
                        .into(imgCity)
                }
            }

            holder.itemView.setOnClickListener {
                try {
                    // Şehir detay sayfasına git
                    (requireActivity() as MainActivity).navigateToCityDetail(city.cityId)
                } catch (e: Exception) {
                    Log.e("ExploreFragment", "Navigation error: ${e.message}", e)
                    Toast.makeText(context, "Navigation error", Toast.LENGTH_SHORT).show()
                }
            }
        }

        override fun getItemCount() = cities.size
    }
}