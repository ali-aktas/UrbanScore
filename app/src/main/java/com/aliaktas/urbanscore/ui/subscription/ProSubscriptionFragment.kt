package com.aliaktas.urbanscore.ui.subscription

import android.animation.Animator
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.aliaktas.urbanscore.MainActivity
import com.aliaktas.urbanscore.R
import com.aliaktas.urbanscore.databinding.FragmentProSubscriptionBinding
import com.aliaktas.urbanscore.util.RevenueCatManager
import com.google.android.material.snackbar.Snackbar
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class ProSubscriptionFragment : Fragment() {

    private var _binding: FragmentProSubscriptionBinding? = null
    private val binding get() = _binding!!

    private val viewModel: ProSubscriptionViewModel by viewModels()
    private val TAG = "ProSubscriptionFragment"

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
        Log.d(TAG, "Fragment oluşturuldu")

        setupUI()
        observeViewModel()

        // Başlangıçta verileri yenileme
        viewModel.refreshSubscriptionData()
    }

    private fun setupUI() {
        Log.d(TAG, "UI ayarları yapılıyor")

        // Geri butonu
        binding.btnBack.setOnClickListener {
            Log.d(TAG, "Geri butonu tıklandı")
            (requireActivity() as MainActivity).handleBackPressed()
        }

        // Plan seçim butonları
        binding.cardMonthlyPlan.setOnClickListener {
            Log.d(TAG, "Aylık plan seçildi")
            viewModel.selectPackage(RevenueCatManager.PLAN_MONTHLY)
            updateSubscriptionSelection(RevenueCatManager.PLAN_MONTHLY)
        }

        binding.cardYearlyPlan.setOnClickListener {
            Log.d(TAG, "Yıllık plan seçildi")
            viewModel.selectPackage(RevenueCatManager.PLAN_YEARLY)
            updateSubscriptionSelection(RevenueCatManager.PLAN_YEARLY)
        }

        // Satın alma butonu
        binding.btnSubscribe.setOnClickListener {
            Log.d(TAG, "Satın al butonu tıklandı")
            viewModel.purchaseSelectedPlan(requireActivity())
        }

        // Geri yükleme butonları
        binding.btnRestorePurchases.setOnClickListener {
            Log.d(TAG, "Geri yükleme butonu tıklandı")
            viewModel.restorePurchases()
        }

        binding.btnRestoreProPurchases.setOnClickListener {
            Log.d(TAG, "Pro geri yükleme butonu tıklandı")
            viewModel.restorePurchases()
        }

        // Abonelik yönetim butonu
        binding.btnManageSubscription.setOnClickListener {
            Log.d(TAG, "Abonelik yönetim butonu tıklandı")
            viewModel.openSubscriptionManagement(requireActivity())
        }

        // Gizlilik politikası
        binding.tvPrivacyPolicy.setOnClickListener {
            Log.d(TAG, "Gizlilik politikası linki tıklandı")
            try {
                val intent = Intent(Intent.ACTION_VIEW).apply {
                    data = Uri.parse("https://raw.githubusercontent.com/ali-aktas/travelr-privacy-policy/refs/heads/main/TravlR%20Privacy%20Policy.txt")
                }
                startActivity(intent)
            } catch (e: Exception) {
                Log.e(TAG, "Gizlilik politikası açılamadı", e)
                showSnackbar("Privacy policy could not be opened")
            }
        }
    }

    private fun observeViewModel() {
        Log.d(TAG, "ViewModel gözlemleniyor")

        viewLifecycleOwner.lifecycleScope.launch {
            // Kullanıcı mesajlarını gözlemle
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch {
                    viewModel.userMessage.collect { message ->
                        showSnackbar(message)
                    }
                }
            }
        }

        viewLifecycleOwner.lifecycleScope.launch {
            // UI state'i gözlemle
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch {
                    viewModel.uiState.collect { state ->
                        Log.d(TAG, "UI state değişti: ${state.javaClass.simpleName}")
                        updateUI(state)
                    }
                }
            }
        }

        // Seçili paket ID'sini gözlemleme
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch {
                    viewModel.selectedPackageId.collect { packageId ->
                        Log.d(TAG, "Seçili paket değişti: $packageId")
                        updateSubscriptionSelection(packageId)
                    }
                }
            }
        }
    }

    private fun updateUI(state: SubscriptionUIState) {
        when (state) {
            is SubscriptionUIState.Loading -> {
                Log.d(TAG, "Loading state gösteriliyor")
                showLoading(true)
                hideAllContentLayouts()
            }

            is SubscriptionUIState.PremiumActive -> {
                Log.d(TAG, "Premium active state gösteriliyor")
                showLoading(false)
                showActiveSubscription()
            }

            is SubscriptionUIState.ReadyForPurchase -> {
                Log.d(TAG, "Ready for purchase state gösteriliyor")
                showLoading(false)
                showSubscriptionOptions()
                updatePackagesInfo(state)
            }

            is SubscriptionUIState.PackagesUnavailable -> {
                Log.d(TAG, "Packages unavailable state gösteriliyor")
                showLoading(false)
                showSubscriptionOptions()
                showSnackbar("Abonelik seçenekleri yüklenemedi. Lütfen internet bağlantınızı kontrol edin ve tekrar deneyin.")

                // Butonları devre dışı bırak
                binding.btnSubscribe.isEnabled = false
                binding.cardMonthlyPlan.isEnabled = false
                binding.cardYearlyPlan.isEnabled = false
            }
        }
    }

    private fun updatePackagesInfo(state: SubscriptionUIState.ReadyForPurchase) {
        Log.d(TAG, "Paket bilgileri güncelleniyor")
        // identifier yerine product.identifier kullanımı
        Log.d(TAG, "Aylık paket: ${state.monthlyPackage?.packageType ?: "Bulunamadı"}")
        Log.d(TAG, "Yıllık paket: ${state.yearlyPackage?.packageType ?: "Bulunamadı"}")

        // Aylık fiyat
        state.monthlyPackage?.let { pkg ->
            binding.tvMonthlyPrice.text = pkg.product.price.formatted
            binding.cardMonthlyPlan.isEnabled = true
        } ?: run {
            binding.tvMonthlyPrice.text = "Kullanılamıyor"
            binding.cardMonthlyPlan.isEnabled = false
        }

        // Yıllık fiyat
        state.yearlyPackage?.let { pkg ->
            binding.tvYearlyPrice.text = pkg.product.price.formatted
            binding.cardYearlyPlan.isEnabled = true

            // Tasarruf hesaplama
            val savingsPercent = viewModel.calculateSavingsPercentage()
            if (savingsPercent > 0) {
                binding.tvSavings.visibility = View.VISIBLE
                binding.tvSavings.text = " (Save $savingsPercent%)"
            } else {
                binding.tvSavings.visibility = View.GONE
            }
        } ?: run {
            binding.tvYearlyPrice.text = "Not available"
            binding.cardYearlyPlan.isEnabled = false
            binding.tvSavings.visibility = View.GONE
        }

        // Satın alma butonu
        binding.btnSubscribe.setOnClickListener {
            Log.d(TAG, "Satın al butonu tıklandı")

            // Lottie animasyonunu başlat
            binding.btnSubscribe.playAnimation()

            // Continue Butonu
            binding.btnSubscribe.setOnClickListener {
                Log.d(TAG, "Satın al butonu tıklandı")

                // Direkt olarak satın alma işlemini başlat
                viewModel.purchaseSelectedPlan(requireActivity())
            }
        }
    }

    private fun updateSubscriptionSelection(packageId: String) {
        Log.d(TAG, "Abonelik seçimi güncelleniyor: $packageId")

        // Renk ID'leri
        val selectedStrokeColor = R.color.primary_purple
        val unselectedStrokeColor = R.color.primary_gray

        // Arka plan renkleri için drawable
        val selectedBackground = R.drawable.profilelistbg
        val transparentBackground = android.R.color.transparent

        // Aylık plan kartı güncelleme
        binding.cardMonthlyPlan.apply {
            strokeColor = ContextCompat.getColor(
                requireContext(),
                if (packageId == RevenueCatManager.PLAN_MONTHLY) selectedStrokeColor else unselectedStrokeColor
            )

            // Arka planı güncelle
            if (packageId == RevenueCatManager.PLAN_MONTHLY) {
                setCardBackgroundColor(ContextCompat.getColor(requireContext(), transparentBackground))
                background = ContextCompat.getDrawable(requireContext(), selectedBackground)
            } else {
                setCardBackgroundColor(ContextCompat.getColor(requireContext(), transparentBackground))
                background = null
            }
        }

        // Yıllık plan kartı güncelleme
        binding.cardYearlyPlan.apply {
            strokeColor = ContextCompat.getColor(
                requireContext(),
                if (packageId == RevenueCatManager.PLAN_YEARLY) selectedStrokeColor else unselectedStrokeColor
            )

            // Arka planı güncelle
            if (packageId == RevenueCatManager.PLAN_YEARLY) {
                setCardBackgroundColor(ContextCompat.getColor(requireContext(), transparentBackground))
                background = ContextCompat.getDrawable(requireContext(), selectedBackground)
            } else {
                setCardBackgroundColor(ContextCompat.getColor(requireContext(), transparentBackground))
                background = null
            }
        }

        // Radio butonları güncelle (görünür olmasalar bile state'i doğru tutmak için)
        binding.radioMonthly.isChecked = packageId == RevenueCatManager.PLAN_MONTHLY
        binding.radioYearly.isChecked = packageId == RevenueCatManager.PLAN_YEARLY
    }

    private fun showLoading(isLoading: Boolean) {
        binding.progressBar.visibility = if (isLoading) View.VISIBLE else View.GONE
    }

    private fun hideAllContentLayouts() {
        binding.subscriptionOptionsLayout.visibility = View.GONE
        binding.proStatusLayout.visibility = View.GONE
    }

    private fun showSubscriptionOptions() {
        binding.subscriptionOptionsLayout.visibility = View.VISIBLE
        binding.proStatusLayout.visibility = View.GONE
    }

    private fun showActiveSubscription() {
        // Premium UI bileşenlerini gizle
        binding.tvProTitle.visibility = View.GONE
        binding.tvProDescription.visibility = View.GONE
        binding.headerLabels.visibility = View.GONE
        binding.tvProText.visibility = View.GONE
        binding.tvFreeText.visibility = View.GONE

        // Abonelik seçeneklerini gizle
        binding.subscriptionOptionsLayout.visibility = View.GONE

        // Pro durumu UI'ını göster
        binding.proStatusLayout.visibility = View.VISIBLE

        // Bitiş tarihini al
        viewModel.getExpiryDate { expiryDate ->
            if (expiryDate != null) {
                binding.tvExpiryDate.text = "Your subscription expires on $expiryDate"
            } else {
                binding.tvExpiryDate.text = "Active subscription"
            }
        }
    }

    private fun showSnackbar(message: String) {
        Log.d(TAG, "Snackbar gösteriliyor: $message")
        Snackbar.make(binding.root, message, Snackbar.LENGTH_LONG).show()
    }

    override fun onResume() {
        super.onResume()
        Log.d(TAG, "Fragment resume oldu, verileri yenileme")

        // Abonelik durumunu yenile
        viewModel.refreshSubscriptionData()

        // Eğer aktif abonelik varsa bitiş tarihini güncelle
        if (binding.proStatusLayout.visibility == View.VISIBLE) {
            viewModel.getExpiryDate { expiryDate ->
                if (expiryDate != null) {
                    binding.tvExpiryDate.text = "Your subscription expires on $expiryDate"
                }
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        Log.d(TAG, "Fragment destroy oldu")
        _binding = null
    }
}