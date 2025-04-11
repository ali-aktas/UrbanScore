package com.aliaktas.urbanscore.navigation

import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.commit
import com.aliaktas.urbanscore.R
import com.aliaktas.urbanscore.ui.auth.LoginFragment
import com.aliaktas.urbanscore.ui.categories.CategoryListFragment
import com.aliaktas.urbanscore.ui.detail.CityDetailFragment

/**
 * Fragment gösterimi ve geçişlerinden sorumlu sınıf.
 * Fragment'leri ekler, gösterir, gizler ve animasyonları yönetir.
 */
class NavigationManager(
    private val fragmentManager: FragmentManager,
    private val containerId: Int,
    private val backStackManager: BackStackManager
) {
    companion object {
        private const val TAG = "NavigationManager"

        // Fragment tagleri için sabitler
        private const val TAG_LOGIN = "LOGIN_FRAGMENT"
        private const val TAG_CITY_DETAIL_PREFIX = "CITY_DETAIL_"
        private const val TAG_CATEGORY_LIST_PREFIX = "CATEGORY_LIST_"

        // Bottom Navigation Tab Indeksleri
        private val TAB_INDICES = mapOf(
            R.id.homeFragment to 0,
            R.id.exploreFragment to 1,
            R.id.allCitiesFragment to 2,
            R.id.profileFragment to 3
        )
    }

    private object Animations {
        val SLIDE_IN_RIGHT = R.anim.slide_in_right  // Sağdan gelen ekran
        val SLIDE_OUT_LEFT = R.anim.slide_out_left  // Sola çıkan ekran
        val SLIDE_IN_LEFT = R.anim.slide_in_left    // Soldan gelen ekran
        val SLIDE_OUT_RIGHT = R.anim.slide_out_right // Sağa çıkan ekran
        val FADE_IN = R.anim.fade_scale_in
        val FADE_OUT = R.anim.fade_scale_out
    }

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
     * @return Gösterilen fragment
     */
    fun showFragment(
        fragment: Fragment,
        addToBackStack: Boolean = true,
        tag: String? = null,
        animationType: AnimationType = AnimationType.SLIDE_HORIZONTAL
    ): Fragment {
        try {
            val fragmentTag = tag ?: fragment.javaClass.simpleName

            // Mevcut fragment'i back stack'e ekle
            if (addToBackStack && activeFragment != null) {
                Log.d(TAG, "Adding to backstack: ${activeFragment?.javaClass?.simpleName}")
                backStackManager.pushToBackStack(activeFragment!!, activeTabId)
            }

            // FragmentTransaction başlat
            fragmentManager.commit {
                // Animasyonları ayarla - her durumda slide animasyonlarını kullan
                // Varsayılan olarak sağdan sola geçiş
                setCustomAnimations(
                    Animations.SLIDE_IN_RIGHT, Animations.SLIDE_OUT_LEFT,
                    Animations.SLIDE_IN_LEFT, Animations.SLIDE_OUT_RIGHT
                )

                // Aktif fragment'i gizle
                activeFragment?.let { hide(it) }

                // Var olan fragment'i bul veya yeni ekle
                val existingFragment = fragmentManager.findFragmentByTag(fragmentTag)

                if (existingFragment != null && existingFragment.isAdded) {
                    Log.d(TAG, "Showing existing fragment: $fragmentTag")
                    show(existingFragment)
                    activeFragment = existingFragment
                } else {
                    Log.d(TAG, "Adding new fragment: $fragmentTag")
                    add(containerId, fragment, fragmentTag)
                    activeFragment = fragment
                }

                // State loss'a izin ver (configuration change durumları için)
                setReorderingAllowed(true)
            }

            Log.d(TAG, "Navigation completed: ${fragment.javaClass.simpleName}")
            return activeFragment ?: fragment
        } catch (e: Exception) {
            Log.e(TAG, "Navigation error: ${e.message}", e)
            // Hata durumunda da fragment referansını döndür
            return fragment
        }
    }

    /**
     * Bottom Navigation için tab konumlarına göre animasyon seçer
     *
     * @param currentTabId Mevcut tab ID
     * @param targetTabId Hedef tab ID
     * @return Uygun animasyon çifti (enterAnim, exitAnim)
     */
    private fun selectTabAnimations(currentTabId: Int, targetTabId: Int): Pair<Int, Int> {
        val currentIndex = TAB_INDICES[currentTabId] ?: 0
        val targetIndex = TAB_INDICES[targetTabId] ?: 0

        return if (targetIndex > currentIndex) {
            // Sağa doğru geçiş - soldan sağa kayma animasyonu
            Pair(Animations.SLIDE_IN_RIGHT, Animations.SLIDE_OUT_LEFT)
        } else {
            // Sola doğru geçiş - sağdan sola kayma animasyonu
            Pair(Animations.SLIDE_IN_LEFT, Animations.SLIDE_OUT_RIGHT)
        }
    }

    /**
     * Belirli bir bottom nav tab'ını seçer ve ilgili fragment'i gösterir
     *
     * @param fragment Gösterilecek fragment
     * @param tabId Tab ID'si
     * @param tag Fragment tag'i
     */
    fun showBottomNavFragment(fragment: Fragment, tabId: Int, tag: String? = null) {
        try {
            Log.d(TAG, "Showing bottom nav fragment: $tabId, ${fragment.javaClass.simpleName}")

            // Animasyonları seç
            val animations = selectTabAnimations(activeTabId, tabId)

            // Aktif tab'ı güncelle
            activeTabId = tabId

            // Fragment'i göster
            fragmentManager.commit {
                // Tab konumlarına göre seçilen animasyonları kullan
                setCustomAnimations(animations.first, animations.second)

                // Şu anki fragment'i gizle
                activeFragment?.let { hide(it) }

                // Tag oluştur
                val fragmentTag = tag ?: fragment.javaClass.simpleName

                // Var olan fragment'i ara
                val existingFragment = fragmentManager.findFragmentByTag(fragmentTag)

                if (existingFragment != null && existingFragment.isAdded) {
                    Log.d(TAG, "Showing existing tab fragment: $fragmentTag")
                    show(existingFragment)
                    activeFragment = existingFragment
                } else {
                    Log.d(TAG, "Adding new tab fragment: $fragmentTag")
                    add(containerId, fragment, fragmentTag)
                    activeFragment = fragment
                }

                setReorderingAllowed(true)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error showing bottom nav fragment: ${e.message}", e)
        }
    }

    /**
     * Şehir detay ekranına geçiş için özel metot
     *
     * @param cityId Şehir ID'si
     * @return Gösterilen fragment
     */
    fun navigateToCityDetail(cityId: String): Fragment {
        Log.d(TAG, "Navigating to city detail: $cityId")
        val fragment = CityDetailFragment().apply {
            arguments = Bundle().apply {
                putString("cityId", cityId)
            }
        }

        val tag = "$TAG_CITY_DETAIL_PREFIX$cityId"
        return showFragment(fragment, true, tag)
    }

    /**
     * Kategori listesi ekranına geçiş için özel metot
     *
     * @param categoryId Kategori ID'si
     * @return Gösterilen fragment
     */
    fun navigateToCategoryList(categoryId: String): Fragment {
        Log.d(TAG, "Navigating to category list: $categoryId")
        val fragment = CategoryListFragment().apply {
            arguments = Bundle().apply {
                putString("categoryId", categoryId)
            }
        }

        val tag = "$TAG_CATEGORY_LIST_PREFIX$categoryId"
        return showFragment(fragment, true, tag)
    }

    /**
     * Login ekranını göstermek için
     */
    fun showLoginFragment() {
        Log.d(TAG, "Showing login fragment")
        try {
            val loginFragment = LoginFragment()

            // Backstack'i temizle
            backStackManager.clearBackStack()

            // Login fragment'ini göster
            fragmentManager.commit {
                // Login ekranı soldan gelsin
                setCustomAnimations(Animations.SLIDE_IN_LEFT, Animations.SLIDE_OUT_RIGHT)
                replace(containerId, loginFragment, TAG_LOGIN)
                setReorderingAllowed(true)
            }

            // Aktif fragment'i güncelle
            activeFragment = loginFragment
        } catch (e: Exception) {
            Log.e(TAG, "Error showing login fragment: ${e.message}", e)
        }
    }

    /**
     * Fragment'i geri tuşu yönetimiyle göster
     *
     * @param fragmentInfo Önceki fragment bilgisi
     */
    fun showFragmentOnBackPressed(fragmentInfo: BackStackManager.FragmentInfo) {
        try {
            Log.d(TAG, "Showing fragment on back pressed: ${fragmentInfo.fragment.javaClass.simpleName}")
            val previousFragment = fragmentInfo.fragment
            val previousId = fragmentInfo.tabId

            // Transaction'ı hazırla
            fragmentManager.commit {
                // Geri tuşunda her zaman sağdan sola geçiş animasyonu kullan
                setCustomAnimations(Animations.SLIDE_IN_LEFT, Animations.SLIDE_OUT_RIGHT)

                // Şu anki fragment'i gizle
                activeFragment?.let { hide(it) }

                // Önceki fragment'i göster
                show(previousFragment)
                setReorderingAllowed(true)
            }

            // Aktif fragment'i ve tab'ı güncelle
            activeFragment = previousFragment
            activeTabId = previousId
        } catch (e: Exception) {
            Log.e(TAG, "Error showing fragment on back press: ${e.message}", e)
        }
    }

    /**
     * Aktif fragment'i döndürür
     * @return Aktif fragment veya null
     */
    fun getActiveFragment(): Fragment? = activeFragment

    /**
     * Aktif tab ID'sini döndürür
     * @return Aktif tab ID'si
     */
    fun getActiveTabId(): Int = activeTabId

    /**
     * Aktif tab ID'sini günceller
     * @param tabId Yeni tab ID'si
     */
    fun setActiveTabId(tabId: Int) {
        Log.d(TAG, "Setting active tab ID: $tabId")
        activeTabId = tabId
    }

    fun setActiveFragment(fragment: Fragment) {
        activeFragment = fragment
    }

    /**
     * Animasyon tipleri
     */
    enum class AnimationType {
        SLIDE_HORIZONTAL,
        FADE
    }
}