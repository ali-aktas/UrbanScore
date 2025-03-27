package com.aliaktas.urbanscore.ui.home.controllers

import android.util.Log
import android.view.View
import com.aliaktas.urbanscore.databinding.FragmentHomeBinding
import com.aliaktas.urbanscore.ui.home.CitiesAdapter
import com.aliaktas.urbanscore.ui.home.HomeState

/**
 * En iyi şehirler listesini yöneten controller
 */
class TopCitiesController(
    private val binding: FragmentHomeBinding,
    private val onCityClick: (String) -> Unit,
    private val onViewAllClick: () -> Unit,
    private val checkInternetBeforeClick: () -> Boolean
) : HomeController {

    private val citiesAdapter = CitiesAdapter()

    override fun bind(view: View) {
        Log.d(TAG, "Setting up top cities RecyclerView")
        setupRecyclerView()
        setupViewAllButton()
    }

    private fun setupRecyclerView() {
        binding.recyclerViewCities.apply {
            adapter = citiesAdapter
            setHasFixedSize(false)
            recycledViewPool.setMaxRecycledViews(0, 15) // Performans optimizasyonu
        }

        // Tıklama olayını ayarla
        citiesAdapter.onItemClick = { city ->
            if (checkInternetBeforeClick()) {
                Log.d(TAG, "City clicked: ${city.cityName}, id: ${city.id}")
                onCityClick(city.id)
            }
        }
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

    override fun update(state: HomeState) {
        // Sadece başarılı durumda şehirler listesini güncelle
        if (state is HomeState.Success) {
            Log.d(TAG, "Updating cities list with ${state.cities.size} cities")

            // Yeni bir liste oluştur - bu, DiffUtil'e tam bir refresh sinyali verir
            val newList = state.cities.toList()
            citiesAdapter.submitList(newList)

            // Alternatif olarak, görünümü tamamen yenilemek için:
            binding.recyclerViewCities.post { citiesAdapter.notifyDataSetChanged() }
        }
    }

    companion object {
        private const val TAG = "TopCitiesController"
    }
}