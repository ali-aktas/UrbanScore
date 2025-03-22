
package com.aliaktas.urbanscore.ui.detail.controllers

import android.view.View
import com.aliaktas.urbanscore.databinding.FragmentCityDetailBinding
import com.aliaktas.urbanscore.ui.detail.CityDetailState
import com.aliaktas.urbanscore.ui.detail.CityDetailViewModel

/**
 * Keşfet butonlarını (YouTube, Google) yöneten controller
 */
class ExploreButtonsController(
    private val binding: FragmentCityDetailBinding,
    private val viewModel: CityDetailViewModel
) : UiController {

    override fun bind(view: View) {
        binding.btnExploreYouTube.setOnClickListener {
            viewModel.openYouTubeSearch()
        }

        binding.btnExploreGoogle.setOnClickListener {
            viewModel.openGoogleSearch()
        }
    }

    override fun update(state: CityDetailState) {
        // Keşfet butonları için bir UI güncellemesi gerekmiyor
    }
}