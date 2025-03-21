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
import com.aliaktas.urbanscore.ads.AdManager
import com.aliaktas.urbanscore.databinding.ActivityMainBinding
import com.aliaktas.urbanscore.navigation.BackStackManager
import com.aliaktas.urbanscore.navigation.BottomNavigationManager
import com.aliaktas.urbanscore.navigation.NavigationManager
import com.aliaktas.urbanscore.ui.auth.ForgotPasswordFragment
import com.aliaktas.urbanscore.ui.auth.RegisterFragment
import com.aliaktas.urbanscore.ui.home.HomeFragment
import com.aliaktas.urbanscore.ui.subscription.ProSubscriptionFragment
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

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

    @Inject
    lateinit var adManager: AdManager

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

        //FirebaseApp.initializeApp(this)

        adManager.initialize()

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

    // Within MainActivity.kt, update this method:
    fun navigateToHomeAfterLogin() {
        try {
            Log.d(TAG, "Navigating to home after login")
            // Clear back stack to prevent returning to login screens
            backStackManager.clearBackStack()

            // Show bottom navigation
            bottomNavigationManager.showBottomNavigation()

            // Navigate to home fragment
            bottomNavigationManager.showBottomNavFragment(R.id.homeFragment)
        } catch (e: Exception) {
            Log.e(TAG, "Navigation error in navigateToHomeAfterLogin", e)

            // Fallback in case of errors
            try {
                // Try direct fragment replacement as fallback
                navigationManager.showFragment(HomeFragment(), false, "HOME_FRAGMENT")
                bottomNavigationManager.showBottomNavigation()
            } catch (e2: Exception) {
                Log.e(TAG, "Critical navigation error", e2)

                // Last resort: recreate activity
                recreate()
            }
        }

    }


    fun navigateToProSubscription() {
        val proFragment = ProSubscriptionFragment()
        navigationManager.showFragment(proFragment, true, "PRO_SUBSCRIPTION_FRAGMENT")
    }


    fun showRegisterFragment() {
        val registerFragment = RegisterFragment()
        navigationManager.showFragment(registerFragment, true, "REGISTER_FRAGMENT")
        bottomNavigationManager.hideBottomNavigation()
    }

    /**
     * Shows the ForgotPasswordFragment
     */
    fun showForgotPasswordFragment() {
        val forgotPasswordFragment = ForgotPasswordFragment()
        navigationManager.showFragment(forgotPasswordFragment, true, "FORGOT_PASSWORD_FRAGMENT")
        bottomNavigationManager.hideBottomNavigation()
    }

    // State'i kaydet
    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putInt("activeTabId", navigationManager.getActiveTabId())
    }
}