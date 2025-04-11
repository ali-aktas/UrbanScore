package com.aliaktas.urbanscore.ui.home.controllers

import android.util.Log
import android.view.View
import com.aliaktas.urbanscore.R
import com.aliaktas.urbanscore.databinding.FragmentHomeBinding
import com.aliaktas.urbanscore.ui.home.HomeState
import com.aliaktas.urbanscore.ui.home.HomeViewModel

/**
 * SwipeRefresh davranışını yöneten controller
 */
class SwipeRefreshController(
    private val binding: FragmentHomeBinding,
    private val viewModel: HomeViewModel,
    private val networkController: NetworkController
) : HomeController {

    override fun bind(view: View) {
        Log.d(TAG, "Setting up swipe refresh")
        setupSwipeRefresh()
    }

    private fun setupSwipeRefresh() {
        binding.swipeRefreshLayout.setOnRefreshListener {
            Log.d(TAG, "SwipeRefresh triggered")
            if (networkController.checkInternetConnection()) {
                viewModel.loadTopRatedCities(true) // Force refresh
            } else {
                Log.d(TAG, "No internet connection, stopping refresh")
                binding.swipeRefreshLayout.isRefreshing = false
            }
        }

        // Renk şemasını ayarla
        binding.swipeRefreshLayout.setColorSchemeResources(
            R.color.primary,
            R.color.primary
        )
    }

    override fun update(state: HomeState) {
        // SwipeRefresh durumunu güncelle
        binding.swipeRefreshLayout.isRefreshing = state is HomeState.Loading && state.oldData != null
    }

    companion object {
        private const val TAG = "SwipeRefreshController"
    }
}