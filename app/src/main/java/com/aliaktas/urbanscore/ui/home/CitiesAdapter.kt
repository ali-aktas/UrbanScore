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

class CitiesAdapter : ListAdapter<CityModel, CitiesAdapter.CityViewHolder>(CityDiffCallback()) {

    var onItemClick: ((CityModel) -> Unit)? = null

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CityViewHolder {
        val binding = ItemCityBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return CityViewHolder(binding)
    }

    override fun onBindViewHolder(holder: CityViewHolder, position: Int) {
        holder.bind(getItem(position))
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

        fun bind(city: CityModel) {
            with(binding) {
                textCityName.text = "${city.cityName}, ${city.country}"
                textRating.text = String.format("%.2f", city.averageRating)
                textRatingCount.text = (position + 1).toString()

                // Loading Flag URL with Glide
                Glide.with(root)
                    .load(city.flagUrl)
                    .transition(DrawableTransitionOptions.withCrossFade())
                    .into(imageFlag)
            }
        }
    }

    private class CityDiffCallback : DiffUtil.ItemCallback<CityModel>() {
        override fun areItemsTheSame(oldItem: CityModel, newItem: CityModel): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: CityModel, newItem: CityModel): Boolean {
            return oldItem == newItem
        }
    }
}