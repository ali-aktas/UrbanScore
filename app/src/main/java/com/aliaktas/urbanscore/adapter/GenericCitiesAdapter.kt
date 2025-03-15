package com.aliaktas.urbanscore.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import com.aliaktas.urbanscore.base.BaseAdapter
import com.aliaktas.urbanscore.databinding.ItemCityBinding
import com.aliaktas.urbanscore.util.ImageLoader
import javax.inject.Inject

/**
 * Generic adapter for displaying cities with a rank, name, country and rating
 */
class GenericCitiesAdapter @Inject constructor(
    private val imageLoader: ImageLoader,
    private val onItemClick: (String) -> Unit
) : BaseAdapter<GenericCitiesAdapter.CityItem, ItemCityBinding>(DIFF_CALLBACK) {

    override fun createBinding(inflater: LayoutInflater, parent: ViewGroup, attachToParent: Boolean): ItemCityBinding {
        return ItemCityBinding.inflate(inflater, parent, false)
    }

    override fun bind(binding: ItemCityBinding, item: CityItem, position: Int) {
        with(binding) {
            textCityName.text = "${item.name}, ${item.country}"
            textRating.text = String.format("%.2f", item.rating)
            textRatingCount.text = (position + 1).toString()

            // Load flag using our ImageLoader utility
            imageLoader.loadFlag(imageFlag, item.flagUrl)

            // Set click listener
            root.setOnClickListener {
                onItemClick(item.id)
            }
        }
    }

    data class CityItem(
        val id: String,
        val name: String,
        val country: String,
        val flagUrl: String,
        val rating: Double
    )

    companion object {
        private val DIFF_CALLBACK = object : DiffUtil.ItemCallback<CityItem>() {
            override fun areItemsTheSame(oldItem: CityItem, newItem: CityItem): Boolean {
                return oldItem.id == newItem.id
            }

            override fun areContentsTheSame(oldItem: CityItem, newItem: CityItem): Boolean {
                return oldItem == newItem
            }
        }
    }
}