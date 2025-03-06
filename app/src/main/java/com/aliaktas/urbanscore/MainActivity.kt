package com.aliaktas.urbanscore

import android.os.Bundle
import android.util.Log
import android.view.View
import androidx.activity.OnBackPressedCallback
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.navigation.NavController
import androidx.navigation.NavDestination
import androidx.navigation.NavOptions
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.setupWithNavController
import com.aliaktas.urbanscore.databinding.ActivityMainBinding
import com.aliaktas.urbanscore.ui.auth.AuthViewModel
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : AppCompatActivity(), NavController.OnDestinationChangedListener {

    private lateinit var binding: ActivityMainBinding
    private lateinit var navController: NavController
    private val authViewModel: AuthViewModel by viewModels()

    // These are the fragments that should show bottom navigation
    private val bottomNavVisibleDestinations = setOf(
        R.id.homeFragment,
        R.id.exploreFragment,
        R.id.profileFragment,
        R.id.allCitiesFragment,
        R.id.cityDetailFragment,
        R.id.categoryListFragment
    )

    // These are the actual bottom navigation menu items
    private val bottomNavMenuItems = setOf(
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

        // Set window insets
        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        // Setup NavController
        val navHostFragment = supportFragmentManager
            .findFragmentById(R.id.nav_host_fragment) as NavHostFragment
        navController = navHostFragment.navController

        // Initial navigation based on login state
        val isLoggedIn = intent.getBooleanExtra("isLoggedIn", false)
        if (isLoggedIn) {
            // Create NavOptions that clear the back stack
            val navOptions = NavOptions.Builder()
                .setPopUpTo(R.id.nav_graph, true)
                .build()

            navController.navigate(R.id.homeFragment, null, navOptions)
        }

        // Setup bottom navigation with NavController using custom implementation
        setupBottomNavigation()

        // Listen for destination changes to show/hide bottom nav
        navController.addOnDestinationChangedListener(this)

        // Setup custom back button handling
        setupBackButtonHandling()
    }

    /**
     * Özel bottom navigation kurulumu
     * Fragment'ların yeniden oluşturulmasını engeller
     */
    private fun setupBottomNavigation() {
        // Özel item seçim listener'ı
        binding.bottomNavigation.setOnItemSelectedListener { item ->
            // NavOptions ile her zaman aynı instance'ı kullanacak şekilde ayarla
            val navOptions = NavOptions.Builder()
                .setLaunchSingleTop(true)
                .setEnterAnim(R.anim.slide_in_left)
                .setExitAnim(R.anim.slide_out_left)
                .setPopEnterAnim(R.anim.slide_in_right)
                .setPopExitAnim(R.anim.slide_out_right)
                .build()

            when (item.itemId) {
                R.id.homeFragment,
                R.id.exploreFragment,
                R.id.profileFragment,
                R.id.allCitiesFragment -> {
                    if (navController.currentDestination?.id != item.itemId) {
                        // Sadece farklı bir tab'a geçildiğinde navigate et
                        navController.navigate(item.itemId, null, navOptions)
                    }
                    true
                }
                else -> false
            }
        }

        // Aynı tab'a tekrar tıklandığında hiçbir şey yapma
        binding.bottomNavigation.setOnItemReselectedListener { /* No-op */ }
    }

    /**
     * Sets up custom back button handling to fix navigation issues:
     * 1. Prevents going back to login from main flow
     * 2. Makes sure "back" navigates correctly between tabs
     * 3. Handles AllCitiesFragment correctly as a bottom nav destination
     */
    private fun setupBackButtonHandling() {
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                val currentDestinationId = navController.currentDestination?.id

                try {
                    when {
                        // If on any bottom nav tab, handle in custom way
                        currentDestinationId in bottomNavMenuItems -> {
                            if (currentDestinationId == R.id.homeFragment) {
                                // If on home, exit app
                                finish()
                            } else {
                                // Otherwise, go to home tab
                                binding.bottomNavigation.selectedItemId = R.id.homeFragment
                            }
                        }

                        // Normal back navigation for content destinations
                        else -> {
                            if (isEnabled) {
                                isEnabled = false
                                navController.navigateUp()
                                isEnabled = true
                            }
                        }
                    }
                } catch (e: Exception) {
                    Log.e("MainActivity", "Error handling back press: ${e.message}", e)

                    // Fallback navigation to homeFragment if there's an error
                    try {
                        binding.bottomNavigation.selectedItemId = R.id.homeFragment
                    } catch (ex: Exception) {
                        Log.e("MainActivity", "Fallback navigation failed: ${ex.message}", ex)
                        finish() // Last resort: exit the app
                    }
                }
            }
        })
    }

    override fun onDestinationChanged(
        controller: NavController,
        destination: NavDestination,
        arguments: Bundle?
    ) {
        try {
            // Show/hide bottom navigation based on destination
            binding.bottomNavigation.visibility = if (destination.id in bottomNavVisibleDestinations) {
                View.VISIBLE
            } else {
                View.GONE
            }

            // Sync selected bottom nav item with current destination
            if (destination.id in bottomNavMenuItems) {
                binding.bottomNavigation.menu.findItem(destination.id)?.isChecked = true
            }
        } catch (e: Exception) {
            Log.e("MainActivity", "Error in onDestinationChanged: ${e.message}", e)
        }
    }

    override fun onSupportNavigateUp(): Boolean {
        return navController.navigateUp() || super.onSupportNavigateUp()
    }
}