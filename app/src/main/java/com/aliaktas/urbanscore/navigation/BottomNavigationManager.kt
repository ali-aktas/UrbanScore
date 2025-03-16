package com.aliaktas.urbanscore.navigation

import android.util.Log
import android.view.View
import androidx.fragment.app.Fragment
import com.aliaktas.urbanscore.R
import com.aliaktas.urbanscore.ui.allcities.AllCitiesFragment
import com.aliaktas.urbanscore.ui.categories.CategoryListFragment
import com.aliaktas.urbanscore.ui.detail.CityDetailFragment
import com.aliaktas.urbanscore.ui.home.HomeFragment
import com.aliaktas.urbanscore.ui.profile.ProfileFragment
import com.aliaktas.urbanscore.ui.search.ExploreFragment
import com.google.android.material.bottomnavigation.BottomNavigationView

/**
 * Bottom navigation kontrolü ve görünürlüğünü yöneten sınıf.
 */
class BottomNavigationManager(
    private val bottomNavigation: BottomNavigationView,
    private val navigationManager: NavigationManager
) {
    private val TAG = "BottomNavManager"

    // Bottom nav görünür olacak fragment'ların listesi
    private val bottomNavVisibleFragments = setOf(
        HomeFragment::class.java,
        ExploreFragment::class.java,
        AllCitiesFragment::class.java,
        ProfileFragment::class.java,
        CityDetailFragment::class.java,
        CategoryListFragment::class.java
    )

    // Lazy fragment instances
    private val homeFragment by lazy { HomeFragment() }
    private val exploreFragment by lazy { ExploreFragment() }
    private val allCitiesFragment by lazy { AllCitiesFragment() }
    private val profileFragment by lazy { ProfileFragment() }

    /**
     * Bottom navigation'ı yapılandırır ve click listener'ları ayarlar
     */
    fun setupBottomNavigation(backStackManager: BackStackManager) {
        Log.d(TAG, "Setting up bottom navigation")
        bottomNavigation.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.homeFragment,
                R.id.exploreFragment,
                R.id.allCitiesFragment,
                R.id.profileFragment -> {
                    // Aynı tab'a tıklandıysa bir şey yapma
                    if (item.itemId != navigationManager.getActiveTabId()) {
                        Log.d(TAG, "Selected bottom nav item: ${item.itemId}")

                        // Back stack'i temizleyelim
                        backStackManager.clearBackStackUntilTab()

                        showBottomNavFragment(item.itemId)
                    } else {
                        Log.d(TAG, "Same tab selected, no action needed")
                    }
                    true
                }
                else -> false
            }
        }

        bottomNavigation.setOnItemReselectedListener {
            Log.d(TAG, "Tab reselected, no action needed")
        }
    }

    /**
     * Bottom navigation'ın görünürlüğünü fragment tipine göre ayarlar
     */
    fun updateBottomNavVisibility(fragment: Fragment) {
        val shouldShowBottomNav = bottomNavVisibleFragments.contains(fragment::class.java)
        Log.d(TAG, "Update bottom nav visibility for ${fragment.javaClass.simpleName}: $shouldShowBottomNav")

        bottomNavigation.visibility = if (shouldShowBottomNav) View.VISIBLE else View.GONE
    }

    /**
     * Bottom nav tab'ına göre ilgili fragment'i gösterir
     */
    fun showBottomNavFragment(menuItemId: Int) {
        // Bottom navigation'ı göster
        Log.d(TAG, "Showing bottom nav fragment for menu item: $menuItemId")
        bottomNavigation.visibility = View.VISIBLE

        // Menu item'ı seç
        bottomNavigation.menu.findItem(menuItemId)?.isChecked = true

        // İlgili fragment'i göster
        val fragment = getFragmentForMenuItem(menuItemId)
        val tag = getTagForMenuItem(menuItemId)
        navigationManager.showBottomNavFragment(fragment, menuItemId, tag)
    }

    /**
     * Menu item ID'sine göre fragment döndürür
     */
    private fun getFragmentForMenuItem(menuItemId: Int): Fragment {
        return when (menuItemId) {
            R.id.homeFragment -> homeFragment
            R.id.exploreFragment -> exploreFragment
            R.id.allCitiesFragment -> allCitiesFragment
            R.id.profileFragment -> profileFragment
            else -> homeFragment // Varsayılan olarak ana sayfaya dön
        }
    }

    /**
     * Menu item ID'sine göre tag döndürür
     */
    private fun getTagForMenuItem(menuItemId: Int): String {
        return when (menuItemId) {
            R.id.homeFragment -> "HOME_FRAGMENT"
            R.id.exploreFragment -> "EXPLORE_FRAGMENT"
            R.id.allCitiesFragment -> "ALL_CITIES_FRAGMENT"
            R.id.profileFragment -> "PROFILE_FRAGMENT"
            else -> "UNKNOWN_FRAGMENT"
        }
    }

    /**
     * Bottom navigation'ı gizler
     */
    fun hideBottomNavigation() {
        Log.d(TAG, "Hiding bottom navigation")
        bottomNavigation.visibility = View.GONE
    }

    /**
     * Bottom navigation'ı gösterir
     */
    fun showBottomNavigation() {
        Log.d(TAG, "Showing bottom navigation")
        bottomNavigation.visibility = View.VISIBLE
    }
}