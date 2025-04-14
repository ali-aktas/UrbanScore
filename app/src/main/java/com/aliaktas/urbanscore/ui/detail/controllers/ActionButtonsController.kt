package com.aliaktas.urbanscore.ui.detail.controllers

import android.content.res.ColorStateList
import android.view.View
import com.aliaktas.urbanscore.R
import com.aliaktas.urbanscore.databinding.FragmentCityDetailBinding
import com.aliaktas.urbanscore.ui.detail.CityDetailState
import com.aliaktas.urbanscore.ui.detail.CityDetailViewModel
import android.graphics.Color

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
                // Outline stil (beyaz çerçeveli, içi boş)
                backgroundTintList = ColorStateList.valueOf(Color.TRANSPARENT)
                strokeColor = ColorStateList.valueOf(Color.WHITE)
                strokeWidth = 1
                setTextColor(Color.WHITE)
            } else {
                text = context.getString(R.string.add_to_wishlist)
                // Orijinal görünüm
                setBackgroundResource(R.drawable.btn_gradient_secondary)
                backgroundTintList = null
                strokeWidth = 0
                setTextColor(Color.WHITE)
            }
        }

        // Puanlama butonunu güncelle
        binding.btnRateCity.apply {
            if (state.hasUserRated) {
                text = context.getString(R.string.update_rating)
                // Beyaz arka plan
                backgroundTintList = ColorStateList.valueOf(Color.GRAY)
                setTextColor(context.getColor(R.color.primary))
            } else {
                text = context.getString(R.string.rate_this_city)
                // Orijinal gradient görünüm
                setBackgroundResource(R.drawable.btn_gradient_primary)
                backgroundTintList = null
                setTextColor(Color.WHITE)
            }
        }
    }
}