package com.aliaktas.urbanscore.ui.detail.controllers

import android.view.View
import androidx.lifecycle.LifecycleCoroutineScope
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.lifecycleScope
import com.aliaktas.urbanscore.databinding.FragmentCityDetailBinding
import com.aliaktas.urbanscore.ui.detail.CityDetailState
import com.aliaktas.urbanscore.ui.detail.RadarChartHelper
import kotlinx.coroutines.launch

/**
 * Radar grafiğini yöneten controller
 */
class RadarChartController(
    private val binding: FragmentCityDetailBinding,
    private val radarChartHelper: RadarChartHelper,
    private val lifecycleOwner: LifecycleOwner
) : UiController {

    private var hasInitializedChart = false

    override fun bind(view: View) {
        lifecycleOwner.lifecycleScope.launch {
            radarChartHelper.setupRadarChart(binding.radarChart)
            hasInitializedChart = true
        }
    }

    override fun update(state: CityDetailState) {
        if (state !is CityDetailState.Success || !hasInitializedChart) return

        // isPartialUpdate=true ise radar grafiğini güncellemeyi atla
        if (state.isPartialUpdate) return

        lifecycleOwner.lifecycleScope.launch {
            radarChartHelper.updateChartData(binding.radarChart, state.city.ratings)
        }
    }
}