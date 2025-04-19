package com.aliaktas.urbanscore.util

import android.content.Context
import android.graphics.Bitmap
import android.view.LayoutInflater
import android.view.View
import android.widget.TextView
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.aliaktas.urbanscore.R
import com.aliaktas.urbanscore.data.model.ShareCityItem
import com.aliaktas.urbanscore.ui.profile.ShareCityAdapter
import com.bumptech.glide.Glide
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext

/**
 * Layout tabanlı Instagram story paylaşım görseli oluşturma sınıfı
 */
class ShareLayoutGenerator(private val context: Context) {

    /**
     * Ziyaret edilen şehirler için paylaşım görselini layout kullanarak oluşturur
     *
     * @param cities Paylaşılacak şehir listesi
     * @param totalVisitedCount Toplam ziyaret edilen şehir sayısı
     * @return Oluşturulan bitmap
     */
    suspend fun createCitiesShareImage(
        cities: List<ShareCityItem>,
        totalVisitedCount: Int
    ): Bitmap {
        // Ana thread'de view oluşturma ve doldurma
        val preparedView = withContext(Dispatchers.Main) {
            // Layout inflate et
            val view = LayoutInflater.from(context).inflate(R.layout.layout_share_cities, null)

            // Şehir listesini doldur
            setupCityList(view, cities)

            // Toplam şehir sayısını ayarla
            setupVisitCount(view, totalVisitedCount)

            // View'ı ölçülendir
            val widthSpec = View.MeasureSpec.makeMeasureSpec(1080, View.MeasureSpec.EXACTLY)
            val heightSpec = View.MeasureSpec.makeMeasureSpec(1920, View.MeasureSpec.EXACTLY)
            view.measure(widthSpec, heightSpec)
            view.layout(0, 0, 1080, 1920)

            // Bayrakların yüklenmesi için kısa bir süre bekle
            delay(800)  // Biraz daha uzun bekle - 800ms

            view
        }

        // Bitmap oluşturma işlemini IO thread'de yapabilirsiniz
        return withContext(Dispatchers.Default) {
            val bitmap = Bitmap.createBitmap(1080, 1920, Bitmap.Config.ARGB_8888)
            val canvas = android.graphics.Canvas(bitmap)

            // Canvas'a çizim işlemini Main thread'de yap
            withContext(Dispatchers.Main) {
                preparedView.draw(canvas)
            }

            bitmap
        }
    }

    private fun setupCityList(view: View, cities: List<ShareCityItem>) {
        val recyclerView = view.findViewById<RecyclerView>(R.id.rvShareCities)
        recyclerView.layoutManager = LinearLayoutManager(context)

        // Maksimum 10 şehir göster
        val displayCities = cities.take(10)
        val adapter = ShareCityAdapter(displayCities)
        recyclerView.adapter = adapter
    }

    private fun setupVisitCount(view: View, totalCount: Int) {
        val tvVisitCount = view.findViewById<TextView>(R.id.tvVisitCount)
        tvVisitCount.text = context.getString(
            R.string.share_visit_count,
            totalCount
        )
    }
}