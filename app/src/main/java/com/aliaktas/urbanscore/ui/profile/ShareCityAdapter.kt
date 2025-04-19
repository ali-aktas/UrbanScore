package com.aliaktas.urbanscore.ui.profile

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.aliaktas.urbanscore.R
import com.aliaktas.urbanscore.data.model.ShareCityItem
import com.bumptech.glide.Glide

class ShareCityAdapter(
    private val cities: List<ShareCityItem>
) : RecyclerView.Adapter<ShareCityAdapter.ViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_share_city, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val city = cities[position]
        holder.bind(city)
    }

    override fun getItemCount(): Int = cities.size

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        private val tvRank: TextView = view.findViewById(R.id.tvRank)
        private val ivCityFlag: ImageView = view.findViewById(R.id.ivCityFlag)
        private val tvCityName: TextView = view.findViewById(R.id.tvCityName)
        private val tvRating: TextView = view.findViewById(R.id.tvRating)

        fun bind(city: ShareCityItem) {
            tvRank.text = city.position.toString()
            tvCityName.text = "${city.name}, ${city.country}"
            tvRating.text = String.format("%.2f", city.rating)

            // Bayrak yükleme
            Glide.with(ivCityFlag.context)
                .load(city.flagUrl)
                .centerCrop()
                .into(ivCityFlag)
        }
    }
}