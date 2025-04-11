package com.aliaktas.urbanscore.util

import android.widget.ImageView
import com.aliaktas.urbanscore.R
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions
import com.bumptech.glide.request.RequestOptions
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ImageLoader @Inject constructor() {

    // Bayraklar için optimize edilmiş RequestOptions
    private val flagOptions by lazy {
        RequestOptions()
            .diskCacheStrategy(DiskCacheStrategy.ALL) // Tüm resmi önbelleğe al
            .override(120, 80) // Bayrak boyutlarını sınırla
            .centerCrop()
            .dontAnimate() // Animasyonları kapat (performans için)
            .placeholder(R.drawable.loginicon2)
            .error(R.drawable.loginicon2)
    }

    // Profil resimleri için optimize edilmiş RequestOptions
    private val avatarOptions by lazy {
        RequestOptions()
            .diskCacheStrategy(DiskCacheStrategy.ALL)
            .override(80, 80)
            .circleCrop()
            .placeholder(R.drawable.loginicon2)
            .error(R.drawable.loginicon2)
    }

    // Genel resimler için RequestOptions
    private val imageOptions by lazy {
        RequestOptions()
            .diskCacheStrategy(DiskCacheStrategy.AUTOMATIC)
            .placeholder(R.drawable.loginicon2)
            .error(R.drawable.loginicon2)
    }

    // Genel resim yükleme - eski metot, diğer metotları çağırıyor
    fun loadImage(
        imageView: ImageView,
        url: String?,
        placeholder: Int = R.drawable.loginicon2,
        error: Int = R.drawable.loginicon2
    ) {
        if (url.isNullOrEmpty()) {
            imageView.setImageResource(placeholder)
            return
        }

        Glide.with(imageView.context.applicationContext)
            .load(url)
            .apply(
                RequestOptions()
                    .placeholder(placeholder)
                    .error(error)
            )
            .transition(DrawableTransitionOptions.withCrossFade())
            .into(imageView)
    }

    // Şehir resmi yükleme
    fun loadCityImage(
        imageView: ImageView,
        url: String?
    ) {
        if (url.isNullOrEmpty()) {
            imageView.setImageResource(R.drawable.loginicon2)
            return
        }

        Glide.with(imageView.context.applicationContext)
            .load(url)
            .apply(imageOptions)
            .into(imageView)
    }

    // Bayrak yükleme - optimize edilmiş versiyon
    fun loadFlag(
        imageView: ImageView,
        url: String?
    ) {
        if (url.isNullOrEmpty()) {
            imageView.setImageResource(R.drawable.loginicon2)
            return
        }

        Glide.with(imageView.context.applicationContext)
            .load(url)
            .apply(flagOptions)
            .into(imageView)
    }

    // Kullanıcı avatarını yükleme - optimize edilmiş versiyon
    fun loadUserAvatar(
        imageView: ImageView,
        url: String?
    ) {
        if (url.isNullOrEmpty()) {
            imageView.setImageResource(R.drawable.loginicon2)
            return
        }

        Glide.with(imageView.context.applicationContext)
            .load(url)
            .apply(avatarOptions)
            .into(imageView)
    }
}