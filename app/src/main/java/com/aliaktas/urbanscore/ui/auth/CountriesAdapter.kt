package com.aliaktas.urbanscore.ui.auth

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.aliaktas.urbanscore.data.model.CountryModel
import com.aliaktas.urbanscore.databinding.ItemCountrySelectionBinding
import com.bumptech.glide.Glide

class CountriesAdapter(
    private val countries: List<CountryModel>
) : RecyclerView.Adapter<CountriesAdapter.CountryViewHolder>() {

    private var selectedPosition = -1

    fun getSelectedCountry(): CountryModel? {
        return if (selectedPosition >= 0) countries[selectedPosition] else null
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CountryViewHolder {
        val binding = ItemCountrySelectionBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return CountryViewHolder(binding)
    }

    override fun onBindViewHolder(holder: CountryViewHolder, position: Int) {
        holder.bind(countries[position], position == selectedPosition)
    }

    override fun getItemCount() = countries.size

    inner class CountryViewHolder(
        private val binding: ItemCountrySelectionBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        init {
            binding.root.setOnClickListener {
                val position = adapterPosition
                if (position != RecyclerView.NO_POSITION) {
                    val oldSelected = selectedPosition
                    selectedPosition = position

                    // Görünümü güncelle
                    if (oldSelected >= 0) notifyItemChanged(oldSelected)
                    notifyItemChanged(selectedPosition)
                }
            }
        }

        fun bind(country: CountryModel, isSelected: Boolean) {
            binding.textCountryName.text = country.name

            // Bayrak yükleme
            if (country.flagUrl.isNotEmpty()) {
                Glide.with(binding.root.context)
                    .load(country.flagUrl)
                    .into(binding.imageCountryFlag)
            }

            // Seçim durumunu güncelle
            binding.cardCountry.isChecked = isSelected
        }
    }
}