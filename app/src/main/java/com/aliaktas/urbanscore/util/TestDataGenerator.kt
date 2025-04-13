package com.aliaktas.urbanscore.util

import android.util.Log
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

/**
 * Test verileri için yardımcı sınıf
 */
object TestDataGenerator {

    // Türkiye şehirleri listesi
    private val turkishCities = listOf(
        "Istanbul", "Ankara", "Izmir", "Bursa", "Antalya",
        "Adana", "Gaziantep", "Konya", "Mersin", "Eskişehir",
        "Kayseri", "Samsun", "Trabzon", "Diyarbakır", "Şanlıurfa",
        "Hatay", "Manisa", "Sakarya", "Aydın", "Denizli",
        "Tekirdağ", "Edirne", "Çanakkale", "Muğla", "Balıkesir",
        "Bolu", "Zonguldak", "Rize", "Artvin", "Kastamonu",
        "Sinop", "Sivas", "Erzurum", "Van", "Hakkari",
        "Mardin", "Batman", "Siirt", "Bitlis", "Muş",
        "Ordu", "Giresun", "Tokat", "Amasya", "Çorum",
        "Malatya", "Elazığ", "Bingöl", "Erzincan", "Ardahan"
    )

    // Bölgeler (Kıtalar)
    private val regions = mapOf(
        "Europe" to listOf("Istanbul"),
        "Asia" to listOf("Ankara", "Izmir", "Bursa", "Antalya", "Adana",
            "Gaziantep", "Konya", "Mersin", "Eskişehir", "Kayseri",
            "Samsun", "Trabzon", "Diyarbakır", "Şanlıurfa", "Hatay",
            "Manisa", "Sakarya", "Aydın", "Denizli", "Tekirdağ",
            "Edirne", "Çanakkale", "Muğla", "Balıkesir", "Bolu",
            "Zonguldak", "Rize", "Artvin", "Kastamonu", "Sinop",
            "Sivas", "Erzurum", "Van", "Hakkari", "Mardin",
            "Batman", "Siirt", "Bitlis", "Muş", "Ordu",
            "Giresun", "Tokat", "Amasya", "Çorum", "Malatya",
            "Elazığ", "Bingöl", "Erzincan", "Ardahan")
    )

    // Şehir nüfusları (2023 TÜİK verileri)
    private val cityPopulations = mapOf(
        "Istanbul" to 15907951,
        "Ankara" to 5661374,
        "Izmir" to 4425123,
        "Bursa" to 3086018,
        "Antalya" to 2511500,
        "Adana" to 2273105,
        "Gaziantep" to 2130449,
        "Konya" to 2290162,
        "Mersin" to 1920136,
        "Eskişehir" to 894447,
        "Kayseri" to 1404766,
        "Samsun" to 1348542,
        "Trabzon" to 807903,
        "Diyarbakır" to 1000268,
        "Şanlıurfa" to 2205407,
        "Hatay" to 1670317,
        "Manisa" to 1428713,
        "Sakarya" to 1029650,
        "Aydın" to 1148085,
        "Denizli" to 1030511,
        "Tekirdağ" to 1107556,
        "Edirne" to 411721,
        "Çanakkale" to 542157,
        "Muğla" to 1017741,
        "Balıkesir" to 1228620,
        "Bolu" to 314268,
        "Zonguldak" to 595952,
        "Rize" to 342056,
        "Artvin" to 167819,
        "Kastamonu" to 371000,
        "Sinop" to 204133,
        "Sivas" to 618615,
        "Erzurum" to 762507,
        "Van" to 1170380,
        "Hakkari" to 271775,
        "Mardin" to 838938,
        "Batman" to 601508,
        "Siirt" to 331051,
        "Bitlis" to 338444,
        "Muş" to 412000,
        "Ordu" to 751745,
        "Giresun" to 427359,
        "Tokat" to 593547,
        "Amasya" to 335808,
        "Çorum" to 525180,
        "Malatya" to 812157,
        "Elazığ" to 595631,
        "Bingöl" to 281222,
        "Erzincan" to 234747,
        "Ardahan" to 99256
    )

    private fun String.toEnglishSlug(): String {
        return this
            .replace('İ', 'i')
            .replace('I', 'i')
            .replace('ı', 'i')
            .replace('ş', 's')
            .replace('Ş', 'S')
            .replace('ğ', 'g')
            .replace('Ğ', 'G')
            .replace('ç', 'c')
            .replace('Ç', 'C')
            .replace('ö', 'o')
            .replace('Ö', 'O')
            .replace('ü', 'u')
            .replace('Ü', 'U')
            .toLowerCase()
            .replace(" ", "-")
    }

