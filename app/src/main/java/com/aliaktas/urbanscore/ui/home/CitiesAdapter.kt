// app/src/main/java/com/aliaktas/urbanscore/ui/home/CitiesAdapter.kt dosyasını aç

package com.aliaktas.urbanscore.ui.home

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.aliaktas.urbanscore.data.model.CityModel
import com.aliaktas.urbanscore.databinding.ItemCityBinding
import com.bumptech.glide.Glide
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions

class CitiesAdapter(private val categoryId: String = "averageRating") : ListAdapter<CityModel, CitiesAdapter.CityViewHolder>(CityDiffCallback()) {

    var onItemClick: ((CityModel) -> Unit)? = null

    // Constructor olmadan kategori ID'sini ayarlamak için
    private var _categoryId = categoryId

    fun setCategoryId(categoryId: String) {
        _categoryId = categoryId
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CityViewHolder {
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

        init {
            binding.root.setOnClickListener {
                val position = bindingAdapterPosition
                if (position != RecyclerView.NO_POSITION) {
                    onItemClick?.invoke(getItem(position))
                }
            }
        }

        fun bind(city: CityModel, position: Int) {
            with(binding) {
                textCityName.text = "${city.cityName}, ${city.country}"

                // Kategori ID'sine göre doğru puanı al
                val rating = when (_categoryId) {
                    "gastronomy" -> city.ratings.gastronomy
                    "aesthetics" -> city.ratings.aesthetics
                    "safety" -> city.ratings.safety
                    "culture" -> city.ratings.culture
                    "livability" -> city.ratings.livability
                    "social" -> city.ratings.social
                    "hospitality" -> city.ratings.hospitality
                    else -> city.averageRating
                }

                textRating.text = String.format("%.2f", rating)
                textRatingCount.text = (position + 1).toString()

                // Loading Flag URL with Glide
                Glide.with(root)
                    .load(city.flagUrl)
                    .transition(DrawableTransitionOptions.withCrossFade())
                    .into(imageFlag)
            }
        }
    }

    // DiffUtil Callback sınıfı
    private class CityDiffCallback : DiffUtil.ItemCallback<CityModel>() {
        override fun areItemsTheSame(oldItem: CityModel, newItem: CityModel): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: CityModel, newItem: CityModel): Boolean {
            // Sadece gösterilen alanları karşılaştır
            return oldItem.id == newItem.id &&
                    oldItem.cityName == newItem.cityName &&
                    oldItem.country == newItem.country &&
                    oldItem.averageRating == newItem.averageRating &&
                    oldItem.flagUrl == newItem.flagUrl
        }
    }
}