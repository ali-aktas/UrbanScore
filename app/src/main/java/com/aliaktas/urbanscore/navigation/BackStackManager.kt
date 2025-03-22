package com.aliaktas.urbanscore.navigation

import android.util.Log
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import com.aliaktas.urbanscore.R
import com.google.android.material.bottomnavigation.BottomNavigationView
import java.util.Stack

/**
 * Fragment backstack yönetiminden sorumlu sınıf.
 * Fragment geçişlerinin geçmişini tutar ve geri tuşu davranışını yönetir.
 */
class BackStackManager(
    private val fragmentManager: FragmentManager
) {
    companion object {
        private const val TAG = "BackStackManager"

        // Bottom navigation IDs for tab items
        private val BOTTOM_NAV_ITEMS = setOf(
            R.id.homeFragment,
            R.id.exploreFragment,
            R.id.allCitiesFragment,
            R.id.profileFragment
        )
    }

    // Fragment bilgilerini saklamak için veri sınıfı
    data class FragmentInfo(
        val fragment: Fragment,
        val tabId: Int
    ) {
        override fun toString(): String = "FragmentInfo(${fragment.javaClass.simpleName}, tabId=$tabId)"
    }

    // Backstack yönetimi için stack yapısı
    private val fragmentBackStack = Stack<FragmentInfo>()

    /**
     * Fragment'i backstack'e ekler
     *
     * @param fragment Eklenecek fragment
     * @param tabId Fragment'in bağlı olduğu tab ID'si
     * @return Eklendiyse true, aksi halde false
     */
    fun pushToBackStack(fragment: Fragment, tabId: Int): Boolean {
        // Fragment'in geçerli olduğundan emin ol
        if (fragment.isAdded && !fragment.isDetached) {
            fragmentBackStack.push(FragmentInfo(fragment, tabId))
            Log.d(TAG, "Added to backstack: ${fragment.javaClass.simpleName}, Stack size: ${fragmentBackStack.size}")
            printBackStack()
            return true
        } else {
            Log.w(TAG, "Skipped pushing invalid fragment to backstack: ${fragment.javaClass.simpleName}")
            return false
        }
    }

    /**
     * Backstack'ten bir önceki fragment'i alır
     *
     * @return Önceki fragment bilgisi veya stack boşsa null
     */
    fun popFromBackStack(): FragmentInfo? {
        if (fragmentBackStack.isEmpty()) {
            Log.d(TAG, "Cannot pop: backstack is empty")
            return null
        }

        return try {
            val info = fragmentBackStack.pop()
            Log.d(TAG, "Popped from backstack: $info")
            printBackStack()
            info
        } catch (e: Exception) {
            Log.e(TAG, "Error popping from backstack", e)
            null
        }
    }

    /**
     * Backstack'i son tab'a kadar temizler
     *
     * @return Temizlenen fragment sayısı
     */
    fun clearBackStackUntilTab(): Int {
        if (fragmentBackStack.isEmpty()) {
            Log.d(TAG, "Backstack is already empty")
            return 0
        }

        var clearedCount = 0
        try {
            Log.d(TAG, "Clearing backstack until last tab...")
            while (fragmentBackStack.isNotEmpty()) {
                val fragmentInfo = fragmentBackStack.peek()
                if (isBottomNavTab(fragmentInfo.tabId)) {
                    Log.d(TAG, "Found tab in backstack, stopping clear: $fragmentInfo")
                    break
                }
                Log.d(TAG, "Removing from backstack: $fragmentInfo")
                fragmentBackStack.pop()
                clearedCount++
            }
            printBackStack()
            return clearedCount
        } catch (e: Exception) {
            Log.e(TAG, "Error clearing backstack until tab: ${e.message}", e)
            return clearedCount
        }
    }

    /**
     * Backstack'i tamamen temizler
     */
    fun clearBackStack() {
        val size = fragmentBackStack.size
        fragmentBackStack.clear()
        Log.d(TAG, "Backstack cleared, removed $size items")
    }

    /**
     * Geri tuşu davranışını oluşturur
     *
     * @param activity Activity referansı
     * @param currentTabId Aktif tab ID'sini döndüren lambda
     * @param bottomNavigation Bottom navigation view referansı
     * @param onNavigateBack Geri navigasyon callback'i
     * @param onNavigateToHome Ana sayfaya navigasyon callback'i
     * @param onFinish Uygulamadan çıkış callback'i
     * @return Oluşturulan OnBackPressedCallback
     */
    fun setupBackPressedCallback(
        activity: AppCompatActivity,
        currentTabId: () -> Int,
        bottomNavigation: BottomNavigationView,
        onNavigateBack: (FragmentInfo) -> Unit,
        onNavigateToHome: () -> Unit,
        onFinish: () -> Unit
    ): OnBackPressedCallback {
        return object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                Log.d(TAG, "Back pressed. Stack size: ${fragmentBackStack.size}")

                // Eğer backstack doluysa, önceki ekrana dön
                if (fragmentBackStack.isNotEmpty()) {
                    handleBackWithNonEmptyStack(
                        bottomNavigation,
                        onNavigateBack,
                        onNavigateToHome
                    )
                } else {
                    handleBackWithEmptyStack(
                        currentTabId(),
                        onNavigateToHome,
                        onFinish
                    )
                }
            }
        }
    }

    /**
     * Backstack doluyken geri tuşu işlemini yönetir
     *
     * @param bottomNavigation Bottom navigation view
     * @param onNavigateBack Geri navigasyon callback'i
     * @param onNavigateToHome Ana sayfaya navigasyon callback'i
     */
    private fun handleBackWithNonEmptyStack(
        bottomNavigation: BottomNavigationView,
        onNavigateBack: (FragmentInfo) -> Unit,
        onNavigateToHome: () -> Unit
    ) {
        val previousFragmentInfo = popFromBackStack()

        // Null check
        if (previousFragmentInfo == null) {
            Log.e(TAG, "Error: Popped null fragment info despite non-empty stack")
            onNavigateToHome()
            return
        }

        // Önceki fragment'e dön
        onNavigateBack(previousFragmentInfo)

        // Eğer bottom nav tab'ıysa, UI'ı güncelle
        if (isBottomNavTab(previousFragmentInfo.tabId)) {
            try {
                val menuItem = bottomNavigation.menu.findItem(previousFragmentInfo.tabId)
                if (menuItem != null) {
                    bottomNavigation.selectedItemId = previousFragmentInfo.tabId
                    Log.d(TAG, "Set bottom nav to tab: ${previousFragmentInfo.tabId}")
                } else {
                    Log.e(TAG, "Menu item not found for id: ${previousFragmentInfo.tabId}")
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error selecting bottom nav item: ${e.message}", e)
            }
        }
    }

    /**
     * Backstack boşken geri tuşu işlemini yönetir
     *
     * @param currentTabId Aktif tab ID'si
     * @param onNavigateToHome Ana sayfaya navigasyon callback'i
     * @param onFinish Uygulamadan çıkış callback'i
     */
    private fun handleBackWithEmptyStack(
        currentTabId: Int,
        onNavigateToHome: () -> Unit,
        onFinish: () -> Unit
    ) {
        // Backstack boşsa ve ana sayfada değilsek, ana sayfaya dön
        if (currentTabId != R.id.homeFragment) {
            Log.d(TAG, "Not on home tab ($currentTabId), navigating to home")
            onNavigateToHome()
        } else {
            // Ana sayfadaysak, uygulamayı kapat
            Log.d(TAG, "On home tab, finishing activity")
            onFinish()
        }
    }

    /**
     * Tab ID'sinin bottom navigation tab'ı olup olmadığını kontrol eder
     *
     * @param tabId Kontrol edilecek tab ID'si
     * @return Bottom nav tab'ı ise true, değilse false
     */
    fun isBottomNavTab(tabId: Int): Boolean = tabId in BOTTOM_NAV_ITEMS

    /**
     * Stack'teki fragment sayısını döndürür
     *
     * @return Backstack uzunluğu
     */
    fun getBackStackSize(): Int = fragmentBackStack.size

    /**
     * Stack boş mu kontrolü
     *
     * @return Stack boşsa true, değilse false
     */
    fun isBackStackEmpty(): Boolean = fragmentBackStack.isEmpty()

    /**
     * Backstack içeriğini loglar (debug için)
     */
    private fun printBackStack() {
        if (fragmentBackStack.isEmpty()) {
            Log.d(TAG, "Backstack is empty")
            return
        }

        Log.d(TAG, "Current backstack (${fragmentBackStack.size} items):")
        fragmentBackStack.forEachIndexed { index, info ->
            Log.d(TAG, "  $index: ${info.fragment.javaClass.simpleName}, tabId=${info.tabId}")
        }
    }
}