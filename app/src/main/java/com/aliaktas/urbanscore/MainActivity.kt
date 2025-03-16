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

    // Bottom nav görünürlük kontrolü için destination setleri
    private val bottomNavVisibleDestinations = setOf(
        R.id.homeFragment,
        R.id.exploreFragment,
        R.id.profileFragment,
        R.id.allCitiesFragment,
        R.id.cityDetailFragment,
        R.id.categoryListFragment
    )

    // Ana bottom nav menü öğeleri
    private val bottomNavMenuItems = setOf(
        R.id.homeFragment,
        R.id.exploreFragment,
        R.id.allCitiesFragment,
        R.id.profileFragment
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

        // Setup bottom navigation
        setupBottomNavigation()

        // Listen for destination changes to show/hide bottom nav
        navController.addOnDestinationChangedListener(this)

        // Setup custom back button handling
        setupBackButtonHandling()
    }

    private fun setupBottomNavigation() {
        // Bottom Navigation click listener
        binding.bottomNavigation.setOnItemSelectedListener { item ->
            // NavOptions ile tek seferlik navigasyon yapılandırması
            val navOptions = NavOptions.Builder()
                .setLaunchSingleTop(true)
                .setEnterAnim(R.anim.material_enter)
                .setExitAnim(R.anim.material_exit)
                .setPopEnterAnim(R.anim.material_enter)
                .setPopExitAnim(R.anim.material_exit)
                .build()

            // Check which item was clicked
            when (item.itemId) {
                R.id.homeFragment,
                R.id.exploreFragment,
                R.id.profileFragment,
                R.id.allCitiesFragment -> {
                    if (navController.currentDestination?.id != item.itemId) {
                        // Only navigate if the destination is different
                        navController.navigate(item.itemId, null, navOptions)
                    }
                    true
                }
                else -> false
            }
        }

        // Do nothing when the same item is reselected
        binding.bottomNavigation.setOnItemReselectedListener { /* No-op */ }
    }

    private fun setupBackButtonHandling() {
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                val currentDestinationId = navController.currentDestination?.id

                try {
                    when {
                        // If on home, exit app
                        currentDestinationId == R.id.homeFragment -> {
                            finish()
                        }
                        // If on any other bottom nav tab, go to home
                        currentDestinationId in bottomNavMenuItems -> {
                            binding.bottomNavigation.selectedItemId = R.id.homeFragment
                        }
                        // Otherwise, use default back navigation
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