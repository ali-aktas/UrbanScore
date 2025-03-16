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
    private val TAG = "BackStackManager"

    // Fragment bilgilerini saklamak için veri sınıfı
    data class FragmentInfo(
        val fragment: Fragment,
        val tabId: Int
    )

    // Bottom navigation menu item'ları için sabit set
    private val bottomNavItems = setOf(
        R.id.homeFragment,
        R.id.exploreFragment,
        R.id.allCitiesFragment,
        R.id.profileFragment
    )

    // Backstack yönetimi için stack yapısı
    private val fragmentBackStack = Stack<FragmentInfo>()

    /**
     * Fragment'i backstack'e ekler
     */
    fun pushToBackStack(fragment: Fragment, tabId: Int) {
        Log.d(TAG, "Pushing to backstack: ${fragment.javaClass.simpleName}, tabId: $tabId")
        fragmentBackStack.push(FragmentInfo(fragment, tabId))
        printBackStack() // Debug log için
    }

    /**
     * Backstack'ten bir önceki fragment'i alır
     * @return Önceki fragment bilgisi veya stack boşsa null
     */
    fun popFromBackStack(): FragmentInfo? {
        if (fragmentBackStack.isEmpty()) {
            Log.d(TAG, "Cannot pop: backstack is empty")
            return null
        }

        try {
            val info = fragmentBackStack.pop()
            Log.d(TAG, "Popping from backstack: ${info.fragment.javaClass.simpleName}, tabId: ${info.tabId}")
            printBackStack() // Debug log için
            return info
        } catch (e: Exception) {
            Log.e(TAG, "Error popping from backstack", e)
            return null
        }
    }

    /**
     * Backstack'i son tab'a kadar temizler
     */
    fun clearBackStackUntilTab() {
        Log.d(TAG, "Clearing backstack until last tab...")
        while (fragmentBackStack.isNotEmpty()) {
            val fragmentInfo = fragmentBackStack.peek()
            if (fragmentInfo.tabId in bottomNavItems) {
                Log.d(TAG, "Found tab in backstack, stopping clear: ${fragmentInfo.fragment.javaClass.simpleName}")
                break
            }
            Log.d(TAG, "Removing from backstack: ${fragmentInfo.fragment.javaClass.simpleName}")
            fragmentBackStack.pop()
        }
        printBackStack() // Debug log için
    }

    /**
     * Backstack'i tamamen temizler
     */
    fun clearBackStack() {
        Log.d(TAG, "Clearing entire backstack")
        fragmentBackStack.clear()
    }

    /**
     * Geri tuşu davranışını oluşturur
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

                // Backstack boş değilse, bir önceki fragment'e dön
                if (fragmentBackStack.isNotEmpty()) {
                    val previousFragmentInfo = popFromBackStack()

                    // Null kontrolü ekle
                    if (previousFragmentInfo == null) {
                        Log.e(TAG, "Error: Popped null fragment info despite non-empty stack")
                        // Stackte sorun var, ana sayfaya yönlendir
                        onNavigateToHome()
                        return
                    }

                    Log.d(TAG, "Navigating back to: ${previousFragmentInfo.fragment.javaClass.simpleName}")

                    // Eğer önceki ekran bottom nav tab'ıysa, ilgili tab'ı seç
                    val isBottomNavTab = previousFragmentInfo.tabId in bottomNavItems

                    // Önceki fragment'e dön
                    onNavigateBack(previousFragmentInfo)

                    // Bottom nav tab'ı ise, UI'da ilgili tab'ı seç
                    if (isBottomNavTab) {
                        Log.d(TAG, "Setting bottom nav selected item to: ${previousFragmentInfo.tabId}")

                        // Menu item'ın mevcut olduğundan emin ol
                        val menuItem = bottomNavigation.menu.findItem(previousFragmentInfo.tabId)
                        if (menuItem != null) {
                            bottomNavigation.selectedItemId = previousFragmentInfo.tabId
                        } else {
                            Log.e(TAG, "Menu item not found for id: ${previousFragmentInfo.tabId}")
                        }
                    }
                } else {
                    Log.d(TAG, "Backstack is empty")

                    // Backstack boşsa ve ana sayfada değilsek, ana sayfaya dön
                    val currentId = currentTabId()
                    if (currentId != R.id.homeFragment) {
                        Log.d(TAG, "Not on home tab, navigating to home")
                        onNavigateToHome()
                    } else {
                        // Ana sayfadaysak, uygulamadan çık
                        Log.d(TAG, "On home tab, finishing activity")
                        onFinish()
                    }
                }
            }
        }
    }

    /**
     * Stack'teki fragment sayısını döndürür
     */
    fun getBackStackSize(): Int = fragmentBackStack.size

    /**
     * Stack boş mu kontrolü
     */
    fun isBackStackEmpty(): Boolean = fragmentBackStack.isEmpty()

    /**
     * Backstack içeriğini loglar (debug için)
     */
    fun printBackStack() {
        Log.d(TAG, "Current backstack (${fragmentBackStack.size} items):")
        fragmentBackStack.forEachIndexed { index, info ->
            Log.d(TAG, "  $index: ${info.fragment.javaClass.simpleName}, tabId: ${info.tabId}")
        }
    }
}