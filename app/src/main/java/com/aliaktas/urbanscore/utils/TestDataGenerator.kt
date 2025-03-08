package com.aliaktas.urbanscore.utils

import android.util.Log
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

/**
 * Test verileri için yardımcı sınıf
 */
object TestDataGenerator {


    // TestDataGenerator.kt dosyasına eklenecek kod

    // Şehirler ve ülkeler
    private val cities = listOf(
        "Paris" to "France",
        "London" to "United Kingdom",
        "Rome" to "Italy",
        "Barcelona" to "Spain",
        "Berlin" to "Germany",
        "Amsterdam" to "Netherlands",
        "Prague" to "Czech Republic",
        "Vienna" to "Austria",
        "Lisbon" to "Portugal",
        "Budapest" to "Hungary",
        "Athens" to "Greece",
        "Dublin" to "Ireland",
        "Copenhagen" to "Denmark",
        "Stockholm" to "Sweden",
        "Oslo" to "Norway",
        "Helsinki" to "Finland",
        "Warsaw" to "Poland",
        "Brussels" to "Belgium",
        "Madrid" to "Spain",
        "Munich" to "Germany",
        "Milan" to "Italy",
        "Zurich" to "Switzerland",
        "Geneva" to "Switzerland",
        "Istanbul" to "Turkey",
        "Kiev" to "Ukraine",
        "Toronto" to "Canada",
        "Vancouver" to "Canada",
        "New York" to "USA",
        "Los Angeles" to "USA",
        "Sydney" to "Australia"
    )

    // Bölgeler
    private val regions = mapOf(
        "France" to "Europe",
        "United Kingdom" to "Europe",
        "Italy" to "Europe",
        "Spain" to "Europe",
        "Germany" to "Europe",
        "Netherlands" to "Europe",
        "Czech Republic" to "Europe",
        "Austria" to "Europe",
        "Portugal" to "Europe",
        "Hungary" to "Europe",
        "Greece" to "Europe",
        "Ireland" to "Europe",
        "Denmark" to "Europe",
        "Sweden" to "Europe",
        "Norway" to "Europe",
        "Finland" to "Europe",
        "Poland" to "Europe",
        "Belgium" to "Europe",
        "Switzerland" to "Europe",
        "Turkey" to "Europe",
        "Ukraine" to "Europe",
        "Canada" to "North America",
        "USA" to "North America",
        "Australia" to "Oceania"
    )

    // Bayrak URL'leri
    private val flagUrls = mapOf(
        "France" to "https://flagcdn.com/w320/fr.png",
        "United Kingdom" to "https://flagcdn.com/w320/gb.png",
        "Italy" to "https://flagcdn.com/w320/it.png",
        "Spain" to "https://flagcdn.com/w320/es.png",
        "Germany" to "https://flagcdn.com/w320/de.png",
        "Netherlands" to "https://flagcdn.com/w320/nl.png",
        "Czech Republic" to "https://flagcdn.com/w320/cz.png",
        "Austria" to "https://flagcdn.com/w320/at.png",
        "Portugal" to "https://flagcdn.com/w320/pt.png",
        "Hungary" to "https://flagcdn.com/w320/hu.png",
        "Greece" to "https://flagcdn.com/w320/gr.png",
        "Ireland" to "https://flagcdn.com/w320/ie.png",
        "Denmark" to "https://flagcdn.com/w320/dk.png",
        "Sweden" to "https://flagcdn.com/w320/se.png",
        "Norway" to "https://flagcdn.com/w320/no.png",
        "Finland" to "https://flagcdn.com/w320/fi.png",
        "Poland" to "https://flagcdn.com/w320/pl.png",
        "Belgium" to "https://flagcdn.com/w320/be.png",
        "Switzerland" to "https://flagcdn.com/w320/ch.png",
        "Turkey" to "https://flagcdn.com/w320/tr.png",
        "Ukraine" to "https://flagcdn.com/w320/ua.png",
        "Canada" to "https://flagcdn.com/w320/ca.png",
        "USA" to "https://flagcdn.com/w320/us.png",
        "Australia" to "https://flagcdn.com/w320/au.png"
    )

    /**
     * Şehir verilerini Firebase'e ekler.
     * @param count Eklenecek şehir sayısı
     */
    suspend fun addCities(count: Int) {
        val firestore = FirebaseFirestore.getInstance()

        try {
            // Batch işlemi başlat
            var batch = firestore.batch()
            var operationCount = 0

            for (i in 0 until count) {
                if (i >= cities.size) break

                val (cityName, country) = cities[i]
                val documentId = "$cityName-$country".toLowerCase().replace(" ", "-")
                val cityDoc = firestore.collection("cities").document(documentId)

                // Rastgele puanlar oluştur (7.0 - 9.5 arasında)
                val environment = (7.0 + (Math.random() * 2.5)).toDouble()
                val safety = (7.0 + (Math.random() * 2.5)).toDouble()
                val livability = (7.0 + (Math.random() * 2.5)).toDouble()
                val cost = (7.0 + (Math.random() * 2.5)).toDouble()
                val social = (7.0 + (Math.random() * 2.5)).toDouble()

                // Genel ortalama puanı hesapla (ağırlıklı ortalama)
                val avgRating = ((environment * 1.3) + (safety * 1.1) + (livability * 1.0) +
                        (cost * 1.0) + (social * 1.2)) / 5.6

                // Rastgele nüfus (500,000 - 10,000,000)
                val population = 500000L + (Math.random() * 9500000L).toLong()

                // DOĞRU FIELD KEY'LERİ KULLAN
                val cityData = hashMapOf(
                    "cityName" to cityName,
                    "country" to country,
                    "region" to (regions[country] ?: "Other"),
                    "flagUrl" to (flagUrls[country] ?: "https://flagcdn.com/w320/un.png"),
                    "population" to population,
                    "averageRating" to (Math.round(avgRating * 100) / 100.0),
                    "ratingCount" to (10 + (Math.random() * 90).toInt()),
                    "ratings" to hashMapOf(
                        "environment" to (Math.round(environment * 100) / 100.0),
                        "safety" to (Math.round(safety * 100) / 100.0),
                        "livability" to (Math.round(livability * 100) / 100.0),
                        "cost" to (Math.round(cost * 100) / 100.0),
                        "social" to (Math.round(social * 100) / 100.0)
                    )
                )

                batch.set(cityDoc, cityData)
                operationCount++

                // 500 işlem limiti yaklaştığında batch işlemini çalıştır ve yeni batch başlat
                if (operationCount >= 450) {
                    batch.commit().await()
                    batch = firestore.batch()
                    operationCount = 0
                }
            }

            // Kalan işlemleri çalıştır
            if (operationCount > 0) {
                batch.commit().await()
            }

            Log.d("TestDataGenerator", "Successfully added $count cities")
        } catch (e: Exception) {
            Log.e("TestDataGenerator", "Error adding cities", e)
            throw e
        }
    }


