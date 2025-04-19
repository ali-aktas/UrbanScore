package com.aliaktas.urbanscore.ui.auth

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.aliaktas.urbanscore.R
import com.aliaktas.urbanscore.data.model.CountryModel
import com.aliaktas.urbanscore.databinding.ItemCountrySelectionBinding
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.bumptech.glide.request.RequestOptions
import me.zhanghai.android.fastscroll.PopupTextProvider
import java.util.Locale

class CountriesAdapter :
    RecyclerView.Adapter<CountriesAdapter.CountryViewHolder>(),
    PopupTextProvider {

    private var allCountries: List<CountryModel> = listOf()
    private var filteredCountries: List<CountryModel> = listOf()
    private var selectedPosition = -1

    // Harf => Pozisyon indeks haritası (hızlı kaydırma için)
    private val letterToPositionMap = mutableMapOf<String, Int>()

    // Ülke seçildiğinde çağrılacak lambda
    var onItemClick: ((CountryModel) -> Unit)? = null

    // Seçilen ülkeyi döndürür
    fun getSelectedCountry(): CountryModel? {
        return if (selectedPosition >= 0 && selectedPosition < filteredCountries.size) {
            filteredCountries[selectedPosition]
        } else {
            null
        }
    }

    // Tüm ülkeleri ayarla ve harf indekslerini güncelle
    fun setCountries(countries: List<CountryModel>) {
        this.allCountries = countries.sortedBy { it.name }
        this.filteredCountries = allCountries
        updateLetterPositionMap()
        notifyDataSetChanged()
    }

    // Sadece arama fonksiyonunu düzeltiyoruz - diğer her şey aynı kalıyor
    fun filter(query: String) {
        // Eski seçimi hatırla
        val oldSelectedCountry = getSelectedCountry()
        selectedPosition = -1

        // Arama terimi boşsa tüm ülkeleri göster
        if (query.isEmpty()) {
            filteredCountries = allCountries
        } else {
            // Normalize edilmiş arama terimi
            val normalizedQuery = normalizeForSearch(query.trim())

            // Basit ve etkili arama algoritması
            filteredCountries = allCountries.filter { country ->
                normalizeForSearch(country.name).contains(normalizedQuery)
            }
        }

        // Eğer seçilen ülke filtrede hala varsa, seçili olarak işaretle
        if (oldSelectedCountry != null) {
            val newPos = filteredCountries.indexOfFirst { it.id == oldSelectedCountry.id }
            if (newPos >= 0) {
                selectedPosition = newPos
            }
        }

        // Harf indekslerini güncelle
        updateLetterPositionMap()
        notifyDataSetChanged()
    }

    // Türkçe karakterleri normalize etmek için yardımcı fonksiyon
    private fun normalizeForSearch(text: String): String {
        return text.lowercase(Locale.getDefault())
            .replace("i̇", "i") // büyük İ'nin lowercase hali
            .replace("İ", "i")
            .replace("ı", "i")
            .replace("ğ", "g")
            .replace("ü", "u")
            .replace("ş", "s")
            .replace("ö", "o")
            .replace("ç", "c")
    }

    // Harf => pozisyon haritasını güncelle
    private fun updateLetterPositionMap() {
        letterToPositionMap.clear()
        filteredCountries.forEachIndexed { index, country ->
            val firstLetter = country.name.substring(0, 1).uppercase(Locale.getDefault())
            if (!letterToPositionMap.containsKey(firstLetter)) {
                letterToPositionMap[firstLetter] = index
            }
        }
    }

    // FastScroller için popup metni sağlayan metot (ilk harf)
    override fun getPopupText(view: View, position: Int): CharSequence {
        if (position < 0 || position >= filteredCountries.size) return ""
        val country = filteredCountries[position]
        return country.name.substring(0, 1).uppercase(Locale.getDefault())
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

    inner class CountryViewHolder(
        private val binding: ItemCountrySelectionBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        init {
            binding.root.setOnClickListener {
                val position = adapterPosition
                if (position != RecyclerView.NO_POSITION) {
                    val oldPosition = selectedPosition
                    selectedPosition = position

                    // Eski ve yeni seçili öğeleri güncelle
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
            // Ülke adı ayarla
            binding.textCountryName.text = country.name

            // Bayrak resmini yükle - optimize edilmiş şekilde
            if (country.flagUrl.isNotEmpty()) {
                Glide.with(binding.root.context)
                    .load(country.flagUrl)
                    .apply(RequestOptions()
                        .diskCacheStrategy(DiskCacheStrategy.ALL)
                        .placeholder(R.drawable.ic_flag)
                        .error(R.drawable.ic_flag)
                    )
                    .into(binding.imageCountryFlag)
            } else {
                binding.imageCountryFlag.setImageResource(R.drawable.ic_flag)
            }

            // Seçim durumunu güncelle
            binding.cardCountry.isChecked = isSelected
        }
    }
}