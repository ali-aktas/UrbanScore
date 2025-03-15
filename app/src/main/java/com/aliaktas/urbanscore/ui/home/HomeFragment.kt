package com.aliaktas.urbanscore.ui.home

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.GridLayout
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import com.aliaktas.urbanscore.R
import com.aliaktas.urbanscore.base.BaseViewModel
import com.aliaktas.urbanscore.data.model.CategoryModel
import com.aliaktas.urbanscore.data.model.CityModel
import com.aliaktas.urbanscore.data.model.CuratedCityItem
import com.aliaktas.urbanscore.databinding.FragmentHomeBinding
import com.bumptech.glide.Glide
import com.google.android.material.snackbar.Snackbar
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import java.util.Locale

@AndroidEntryPoint
class HomeFragment : Fragment() {

    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!

    private val viewModel: HomeViewModel by viewModels()
    private val citiesAdapter = CitiesAdapter()
    private val categoriesAdapter = CategoriesAdapter()

    // Fragment'ın görünür olup olmadığını izlemek için
    private var isFragmentVisible = false

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

        setupRecyclerViews()
        setupClickListeners()
        setupAnimations()
        observeViewModel()

        // HomeFragment.kt içine ekle - onViewCreated metodunda
        lifecycleScope.launch {
            Log.d("HomeFragment", "Loading editors choice cities for UI")
            try {
                viewModel.cityRepository.getCuratedCities("editors_choice")
                    .collect { cities ->
                        Log.d("HomeFragment", "Updating UI with ${cities.size} cities")
                        updateEditorsChoiceUI(cities)
                    }
            } catch (e: Exception) {
                Log.e("HomeFragment", "Error showing curated cities", e)
            }
        }

    }




    // HomeFragment.kt'de updateEditorsChoiceUI metodunu güncelle
    private fun updateEditorsChoiceUI(cities: List<CuratedCityItem>) {
        try {
            Log.d("HomeFragment", "Updating UI with ${cities.size} cities")

            // GridLayout'ı düzgün yapılandır
            binding.editorsChoices.removeAllViews()
            binding.editorsChoices.columnCount = 2 // İki sütunlu GridLayout
            binding.editorsChoices.useDefaultMargins = true

            // Reklam kartını oluştur
            val adCard = createAdCard()

            // Şehirleri ekle
            cities.forEachIndexed { index, city ->
                // 3. pozisyonda (index=2) reklam göster
                if (index == 2) {
                    binding.editorsChoices.addView(adCard)
                }

                // Bu şehir için kartı oluştur ve ekle (index numarasını gönder)
                val cityCard = createCityCard(city, index)
                binding.editorsChoices.addView(cityCard)
            }

            // Eğer 3 şehirden az varsa, reklam kartını yine de ekle
            if (cities.size < 3) {
                binding.editorsChoices.addView(adCard)
            }

            Log.d("HomeFragment", "Editors choice UI updated with ${cities.size} cities and 1 ad")
        } catch (e: Exception) {
            Log.e("HomeFragment", "Error updating editors choice UI", e)
        }
    }

    private fun createCityCard(city: CuratedCityItem, index: Int): View {
        // Mevcut XML layout'u kullan
        val inflater = LayoutInflater.from(requireContext())
        val card = inflater.inflate(R.layout.item_editors_choice_city, null, false)

        // GridLayout için parametreler ayarla
        val params = GridLayout.LayoutParams()
        params.width = 0  // 0 genişlik = parent genişliğinin 1 column weight kadar kısmı
        params.height = GridLayout.LayoutParams.WRAP_CONTENT
        params.columnSpec = GridLayout.spec(GridLayout.UNDEFINED, 1f)  // column weight = 1
        params.rowSpec = GridLayout.spec(GridLayout.UNDEFINED)
        params.setMargins(4, 4, 4, 4)
        card.layoutParams = params

        // Şehir adını al (örn. "istanbul-turkey" -> "Istanbul")
        val cityName = city.cityId.split("-").firstOrNull()?.capitalize() ?: ""
        val country = city.cityId.split("-").getOrNull(1)?.capitalize() ?: ""

        // View'ları güncelle
        card.findViewById<TextView>(R.id.txtEditorsChoiceCityName).text = cityName
        card.findViewById<TextView>(R.id.txtEditorsChoiceCountry).text = country
        card.findViewById<TextView>(R.id.txtEditorsChoiceRating).text = "4.8" // Default değer

        // BURAYA GÖRSEL YÜKLEME KODUNU EKLE
        val imageView = card.findViewById<ImageView>(R.id.imgEditorsChoiceCity)
        if (city.imageUrl.isNotEmpty()) {
            // URL'den görsel yükleme
            Glide.with(requireContext())
                .load(city.imageUrl)
                .centerCrop()
                .into(imageView)
        } else {
            // Görsel yoksa arka plan rengi/resmi ayarla
            val backgroundResource = when (index % 5) {
                0 -> R.drawable.category_landscape_bg
                1 -> R.drawable.category_safety_bg
                2 -> R.drawable.category_livability_bg
                3 -> R.drawable.category_cost_bg
                else -> R.drawable.category_social_bg
            }
            imageView.setBackgroundResource(backgroundResource)
        }

        // Kart tıklama işlemi
        card.setOnClickListener {
            try {
                val action = HomeFragmentDirections.actionHomeFragmentToCityDetailFragment(city.cityId)
                findNavController().navigate(action)
            } catch (e: Exception) {
                Log.e("HomeFragment", "Navigation error: ${e.message}", e)
                Toast.makeText(requireContext(), "Error opening city details", Toast.LENGTH_SHORT).show()
            }
        }

        return card
    }

    // Reklam kartı oluşturan metodu güncelle - GridLayout parametrelerini ekle
    private fun createAdCard(): View {
        // Mevcut XML layout'u kullan
        val inflater = LayoutInflater.from(requireContext())
        val card = inflater.inflate(R.layout.item_editors_choice_city, null, false)

        // GridLayout için parametreler ayarla
        val params = GridLayout.LayoutParams()
        params.width = 0  // 0 genişlik = parent genişliğinin 1 column weight kadar kısmı
        params.height = GridLayout.LayoutParams.WRAP_CONTENT
        params.columnSpec = GridLayout.spec(GridLayout.UNDEFINED, 1f)  // column weight = 1
        params.rowSpec = GridLayout.spec(GridLayout.UNDEFINED)
        params.setMargins(4, 4, 4, 4)
        card.layoutParams = params

        // Arkaplanı değiştir
        card.findViewById<ImageView>(R.id.imgEditorsChoiceCity).setBackgroundColor(
            ContextCompat.getColor(requireContext(), R.color.yellow_500)
        )

        // "REKLAM" yazısı ekle
        card.findViewById<TextView>(R.id.txtEditorsChoiceCityName).text = "REKLAM"
        card.findViewById<TextView>(R.id.txtEditorsChoiceCountry).text = "Reklam Alanı"
        card.findViewById<TextView>(R.id.txtEditorsChoiceRating).visibility = View.GONE

        return card
    }





    // Yardımcı extension function
    private fun String.capitalize(): String {
        return this.replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString() }
    }


    override fun onResume() {
        super.onResume()
        isFragmentVisible = true
        // Fragment yeniden görünür olduğunda verileri kontrol et
        // Önbellekte veri varsa hemen gösterilecek, yoksa yüklenecek
        viewModel.refreshCities(false)
    }

    override fun onPause() {
        super.onPause()
        isFragmentVisible = false
    }

    private fun setupRecyclerViews() {
        // Categories RecyclerView
        binding.recyclerViewCategories.apply {
            adapter = categoriesAdapter
        }

        // Kategorileri göster
        categoriesAdapter.submitList(CategoryModel.getDefaultCategories())

        // Kategori tıklama işleyicisi
        categoriesAdapter.onItemClick = { category ->
            Log.d("HomeFragment", "Category clicked: ${category.title}")
            // Seçilen kategoriye göre viewModel'i güncelle
            viewModel.switchCategory(category.ratingType)
            // Kategori listesine git
            navigateToCategoryList(category.ratingType)
        }


        // Cities RecyclerView (mevcut kodu koru)
        binding.recyclerViewCities.apply {
            adapter = citiesAdapter
        }
        citiesAdapter.onItemClick = { city ->
            try {
                Log.d("HomeFragment", "City clicked: ${city.cityName}")
                val action = HomeFragmentDirections.actionHomeFragmentToCityDetailFragment(city.id)
                findNavController().navigate(action)
            } catch (e: Exception) {
                Log.e("HomeFragment", "Navigation error: ${e.message}", e)
                Toast.makeText(context, "Navigation error: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun navigateToCategoryList(categoryId: String) {
        try {
            val action = HomeFragmentDirections.actionHomeFragmentToCategoryListFragment(categoryId)
            findNavController().navigate(action)
        } catch (e: Exception) {
            Log.e("HomeFragment", "Navigation error: ${e.message}", e)
            Toast.makeText(context, "Navigation error: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    private fun setupClickListeners() {
        // "View All" button click - simple navigation to CategoryList
        binding.txtViewListFragment.setOnClickListener {
            navigateToCategoryList()
        }

        // CardView click - same as View All
        binding.cardCitiesList.setOnClickListener {
            navigateToCategoryList()
        }

        // Retry button for error state
        binding.btnRetry.setOnClickListener {
            viewModel.refreshCities()
        }
    }

    private fun navigateToCategoryList() {
        try {
            // Simple navigation without shared element transitions
            val action = HomeFragmentDirections.actionHomeFragmentToCategoryListFragment()
            findNavController().navigate(action)
        } catch (e: Exception) {
            Log.e("HomeFragment", "Navigation error: ${e.message}", e)
            Toast.makeText(context, "Navigation error: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    private fun setupAnimations() {
        binding.animationLoading.apply {
            setAnimation(R.raw.loading_animation)
            playAnimation()
        }
        binding.animationError.apply {
            setAnimation(R.raw.error_animation)
            playAnimation()
        }
    }

    // HomeFragment.kt
    private fun observeViewModel() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                // HomeState gözlemle
                launch {
                    viewModel.homeState.collect { state ->
                        updateUI(state)
                    }
                }

                // Loading state'i gözlemle
                launch {
                    viewModel.isLoading.collect { isLoading ->
                        binding.animationLoading.isVisible = isLoading && (viewModel.homeState.value is HomeState.Initial)
                    }
                }

                // UI Events'i gözlemle
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
            is BaseViewModel.UiEvent.RefreshData -> {
                viewModel.refreshCities(true)
            }
            else -> {
                // Handle other events
            }
        }
    }

    private fun updateUI(state: HomeState) {
        when (state) {
            is HomeState.Initial -> {
                binding.loadingContainer.isVisible = false
                binding.errorContainer.isVisible = false
                binding.cardCitiesList.isVisible = false
                // Boş durum gösteriliyor
            }

            is HomeState.Loading -> {
                if (state.oldData != null && state.oldData.isNotEmpty()) {
                    // Eski veriler varsa yükleme sırasında göstermeye devam et
                    binding.loadingContainer.isVisible = false
                    binding.errorContainer.isVisible = false
                    binding.cardCitiesList.isVisible = true

                    // Eski verileri adapter'a set et
                    citiesAdapter.submitList(state.oldData)

                    // Burada yükleniyor animasyonunu gösterme
                    // Bu sayede kullanıcı dikkat dağıtıcı bir animasyon görmez
                } else {
                    // Veri yoksa normal yükleme ekranını göster
                    binding.loadingContainer.isVisible = true
                    binding.animationLoading.isVisible = true
                    binding.errorContainer.isVisible = false
                    binding.cardCitiesList.isVisible = false
                    binding.animationLoading.playAnimation()
                }
            }

            is HomeState.Success -> {
                binding.loadingContainer.isVisible = false
                binding.errorContainer.isVisible = false
                binding.cardCitiesList.isVisible = true

                // Verileri adapter'a set et
                // Eğer gösterilen liste ile yeni gelen liste farklıysa güncelle
                if (!areListsEqual(citiesAdapter.currentList, state.cities)) {
                    citiesAdapter.submitList(state.cities)
                }

                Log.d("HomeFragment", "Received ${state.cities.size} cities")
            }

            is HomeState.Error -> {
                // Eğer adapter'da veri varsa onları göstermeye devam et
                if (citiesAdapter.itemCount > 0) {
                    binding.loadingContainer.isVisible = false
                    binding.errorContainer.isVisible = false
                    binding.cardCitiesList.isVisible = true

                    Toast.makeText(context, state.message, Toast.LENGTH_SHORT).show()
                } else {
                    binding.loadingContainer.isVisible = false
                    binding.errorContainer.isVisible = true
                    binding.cardCitiesList.isVisible = false
                    binding.textError.text = state.message
                    binding.animationError.playAnimation()
                }

                Log.e("HomeFragment", "Error: ${state.message}")
            }
        }
    }

    // İki listenin içerik olarak eşit olup olmadığını kontrol eden yardımcı fonksiyon
    private fun areListsEqual(list1: List<CityModel>, list2: List<CityModel>): Boolean {
        if (list1.size != list2.size) return false

        // İçerik kontrolü - sadece ID'leri karşılaştır (performans için)
        return list1.zip(list2).all { (item1, item2) -> item1.id == item2.id }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}