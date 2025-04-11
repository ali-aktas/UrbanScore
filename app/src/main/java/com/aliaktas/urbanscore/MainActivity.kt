package com.aliaktas.urbanscore

import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.View
import androidx.activity.OnBackPressedCallback
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import com.aliaktas.urbanscore.ads.AdManager
import com.aliaktas.urbanscore.databinding.ActivityMainBinding
import com.aliaktas.urbanscore.navigation.BackStackManager
import com.aliaktas.urbanscore.navigation.BottomNavigationManager
import com.aliaktas.urbanscore.navigation.NavigationManager
import com.aliaktas.urbanscore.ui.auth.ForgotPasswordFragment
import com.aliaktas.urbanscore.ui.auth.LoginFragment
import com.aliaktas.urbanscore.ui.auth.RegisterFragment
import com.aliaktas.urbanscore.ui.categories.CategoryListFragment
import com.aliaktas.urbanscore.ui.detail.CityDetailFragment
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

    /**
     * Standart navigasyon yardımcı metodu.
     * Tüm navigasyon işlemleri bu metot üzerinden yapılmalıdır.
     *
     * @param fragment Gösterilecek fragment
     * @param tag Fragment için tag (null geçilirse fragment sınıf adı kullanılır)
     * @param addToBackStack Fragment'ı backstack'e ekleyip eklemeyeceği
     * @return Gösterilen fragment örneği
     */
    private fun navigateTo(
        fragment: Fragment,
        tag: String? = null,
        addToBackStack: Boolean = true
    ): Fragment {
        try {
            Log.d(TAG, "Navigating to ${fragment.javaClass.simpleName}, tag=$tag, addToBackStack=$addToBackStack")

            // Fragment'ı göster
            val displayedFragment = navigationManager.showFragment(fragment, addToBackStack, tag)

            // Bottom nav görünürlüğünü güncelle
            bottomNavigationManager.updateBottomNavVisibility(displayedFragment)

            return displayedFragment
        } catch (e: Exception) {
            Log.e(TAG, "Navigation error in navigateTo: ${e.message}", e)
            // Kullanıcıya hata gösterme işlemi buraya eklenebilir
            return fragment
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

    /**
     * Fragment'lerdeki UI geri butonları için kullanılacak yardımcı metot
     */
    fun handleBackPressed() {
        Log.d(TAG, "Manual back pressed triggered")
        onBackPressedDispatcher.onBackPressed()
    }

    fun showLoginFragment() {
        try {
            // Bottom navigation'ı gizle
            bottomNavigationManager.hideBottomNavigation()

            // Önemli: tüm fragmentları kaldır ve backstack'i sıfırla
            supportFragmentManager.popBackStackImmediate(null, FragmentManager.POP_BACK_STACK_INCLUSIVE)

            // Yeni bir LoginFragment ekle
            val loginFragment = LoginFragment()
            supportFragmentManager.beginTransaction()
                .replace(R.id.fragment_container, loginFragment, "LOGIN_FRAGMENT")
                .commit()

            // NavigationManager'ı güncelle
            navigationManager.setActiveFragment(loginFragment)
        } catch (e: Exception) {
            Log.e(TAG, "Error showing login fragment: ${e.message}", e)
        }
    }

    fun navigateToCityDetail(cityId: String) {
        try {
            Log.d(TAG, "Navigating to city detail: $cityId")
            val fragment = CityDetailFragment().apply {
                arguments = Bundle().apply {
                    putString("cityId", cityId)
                }
            }
 
            // Yeni kullanıcı girişinden sonra ilk kez CityDetail'e gidildiğinde
            // Home fragment'ini backstack'e ekleyelim
            if (backStackManager.isBackStackEmpty()) {
                // Home fragment'i backstack'e ekle ki, işlemlerden sonra geri dönecek yer olsun
                val homeFragment = HomeFragment()
                backStackManager.pushToBackStack(homeFragment, R.id.homeFragment)
                Log.d(TAG, "Empty backstack detected, adding home to backstack")
            }

            val tag = "CITY_DETAIL_$cityId"
            navigationManager.showFragment(fragment, true, tag)
        } catch (e: Exception) {
            Log.e(TAG, "Navigation error: ${e.message}", e)
        }
    }

    /**
     * Kategori listesi ekranına geçiş için public API
     */
    fun navigateToCategoryList(categoryId: String) {
        val fragment = CategoryListFragment().apply {
            arguments = Bundle().apply {
                putString("categoryId", categoryId)
            }
        }
        val tag = "CATEGORY_LIST_$categoryId"
        navigateTo(fragment, tag, true)
    }


    fun navigateToHomeAfterLogin() {
        try {
            // Bottom navigation'ı görünür yap - BU SATIR EKSİKTİ!
            bottomNavigationManager.showBottomNavigation()

            // Start with a synthetic "home" backstack entry
            val homeFragment = HomeFragment()
            navigationManager.showBottomNavFragment(homeFragment, R.id.homeFragment, "HOME_FRAGMENT")

            // Home tab'ı seçili yapın
            binding.bottomNavigation.selectedItemId = R.id.homeFragment
        } catch (e: Exception) {
            Log.e(TAG, "Error navigating to home: ${e.message}")
        }
    }

    /**
     * Pro abonelik ekranına geçiş
     */
    fun navigateToProSubscription() {
        val proFragment = ProSubscriptionFragment()
        navigateTo(proFragment, "PRO_SUBSCRIPTION_FRAGMENT", true)
    }

    /**
     * Kayıt ekranını göster
     */
    fun showRegisterFragment() {
        val registerFragment = RegisterFragment()

        // Bottom navigation'ı gizle
        bottomNavigationManager.hideBottomNavigation()

        // Fragment'i göster
        navigateTo(registerFragment, "REGISTER_FRAGMENT", true)
    }

    /**
     * Şifre sıfırlama ekranını göster
     */
    fun showForgotPasswordFragment() {
        val forgotPasswordFragment = ForgotPasswordFragment()

        // Bottom navigation'ı gizle
        bottomNavigationManager.hideBottomNavigation()

        // Fragment'i göster
        navigateTo(forgotPasswordFragment, "FORGOT_PASSWORD_FRAGMENT", true)
    }

    // State'i kaydet
    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putInt("activeTabId", navigationManager.getActiveTabId())
    }

}