package com.aliaktas.urbanscore

import android.os.Bundle
import android.util.Log
import android.view.View
import androidx.activity.OnBackPressedCallback
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.fragment.app.Fragment
import com.aliaktas.urbanscore.databinding.ActivityMainBinding
import com.aliaktas.urbanscore.navigation.BackStackManager
import com.aliaktas.urbanscore.navigation.BottomNavigationManager
import com.aliaktas.urbanscore.navigation.NavigationManager
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : AppCompatActivity() {
    private val TAG = "MainActivity"

    private lateinit var binding: ActivityMainBinding

    // Yönetici sınıflar
    private lateinit var backStackManager: BackStackManager
    private lateinit var navigationManager: NavigationManager
    private lateinit var bottomNavigationManager: BottomNavigationManager

    // Geri tuşu callback'i
    private lateinit var backPressedCallback: OnBackPressedCallback

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Set window insets
        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        // Yönetici sınıfları başlat
        setupManagers()

        // Geri tuşu yönetimini kur
        setupBackButtonHandling()

        // Giriş durumuna göre başlangıç fragment'ini göster
        val isLoggedIn = intent.getBooleanExtra("isLoggedIn", false)

        if (savedInstanceState == null) {
            if (isLoggedIn) {
                // Giriş yapmışsa ana sayfayı göster
                bottomNavigationManager.showBottomNavFragment(R.id.homeFragment)
            } else {
                // Giriş yapmamışsa login ekranını göster
                showLoginFragment()
            }
        } else {
            // Ekran döndürme gibi durumlarda state'i geri yükle
            val activeTabId = savedInstanceState.getInt("activeTabId", R.id.homeFragment)
            navigationManager.setActiveTabId(activeTabId)

            if (isLoggedIn) {
                bottomNavigationManager.showBottomNavFragment(activeTabId)
            } else {
                showLoginFragment()
            }
        }
    }

    private fun setupManagers() {
        // BackStackManager'ı başlat
        backStackManager = BackStackManager(supportFragmentManager)

        // NavigationManager'ı başlat
        navigationManager = NavigationManager(
            supportFragmentManager,
            R.id.fragment_container,
            backStackManager
        )

        // BottomNavigationManager'ı başlat
        bottomNavigationManager = BottomNavigationManager(
            binding.bottomNavigation,
            navigationManager
        )

        // Bottom navigation'ı kur
        bottomNavigationManager.setupBottomNavigation(backStackManager)
    }

    private fun setupBackButtonHandling() {
        backPressedCallback = backStackManager.setupBackPressedCallback(
            this,
            currentTabId = { navigationManager.getActiveTabId() },
            bottomNavigation = binding.bottomNavigation,
            onNavigateBack = { fragmentInfo ->
                navigationManager.showFragmentOnBackPressed(fragmentInfo)
                bottomNavigationManager.updateBottomNavVisibility(fragmentInfo.fragment)
            },
            onNavigateToHome = {
                bottomNavigationManager.showBottomNavFragment(R.id.homeFragment)
            },
            onFinish = {
                finish()
            }
        )

        // Callback'i ekle
        onBackPressedDispatcher.addCallback(this, backPressedCallback)
    }

    // Public API for Fragments

    /**
     * Fragment'lerdeki UI geri butonları için kullanılacak yardımcı metot
     */
    fun handleBackPressed() {
        Log.d(TAG, "Manual back pressed triggered")
        onBackPressedDispatcher.onBackPressed()
    }

    /**
     * Login ekranını göster ve bottom navigation'ı gizle
     */
    fun showLoginFragment() {
        // Bottom navigation'ı gizle
        bottomNavigationManager.hideBottomNavigation()

        // Login fragment'ini göster
        navigationManager.showLoginFragment()
    }

    /**
     * Şehir detay ekranına geçiş için public API
     */
    fun navigateToCityDetail(cityId: String) {
        val fragment = navigationManager.navigateToCityDetail(cityId)
        bottomNavigationManager.updateBottomNavVisibility(fragment)
    }

    /**
     * Kategori listesi ekranına geçiş için public API
     */
    fun navigateToCategoryList(categoryId: String) {
        val fragment = navigationManager.navigateToCategoryList(categoryId)
        bottomNavigationManager.updateBottomNavVisibility(fragment)
    }

    /**
     * Login ekranından ana sayfaya gitmek için yardımcı metot
     */
    fun navigateToHomeAfterLogin() {
        backStackManager.clearBackStack() // Backstack'i temizle
        bottomNavigationManager.showBottomNavFragment(R.id.homeFragment)
    }

    // State'i kaydet
    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putInt("activeTabId", navigationManager.getActiveTabId())
    }
}