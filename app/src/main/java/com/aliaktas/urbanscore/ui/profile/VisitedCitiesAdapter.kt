package com.aliaktas.urbanscore.ui.profile

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.aliaktas.urbanscore.databinding.ItemVisitedCitiesBinding
import com.bumptech.glide.Glide
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions

class VisitedCitiesAdapter(
    private val onItemClick: (String) -> Unit
) : ListAdapter<VisitedCityItem, VisitedCitiesAdapter.ViewHolder>(DIFF_CALLBACK) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemVisitedCitiesBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = getItem(position)
        holder.bind(item, position + 1) // Pozisyonu 1'den başlat
    }

    inner class ViewHolder(private val binding: ItemVisitedCitiesBinding) :
        RecyclerView.ViewHolder(binding.root) {

        init {
            binding.root.setOnClickListener {
                val position = bindingAdapterPosition
                if (position != RecyclerView.NO_POSITION) {
                    onItemClick(getItem(position).id)
                }
            }
        }

        fun bind(city: VisitedCityItem, position: Int) {
            binding.textCityName.text = "${city.name}, ${city.country}"
            binding.textRating.text = String.format("%.2f", city.userRating)
            binding.textRatingCount.text = position.toString()

            // Bayrak resmini yükle - Glide önbelleğe alır, bu yüzden tekrar tekrar çağrılması sorun değil
            Glide.with(binding.root.context)
                .load(city.flagUrl)
                .transition(DrawableTransitionOptions.withCrossFade())
                .into(binding.imageFlag)
        }
    }

    companion object {
        private val DIFF_CALLBACK = object : DiffUtil.ItemCallback<VisitedCityItem>() {
            override fun areItemsTheSame(oldItem: VisitedCityItem, newItem: VisitedCityItem): Boolean {
                return oldItem.id == newItem.id
            }

            override fun areContentsTheSame(oldItem: VisitedCityItem, newItem: VisitedCityItem): Boolean {
                return oldItem == newItem
            }
        }
    }
}