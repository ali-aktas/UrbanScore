package com.aliaktas.urbanscore.util

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Shader
import android.graphics.Typeface
import android.os.Environment
import androidx.core.content.FileProvider
import androidx.core.content.res.ResourcesCompat
import com.aliaktas.urbanscore.R
import com.aliaktas.urbanscore.ui.profile.VisitedCitiesAdapter
import java.io.File
import java.io.FileOutputStream
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ImageUtils @Inject constructor(
    private val context: Context
) {
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
            textSize = 70f
            typeface = ResourcesCompat.getFont(context, R.font.poppins_bold)
            textAlign = Paint.Align.CENTER
            isAntiAlias = true
        }
        canvas.drawText("UrbanRate App", width / 2f, 200f, titlePaint)

        // Slogan ekle
        val sloganPaint = Paint().apply {
            color = Color.parseColor("#C0FFFFFF")
            textSize = 42f
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
            textSize = 48f
            typeface = ResourcesCompat.getFont(context, R.font.poppins_medium)
            isAntiAlias = true
        }
        val ratingPaint = Paint().apply {
            color = Color.parseColor("#FFB500")
            textSize = 48f
            typeface = ResourcesCompat.getFont(context, R.font.poppins_bold)
            isAntiAlias = true
            textAlign = Paint.Align.RIGHT
        }

        // Şehir başına Y pozisyonu
        val startY = 500f
        val lineHeight = 80f

        for (i in 0 until maxCities) {
            val city = visitedCities[i]
            val yPosition = startY + (i * lineHeight)

            // Sıralama numarası
            val rankPaint = Paint().apply {
                color = Color.parseColor("#14DEE0")
                textSize = 48f
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
            color = Color.parseColor("#80FFFFFF")
            textSize = 36f
            typeface = ResourcesCompat.getFont(context, R.font.roboto_extralight)
            textAlign = Paint.Align.CENTER
            isAntiAlias = true
        }
        canvas.drawText("Shared via UrbanRate App", width / 2f, height - 100f, footerPaint)

        return bitmap
    }

    /**
     * Bitmap'i paylaşmak için intent oluşturur
     */
    fun createShareImageIntent(bitmap: Bitmap): Intent {
        val storageDir = context.getExternalFilesDir(Environment.DIRECTORY_PICTURES)
        val imageFile = File.createTempFile(
            "urbanrate_cities_",
            ".png",
            storageDir
        )

        FileOutputStream(imageFile).use { out ->
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
        }

        // FileProvider kullanarak URI oluştur
        val imageUri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            imageFile
        )

        // Instagram Stories'e paylaşım intent'i oluştur
        val storiesIntent = Intent("com.instagram.share.ADD_TO_STORY").apply {
            setDataAndType(imageUri, "image/png")
            flags = Intent.FLAG_GRANT_READ_URI_PERMISSION
        }

        // Instagram uygulaması yüklü mü kontrol et
        if (storiesIntent.resolveActivity(context.packageManager) != null) {
            return storiesIntent
        }

        // Instagram yüklü değilse genel paylaşım menüsünü göster
        return Intent(Intent.ACTION_SEND).apply {
            type = "image/png"
            putExtra(Intent.EXTRA_STREAM, imageUri)
            flags = Intent.FLAG_GRANT_READ_URI_PERMISSION
        }
    }
}