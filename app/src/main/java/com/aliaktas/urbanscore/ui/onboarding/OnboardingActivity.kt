package com.aliaktas.urbanscore.ui.onboarding

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.viewpager2.widget.ViewPager2 // ViewPager2 importu
import com.aliaktas.urbanscore.MainActivity
import com.aliaktas.urbanscore.R
import com.aliaktas.urbanscore.databinding.ActivityOnboardingBinding // Oluşturulan Binding sınıfı
import com.aliaktas.urbanscore.util.AppPreferences
import com.google.android.material.tabs.TabLayoutMediator
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class OnboardingActivity : AppCompatActivity() {

    private lateinit var binding: ActivityOnboardingBinding // Binding değişkeni
    private lateinit var onboardingAdapter: OnboardingAdapter

    @Inject
    lateinit var appPreferences: AppPreferences

    private val onboardingImageResources = listOf(
        R.drawable.roamly_onboarding_screen_1,
        R.drawable.roamly_onboarding_screen_2,
        R.drawable.roamly_onboarding_screen_3
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Layout'u binding ile inflate et ve contentView olarak ayarla
        binding = ActivityOnboardingBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupViewPager()
        setupButtons()
    }

    private fun setupViewPager() {
        onboardingAdapter = OnboardingAdapter(this, onboardingImageResources)
        // ViewPager2'ye binding üzerinden erişim
        binding.viewPagerOnboarding.adapter = onboardingAdapter

        // TabLayout'a binding üzerinden erişim
        TabLayoutMediator(binding.tabLayoutDots, binding.viewPagerOnboarding) { tab, position ->
            // Nokta göstergeleri için özel bir stil gerekmiyorsa bu kısım boş kalabilir.
        }.attach()

        // ViewPager2'ye binding üzerinden erişim
        binding.viewPagerOnboarding.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
                super.onPageSelected(position)
                if (position == onboardingImageResources.size - 1) {
                    // buttonNext'e binding üzerinden erişim
                    binding.buttonNext.setText(R.string.onboarding_get_started)
                } else {
                    binding.buttonNext.setText(R.string.onboarding_next)
                }
            }
        })
    }

    private fun setupButtons() {
        // buttonNext'e binding üzerinden erişim
        binding.buttonNext.setOnClickListener {
            val currentItem = binding.viewPagerOnboarding.currentItem // binding üzerinden erişim
            if (currentItem < onboardingImageResources.size - 1) {
                binding.viewPagerOnboarding.currentItem = currentItem + 1 // binding üzerinden erişim
            } else {
                navigateToMainApp()
            }
        }

        // buttonSkip'e binding üzerinden erişim
        binding.buttonSkip.setOnClickListener {
            navigateToMainApp()
        }
    }

    private fun navigateToMainApp() {
        appPreferences.setOnboardingSeen(true)
        val intent = Intent(this, MainActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        finish()
    }
}
