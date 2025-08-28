package com.aliaktas.urbanscore.ui.splash

import android.annotation.SuppressLint
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import androidx.appcompat.app.AppCompatActivity
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.aliaktas.urbanscore.MainActivity
import com.aliaktas.urbanscore.ads.AdManager
import com.aliaktas.urbanscore.databinding.ActivitySplashBinding
import com.aliaktas.urbanscore.ui.onboarding.OnboardingActivity
import com.aliaktas.urbanscore.util.AppPreferences
import com.google.firebase.auth.FirebaseAuth
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@SuppressLint("CustomSplashScreen")
@AndroidEntryPoint
class SplashActivity : AppCompatActivity() {

    private lateinit var binding: ActivitySplashBinding

    @Inject
    lateinit var auth: FirebaseAuth

    @Inject
    lateinit var adManager: AdManager

    @Inject
    lateinit var appPreferences: AppPreferences

    override fun onCreate(savedInstanceState: Bundle?) {
        // Splash screen'i yükle
        installSplashScreen()

        super.onCreate(savedInstanceState)
        binding = ActivitySplashBinding.inflate(layoutInflater)
        setContentView(binding.root)

        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        // Uygulama sürümünü kontrol et
        checkAppVersion()

        // UI hazır olduktan sonra GDPR ve reklam başlatma
        adManager.initialize(this) {
            // Reklamlar başlatıldı, uygulamaya devam edebiliriz
            val isUserLoggedIn = auth.currentUser != null
            val isFirstLaunch = appPreferences.isFirstLaunch

            val delayMillis = when {
                isFirstLaunch -> 3000 // İlk açılışta uzun splash
                !isUserLoggedIn -> 2500 // Oturum açılmamışsa orta uzunlukta
                else -> 2500 // Oturum açıksa kısa
            }

            // Animasyonu başlat
            binding.lottieAnimation.playAnimation()

            // Belirlenen süre sonra ana aktiviteye geç
            Handler(Looper.getMainLooper()).postDelayed({
                navigateToMainActivity()

                // İlk açılış ise, işareti güncelle
                if (isFirstLaunch) {
                    appPreferences.setFirstLaunchComplete()
                }
            }, delayMillis.toLong())
        }
    }

    private fun checkAppVersion() {
        try {
            val packageInfo = packageManager.getPackageInfo(packageName, 0)
            val currentVersion = packageInfo.versionCode
            val lastVersion = appPreferences.getCurrentVersionCode()

            // Uygulama güncellenmiş mi kontrol et
            if (currentVersion > lastVersion) {
                appPreferences.updateVersionCode(currentVersion)
                // Büyük bir güncelleme olup olmadığını kontrol edebilirsin
                // Büyük güncellemede kullanıcıya "What's new" ekranı gösterme seçeneği
            }
        } catch (e: PackageManager.NameNotFoundException) {
            e.printStackTrace()
        }
    }

    private fun navigateToMainActivity() {
        // Onboarding kontrolü ekle
        if (!appPreferences.isOnboardingSeen()) {
            // İlk kez açılıyor, onboarding göster
            val intent = Intent(this, OnboardingActivity::class.java)
            startActivity(intent)
            finish()
            overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)
            return
        }

        // Onboarding görülmüş, normal akışa devam et
        val intent = Intent(this, MainActivity::class.java)
        // Kullanıcı giriş durumunu intent'e ekle
        intent.putExtra("isLoggedIn", auth.currentUser != null)
        startActivity(intent)
        finish()

        // İsteğe bağlı: Güzel bir geçiş animasyonu
        overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)
    }
}