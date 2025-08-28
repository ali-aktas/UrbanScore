package com.aliaktas.urbanscore.ui.onboarding

import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.viewpager2.adapter.FragmentStateAdapter

class OnboardingAdapter(
    fragmentActivity: FragmentActivity,
    private val imageResources: List<Int> // Drawable resource ID'lerinin listesi
) : FragmentStateAdapter(fragmentActivity) {

    override fun getItemCount(): Int {
        return imageResources.size
    }

    override fun createFragment(position: Int): Fragment {
        // Her bir pozisyon için OnboardingImagePageFragment'ın yeni bir örneğini oluşturur
        // ve o pozisyona karşılık gelen görselin resource ID'sini fragment'a iletir.
        return OnboardingImagePageFragment.newInstance(imageResources[position])
    }
}
