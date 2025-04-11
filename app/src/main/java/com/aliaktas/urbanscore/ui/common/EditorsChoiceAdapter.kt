package com.aliaktas.urbanscore.ui.common

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.aliaktas.urbanscore.R
import com.aliaktas.urbanscore.data.model.CuratedCityItem
import com.aliaktas.urbanscore.databinding.ItemEditorsChoiceCityBinding
import com.bumptech.glide.Glide
import java.util.Locale

/**
 * Adapter for editors' choice cities, designed to be used in a horizontal RecyclerView
 * More efficient than building GridLayout views manually
 */
class EditorsChoiceAdapter :
    ListAdapter<CuratedCityItem, EditorsChoiceAdapter.ViewHolder>(EditorsChoiceDiffCallback()) {

    var onItemClick: ((String) -> Unit)? = null

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemEditorsChoiceCityBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(getItem(position), position)
    }

    inner class ViewHolder(private val binding: ItemEditorsChoiceCityBinding) :
        RecyclerView.ViewHolder(binding.root) {

        init {
            binding.root.setOnClickListener {
                val position = bindingAdapterPosition
                if (position != RecyclerView.NO_POSITION) {
                    onItemClick?.invoke(getItem(position).cityId)
                }
            }
        }

        fun bind(item: CuratedCityItem, position: Int) {
            // Parse city name and country from cityId
            val cityParts = item.cityId.split("-")
            val cityName = cityParts.firstOrNull()?.capitalize() ?: ""
            val country = cityParts.getOrNull(1)?.capitalize() ?: ""

            // Set text
            binding.txtEditorsChoiceCityName.text = cityName
            binding.txtEditorsChoiceCountry.text = country
            //binding.txtEditorsChoiceRating.text = "4.8" // Default value

            // Load image (if available) or use placeholder background
            if (item.imageUrl.isNotEmpty()) {
                Glide.with(binding.root.context)
                    .load(item.imageUrl)
                    .error(R.drawable.city_placeholder) // Yükleme hatası durumunda
                    .placeholder(R.drawable.city_placeholder) // Yükleme sırasında
                    .centerCrop()
                    .into(binding.imgEditorsChoiceCity)
            } else {
                // Yeni city_placeholder görselini kullan
                Glide.with(binding.root.context)
                    .load(R.drawable.city_placeholder)
                    .centerCrop()
                    .into(binding.imgEditorsChoiceCity)
            }
        }
    }

    private class EditorsChoiceDiffCallback : DiffUtil.ItemCallback<CuratedCityItem>() {
        override fun areItemsTheSame(oldItem: CuratedCityItem, newItem: CuratedCityItem): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: CuratedCityItem, newItem: CuratedCityItem): Boolean {
            return oldItem == newItem
        }
    }
}

// Extension function to capitalize string
private fun String.capitalize(): String =
    replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString() }