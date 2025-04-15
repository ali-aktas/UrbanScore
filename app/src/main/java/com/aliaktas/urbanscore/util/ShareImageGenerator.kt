package com.aliaktas.urbanscore.util

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.RectF
import android.graphics.Typeface
import android.graphics.Paint.Align
import android.graphics.BitmapFactory
import androidx.core.content.ContextCompat
import androidx.core.content.res.ResourcesCompat
import androidx.core.graphics.toRect
import com.aliaktas.urbanscore.R
import com.aliaktas.urbanscore.ui.profile.VisitedCityItem

/**
 * Paylaşım görselleri oluşturmaktan sorumlu sınıf
 */
class ShareImageGenerator(private val context: Context) {

    /**
     * Ziyaret edilen şehirler için Instagram Story formatında paylaşım görseli oluşturur
     *
     * @param visitedCities Şehir listesi
     * @param totalCount Toplam şehir sayısı
     * @return Oluşturulan bitmap
     */
    fun createVisitedCitiesImage(
        visitedCities: List<VisitedCityItem>,
        totalCount: Int
    ): Bitmap {
        // Instagram Stories için 1080x1920 boyutunda bir bitmap oluştur
        val width = 1080
        val height = 1920
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)

        // 1. Arkaplan resmini çiz
        val background = BitmapFactory.decodeResource(context.resources, R.drawable.share_bg)
        val backgroundRect = Rect(0, 0, width, height)
        canvas.drawBitmap(background, null, backgroundRect, null)

        // 2. Liste arkaplanını çiz - Ekranın ortasına yerleştir
        val listBgPadding = 40 // Kenarlardan boşluk
        val listBgTop = height * 0.25f.toInt() // Üstten %25 aşağıda başlasın
        val listBgBottom = height * 0.78f.toInt() // Alttan %22 boşluk bırak (alt kısımdaki logo için)

        val listBackground = BitmapFactory.decodeResource(context.resources, R.drawable.mainlistbg)
        val listBgRect = Rect(
            listBgPadding,
            listBgTop,
            width - listBgPadding,
            listBgBottom
        )
        canvas.drawBitmap(listBackground, null, listBgRect, null)

        // 3. Slogan Başlığını ekle (Liste arkaplanının üstüne)
        val sloganPaint = Paint().apply {
            color = ContextCompat.getColor(context, R.color.primary_purple)
            textSize = 75f
            typeface = ResourcesCompat.getFont(context, R.font.montserrat_alternates_bold)
            textAlign = Paint.Align.CENTER
            isAntiAlias = true
        }

        // Sloganı ekle - liste arkaplanının üstünde
        val sloganY = listBgTop - 120f // Daha yukarıda olsun
        canvas.drawText("My best 10 cities I've traveled", width / 2f, sloganY, sloganPaint)

        // 4. Alt başlığı ekle (isteğe bağlı)
        val subtitlePaint = Paint().apply {
            color = Color.WHITE
            textSize = 46f
            typeface = ResourcesCompat.getFont(context, R.font.montserrat_alternates_bold)
            textAlign = Paint.Align.CENTER
            isAntiAlias = true
        }

        // Alt başlık
        val subtitleY = listBgTop - 40f
        canvas.drawText("City rankings based on my personal experience", width / 2f, subtitleY, subtitlePaint)

        // 5. Şehir listesini ekle
        val maxCities = minOf(10, visitedCities.size) // Maksimum 10 şehir
        val listStartY = listBgTop + 180 // Liste başlangıç pozisyonu - öncekinden daha aşağıda
        val lineHeight = (listBgBottom - listStartY - 80) / 10f // Satır aralığı

        val rankPaint = Paint().apply {
            color = ContextCompat.getColor(context, R.color.primary_purple)
            textSize = 48f
            typeface = ResourcesCompat.getFont(context, R.font.montserrat_alternates_bold)
            textAlign = Paint.Align.CENTER
            isFakeBoldText = true
            isAntiAlias = true
        }

        val cityPaint = Paint().apply {
            color = Color.WHITE
            textSize = 42f
            typeface = ResourcesCompat.getFont(context, R.font.montserrat_alternates_bold)
            isAntiAlias = true
        }

        val ratingPaint = Paint().apply {
            color = ContextCompat.getColor(context, R.color.primary_purple)
            textSize = 48f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            textAlign = Paint.Align.CENTER
            isAntiAlias = true
        }

        // Şehir listesini çiz
        for (i in 0 until maxCities) {
            val city = visitedCities[i]
            val yPosition = listStartY + (i * lineHeight)

            // Sıra numarası için daire çiz
            val rankCenterX = listBgPadding + 60
            val rankCenterY = yPosition
            val rankRadius = 35f

            val circlePaint = Paint().apply {
                color = Color.WHITE
                style = Paint.Style.FILL
                isAntiAlias = true
            }

            canvas.drawCircle(rankCenterX.toFloat(), rankCenterY, rankRadius, circlePaint)

            // Sıra numarası
            canvas.drawText("${i + 1}", rankCenterX.toFloat(), rankCenterY + 18f, rankPaint)

            // Şehir adı - sınırlı uzunlukta
            val cityText = "${city.name}, ${city.country}"
            val shortenedText = if (cityText.length > 22) {
                cityText.substring(0, 19) + "..."
            } else {
                cityText
            }
            canvas.drawText(shortenedText, rankCenterX + 80f, yPosition + 15f, cityPaint)

            // Şehir puanı (sağ tarafta)
            val rating = String.format("%.1f", city.userRating)
            canvas.drawText(rating, width - listBgPadding - 80f, yPosition + 15f, ratingPaint)
        }

        // 6. Alt logo ve bilgi kısmını ekle - orijinal boyut oranlarını koruyarak
        val bottomImage = BitmapFactory.decodeResource(context.resources, R.drawable.share_bottomitems)

        // Orijinal boyut oranlarını al
        val originalWidth = bottomImage.width
        val originalHeight = bottomImage.height

        // Sabit genişlik ver ve yüksekliği orijinal oranlarla hesapla
        val targetWidth = width * 0.8f // Ekranın %80'i kadar genişlik
        val targetHeight = targetWidth * originalHeight / originalWidth

        // Görseli ortala ve alt kısma yerleştir
        val bottomRect = RectF(
            (width - targetWidth) / 2, // Ortala
            height - targetHeight - 80,  // Alt kısımda - biraz boşluk bırak
            (width + targetWidth) / 2, // Genişlik
            (height - 80).toFloat()   // Alt boşlukla
        )

        canvas.drawBitmap(bottomImage, null, bottomRect.toRect(), null)

        return bitmap
    }
}