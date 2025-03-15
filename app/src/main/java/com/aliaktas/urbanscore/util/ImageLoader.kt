package com.aliaktas.urbanscore.util

import android.widget.ImageView
import com.aliaktas.urbanscore.R
import com.bumptech.glide.Glide
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions
import com.bumptech.glide.request.RequestOptions
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ImageLoader @Inject constructor() {

    fun loadImage(
        imageView: ImageView,
        url: String?,
        placeholder: Int = R.drawable.loginicon2, // Varsayılan bir drawable kullanın
        error: Int = R.drawable.loginicon2 // Varsayılan bir drawable kullanın
    ) {
        Glide.with(imageView.context)
            .load(url)
            .apply(
                RequestOptions()
                    .placeholder(placeholder)
                    .error(error)
            )
            .transition(DrawableTransitionOptions.withCrossFade())
            .into(imageView)
    }

    fun loadCityImage(
        imageView: ImageView,
        url: String?
    ) {
        loadImage(imageView, url, R.drawable.loginicon2, R.drawable.loginicon2) // Mevcut drawable kullanın
    }

    fun loadFlag(
        imageView: ImageView,
        url: String?
    ) {
        Glide.with(imageView.context)
            .load(url)
            .apply(
                RequestOptions()
                    .placeholder(R.drawable.loginicon2) // Mevcut drawable kullanın
                    .error(R.drawable.loginicon2) // Mevcut drawable kullanın
            )
            .centerCrop()
            .into(imageView)
    }

    fun loadUserAvatar(
        imageView: ImageView,
        url: String?
    ) {
        Glide.with(imageView.context)
            .load(url)
            .apply(
                RequestOptions()
                    .placeholder(R.drawable.loginicon2) // Mevcut drawable kullanın
                    .error(R.drawable.loginicon2) // Mevcut drawable kullanın
            )
            .circleCrop()
            .into(imageView)
    }
}