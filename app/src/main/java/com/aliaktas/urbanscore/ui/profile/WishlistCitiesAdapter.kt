package com.aliaktas.urbanscore.ui.profile

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.aliaktas.urbanscore.databinding.ItemWishlistCityBinding
import com.bumptech.glide.Glide
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions

class WishlistCitiesAdapter(
    private val onItemClick: (String) -> Unit,
    private val onRemoveClick: ((String) -> Unit)? = null
) : ListAdapter<WishlistCityItem, WishlistCitiesAdapter.ViewHolder>(DIFF_CALLBACK) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemWishlistCityBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class ViewHolder(private val binding: ItemWishlistCityBinding) :
        RecyclerView.ViewHolder(binding.root) {

        init {
            binding.root.setOnClickListener {
                val position = bindingAdapterPosition
                if (position != RecyclerView.NO_POSITION) {
                    onItemClick(getItem(position).id)
                }
            }

            binding.btnRemove.setOnClickListener {
                val position = bindingAdapterPosition
                if (position != RecyclerView.NO_POSITION) {
                    onRemoveClick?.invoke(getItem(position).id)
                }
            }
        }

        fun bind(city: WishlistCityItem) {
            binding.textCityName.text = "${city.name}, ${city.country}"

            // Bayrak resmini yükle
            Glide.with(binding.root.context)
                .load(city.flagUrl)
                .transition(DrawableTransitionOptions.withCrossFade())
                .into(binding.imageFlag)
        }
    }

    companion object {
        private val DIFF_CALLBACK = object : DiffUtil.ItemCallback<WishlistCityItem>() {
            override fun areItemsTheSame(oldItem: WishlistCityItem, newItem: WishlistCityItem): Boolean {
                return oldItem.id == newItem.id
            }

            override fun areContentsTheSame(oldItem: WishlistCityItem, newItem: WishlistCityItem): Boolean {
                return oldItem == newItem
            }
        }
    }
}