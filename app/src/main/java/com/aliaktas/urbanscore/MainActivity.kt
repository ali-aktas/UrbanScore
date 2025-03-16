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
import androidx.fragment.app.FragmentManager
import com.aliaktas.urbanscore.databinding.ActivityMainBinding
import com.aliaktas.urbanscore.ui.allcities.AllCitiesFragment
import com.aliaktas.urbanscore.ui.auth.LoginFragment
import com.aliaktas.urbanscore.ui.categories.CategoryListFragment
import com.aliaktas.urbanscore.ui.detail.CityDetailFragment
import com.aliaktas.urbanscore.ui.home.HomeFragment
import com.aliaktas.urbanscore.ui.profile.ProfileFragment
import com.aliaktas.urbanscore.ui.search.ExploreFragment
import dagger.hilt.android.AndroidEntryPoint
import java.util.Stack

@AndroidEntryPoint
class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding

    // Bottom navigation fragment'leri
    private val homeFragment by lazy { HomeFragment() }
    private val exploreFragment by lazy { ExploreFragment() }
    private val allCitiesFragment by lazy { AllCitiesFragment() }
    private val profileFragment by lazy { ProfileFragment() }

    // Aktif fragment'i takip etmek için
    private var activeFragment: Fragment? = null
    private var activeTabId = R.id.homeFragment

    // Backstack yönetimi
    private val fragmentBackStack = Stack<FragmentInfo>()

    // Bottom navigation menu item'ları
    private val bottomNavItems = setOf(
        R.id.homeFragment,
        R.id.exploreFragment,
        R.id.allCitiesFragment,
        R.id.profileFragment
    )

    // Bottom navigation'ın görünür olması gereken fragment listesi
    private val bottomNavVisibleFragments = setOf(
        HomeFragment::class.java,
        ExploreFragment::class.java,
        AllCitiesFragment::class.java,
        ProfileFragment::class.java,
        CityDetailFragment::class.java,   // Şehir detay ekranlarında görünür olsun
        CategoryListFragment::class.java  // Kategori listelerinde görünür olsun
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

        setupBottomNavigation()
        setupBackButtonHandling()

        // Giriş durumuna göre başlangıç fragment'ini göster
        val isLoggedIn = intent.getBooleanExtra("isLoggedIn", false)

        if (savedInstanceState == null) {
            if (isLoggedIn) {
                // Giriş yapmışsa ana sayfayı göster
                showBottomNavFragment(R.id.homeFragment)
            } else {
                // Giriş yapmamışsa login ekranını göster
                showLoginFragment()
            }
        } else {
            // Ekran döndürme gibi durumlarda state'i geri yükle
            activeTabId = savedInstanceState.getInt("activeTabId", R.id.homeFragment)

            if (isLoggedIn) {
                showBottomNavFragment(activeTabId)
            } else {
                showLoginFragment()
            }
        }
    }

    private fun setupBottomNavigation() {
        binding.bottomNavigation.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.homeFragment,
                R.id.exploreFragment,
                R.id.allCitiesFragment,
                R.id.profileFragment -> {
                    // Aynı tab'a tıklandıysa bir şey yapma
                    if (item.itemId != activeTabId) {
                        // Back stack'i temizleyelim
                        clearBackStackUntilTab()

                        showBottomNavFragment(item.itemId)
                    }
                    true
                }
                else -> false
            }
        }

        binding.bottomNavigation.setOnItemReselectedListener { /* Hiçbir şey yapma */ }
    }

    fun showLoginFragment() {
        // Bottom navigation'ı gizle
        binding.bottomNavigation.visibility = View.GONE

        // Backstack'i temizle
        fragmentBackStack.clear()

        // Login fragment'ini göster
        val loginFragment = LoginFragment()
        supportFragmentManager.beginTransaction()
            .setCustomAnimations(
                R.anim.fade_scale_in,
                R.anim.fade_scale_out
            )
            .replace(R.id.fragment_container, loginFragment, "LOGIN_FRAGMENT")
            .commit()

        // Aktif fragment'i güncelle
        activeFragment = loginFragment
    }

    private fun showBottomNavFragment(menuItemId: Int) {
        // Bottom navigation'ı göster
        binding.bottomNavigation.visibility = View.VISIBLE

        // Aktif tab'ı güncelle
        activeTabId = menuItemId
        binding.bottomNavigation.menu.findItem(menuItemId)?.isChecked = true

        // İlgili fragment'i al
        val fragmentToShow = getFragmentForMenuItem(menuItemId)

        // Eğer fragment zaten aktifse, bir şey yapma
        if (fragmentToShow == activeFragment) return

        // Transaction başlat
        val transaction = supportFragmentManager.beginTransaction()

        // Animasyonları ayarla
        transaction.setCustomAnimations(
            R.anim.fade_scale_in,
            R.anim.fade_scale_out
        )

        // Aktif fragment'i gizle
        if (activeFragment != null) {
            transaction.hide(activeFragment!!)
        }

        // Yeni fragment ekle veya göster
        if (fragmentToShow.isAdded) {
            transaction.show(fragmentToShow)
        } else {
            transaction.add(R.id.fragment_container, fragmentToShow, getTagForMenuItem(menuItemId))
        }

        // Transaction'ı tamamla (backstack'e ekleme!)
        transaction.commit()

        // Aktif fragment'i güncelle
        activeFragment = fragmentToShow
    }

    private fun getFragmentForMenuItem(menuItemId: Int): Fragment {
        return when (menuItemId) {
            R.id.homeFragment -> homeFragment
            R.id.exploreFragment -> exploreFragment
            R.id.allCitiesFragment -> allCitiesFragment
            R.id.profileFragment -> profileFragment
            else -> homeFragment // Varsayılan olarak ana sayfaya dön
        }
    }

    private fun getTagForMenuItem(menuItemId: Int): String {
        return when (menuItemId) {
            R.id.homeFragment -> "HOME_FRAGMENT"
            R.id.exploreFragment -> "EXPLORE_FRAGMENT"
            R.id.allCitiesFragment -> "ALL_CITIES_FRAGMENT"
            R.id.profileFragment -> "PROFILE_FRAGMENT"
            else -> "UNKNOWN_FRAGMENT"
        }
    }

    // Fragment yönetimi için public API
    private fun navigateToFragment(fragment: Fragment, addToBackStack: Boolean = true) {
        try {
            // Log fragment navigation
            Log.d("MainActivity", "Navigating to: ${fragment.javaClass.simpleName}")

            // Önce aktif fragment ve tab durumunu kaydet
            if (addToBackStack && activeFragment != null) {
                fragmentBackStack.push(FragmentInfo(activeFragment!!, activeTabId))
            }

            // Transaction'ı hazırla
            val transaction = supportFragmentManager.beginTransaction()
                .setCustomAnimations(
                    R.anim.slide_in_right,
                    R.anim.slide_out_left,
                    R.anim.slide_in_left,
                    R.anim.slide_out_right
                )

            // Şu anki fragment'i gizle
            activeFragment?.let { transaction.hide(it) }

            // Detay fragment'ları için özel tag oluştur
            val fragmentTag = when (fragment) {
                is CityDetailFragment -> {
                    val cityId = fragment.arguments?.getString("cityId") ?: "unknown"
                    "CITY_DETAIL_${cityId}"
                }
                is CategoryListFragment -> {
                    val categoryId = fragment.arguments?.getString("categoryId") ?: "unknown"
                    "CATEGORY_LIST_${categoryId}"
                }
                else -> fragment.javaClass.simpleName
            }

            // Var olan fragment'i bul veya yeni ekle
            val existingFragment = supportFragmentManager.findFragmentByTag(fragmentTag)

            if (existingFragment != null && existingFragment.isAdded) {
                transaction.show(existingFragment)
                activeFragment = existingFragment
            } else {
                transaction.add(R.id.fragment_container, fragment, fragmentTag)
                activeFragment = fragment
            }

            // Bottom nav görünürlüğünü ayarla
            updateBottomNavVisibility(fragment)

            // Transaction'ı tamamla
            transaction.commit()
        } catch (e: Exception) {
            Log.e("MainActivity", "Navigation error: ${e.message}", e)
        }
    }

    // Bottom navigation görünürlüğünü güncelle
    private fun updateBottomNavVisibility(fragment: Fragment) {
        val shouldShowBottomNav = bottomNavVisibleFragments.contains(fragment::class.java)
        binding.bottomNavigation.visibility = if (shouldShowBottomNav) View.VISIBLE else View.GONE
    }

    // Şehir detay ekranına geçiş için özel metot
    fun navigateToCityDetail(cityId: String) {
        // Her seferinde yeni bir fragment instance'ı oluştur
        val cityDetailFragment = CityDetailFragment().apply {
            arguments = Bundle().apply {
                putString("cityId", cityId)
            }
        }
        navigateToFragment(cityDetailFragment)
    }

    // Kategori detay ekranına geçiş için özel metot
    fun navigateToCategoryList(categoryId: String) {
        // Her seferinde yeni bir fragment instance'ı oluştur
        val categoryListFragment = CategoryListFragment().apply {
            arguments = Bundle().apply {
                putString("categoryId", categoryId)
            }
        }
        navigateToFragment(categoryListFragment)
    }

    // Backstack'i belirli bir tab'a kadar temizle
    private fun clearBackStackUntilTab() {
        while (fragmentBackStack.isNotEmpty()) {
            val fragmentInfo = fragmentBackStack.peek()
            if (fragmentInfo.fragmentId in bottomNavItems) {
                break
            }
            fragmentBackStack.pop()
        }
    }

    private fun setupBackButtonHandling() {
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                // Backstack boş değilse, bir önceki fragment'e dön
                if (fragmentBackStack.isNotEmpty()) {
                    val previousFragmentInfo = fragmentBackStack.pop()
                    val previousFragment = previousFragmentInfo.fragment
                    val previousId = previousFragmentInfo.fragmentId

                    // Transaction'ı hazırla
                    val transaction = supportFragmentManager.beginTransaction()
                        .setCustomAnimations(
                            R.anim.slide_in_left,
                            R.anim.slide_out_right
                        )

                    // Şu anki fragment'i gizle
                    activeFragment?.let { transaction.hide(it) }

                    // Önceki fragment'i göster
                    transaction.show(previousFragment)

                    // Bottom nav görünürlüğü ve seçimi güncelle
                    val isBottomNavFragment = previousId in bottomNavItems
                    updateBottomNavVisibility(previousFragment)

                    if (isBottomNavFragment) {
                        binding.bottomNavigation.selectedItemId = previousId
                        activeTabId = previousId
                    }

                    // Transaction'ı tamamla
                    transaction.commit()

                    // Aktif fragment'i güncelle
                    activeFragment = previousFragment
                } else {
                    // Backstack boşsa ve ana sayfada değilsek, ana sayfaya dön
                    if (activeTabId != R.id.homeFragment) {
                        showBottomNavFragment(R.id.homeFragment)
                    } else {
                        // Ana sayfadaysak, uygulamadan çık
                        finish()
                    }
                }
            }
        })
    }

    // Login ekranından ana sayfaya gitmek için yardımcı metot
    fun navigateToHomeAfterLogin() {
        fragmentBackStack.clear() // Backstack'i temizle
        showBottomNavFragment(R.id.homeFragment)
    }

    // State'i kaydet
    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putInt("activeTabId", activeTabId)
    }

    // Fragment bilgilerini saklamak için yardımcı sınıf
    private data class FragmentInfo(
        val fragment: Fragment,
        val fragmentId: Int
    )
}