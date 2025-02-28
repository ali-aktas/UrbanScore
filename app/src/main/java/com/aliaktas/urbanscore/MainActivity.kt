package com.aliaktas.urbanscore

import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.navigation.NavController
import androidx.navigation.NavDestination
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.setupWithNavController
import com.aliaktas.urbanscore.data.model.CategoryRatings
import com.aliaktas.urbanscore.data.model.CityModel
import com.aliaktas.urbanscore.databinding.ActivityMainBinding
import com.aliaktas.urbanscore.ui.auth.AuthViewModel
import com.google.firebase.firestore.FirebaseFirestore
import dagger.hilt.android.AndroidEntryPoint
import androidx.activity.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.NavOptions
import androidx.navigation.ui.navigateUp
import com.aliaktas.urbanscore.ui.auth.AuthState
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.launch

@AndroidEntryPoint
class MainActivity : AppCompatActivity(), NavController.OnDestinationChangedListener {

    private lateinit var binding: ActivityMainBinding
    private lateinit var navController: NavController
    private val authViewModel: AuthViewModel by viewModels()

    // Bottom nav'ın görünür olması gereken fragment ID'leri
    private val bottomNavVisibleDestinations = setOf(
        R.id.homeFragment,
        R.id.exploreFragment,
        R.id.profileFragment
    )

    // Auth fragmentleri
    private val authDestinations = setOf(
        R.id.loginFragment
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

        // Oturum durumunu kontrol et ve yönlendir
        checkAuthStateAndNavigate()

        // Bottom Navigation ile Navigation Controller'ı bağla
        binding.bottomNavigation.setupWithNavController(navController)

        // Seçili olmayan menü öğeleri için de tıklama etkinleştirme
        binding.bottomNavigation.setOnItemReselectedListener { /* Bir şey yapma */ }

        // Bottom Navigation için animasyon ekleme
        setupBottomNavigationAnimation()

        // Destination değişikliklerini dinle
        navController.addOnDestinationChangedListener(this)
    }

    private fun checkAuthStateAndNavigate() {
        if (userIsLoggedIn()) {
            // Oturum açık, doğrudan HomeFragment'a git
            navController.navigate(R.id.homeFragment)
        } else {
            // Oturum kapalı, LoginFragment'a git
            navController.navigate(R.id.loginFragment)
        }
    }

    private fun userIsLoggedIn(): Boolean {
        // Firebase Auth veya başka bir oturum kontrol mekanizması
        return FirebaseAuth.getInstance().currentUser != null
    }

    private fun observeAuthState() {
        lifecycleScope.launch {
            authViewModel.state.collect { state ->
                when (state) {
                    is AuthState.Authenticated -> {
                        // Kullanıcı giriş yapmış, eğer auth fragmentinde ise ana sayfaya yönlendir
                        val currentDestination = navController.currentDestination?.id
                        if (currentDestination in authDestinations) {
                            navController.navigate(R.id.homeFragment)
                        }
                    }
                    is AuthState.Unauthenticated -> {
                        // Kullanıcı çıkış yapmış, eğer auth gerektiren fragmentteyse login'e yönlendir
                        val currentDestination = navController.currentDestination?.id
                        if (currentDestination != null && currentDestination !in authDestinations) {
                            navController.navigate(R.id.loginFragment)
                        }
                    }
                    else -> {
                        // Diğer durumlar (loading, error, initial)
                    }
                }
            }
        }
    }

    private fun setupBottomNavigationAnimation() {
        // Burada bottom navigation için güzel animasyonlar ekleyebilirsiniz
        binding.bottomNavigation.setOnItemSelectedListener { item ->
            // Her bir item için özel animasyon veya görsel efekt ekleyebilirsiniz
            when (item.itemId) {
                R.id.homeFragment -> {
                    // Home item seçildiğinde yapılacak özel işlemler
                }
                R.id.exploreFragment -> {
                    // Explore item seçildiğinde yapılacak özel işlemler
                }
                R.id.profileFragment -> {
                    // Profile item seçildiğinde yapılacak özel işlemler
                }
            }
            // Navigation component'in default davranışını koruyun
            return@setOnItemSelectedListener true
        }
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