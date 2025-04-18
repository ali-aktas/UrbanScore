package com.aliaktas.urbanscore.ui.auth

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.aliaktas.urbanscore.data.model.CountryModel
import com.aliaktas.urbanscore.databinding.ItemCountrySelectionBinding
import com.bumptech.glide.Glide
import com.bumptech.glide.request.RequestOptions
import me.zhanghai.android.fastscroll.PopupTextProvider
import java.util.Locale

class CountriesAdapter :
    RecyclerView.Adapter<CountriesAdapter.CountryViewHolder>(),
    PopupTextProvider {  // Yeni FastScroller için implementasyon

    private var allCountries: List<CountryModel> = listOf()
    private var filteredCountries: List<CountryModel> = listOf()
    private var selectedPosition = -1

    // Ülke seçildiğinde çağrılacak metot
    var onItemClick: ((CountryModel) -> Unit)? = null

    // Seçilen ülkeyi döndürür
    fun getSelectedCountry(): CountryModel? {
        return if (selectedPosition >= 0 && selectedPosition < filteredCountries.size) {
            filteredCountries[selectedPosition]
        } else {
            null
        }
    }

    // Tüm ülkeleri set et
    fun setCountries(countries: List<CountryModel>) {
        this.allCountries = countries.sortedBy { it.name } // Alfabetik sırala
        this.filteredCountries = allCountries
        notifyDataSetChanged()
    }

    // Arama metodu
    fun filter(query: String) {
        // Eski seçimi temizle
        val oldSelectedCountry = getSelectedCountry()
        selectedPosition = -1

        // Filtreleme
        filteredCountries = if (query.isEmpty()) {
            allCountries
        } else {
            allCountries.filter {
                it.name.lowercase(Locale.getDefault()).contains(query.lowercase(Locale.getDefault()))
            }
        }

        // Eğer seçili bir ülke varsa ve filtrede hala mevcutsa, seçili kalsın
        if (oldSelectedCountry != null) {
            val newPos = filteredCountries.indexOfFirst { it.id == oldSelectedCountry.id }
            if (newPos >= 0) {
                selectedPosition = newPos
            }
        }

        notifyDataSetChanged()
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
        holder.bind(filteredCountries[position], position == selectedPosition)
    }

    override fun getItemCount() = filteredCountries.size

    // Yeni FastScroller için popup metin sağlayıcı (ilk harf)
    override fun getPopupText(view: View, position: Int): CharSequence {
        return filteredCountries[position].name.substring(0, 1).uppercase(Locale.getDefault())
    }

    inner class CountryViewHolder(
        private val binding: ItemCountrySelectionBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        init {
            binding.root.setOnClickListener {
                val position = adapterPosition
                if (position != RecyclerView.NO_POSITION) {
                    val oldPosition = selectedPosition
                    selectedPosition = position

                    // Görünümü güncelle
                    if (oldPosition >= 0 && oldPosition < filteredCountries.size) {
                        notifyItemChanged(oldPosition)
                    }
                    notifyItemChanged(selectedPosition)

                    // Callback'i çağır
                    onItemClick?.invoke(filteredCountries[position])
                }
            }
        }

        fun bind(country: CountryModel, isSelected: Boolean) {
            // Ülke adı
            binding.textCountryName.text = country.name

            // Bayrak - Glide ile optimize edilmiş yükleme
            if (country.flagUrl.isNotEmpty()) {
                Glide.with(binding.root.context)
                    .load(country.flagUrl)
                    .apply(RequestOptions()
                        .placeholder(com.aliaktas.urbanscore.R.drawable.ic_flag)
                        .error(com.aliaktas.urbanscore.R.drawable.ic_flag)
                    )
                    .into(binding.imageCountryFlag)
            } else {
                binding.imageCountryFlag.setImageResource(com.aliaktas.urbanscore.R.drawable.ic_flag)
            }

            // Seçim durumunu güncelle
            binding.cardCountry.isChecked = isSelected
        }
    }
}