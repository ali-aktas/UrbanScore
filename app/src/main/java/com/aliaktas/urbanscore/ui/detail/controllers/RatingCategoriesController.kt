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
            binding.txtAestheticsRating.text = formatter.formatRating(aesthetics)
            binding.txtSafetyRating.text = formatter.formatRating(safety)
            binding.txtHospitalityRating.text = formatter.formatRating(hospitality)
            binding.txtGastronomyRating.text = formatter.formatRating(gastronomy)
            binding.txtLivabilityRating.text = formatter.formatRating(livability)
            binding.txtCultureRating.text = formatter.formatRating(culture)
            binding.txtSocialRating.text = formatter.formatRating(social)
        }
    }
}