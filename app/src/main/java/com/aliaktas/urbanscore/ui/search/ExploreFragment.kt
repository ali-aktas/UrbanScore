package com.aliaktas.urbanscore.ui.search

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.recyclerview.widget.RecyclerView
import androidx.viewpager2.widget.ViewPager2
import com.aliaktas.urbanscore.R
import com.aliaktas.urbanscore.databinding.FragmentExploreBinding
import com.aliaktas.urbanscore.databinding.ItemFeaturedCityBinding
import com.bumptech.glide.Glide

class ExploreFragment : Fragment() {

    private var _binding: FragmentExploreBinding? = null
    private val binding get() = _binding!!
    private lateinit var cityAdapter: FeaturedCityAdapter

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentExploreBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // ViewPager2'nin yüksekliğini belirli bir değer olarak ayarla
        val layoutParams = binding.cityViewPager.layoutParams
        layoutParams.height = resources.getDimensionPixelSize(R.dimen.featured_card_height) +
                resources.getDimensionPixelSize(R.dimen.card_padding)
        binding.cityViewPager.layoutParams = layoutParams

        setupCityCarousel()
        setupClickListeners()
    }

    private fun setupCityCarousel() {
        // Mock veriler
        val cities = listOf(
            FeaturedCity("1", "Istanbul", "https://example.com/olsztyn.jpg"),
            FeaturedCity("2", "Barcelona", "https://example.com/barcelona.jpg"),
            FeaturedCity("3", "Paris", "https://example.com/paris.jpg"),
            FeaturedCity("4", "Tokyo", "https://example.com/tokyo.jpg"),
            FeaturedCity("5", "Olsztyn", "https://example.com/istanbul.jpg")
        )

        cityAdapter = FeaturedCityAdapter(cities)

        binding.cityViewPager.apply {
            adapter = cityAdapter
            offscreenPageLimit = 3
            setPageTransformer(CityCardTransformer())

            // Clip özelliklerini ayarla
            clipToPadding = false
            clipChildren = false

            // NestedScrollingEnabled özelliğini false yap (iç içe kaydırma sorunlarını önler)
            getChildAt(0).apply {
                overScrollMode = View.OVER_SCROLL_NEVER
            }

            val pageMargin = resources.getDimensionPixelOffset(R.dimen.card_padding) / 2
            val pageOffset = resources.getDimensionPixelOffset(R.dimen.card_padding)

            setPageTransformer { page, position ->
                val myOffset = position * -(2 * pageOffset + pageMargin)
                if (position < -1) {
                    page.translationX = -myOffset
                } else if (position <= 1) {
                    val scaleFactor = 0.80f.coerceAtLeast(1 - Math.abs(position * 0.45f))
                    page.translationX = myOffset
                    page.scaleY = scaleFactor
                    page.scaleX = scaleFactor
                    page.alpha = 0.5f + (1 - Math.abs(position)) * 0.5f
                } else {
                    page.translationX = -myOffset
                }
            }
        }
    }

    private fun setupClickListeners() {
        binding.cardSearch.setOnClickListener {
            // Arama işlevini başlat
            Toast.makeText(context, "Search will be implemented", Toast.LENGTH_SHORT).show()
        }

        binding.btnSuggestCity.setOnClickListener {
            // Şehir önerme diyaloğu
            Toast.makeText(context, "City suggestion will be implemented", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}

// Veri modeli sınıfı
data class FeaturedCity(
    val id: String,
    val name: String,
    val imageUrl: String
)

// Adapter
class FeaturedCityAdapter(private val cities: List<FeaturedCity>) :
    RecyclerView.Adapter<FeaturedCityAdapter.CityViewHolder>() {

    class CityViewHolder(val binding: ItemFeaturedCityBinding) :
        RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CityViewHolder {
        val binding = ItemFeaturedCityBinding.inflate(
            LayoutInflater.from(parent.context), parent, false)
        return CityViewHolder(binding)
    }

    override fun onBindViewHolder(holder: CityViewHolder, position: Int) {
        val city = cities[position]
        with(holder.binding) {
            txtCityName.text = city.name

            // Gerçek uygulamada Glide ile resim yükleme
            // Glide.with(root).load(city.imageUrl).into(imgCity)
            // PNG dosyasını yükle
            Glide.with(root.context)
                .load(R.drawable.istanbulphoto) // Attığın PNG dosyasının adı
                .centerCrop()
                .into(imgCity)

        }

        holder.itemView.setOnClickListener {
            // Şehir detay sayfasına git
            // Navigation ile yönlendirme yaparız
        }
    }

    override fun getItemCount() = cities.size
}