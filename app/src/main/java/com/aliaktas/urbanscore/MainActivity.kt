package com.aliaktas.urbanscore

import android.os.Bundle
import android.util.Log.d
import android.util.Log.e
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.navigation.NavController
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.setupWithNavController
import com.aliaktas.urbanscore.data.model.CategoryRatings
import com.aliaktas.urbanscore.data.model.CityModel
import com.aliaktas.urbanscore.databinding.ActivityMainBinding
import com.google.firebase.firestore.FirebaseFirestore
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var navController: NavController

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // ViewBinding ile insets ayarını yap
        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        // Navigation Controller'ı ayarla
        val navHostFragment = supportFragmentManager
            .findFragmentById(R.id.nav_host_fragment) as NavHostFragment
        navController = navHostFragment.navController

        // Bottom Navigation ile Navigation Controller'ı bağla
        binding.bottomNavigation.setupWithNavController(navController)

        // Seçili olmayan menü öğeleri için de tıklama etkinleştirme
        binding.bottomNavigation.setOnItemReselectedListener { /* Bir şey yapma */ }

        // Test amaçlı - geliştirme tamamlandığında kaldırın veya yorum satırına alın
        // addCitiesToFirestore()
    }

    // Back tuşu yönetimi için
    override fun onSupportNavigateUp(): Boolean {
        return navController.navigateUp() || super.onSupportNavigateUp()
    }

    // Manuel şehir ekleme fonksiyonu - test amaçlı kullanın
    private fun addCitiesToFirestore() {
        val cities = listOf(
            CityModel(
                id = "", // ID boş bırakılır, Firestore otomatik atar
                cityName = "Olsztyn",
                country = "Poland",
                flagUrl = "https://flagcdn.com/w320/pl.png",
                population = 240000,
                averageRating = 7.5,
                ratingCount = 10,
                ratings = CategoryRatings(
                    cuisine = 6.0,
                    hospitality = 8.0,
                    landscapeVibe = 6.0,
                    natureClimate = 8.0,
                    lifestyle = 7.0,
                    safetySerenity = 10.0
                )
            )
        )

        val firestore = FirebaseFirestore.getInstance()
        val batch = firestore.batch()

        cities.forEach { city ->
            // Otomatik ID oluştur
            val docRef = firestore.collection("cities").document()
            batch.set(docRef, city)
        }

        batch.commit()
            .addOnSuccessListener {
                d("Firebase", "Batch commit successful")
                Toast.makeText(this, "Cities successfully added", Toast.LENGTH_SHORT).show()
            }
            .addOnFailureListener { e ->
                e("Firebase", "Error adding cities", e)
                Toast.makeText(this, "Error adding a city: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }
}