package com.aliaktas.urbanscore.util

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Shader
import androidx.core.content.res.ResourcesCompat
import com.aliaktas.urbanscore.R
import com.aliaktas.urbanscore.ui.profile.VisitedCitiesAdapter

/**
 * Şehir paylaşım görsellerini oluşturmak için yardımcı sınıf
 */
class ShareImageGenerator(private val context: Context) {

    /**
     * Ziyaret edilen şehirlerin listesinden bir Instagram paylaşım görseli oluşturur
     *
     * @param visitedCities Ziyaret edilen şehirler listesi
     * @param totalVisitedCount Toplam ziyaret edilen şehir sayısı
     * @return Oluşturulan bitmap
     */
    fun createVisitedCitiesImage(
        visitedCities: List<VisitedCitiesAdapter.VisitedCityItem>,
        totalVisitedCount: Int
    ): Bitmap {
        // Instagram Stories için 1080x1920 boyutunda bir bitmap oluştur (9:16 oranı)
        val width = 1080
        val height = 1920
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)

        // Gradient arkaplan çiz (koyu yeşilden siyaha)
        val startColor = Color.parseColor("#1E4D40") // Koyu yeşil
        val endColor = Color.BLACK
        val gradientPaint = Paint()
        gradientPaint.shader = LinearGradient(
            0f, 0f, 0f, height.toFloat(),
            startColor, endColor,
            Shader.TileMode.CLAMP
        )
        canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), gradientPaint)

        // Uygulama başlığını ekle
        val titlePaint = Paint().apply {
            color = Color.WHITE
            textSize = 70f // Biraz küçülttüm
            typeface = ResourcesCompat.getFont(context, R.font.poppins_bold)
            textAlign = Paint.Align.CENTER
            isAntiAlias = true
        }
        canvas.drawText("UrbanRate App", width / 2f, 200f, titlePaint)

        // Slogan ekle
        val sloganPaint = Paint().apply {
            color = Color.parseColor("#C0FFFFFF") // Hafif şeffaf beyaz
            textSize = 42f // Biraz küçülttüm
            typeface = ResourcesCompat.getFont(context, R.font.poppins_medium)
            textAlign = Paint.Align.CENTER
            isAntiAlias = true
        }

        // Toplam şehir sayısı bilgisini ekle
        val infoText = if (totalVisitedCount > 15) {
            "My top 15 cities out of $totalVisitedCount I've visited"
        } else {
            "My ${visitedCities.size} visited cities"
        }
        canvas.drawText(infoText, width / 2f, 300f, sloganPaint)

        // Şehir listesini ekle
        val maxCities = minOf(15, visitedCities.size)
        val textPaint = Paint().apply {
            color = Color.WHITE
            textSize = 48f // Daha küçük yazı boyutu
            typeface = ResourcesCompat.getFont(context, R.font.poppins_medium)
            isAntiAlias = true
        }
        val ratingPaint = Paint().apply {
            color = Color.parseColor("#FFB500") // Rating puanları için altın/sarı renk
            textSize = 48f // Daha küçük yazı boyutu
            typeface = ResourcesCompat.getFont(context, R.font.poppins_bold)
            isAntiAlias = true
            textAlign = Paint.Align.RIGHT // Sağa hizalama
        }

        // Şehir başına Y pozisyonu
        val startY = 500f
        val lineHeight = 80f // Satır aralığını azalttım

        for (i in 0 until maxCities) {
            val city = visitedCities[i]
            val yPosition = startY + (i * lineHeight)

            // Sıralama numarası
            val rankPaint = Paint().apply {
                color = Color.parseColor("#14DEE0") // Turkuaz-mavi
                textSize = 48f // Daha küçük yazı boyutu
                typeface = ResourcesCompat.getFont(context, R.font.poppins_bold)
                textAlign = Paint.Align.LEFT
                isAntiAlias = true
            }
            canvas.drawText("${i + 1}.", 80f, yPosition, rankPaint)

            // Şehir adı
            val cityText = "${city.name}, ${city.country}"
            // Şehir adını kısaltma işlevi
            val shortenedText = if (cityText.length > 20) {
                cityText.substring(0, 17) + "..."
            } else {
                cityText
            }
            canvas.drawText(shortenedText, 160f, yPosition, textPaint)

            // Değerlendirme puanı - 2 ondalık basamakla
            val ratingText = String.format("%.2f", city.userRating)
            // Puanları sağa hizalama
            canvas.drawText(ratingText, width - 80f, yPosition, ratingPaint)
        }

        // Alt bilgi ekle
        val footerPaint = Paint().apply {
            color = Color.parseColor("#80FFFFFF") // Yarı şeffaf beyaz
            textSize = 36f // Biraz küçülttüm
            typeface = ResourcesCompat.getFont(context, R.font.roboto_extralight)
            textAlign = Paint.Align.CENTER
            isAntiAlias = true
        }
        canvas.drawText("Shared via UrbanRate App", width / 2f, height - 100f, footerPaint)

        return bitmap
    }
}