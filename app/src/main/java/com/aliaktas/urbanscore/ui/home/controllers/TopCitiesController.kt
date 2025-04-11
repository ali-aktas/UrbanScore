package com.aliaktas.urbanscore.ui.home.controllers

import android.util.Log
import android.view.View
import androidx.recyclerview.widget.LinearLayoutManager
import com.aliaktas.urbanscore.databinding.FragmentHomeBinding
import com.aliaktas.urbanscore.ui.home.CitiesAdapter
import com.aliaktas.urbanscore.ui.home.HomeState
import com.aliaktas.urbanscore.util.ImageLoader
import javax.inject.Inject

/**
 * En iyi şehirler listesini yöneten controller
 */
class TopCitiesController @Inject constructor(
    private val binding: FragmentHomeBinding,
    private val onCityClick: (String) -> Unit,
    private val onViewAllClick: () -> Unit,
    private val checkInternetBeforeClick: () -> Boolean,
    private val imageLoader: ImageLoader // ImageLoader dependency ekledik
) : HomeController {

    private val citiesAdapter = CitiesAdapter("averageRating", imageLoader)

    override fun bind(view: View) {
        Log.d(TAG, "Setting up top cities RecyclerView")
        setupRecyclerView()
        setupViewAllButton()
    }

    private fun setupViewAllButton() {
        // "View All" butonuna tıklama olayını ayarla
        binding.txtViewListFragment.setOnClickListener {
            if (checkInternetBeforeClick()) {
                Log.d(TAG, "View all cities clicked")
                onViewAllClick()
            }
        }

        // Card'a tıklama olayını ayarla
        binding.cardCitiesList.setOnClickListener {
            if (checkInternetBeforeClick()) {
                Log.d(TAG, "View all cities card clicked")
                onViewAllClick()
            }
        }
    }

    private fun setupRecyclerView() {
        binding.recyclerViewCities.apply {
            // RecyclerView yapılandırması
            setHasFixedSize(true)
            itemAnimator = null  // Animasyonları kapat

            // ViewHolder önbellek boyutunu artır
            setItemViewCacheSize(20)

            // RecyclerView havuzunu genişlet
            recycledViewPool.setMaxRecycledViews(0, 30)

            // Ekran dışı öğeleri önden yükle
            val layoutManager = LinearLayoutManager(context)
            layoutManager.initialPrefetchItemCount = 10
            this.layoutManager = layoutManager

            // Adaptörü atamadan önce diğer yapılandırmaları tamamla
            adapter = citiesAdapter
        }

        // Tıklama olayını ayarla
        citiesAdapter.onItemClick = { city ->
            if (checkInternetBeforeClick()) {
                Log.d(TAG, "City clicked: ${city.cityName}, id: ${city.id}")
                onCityClick(city.id)
            }
        }

        // İlk yüklemeyi optimize et - boş bir liste göster, sonra gerçek veri gelince güncelle
        citiesAdapter.submitList(emptyList())
    }

    override fun update(state: HomeState) {
        if (state is HomeState.Success) {
            // DiffUtil'in düzgün çalışabilmesi için direkt submitList çağrılmalı
            citiesAdapter.submitList(state.cities)

            // notifyDataSetChanged kullanımını kaldırdık - performansı çok olumsuz etkiliyor
        }
    }

    override fun shouldUpdate(state: HomeState): Boolean {
        // Sadece Success veya Loading state'lerinde güncelle
        return state is HomeState.Success || state is HomeState.Loading
    }

    companion object {
        private const val TAG = "TopCitiesController"
    }
}