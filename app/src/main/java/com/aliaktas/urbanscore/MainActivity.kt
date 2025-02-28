package com.aliaktas.urbanscore

import android.os.Bundle
import android.view.View
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.navigation.NavController
import androidx.navigation.NavDestination
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.setupWithNavController
import com.aliaktas.urbanscore.databinding.ActivityMainBinding
import dagger.hilt.android.AndroidEntryPoint
import androidx.activity.viewModels
import com.aliaktas.urbanscore.ui.auth.AuthViewModel

@AndroidEntryPoint
class MainActivity : AppCompatActivity(), NavController.OnDestinationChangedListener {

    private lateinit var binding: ActivityMainBinding
    private lateinit var navController: NavController
    private val authViewModel: AuthViewModel by viewModels()

    // Bottom nav'ın görünür olması gereken fragment ID'leri
    private val bottomNavVisibleDestinations = setOf(
        R.id.homeFragment,
        R.id.exploreFragment,
        R.id.profileFragment,
        R.id.allCitiesFragment
    )

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

        // Intent'ten giriş durumunu al
        val isLoggedIn = intent.getBooleanExtra("isLoggedIn", false)

        // Kullanıcı durumuna göre başlangıç yönlendirmesi
        if (isLoggedIn) {
            navController.navigate(R.id.homeFragment)
        } else {
            navController.navigate(R.id.loginFragment)
        }

        // Bottom Navigation ile Navigation Controller'ı bağla
        binding.bottomNavigation.setupWithNavController(navController)

        // Seçili olmayan menü öğeleri için de tıklama etkinleştirme
        binding.bottomNavigation.setOnItemReselectedListener { /* Bir şey yapma */ }

        // Destination değişikliklerini dinle
        navController.addOnDestinationChangedListener(this)
    }

    // Back tuşu yönetimi için
    override fun onSupportNavigateUp(): Boolean {
        return navController.navigateUp() || super.onSupportNavigateUp()
    }

    // NavController.OnDestinationChangedListener implementation
    override fun onDestinationChanged(
        controller: NavController,
        destination: NavDestination,
        arguments: Bundle?
    ) {
        // Bottom navigation'ı gerektiğinde gizle/göster
        if (destination.id in bottomNavVisibleDestinations) {
            binding.bottomNavigation.visibility = View.VISIBLE
        } else {
            binding.bottomNavigation.visibility = View.GONE
        }
    }
}