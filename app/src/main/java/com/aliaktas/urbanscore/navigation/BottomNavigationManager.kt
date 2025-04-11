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
    companion object {
        private const val TAG = "BottomNavManager"

        // Fragment tag constants
        private const val TAG_HOME = "HOME_FRAGMENT"
        private const val TAG_EXPLORE = "EXPLORE_FRAGMENT"
        private const val TAG_ALL_CITIES = "ALL_CITIES_FRAGMENT"
        private const val TAG_PROFILE = "PROFILE_FRAGMENT"

        // Tab IDs - Navigation View'daki item ID'leri ile eşleşmelidir
        private val TAB_IDS = setOf(
            R.id.homeFragment,
            R.id.exploreFragment,
            R.id.allCitiesFragment,
            R.id.profileFragment
        )
    }

    /**
     * Bottom navigation görünür olacak fragment türleri
     */
    private val bottomNavVisibleFragments = setOf(
        HomeFragment::class.java,
        ExploreFragment::class.java,
        AllCitiesFragment::class.java,
        ProfileFragment::class.java,
        CityDetailFragment::class.java,
        CategoryListFragment::class.java
    )

    /**
     * Fragment map - tab ID'lerine göre lazy olarak oluşturulan fragment'ler
     */
    private val fragmentMap = mapOf(
        R.id.homeFragment to lazy { HomeFragment() },
        R.id.exploreFragment to lazy { ExploreFragment() },
        R.id.allCitiesFragment to lazy { AllCitiesFragment() },
        R.id.profileFragment to lazy { ProfileFragment() }
    )

    fun setupBottomNavigation(backStackManager: BackStackManager) {
        Log.d(TAG, "Setting up bottom navigation")

        // Tab seçim listener'ı ayarla
        bottomNavigation.setOnItemSelectedListener { item ->
            when (item.itemId) {
                in TAB_IDS -> {
                    // handleTabSelection yerine handleTabNavigation kullanılacak
                    handleTabNavigation(item.itemId, backStackManager)
                    true
                }
                else -> {
                    Log.w(TAG, "Unknown menu item selected: ${item.itemId}")
                    false
                }
            }
        }

        // Tab yeniden seçim listener'ı ayarla
        bottomNavigation.setOnItemReselectedListener { item ->
            // Tab yeniden seçildiğinde, kullanıcı aynı tab'in ana ekranına geri dönmeli
            Log.d(TAG, "Tab reselected: ${item.title}")

            // Aktif fragment bir tab fragment mi (ana ekran mı) değil mi kontrol et
            val activeFragment = navigationManager.getActiveFragment()
            val isMainTabFragment = when(activeFragment) {
                is HomeFragment, is ExploreFragment, is AllCitiesFragment, is ProfileFragment -> true
                else -> false
            }

            // Eğer ana tab ekranında değilsek, o tab'in ana ekranına dön
            if (!isMainTabFragment) {
                showBottomNavFragment(item.itemId)
            }
            // Ana ekrandaysak hiçbir şey yapma
        }
    }

    // YENİ METHOD: Hem tab seçimini hem de ana tab'a dönüşü işler
    private fun handleTabNavigation(tabId: Int, backStackManager: BackStackManager) {
        // Aktif fragment ve tab bilgilerini al
        val currentTabId = navigationManager.getActiveTabId()
        val activeFragment = navigationManager.getActiveFragment()

        // Aktif fragment bir tab fragment mi?
        val isMainTabFragment = activeFragment is HomeFragment ||
                activeFragment is ExploreFragment ||
                activeFragment is AllCitiesFragment ||
                activeFragment is ProfileFragment

        // DURUM 1: Aynı tab seçildi, ama ana tab ekranında değiliz - ana tab'a dön
        if (tabId == currentTabId && !isMainTabFragment) {
            Log.d(TAG, "Same tab selected but not on tab fragment, returning to tab: $tabId")
            showBottomNavFragment(tabId)
            return
        }

        // DURUM 2: Farklı bir tab seçildi
        if (tabId != currentTabId) {
            Log.d(TAG, "Tab changed from $currentTabId to $tabId")

            // Back stack'i tab'a kadar temizle
            val clearedCount = backStackManager.clearBackStackUntilTab()
            Log.d(TAG, "Cleared $clearedCount fragments from backstack")

            // Seçilen tab'ı göster
            showBottomNavFragment(tabId)
        }

        // DURUM 3: Aynı tab seçildi ve zaten ana tab ekranındayız - hiçbir şey yapma
        // (Bu durum için ekstra kod yazmaya gerek yok)
    }


    fun updateBottomNavVisibility(fragment: Fragment) {
        val shouldShowBottomNav = shouldShowBottomNav(fragment)
        val fragmentName = fragment.javaClass.simpleName

        if (bottomNavigation.visibility == View.VISIBLE && !shouldShowBottomNav) {
            Log.d(TAG, "Hiding bottom nav for $fragmentName")
            bottomNavigation.visibility = View.GONE
        } else if (bottomNavigation.visibility == View.GONE && shouldShowBottomNav) {
            Log.d(TAG, "Showing bottom nav for $fragmentName")
            bottomNavigation.visibility = View.VISIBLE
        }
    }

    /**
     * Verilen fragment için bottom nav görünür olmalı mı belirler
     *
     * @param fragment Kontrol edilecek fragment
     * @return Bottom nav görünür olmalıysa true, gizli olmalıysa false
     */
    private fun shouldShowBottomNav(fragment: Fragment): Boolean {
        return bottomNavVisibleFragments.contains(fragment::class.java)
    }

    /**
     * Bottom nav tab'ına göre ilgili fragment'i gösterir
     *
     * @param menuItemId Gösterilecek tab ID'si
     * @return İşlem başarılıysa true, değilse false
     */
    fun showBottomNavFragment(menuItemId: Int): Boolean {
        try {
            // Bottom navigation'ı görünür yap
            bottomNavigation.visibility = View.VISIBLE

            // Menu item'ı seç
            val menuItem = bottomNavigation.menu.findItem(menuItemId)
            if (menuItem == null) {
                Log.e(TAG, "Menu item not found for ID: $menuItemId")
                return false
            }

            menuItem.isChecked = true

            // Fragment map'ten fragment'i al ve göster
            val fragmentLazy = fragmentMap[menuItemId]
            if (fragmentLazy != null) {
                val fragment = fragmentLazy.value
                val tag = getTagForMenuItem(menuItemId)

                navigationManager.showBottomNavFragment(fragment, menuItemId, tag)
                Log.d(TAG, "Showed tab fragment: ${fragment.javaClass.simpleName} with ID $menuItemId")
                return true
            } else {
                Log.e(TAG, "Fragment not found for menu item: $menuItemId")
                return false
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error showing bottom nav fragment: ${e.message}", e)
            return false
        }
    }

    /**
     * Menu item ID'sine göre fragment tag'i döndürür
     *
     * @param menuItemId Menu item ID'si
     * @return Fragment tag string'i
     */
    private fun getTagForMenuItem(menuItemId: Int): String {
        return when (menuItemId) {
            R.id.homeFragment -> TAG_HOME
            R.id.exploreFragment -> TAG_EXPLORE
            R.id.allCitiesFragment -> TAG_ALL_CITIES
            R.id.profileFragment -> TAG_PROFILE
            else -> "FRAGMENT_$menuItemId"
        }
    }

    /**
     * Bottom navigation'ı gizler
     */
    fun hideBottomNavigation() {
        if (bottomNavigation.visibility == View.VISIBLE) {
            bottomNavigation.visibility = View.GONE
            Log.d(TAG, "Bottom navigation hidden")
        }
    }

    /**
     * Bottom navigation'ı gösterir
     */
    fun showBottomNavigation() {
        if (bottomNavigation.visibility == View.GONE) {
            bottomNavigation.visibility = View.VISIBLE
            Log.d(TAG, "Bottom navigation shown")
        }
    }

    /**
     * Bottom navigation'da belirli bir tab'ı seçer
     *
     * @param tabId Seçilecek tab ID'si
     * @return İşlem başarılıysa true, değilse false
     */
    fun selectTab(tabId: Int): Boolean {
        if (tabId !in TAB_IDS) {
            Log.e(TAG, "Invalid tab ID: $tabId")
            return false
        }

        try {
            bottomNavigation.selectedItemId = tabId
            return true
        } catch (e: Exception) {
            Log.e(TAG, "Error selecting tab: ${e.message}", e)
            return false
        }
    }
}