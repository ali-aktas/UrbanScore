package com.aliaktas.urbanscore.ui.detail.controllers

import android.view.View
import com.aliaktas.urbanscore.R
import com.aliaktas.urbanscore.databinding.FragmentCityDetailBinding
import com.aliaktas.urbanscore.ui.detail.CityDetailState
import com.aliaktas.urbanscore.ui.detail.CityDetailViewModel

/**
 * Eylem butonlarını (Şehri puanla, İstek listesine ekle) yöneten controller
 */
class ActionButtonsController(
    private val binding: FragmentCityDetailBinding,
    private val viewModel: CityDetailViewModel
) : UiController {

    override fun bind(view: View) {
        // Tıklama işleyicilerini ayarla
        binding.btnRateCity.setOnClickListener {
            viewModel.showRatingSheet()
        }

        binding.btnAddToWishlist.setOnClickListener {
            viewModel.toggleWishlist()
        }
    }

    override fun update(state: CityDetailState) {
        if (state !is CityDetailState.Success) return

        // İstek listesi butonunu güncelle
        binding.btnAddToWishlist.apply {
            if (state.isInWishlist) {
                text = context.getString(R.string.in_your_list)
                setBackgroundColor(context.getColor(android.R.color.black))
            } else {
                text = context.getString(R.string.add_to_wishlist)
                setBackgroundColor(context.getColor(R.color.primary_purple))
            }
        }

        // Puanlama butonunu güncelle
        binding.btnRateCity.apply {
            text = if (state.hasUserRated) {
                context.getString(R.string.update_rating)
            } else {
                context.getString(R.string.rate_this_city)
            }
        }
    }
}