    /**
     * Firebase'deki tüm şehirleri siler
     */
    suspend fun clearAllCities() {
        val firestore = FirebaseFirestore.getInstance()

        try {
            // Tüm şehirleri al
            val snapshot = firestore.collection("cities").get().await()

            if (snapshot.isEmpty) {
                Log.d("TestDataGenerator", "No cities to delete")
                return
            }

            // Batch işlemi başlat (maksimum 500 işlem)
            var batch = firestore.batch()
            var operationCount = 0

            // Her belgeyi silme işlemine ekle
            for (document in snapshot.documents) {
                batch.delete(document.reference)
                operationCount++

                // 500 işlem limiti yaklaştığında batch işlemini çalıştır ve yeni batch başlat
                if (operationCount >= 450) {
                    batch.commit().await()
                    batch = firestore.batch()
                    operationCount = 0
                }
            }

            // Kalan işlemleri çalıştır
            if (operationCount > 0) {
                batch.commit().await()
            }

            Log.d("TestDataGenerator", "Successfully deleted ${snapshot.size()} cities")
        } catch (e: Exception) {
            Log.e("TestDataGenerator", "Error deleting cities", e)
            throw e
        }
    }
}


/*

// HomeFragment.kt - onViewCreated içinde kullanılan manuel şehir ekleme kodu

        if (BuildConfig.DEBUG) {
            binding.txtAppName.setOnLongClickListener {
                lifecycleScope.launch {
                    try {
                        // Önce mevcut şehirleri kontrol et
                        val firestore = FirebaseFirestore.getInstance()
                        val snapshot = firestore.collection("cities").get().await()

                        if (snapshot.isEmpty) {
                            // Şehir yoksa, yeni şehirler ekle
                            Toast.makeText(requireContext(), "30 şehir ekleniyor...", Toast.LENGTH_SHORT).show()
                            TestDataGenerator.addCities(30)
                            Toast.makeText(requireContext(), "30 şehir başarıyla eklendi!", Toast.LENGTH_SHORT).show()
                        } else {
                            // Şehirler varsa, silip yeniden ekle (kullanıcıya sor)
                            val builder = AlertDialog.Builder(requireContext())
                            builder.setTitle("Test Verilerini Yenile")
                                .setMessage("Mevcut ${snapshot.size()} şehir var. Tümünü silip 30 yeni şehir eklemek ister misiniz?")
                                .setPositiveButton("Evet") { _, _ ->
                                    lifecycleScope.launch {
                                        try {
                                            Toast.makeText(requireContext(), "Şehirler siliniyor...", Toast.LENGTH_SHORT).show()
                                            TestDataGenerator.clearAllCities()

                                            Toast.makeText(requireContext(), "30 yeni şehir ekleniyor...", Toast.LENGTH_SHORT).show()
                                            TestDataGenerator.addCities(30)

                                            Toast.makeText(requireContext(), "Şehirler yenilendi!", Toast.LENGTH_SHORT).show()
                                            viewModel.refreshCities(true)
                                        } catch (e: Exception) {
                                            Toast.makeText(requireContext(), "Hata: ${e.message}", Toast.LENGTH_SHORT).show()
                                            Log.e("HomeFragment", "Error refreshing cities", e)
                                        }
                                    }
                                }
                                .setNegativeButton("Hayır", null)
                                .show()
                        }

                        // Her durumda verileri yenile
                        viewModel.refreshCities(true)

                    } catch (e: Exception) {
                        Toast.makeText(requireContext(), "Hata: ${e.message}", Toast.LENGTH_SHORT).show()
                        Log.e("HomeFragment", "Error checking cities", e)
                    }
                }
                true
            }
        }

 */


/*

// tüm şehirleri silme kodu

if (BuildConfig.DEBUG) {
    binding.txtAppName.setOnLongClickListener {
        // App adına uzun basınca şehirleri sil
        lifecycleScope.launch {
            try {
                Toast.makeText(requireContext(), "Şehirler siliniyor...", Toast.LENGTH_SHORT).show()
                TestDataGenerator.clearAllCities()
                Toast.makeText(requireContext(), "Tüm şehirler silindi!", Toast.LENGTH_SHORT).show()
                // Şehirler silindikten sonra, HomeViewModel'i yenile
                viewModel.refreshCities(true)
            } catch (e: Exception) {
                Toast.makeText(requireContext(), "Hata: ${e.message}", Toast.LENGTH_SHORT).show()
                Log.e("HomeFragment", "Error clearing cities", e)
            }
        }
        true
    }
}

 */