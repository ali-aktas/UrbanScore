package com.aliaktas.urbanscore.util

import android.widget.ImageView
import android.widget.TextView
import androidx.databinding.BindingAdapter
import com.aliaktas.urbanscore.R
import com.bumptech.glide.Glide
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions

/**
 * Data binding adaptörleri - UI elementleri ile veri modelleri arasında bağlantı kurar
 */
object BindingAdapters {

    @BindingAdapter("cityFlag")
    @JvmStatic
    fun loadCityFlag(imageView: ImageView, flagUrl: String?) {
        if (!flagUrl.isNullOrEmpty()) {
            Glide.with(imageView.context)
                .load(flagUrl)
                .transition(DrawableTransitionOptions.withCrossFade())
                .into(imageView)
        }
    }

    @BindingAdapter("ratingText")
    @JvmStatic
    fun setRatingText(textView: TextView, rating: Double) {
        textView.text = String.format("%.2f", rating)
    }

    @BindingAdapter("cityName", "countryName")
    @JvmStatic
    fun setCityCountryText(textView: TextView, cityName: String, countryName: String) {
        textView.text = "$cityName, $countryName"
    }

    @BindingAdapter("positionNumber")
    @JvmStatic
    fun setPositionNumber(textView: TextView, position: Int) {
        textView.text = (position + 1).toString()
    }
}