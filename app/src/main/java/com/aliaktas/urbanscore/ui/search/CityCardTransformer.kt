// Bu sınıfı ui/search/CityCardTransformer.kt dosyasında oluştur
package com.aliaktas.urbanscore.ui.search

import android.view.View
import androidx.viewpager2.widget.ViewPager2
import kotlin.math.abs

class CityCardTransformer : ViewPager2.PageTransformer {
    private val MIN_SCALE = 0.85f
    private val MIN_ALPHA = 0.8f

    override fun transformPage(page: View, position: Float) {
        val pageWidth = page.width
        val pageHeight = page.height

        when {
            position < -1 -> { // [-Infinity,-1)
                // Sol kenardan dışarı çıkan sayfa
                page.alpha = 1f
                page.scaleX = MIN_SCALE
                page.scaleY = MIN_SCALE
            }
            position <= 1 -> { // [-1,1]
                // Normalleştirilmiş pozisyon [-1,0] ekranın solunda, [0,1] merkez ve sağ
                val scaleFactor = MIN_SCALE.coerceAtLeast(1 - abs(position) * (1 - MIN_SCALE))
                val alphaFactor = MIN_ALPHA.coerceAtLeast(1 - abs(position) * (1 - MIN_ALPHA))

                // Ekran merkezinden uzaklığa göre ölçeklendirme
                page.scaleX = scaleFactor
                page.scaleY = scaleFactor
                page.alpha = alphaFactor

                // Sayfayı yüksekliğe göre biraz yukarı kaydır
                val yPosition = position * (pageHeight / 4)
                page.translationY = yPosition
            }
            else -> { // (1,+Infinity]
                // Sağ kenardan dışarı çıkan sayfa
                page.alpha = 1f
                page.scaleX = MIN_SCALE
                page.scaleY = MIN_SCALE
            }
        }
    }
}