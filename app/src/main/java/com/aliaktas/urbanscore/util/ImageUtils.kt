package com.aliaktas.urbanscore.util

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.os.Environment
import androidx.core.content.FileProvider
import com.aliaktas.urbanscore.data.model.ShareCityItem
import com.aliaktas.urbanscore.ui.profile.VisitedCityItem
import java.io.File
import java.io.FileOutputStream
import javax.inject.Inject
import javax.inject.Singleton

// app/src/main/java/com/aliaktas/urbanscore/util/ImageUtils.kt içinde değişiklik

@Singleton
class ImageUtils @Inject constructor(
    private val context: Context
) {
    // Eski ShareImageGenerator'ı referans olarak tutalım
    private val shareImageGenerator = ShareImageGenerator(context)

    // Yeni layout tabanlı generator'ı ekleyelim
    private val shareLayoutGenerator = ShareLayoutGenerator(context)

    /**
     * Ziyaret edilen şehirler için paylaşım görseli oluşturur
     *
     * @param cities Şehir listesi
     * @param totalCount Toplam şehir sayısı
     * @return Oluşturulan bitmap
     */
    suspend fun createVisitedCitiesImage(
        cities: List<VisitedCityItem>,
        totalCount: Int
    ): Bitmap {
        // VisitedCityItem'dan ShareCityItem'a dönüştür
        val shareCities = cities.mapIndexed { index, city ->
            ShareCityItem(
                id = city.id,
                name = city.name,
                country = city.country,
                flagUrl = city.flagUrl,
                rating = city.userRating,
                position = index + 1
            )
        }

        // Yeni layout generator'ı kullan
        return shareLayoutGenerator.createCitiesShareImage(shareCities, totalCount)
    }

    /**
     * Bitmap'i paylaşmak için intent oluşturur
     * @param bitmap Paylaşılacak görsel
     * @return Oluşturulan intent
     */
    fun createShareImageIntent(bitmap: Bitmap): Intent {
        // Geçici dosya oluştur
        val storageDir = context.getExternalFilesDir(Environment.DIRECTORY_PICTURES)
        val imageFile = File.createTempFile(
            "urbanrate_cities_",
            ".png",
            storageDir
        )

        // Bitmap'i dosyaya kaydet
        FileOutputStream(imageFile).use { out ->
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
        }

        // FileProvider kullanarak güvenli URI oluştur
        val imageUri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            imageFile
        )

        // Instagram'a özel intent oluştur
        val storiesIntent = Intent("com.instagram.share.ADD_TO_STORY").apply {
            setDataAndType(imageUri, "image/png")
            flags = Intent.FLAG_GRANT_READ_URI_PERMISSION
        }

        // Instagram yüklü mü kontrol et
        if (storiesIntent.resolveActivity(context.packageManager) != null) {
            return storiesIntent
        }

        // Instagram yoksa genel paylaşım menüsünü göster
        return Intent(Intent.ACTION_SEND).apply {
            type = "image/png"
            putExtra(Intent.EXTRA_STREAM, imageUri)
            flags = Intent.FLAG_GRANT_READ_URI_PERMISSION
        }
    }
}