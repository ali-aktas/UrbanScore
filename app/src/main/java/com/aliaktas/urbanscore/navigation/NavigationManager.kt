package com.aliaktas.urbanscore.navigation

import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import com.aliaktas.urbanscore.R
import com.aliaktas.urbanscore.ui.allcities.AllCitiesFragment
import com.aliaktas.urbanscore.ui.auth.LoginFragment
import com.aliaktas.urbanscore.ui.categories.CategoryListFragment
import com.aliaktas.urbanscore.ui.detail.CityDetailFragment
import com.aliaktas.urbanscore.ui.home.HomeFragment
import com.aliaktas.urbanscore.ui.profile.ProfileFragment
import com.aliaktas.urbanscore.ui.search.ExploreFragment

/**
 * Fragment gösterimi ve geçişlerinden sorumlu sınıf.
 * Fragment'leri ekler, gösterir, gizler ve animasyonları yönetir.
 */
class NavigationManager(
    private val fragmentManager: FragmentManager,
    private val containerId: Int,
    private val backStackManager: BackStackManager
) {
    private val TAG = "NavigationManager"

    // Aktif fragment referansı
    private var activeFragment: Fragment? = null

    // Aktif tab ID'si
    private var activeTabId = R.id.homeFragment

    /**
     * Yeni bir fragment'i gösterir veya var olan bir fragment'i öne çıkarır.
     *
     * @param fragment Gösterilecek fragment
     * @param addToBackStack Fragment'i backstack'e eklenip eklenmeyeceği
     * @param tag Fragment için tag
     * @param animationType Geçiş animasyonu tipi
     */
    fun showFragment(
        fragment: Fragment,
        addToBackStack: Boolean = true,
        tag: String? = null,
        animationType: AnimationType = AnimationType.SLIDE_HORIZONTAL
    ) {
        try {
            val fragmentTag = tag ?: fragment.javaClass.simpleName

            // ÖNEMLİ: Önce backstack'e ekle (düzeltildi)
            if (addToBackStack && activeFragment != null) {
                Log.d(TAG, "Adding current fragment to backstack before showing new fragment")
                backStackManager.pushToBackStack(activeFragment!!, activeTabId)
            }

            // Transaction başlat
            val transaction = fragmentManager.beginTransaction()

            // Animasyonları ayarla
            when (animationType) {
                AnimationType.SLIDE_HORIZONTAL -> {
                    transaction.setCustomAnimations(
                        R.anim.slide_in_right,
                        R.anim.slide_out_left,
                        R.anim.slide_in_left,
                        R.anim.slide_out_right
                    )
                }
                AnimationType.FADE -> {
                    transaction.setCustomAnimations(
                        R.anim.fade_scale_in,
                        R.anim.fade_scale_out
                    )
                }
            }

            // Mevcut fragment'i gizle
            activeFragment?.let { transaction.hide(it) }

            // Var olan fragment'i bul veya yeni ekle
            val existingFragment = fragmentManager.findFragmentByTag(fragmentTag)

            if (existingFragment != null && existingFragment.isAdded) {
                Log.d(TAG, "Showing existing fragment: $fragmentTag")
                transaction.show(existingFragment)
                activeFragment = existingFragment
            } else {
                Log.d(TAG, "Adding new fragment: $fragmentTag")
                transaction.add(containerId, fragment, fragmentTag)
                activeFragment = fragment
            }

            // Transaction'ı tamamla
            transaction.commit()

            Log.d(TAG, "Navigation completed: ${fragment.javaClass.simpleName}")
        } catch (e: Exception) {
            Log.e(TAG, "Navigation error: ${e.message}", e)
        }
    }

    /**
     * Belirli bir bottom nav tab'ını seçer ve ilgili fragment'i gösterir
     */
    fun showBottomNavFragment(fragment: Fragment, tabId: Int, tag: String? = null) {
        // Aktif tab'ı güncelle
        Log.d(TAG, "Showing bottom nav fragment: $tabId, ${fragment.javaClass.simpleName}")
        activeTabId = tabId

        // Fragment'i göster
        val transaction = fragmentManager.beginTransaction()
            .setCustomAnimations(
                R.anim.fade_scale_in,
                R.anim.fade_scale_out
            )

        // Şu anki fragment'i gizle
        activeFragment?.let {
            Log.d(TAG, "Hiding current fragment: ${it.javaClass.simpleName}")
            transaction.hide(it)
        }

        // Tag oluştur
        val fragmentTag = tag ?: fragment.javaClass.simpleName

        // Var olan fragment'i ara
        val existingFragment = fragmentManager.findFragmentByTag(fragmentTag)

        if (existingFragment != null && existingFragment.isAdded) {
            Log.d(TAG, "Showing existing tab fragment: $fragmentTag")
            transaction.show(existingFragment)
            activeFragment = existingFragment
        } else {
            Log.d(TAG, "Adding new tab fragment: $fragmentTag")
            transaction.add(containerId, fragment, fragmentTag)
            activeFragment = fragment
        }

        transaction.commit()
    }

    /**
     * Şehir detay ekranına geçiş için özel metot
     */
    fun navigateToCityDetail(cityId: String): Fragment {
        Log.d(TAG, "Navigating to city detail: $cityId")
        val fragment = CityDetailFragment().apply {
            arguments = Bundle().apply {
                putString("cityId", cityId)
            }
        }

        val tag = "CITY_DETAIL_$cityId"
        showFragment(fragment, true, tag)
        return fragment
    }

    /**
     * Kategori listesi ekranına geçiş için özel metot
     */
    fun navigateToCategoryList(categoryId: String): Fragment {
        Log.d(TAG, "Navigating to category list: $categoryId")
        val fragment = CategoryListFragment().apply {
            arguments = Bundle().apply {
                putString("categoryId", categoryId)
            }
        }

        val tag = "CATEGORY_LIST_$categoryId"
        showFragment(fragment, true, tag)
        return fragment
    }

    /**
     * Login ekranını göstermek için
     */
    fun showLoginFragment() {
        Log.d(TAG, "Showing login fragment")
        val loginFragment = LoginFragment()

        // Backstack'i temizle
        backStackManager.clearBackStack()

        // Login fragment'ini göster
        fragmentManager.beginTransaction()
            .setCustomAnimations(
                R.anim.fade_scale_in,
                R.anim.fade_scale_out
            )
            .replace(containerId, loginFragment, "LOGIN_FRAGMENT")
            .commit()

        // Aktif fragment'i güncelle
        activeFragment = loginFragment
    }

    /**
     * Fragment'i geri tuşu yönetimiyle göster
     */
    fun showFragmentOnBackPressed(fragmentInfo: BackStackManager.FragmentInfo) {
        Log.d(TAG, "Showing fragment on back pressed: ${fragmentInfo.fragment.javaClass.simpleName}")
        val previousFragment = fragmentInfo.fragment
        val previousId = fragmentInfo.tabId

        // Transaction'ı hazırla
        val transaction = fragmentManager.beginTransaction()
            .setCustomAnimations(
                R.anim.slide_in_left,
                R.anim.slide_out_right
            )

        // Şu anki fragment'i gizle
        activeFragment?.let {
            Log.d(TAG, "Hiding current fragment: ${it.javaClass.simpleName}")
            transaction.hide(it)
        }

        // Önceki fragment'i göster
        Log.d(TAG, "Showing previous fragment: ${previousFragment.javaClass.simpleName}")
        transaction.show(previousFragment)

        // Transaction'ı tamamla
        transaction.commit()

        // Aktif fragment'i ve tab'ı güncelle
        activeFragment = previousFragment
        activeTabId = previousId
    }

    /**
     * Aktif fragment'i döndürür
     */
    fun getActiveFragment(): Fragment? = activeFragment

    /**
     * Aktif tab ID'sini döndürür
     */
    fun getActiveTabId(): Int = activeTabId

    /**
     * Aktif tab ID'sini günceller
     */
    fun setActiveTabId(tabId: Int) {
        Log.d(TAG, "Setting active tab ID: $tabId")
        activeTabId = tabId
    }

    /**
     * Animasyon tipleri
     */
    enum class AnimationType {
        SLIDE_HORIZONTAL,
        FADE
    }
}