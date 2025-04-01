// app/src/main/java/com/aliaktas/urbanscore/ui/home/CitiesAdapter.kt dosyasını aç

package com.aliaktas.urbanscore.ui.home

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.aliaktas.urbanscore.R
import com.aliaktas.urbanscore.data.model.CityModel
import com.aliaktas.urbanscore.databinding.ItemCityBinding
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions
import com.bumptech.glide.request.RequestOptions

class CitiesAdapter(private val categoryId: String = "averageRating")
    : ListAdapter<CityModel, CitiesAdapter.CityViewHolder>(CityDiffCallback()) {

    // Tıklama olayı callback'i - eksik tanım buydu
    var onItemClick: ((CityModel) -> Unit)? = null

    // Önbellek için Glide request options
    private val glideOptions by lazy {
        RequestOptions()
            .diskCacheStrategy(DiskCacheStrategy.ALL)
            .override(150, 150)  // Boyutu sınırla
            .dontAnimate() // Animasyonları kapat
            .dontTransform() // Dönüşümleri kapat
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CityViewHolder {
        // View inflation performansını artır
        val binding = ItemCityBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return CityViewHolder(binding)
    }

    override fun onBindViewHolder(holder: CityViewHolder, position: Int) {
        // Binding işlemini optimize et
        holder.bind(getItem(position), position)
    }

    inner class CityViewHolder(
        private val binding: ItemCityBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        // Görünüm referanslarını cache'le
        private val cityText = binding.textCityName
        private val ratingText = binding.textRating
        private val positionText = binding.textRatingCount
        private val flagImage = binding.imageFlag

        init {
            binding.root.setOnClickListener {
                val position = bindingAdapterPosition
                if (position != RecyclerView.NO_POSITION) {
                    onItemClick?.invoke(getItem(position))
                }
            }
        }

        fun bind(city: CityModel, position: Int) {
            // String birleştirmeyi optimize et
            cityText.text = buildString {
                append(city.cityName)
                append(", ")
                append(city.country)
            }

            // Kategori puanlaması
            val rating = when (categoryId) {
                "gastronomy" -> city.ratings.gastronomy
                "aesthetics" -> city.ratings.aesthetics
                "safety" -> city.ratings.safety
                "culture" -> city.ratings.culture
                "livability" -> city.ratings.livability
                "social" -> city.ratings.social
                "hospitality" -> city.ratings.hospitality
                else -> city.averageRating
            }

            // Önceden formatlanmış string kullan
            ratingText.text = formatRating(rating)
            positionText.text = (position + 1).toString()

            // Performans kritik: Glide optimize edilmiş kullanımı
            Glide.with(binding.root)
                .load(city.flagUrl)
                .apply(glideOptions)
                .into(flagImage)
        }

        // Rating formatı için helper metodu - her seferinde String.format çağırmayı önler
        private fun formatRating(rating: Double): String {
            return ((rating * 100).toInt() / 100.0).toString()
        }
    }

    // DiffUtil optimize edildi
    private class CityDiffCallback : DiffUtil.ItemCallback<CityModel>() {
        override fun areItemsTheSame(oldItem: CityModel, newItem: CityModel): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: CityModel, newItem: CityModel): Boolean {
            return oldItem.id == newItem.id
        }
    }
}