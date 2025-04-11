package com.aliaktas.urbanscore.ui.home

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.aliaktas.urbanscore.data.model.CityModel
import com.aliaktas.urbanscore.databinding.ItemCityBinding
import com.aliaktas.urbanscore.util.ImageLoader
import java.util.Locale
import javax.inject.Inject

class CitiesAdapter @Inject constructor(
    private val categoryId: String = "averageRating",
    private val imageLoader: ImageLoader // ImageLoader sınıfını ekledik
) : ListAdapter<CityModel, CitiesAdapter.CityViewHolder>(CityDiffCallback()) {

    // Tıklama olayı callback'i
    var onItemClick: ((CityModel) -> Unit)? = null

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CityViewHolder {
        // View inflation
        val binding = ItemCityBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return CityViewHolder(binding)
    }

    override fun onBindViewHolder(holder: CityViewHolder, position: Int) {
        holder.bind(getItem(position), position)
    }

    inner class CityViewHolder(
        private val binding: ItemCityBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        // Görünüm referanslarını cache'le
        private val cityText = binding.textCityName
        private val ratingText = binding.textRating
        private val positionText = binding.textRatingCount

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

            // Performans kritik: ImageLoader kullanımı
            imageLoader.loadFlag(binding.imageFlag, city.flagUrl)
        }

        // Rating formatı için helper metodu - her seferinde String.format çağırmayı önler
        private fun formatRating(rating: Double): String {
            return String.format(Locale.ENGLISH, "%.2f", rating)
        }
    }

    // DiffUtil
    private class CityDiffCallback : DiffUtil.ItemCallback<CityModel>() {
        override fun areItemsTheSame(oldItem: CityModel, newItem: CityModel): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: CityModel, newItem: CityModel): Boolean {
            return oldItem.id == newItem.id &&
                    oldItem.averageRating == newItem.averageRating
        }
    }
}