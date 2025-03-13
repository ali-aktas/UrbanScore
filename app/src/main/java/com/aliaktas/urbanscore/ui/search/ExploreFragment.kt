package com.aliaktas.urbanscore.ui.search

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.RecyclerView
import androidx.viewpager2.widget.ViewPager2
import com.aliaktas.urbanscore.R
import com.aliaktas.urbanscore.data.model.CityModel
import com.aliaktas.urbanscore.data.model.CuratedCityItem
import com.aliaktas.urbanscore.databinding.FragmentExploreBinding
import com.aliaktas.urbanscore.databinding.ItemFeaturedCityBinding
import com.bumptech.glide.Glide
import com.google.firebase.firestore.FirebaseFirestore
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import java.util.Locale
import javax.inject.Inject

@AndroidEntryPoint
class ExploreFragment : Fragment() {

    private var _binding: FragmentExploreBinding? = null
    private val binding get() = _binding!!

    private lateinit var cityAdapter: FeaturedCityAdapter
    private val allCities = mutableListOf<CityModel>() // Tüm şehirleri saklamak için

    @Inject
    lateinit var firestore: FirebaseFirestore

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
        loadAllCities() // Önce tüm şehirleri bir kere yükle
        setupClickListeners()
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

    private fun loadAllCities() {
        lifecycleScope.launch {
            try {
                binding.progressBar.visibility = View.VISIBLE

                // Tüm şehirleri yükle
                firestore.collection("cities")
                    .limit(100) // Makul bir limit
                    .get()
                    .addOnSuccessListener { snapshot ->
                        val cities = snapshot.documents.mapNotNull { doc ->
                            try {
                                val model = doc.toObject(CityModel::class.java)
                                model?.copy(id = doc.id)
                            } catch (e: Exception) {
                                null
                            }
                        }

                        allCities.clear()
                        allCities.addAll(cities)

                        binding.progressBar.visibility = View.GONE

                        // Şimdilik popüler şehirleri yükle - normal akış
                        loadPopularCities()
                    }
                    .addOnFailureListener { e ->
                        binding.progressBar.visibility = View.GONE
                        Toast.makeText(context, "Error loading cities: ${e.message}", Toast.LENGTH_SHORT).show()

                        // Hata durumunda da popüler şehirleri yüklemeyi dene
                        loadPopularCities()
                    }
            } catch (e: Exception) {
                binding.progressBar.visibility = View.GONE
                Log.e("ExploreFragment", "Error loading all cities", e)

                // Hata durumunda da popüler şehirleri yüklemeyi dene
                loadPopularCities()
            }
        }
    }

    private fun loadPopularCities() {
        // Firebase'den "popular_cities" verilerini yükle
        lifecycleScope.launch {
            try {
                Log.d("ExploreFragment", "Loading popular cities")

                // "curated_lists" koleksiyonundan "popular_cities" tipindeki verileri al
                firestore.collection("curated_lists")
                    .whereEqualTo("listType", "popular_cities")
                    // position'a göre sıralayalım, ama sorgu hatası alırsak manual sıralama yaparız
                    .get()
                    .addOnSuccessListener { snapshot ->
                        Log.d("ExploreFragment", "Found ${snapshot.size()} popular cities")

                        val cities = snapshot.documents.mapNotNull { doc ->
                            try {
                                val model = doc.toObject(CuratedCityItem::class.java)
                                model?.copy(id = doc.id)
                            } catch (e: Exception) {
                                Log.e("ExploreFragment", "Error converting document", e)
                                null
                            }
                        }

                        // position'a göre sırala
                        val sortedCities = cities.sortedBy { it.position }

                        // Şehirleri adaptöre aktar
                        updateCities(sortedCities)
                    }
                    .addOnFailureListener { e ->
                        Log.e("ExploreFragment", "Error loading popular cities", e)
                        Toast.makeText(context, "Şehirleri yüklerken hata oluştu", Toast.LENGTH_SHORT).show()

                        // Hata durumunda mock verilerle devam et
                        showMockCities()
                    }
            } catch (e: Exception) {
                Log.e("ExploreFragment", "Exception in loadPopularCities", e)
                showMockCities()
            }
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

    private fun setupClickListeners() {
        binding.cardSearch.setOnClickListener {
            showSearchBottomSheet()
        }

        binding.btnSuggestCity.setOnClickListener {
            // Şehir önerme diyaloğu
            Toast.makeText(context, "City suggestion will be implemented", Toast.LENGTH_SHORT).show()
        }
    }

    private fun showSearchBottomSheet() {
        if (allCities.isEmpty()) {
            binding.progressBar.visibility = View.VISIBLE

            lifecycleScope.launch {
                try {
                    firestore.collection("cities")
                        .limit(100)
                        .get()
                        .addOnSuccessListener { snapshot ->
                            binding.progressBar.visibility = View.GONE

                            val cities = snapshot.documents.mapNotNull { doc ->
                                try {
                                    val model = doc.toObject(CityModel::class.java)
                                    model?.copy(id = doc.id)
                                } catch (e: Exception) {
                                    null
                                }
                            }

                            allCities.clear()
                            allCities.addAll(cities)

                            showBottomSheetWithCities()
                        }
                        .addOnFailureListener { e ->
                            binding.progressBar.visibility = View.GONE
                            Toast.makeText(context, "Error loading cities: ${e.message}", Toast.LENGTH_SHORT).show()
                        }
                } catch (e: Exception) {
                    binding.progressBar.visibility = View.GONE
                    Log.e("ExploreFragment", "Error loading cities for search", e)
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





    private fun navigateToCityDetail(cityId: String) {
        try {
            val action = ExploreFragmentDirections.actionExploreFragmentToCityDetailFragment(cityId)
            findNavController().navigate(action)
        } catch (e: Exception) {
            Log.e("ExploreFragment", "Navigation error: ${e.message}", e)
            Toast.makeText(context, "Navigation error", Toast.LENGTH_SHORT).show()
        }
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
                    val action = ExploreFragmentDirections.actionExploreFragmentToCityDetailFragment(city.cityId)
                    findNavController().navigate(action)
                } catch (e: Exception) {
                    Log.e("ExploreFragment", "Navigation error: ${e.message}", e)
                    Toast.makeText(context, "Navigation error", Toast.LENGTH_SHORT).show()
                }
            }
        }

        override fun getItemCount() = cities.size
    }
}