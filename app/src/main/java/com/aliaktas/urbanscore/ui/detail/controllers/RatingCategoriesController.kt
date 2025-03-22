package com.aliaktas.urbanscore.ui.detail.controllers

import android.view.View
import com.aliaktas.urbanscore.databinding.FragmentCityDetailBinding
import com.aliaktas.urbanscore.ui.detail.CityDetailFormatter
import com.aliaktas.urbanscore.ui.detail.CityDetailState

/**
 * Kategori puanlarını (Güvenlik, Çevre, vb.) yöneten controller
 */
class RatingCategoriesController(
    private val binding: FragmentCityDetailBinding,
    private val formatter: CityDetailFormatter
) : UiController {

    override fun bind(view: View) {
        // İlk yapılandırma gerekirse buraya eklenecek
    }

    override fun update(state: CityDetailState) {
        if (state !is CityDetailState.Success) return

        // Kategori puanlarını güncelle
        with(state.city.ratings) {
            binding.txtEnvironmentRating.text = formatter.formatRating(environment)
            binding.txtSafetyRating.text = formatter.formatRating(safety)
            binding.txtLivabilityRating.text = formatter.formatRating(livability)
            binding.txtCostRating.text = formatter.formatRating(cost)
            binding.txtSocialRating.text = formatter.formatRating(social)
        }
    }
}