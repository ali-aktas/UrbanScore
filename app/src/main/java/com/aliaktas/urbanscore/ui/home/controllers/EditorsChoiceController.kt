package com.aliaktas.urbanscore.ui.home.controllers

import android.util.Log
import android.view.View
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import com.aliaktas.urbanscore.databinding.FragmentHomeBinding
import com.aliaktas.urbanscore.ui.common.EditorsChoiceAdapter
import com.aliaktas.urbanscore.ui.home.HomeState
import com.aliaktas.urbanscore.ui.home.HomeViewModel
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

/**
 * Editör seçimlerini yöneten controller
 */
class EditorsChoiceController(
    private val binding: FragmentHomeBinding,
    private val viewModel: HomeViewModel,
    private val lifecycleOwner: LifecycleOwner,
    private val onCityClick: (String) -> Unit,
    private val checkInternetBeforeClick: () -> Boolean
) : HomeController {

    private val editorsChoiceAdapter = EditorsChoiceAdapter()
    private var isVisible = false

    override fun bind(view: View) {

        binding.txtEditorsChoicesTitle.visibility = View.GONE
        binding.recyclerViewEditorsChoice.visibility = View.GONE

        setupRecyclerView()
        observeViewModel()
    }

    private fun setupRecyclerView() {
        binding.recyclerViewEditorsChoice.apply {
            adapter = editorsChoiceAdapter
            layoutManager = LinearLayoutManager(context, LinearLayoutManager.HORIZONTAL, false)
            setHasFixedSize(false)
        }

        // Tıklama olayını ayarla
        editorsChoiceAdapter.onItemClick = { cityId ->
            if (checkInternetBeforeClick()) {
                Log.d(TAG, "Editors choice city clicked: $cityId")
                onCityClick(cityId)
            }
        }
    }

    private fun observeViewModel() {
        // EditorsChoice kendi Flow'unu kullanır
        lifecycleOwner.lifecycleScope.launch {
            lifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.popularCitiesState.collectLatest { cities ->
                    Log.d(TAG, "Editors choice updated: ${cities.size} cities")
                    editorsChoiceAdapter.submitList(cities)
                }
            }
        }
    }

    override fun update(state: HomeState) {
        if (state is HomeState.Success && !isVisible) {
            // Top Rated Cities başarıyla yüklendiğinde Popular Cities'i göster
            binding.txtEditorsChoicesTitle.visibility = View.VISIBLE
            binding.recyclerViewEditorsChoice.visibility = View.VISIBLE
            isVisible = true
        }
    }

    companion object {
        private const val TAG = "EditorsChoiceController"
    }
}