    // Bayrak URL'leri
    private val flagUrl = "https://flagcdn.com/w320/tr.png"

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
                if (i >= turkishCities.size) break

                val cityName = turkishCities[i]
                val documentId = cityName.toEnglishSlug()
                val cityDoc = firestore.collection("cities").document(documentId)

                // Rastgele puanlar oluştur (5.0 - 7.0 arasında)
                val generateRandomRating = {
                    5.0 + (Math.random() * 2.0)
                }
                val gastronomy = generateRandomRating()
                val aesthetics = generateRandomRating()
                val safety = generateRandomRating()
                val culture = generateRandomRating()
                val livability = generateRandomRating()
                val social = generateRandomRating()
                val hospitality = generateRandomRating()

                // Genel ortalama puanı hesapla (ağırlıklı ortalama)
                val avgRating = ((gastronomy * 1.0) + (aesthetics * 1.4) + (safety * 1.0) +
                        (culture * 1.0) + (livability * 1.0) + (social * 1.0) +
                        (hospitality * 1.0)) / 7.4

                // Şehrin nüfusunu al, yoksa rastgele üret
                val population = cityPopulations[cityName]?.toLong() ?:
                (100000L + (Math.random() * 14900000L).toLong())

                // Rating count 5 ile 10 arasında
                val ratingCount = 5 + (Math.random() * 6).toInt()

                // Şehrin bölgesini bul
                val region = regions.entries.find { it.value.contains(cityName) }?.key ?: "Other"

                // DOĞRU FIELD KEY'LERİ KULLAN
                val cityData = hashMapOf(
                    "cityName" to cityName,
                    "country" to "Turkiye",
                    "region" to region,
                    "flagUrl" to flagUrl,
                    "population" to population,
                    "averageRating" to (Math.round(avgRating * 100) / 100.0),
                    "ratingCount" to ratingCount,
                    "ratings" to hashMapOf(
                        "gastronomy" to (Math.round(gastronomy * 100) / 100.0),
                        "aesthetics" to (Math.round(aesthetics * 100) / 100.0),
                        "safety" to (Math.round(safety * 100) / 100.0),
                        "culture" to (Math.round(culture * 100) / 100.0),
                        "livability" to (Math.round(livability * 100) / 100.0),
                        "social" to (Math.round(social * 100) / 100.0),
                        "hospitality" to (Math.round(hospitality * 100) / 100.0)
                    ),
                    // Gelecekteki badge'ler için boş map
                    "badges" to hashMapOf<String, Int>()
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
            binding.mainContainer.setOnLongClickListener {
                lifecycleScope.launch {
                    try {
                        // Mevcut kod
                        val firestore = FirebaseFirestore.getInstance()
                        val snapshot = firestore.collection("cities").get().await()

                        if (snapshot.isEmpty) {
                            Toast.makeText(this@MainActivity, "30 şehir ekleniyor...", Toast.LENGTH_SHORT).show()
                            TestDataGenerator.addCities(30)
                            Toast.makeText(this@MainActivity, "30 şehir başarıyla eklendi!", Toast.LENGTH_SHORT).show()
                        } else {
                            // Dialog kısmı
                            AlertDialog.Builder(this@MainActivity)
                                .setTitle("Test Verilerini Yenile")
                                .setMessage("Mevcut ${snapshot.size()} şehir var. Tümünü silip 30 yeni şehir eklemek ister misiniz?")
                                .setPositiveButton("Evet") { _, _ ->
                                    lifecycleScope.launch {
                                        try {
                                            Toast.makeText(this@MainActivity, "Şehirler siliniyor...", Toast.LENGTH_SHORT).show()
                                            TestDataGenerator.clearAllCities()

                                            Toast.makeText(this@MainActivity, "30 yeni şehir ekleniyor...", Toast.LENGTH_SHORT).show()
                                            TestDataGenerator.addCities(30)

                                        } catch (e: Exception) {
                                            Toast.makeText(this@MainActivity, "Hata: ${e.message}", Toast.LENGTH_SHORT).show()
                                            Log.e("MainActivity", "Error refreshing cities", e)
                                        }
                                    }
                                }
                                .setNegativeButton("Hayır", null)
                                .show()
                        }


                    } catch (e: Exception) {
                        Toast.makeText(this@MainActivity, "Hata: ${e.message}", Toast.LENGTH_SHORT).show()
                        Log.e("MainActivity", "Error checking cities", e)
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