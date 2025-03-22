package com.aliaktas.urbanscore.ui.detail.controllers

import android.view.View
import com.aliaktas.urbanscore.databinding.FragmentCityDetailBinding
import com.aliaktas.urbanscore.ui.detail.CityDetailFormatter
import com.aliaktas.urbanscore.ui.detail.CityDetailState
import com.bumptech.glide.Glide

/**
 * Şehir temel bilgilerini (isim, bayrak, ülke vb.) yöneten controller
 */
class BasicInfoController(
    private val binding: FragmentCityDetailBinding,
    private val formatter: CityDetailFormatter
) : UiController {

    override fun bind(view: View) {
        // İlk yapılandırma gerekirse buraya eklenecek
    }

    override fun update(state: CityDetailState) {
        if (state !is CityDetailState.Success) return

        val city = state.city

        // Şehir bilgilerini güncelle
        with(binding) {
            textCityName.text = city.cityName
            txtRatingCount.text = formatter.getRatingCountText(
                { id, quantity, arg -> root.context.resources.getQuantityString(id, quantity, arg) },
                city.ratingCount
            )

            // Bayrağı yükle
            Glide.with(root.context)
                .load(city.flagUrl)
                .centerCrop()
                .into(imgFlag)

            // Şehir detayları
            txtCountry.text = city.country
            txtRegion.text = city.region
            txtPopulation.text = formatter.formatPopulation(city.population)

            // Ortalama puan
            txtAverageRating.text = formatter.formatRating(city.averageRating)
        }
    }
}