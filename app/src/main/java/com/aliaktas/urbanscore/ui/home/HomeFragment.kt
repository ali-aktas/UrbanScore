package com.aliaktas.urbanscore.ui.home

import android.app.AlertDialog
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
import androidx.navigation.fragment.findNavController
import com.aliaktas.urbanscore.BuildConfig
import com.aliaktas.urbanscore.R
import com.aliaktas.urbanscore.data.model.CategoryModel
import com.aliaktas.urbanscore.data.model.CityModel
import com.aliaktas.urbanscore.databinding.FragmentHomeBinding
import com.aliaktas.urbanscore.utils.TestDataGenerator
import com.google.firebase.firestore.FirebaseFirestore
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

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

    private fun observeViewModel() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.state.collect { state ->
                    if (isFragmentVisible) {
                        updateUI(state)
                    }
                }
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