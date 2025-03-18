package com.aliaktas.urbanscore.ui.subscription

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.aliaktas.urbanscore.MainActivity
import com.aliaktas.urbanscore.databinding.FragmentProSubscriptionBinding
import com.aliaktas.urbanscore.util.PreferenceManager
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

/**
 * Pro abonelik ekranı.
 * RevenueCat ile asıl entegrasyon yapılana kadar basit bir UI gösterir.
 */
@AndroidEntryPoint
class ProSubscriptionFragment : Fragment() {

    private var _binding: FragmentProSubscriptionBinding? = null
    private val binding get() = _binding!!

    @Inject
    lateinit var preferenceManager: PreferenceManager

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentProSubscriptionBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupUI()
        setupListeners()
    }

    private fun setupUI() {
        // Pro durumunu kontrol et ve UI'ı güncelle
        updateProStatus()

        // Geçici fiyat gösterimi
        binding.tvMonthlyPrice.text = "$2.99"
        binding.tvYearlyPrice.text = "$19.99"
    }

    private fun setupListeners() {
        // Geri butonu
        binding.btnBack.setOnClickListener {
            (requireActivity() as MainActivity).handleBackPressed()
        }

        // Aylık abonelik butonu - geçici olarak Pro yapacak
        binding.btnMonthlySubscription.setOnClickListener {
            purchaseMonthlySubscription()
        }

        // Yıllık abonelik butonu - geçici olarak Pro yapacak
        binding.btnYearlySubscription.setOnClickListener {
            purchaseYearlySubscription()
        }

        // Aboneliği yenileme butonu
        binding.btnRenewSubscription.setOnClickListener {
            if (preferenceManager.isProUser()) {
                Toast.makeText(
                    requireContext(),
                    "Your subscription is still active",
                    Toast.LENGTH_SHORT
                ).show()
            } else {
                showSubscriptionOptions()
            }
        }
    }

    private fun updateProStatus() {
        val isPro = preferenceManager.isProUser()

        if (isPro) {
            // Pro kullanıcı, abonelik durumunu göster
            binding.proStatusLayout.visibility = View.VISIBLE
            binding.subscriptionOptionsLayout.visibility = View.GONE

            // Abonelik bitiş zamanını göster (geçici olarak +30 gün)
            val expiryTime = preferenceManager.getProExpiryTime()
            if (expiryTime > 0) {
                val expiryDate = java.text.SimpleDateFormat(
                    "dd MMM yyyy", java.util.Locale.getDefault()
                ).format(java.util.Date(expiryTime))
                binding.tvExpiryDate.text = "Your subscription expires on $expiryDate"
            }
        } else {
            // Normal kullanıcı, abonelik seçeneklerini göster
            binding.proStatusLayout.visibility = View.GONE
            binding.subscriptionOptionsLayout.visibility = View.VISIBLE
        }
    }

    private fun showSubscriptionOptions() {
        binding.proStatusLayout.visibility = View.GONE
        binding.subscriptionOptionsLayout.visibility = View.VISIBLE
    }

    /**
     * Geçici olarak aylık abonelik satın alımı simülasyonu.
     * Gerçek implementasyonda RevenueCat ile değiştirilecek.
     */
    private fun purchaseMonthlySubscription() {
        // PRO durumunu aktifleştir
        preferenceManager.setProUser(true)

        // Geçici bitiş tarihi (30 gün sonra)
        val expiryTime = System.currentTimeMillis() + (30 * 24 * 60 * 60 * 1000L)
        preferenceManager.setProExpiryTime(expiryTime)

        Toast.makeText(
            requireContext(),
            "Successfully subscribed to Pro! (Test Mode)",
            Toast.LENGTH_SHORT
        ).show()

        updateProStatus()
    }

    /**
     * Geçici olarak yıllık abonelik satın alımı simülasyonu.
     * Gerçek implementasyonda RevenueCat ile değiştirilecek.
     */
    private fun purchaseYearlySubscription() {
        // PRO durumunu aktifleştir
        preferenceManager.setProUser(true)

        // Geçici bitiş tarihi (365 gün sonra)
        val expiryTime = System.currentTimeMillis() + (365 * 24 * 60 * 60 * 1000L)
        preferenceManager.setProExpiryTime(expiryTime)

        Toast.makeText(
            requireContext(),
            "Successfully subscribed to Pro (Yearly)! (Test Mode)",
            Toast.LENGTH_SHORT
        ).show()

        updateProStatus()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}