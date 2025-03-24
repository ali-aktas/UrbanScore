package com.aliaktas.urbanscore.ui.subscription

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
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

        setupUI()
        observeViewModel()
    }

    private fun setupUI() {
        // Geri butonu
        binding.btnBack.setOnClickListener {
            (requireActivity() as MainActivity).handleBackPressed()
        }

        // Abonelik planı seçimi
        binding.cardMonthlyPlan.setOnClickListener {
            viewModel.selectPackage(RevenueCatManager.PLAN_MONTHLY)
            updateSubscriptionSelection(RevenueCatManager.PLAN_MONTHLY)
        }

        binding.cardYearlyPlan.setOnClickListener {
            viewModel.selectPackage(RevenueCatManager.PLAN_YEARLY)
            updateSubscriptionSelection(RevenueCatManager.PLAN_YEARLY)
        }

        // Varsayılan olarak aylık plan seçili
        updateSubscriptionSelection(RevenueCatManager.PLAN_MONTHLY)

        // Abonelik butonu
        binding.btnSubscribe.setOnClickListener {
            viewModel.purchaseSelectedPlan(requireActivity())
        }

        // Restore butonları
        binding.btnRestorePurchases.setOnClickListener {
            viewModel.restorePurchases()
        }

        binding.btnRestoreProPurchases.setOnClickListener {
            viewModel.restorePurchases()
        }

        // Abonelik yönetim butonu
        binding.btnManageSubscription.setOnClickListener {
            viewModel.openSubscriptionManagement(requireActivity())
        }

        // Gizlilik politikası
        binding.tvPrivacyPolicy.setOnClickListener {
            try {
                val intent = Intent(Intent.ACTION_VIEW).apply {
                    data = Uri.parse("https://aliaktasapp.blogspot.com/p/urbanrate-privacy-policy.html")
                }
                startActivity(intent)
            } catch (e: Exception) {
                Toast.makeText(requireContext(), "Could not open privacy policy", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun updateSubscriptionSelection(packageId: String) {
        // Kartların stroke rengini güncelle
        binding.cardMonthlyPlan.strokeColor = ContextCompat.getColor(
            requireContext(),
            if (packageId == RevenueCatManager.PLAN_MONTHLY) R.color.auth_accent else R.color.rating_color
        )

        binding.cardYearlyPlan.strokeColor = ContextCompat.getColor(
            requireContext(),
            if (packageId == RevenueCatManager.PLAN_YEARLY) R.color.auth_accent else R.color.accent
        )

        // Radyo butonları güncelle (görünmez olsalar bile state'i güncel tut)
        binding.radioMonthly.isChecked = packageId == RevenueCatManager.PLAN_MONTHLY
        binding.radioYearly.isChecked = packageId == RevenueCatManager.PLAN_YEARLY
    }

    private fun observeViewModel() {
        // UI state'i gözlemle
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.uiState.collect { state ->
                    updateUI(state)
                }
            }
        }

        // Paket bilgilerini gözlemle
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.packagesState.collect { state ->
                    updatePackageInfo(state)
                }
            }
        }
    }

    private fun updateUI(state: ProSubscriptionState) {
        when (state) {
            is ProSubscriptionState.Loading -> {
                showLoading(true)
            }
            is ProSubscriptionState.NotSubscribed -> {
                showLoading(false)
                showSubscriptionOptions()
            }
            is ProSubscriptionState.Processing -> {
                showLoading(true)
            }
            is ProSubscriptionState.SubscriptionActive -> {
                showLoading(false)
                showActiveSubscription()
            }
            is ProSubscriptionState.Error -> {
                showLoading(false)
                showError(state.message)
            }
        }
    }

    private fun updatePackageInfo(state: ProSubscriptionViewModel.PackageUIState) {
        // Aylık fiyat
        state.monthlyPackage?.let { pkg ->
            binding.tvMonthlyPrice.text = pkg.product.price.formatted
        }

        // Yıllık fiyat ve tasarruf hesaplaması
        state.yearlyPackage?.let { pkg ->
            binding.tvYearlyPrice.text = pkg.product.price.formatted

            // Tasarruf hesaplaması
            if (state.monthlyPackage != null) {
                val monthlyPrice = state.monthlyPackage.product.price.amountMicros
                val yearlyPrice = pkg.product.price.amountMicros
                val monthlyPricePerYear = monthlyPrice * 12

                val savingsPercent = if (monthlyPricePerYear > 0) {
                    ((monthlyPricePerYear - yearlyPrice) * 100.0 / monthlyPricePerYear).toInt()
                } else {
                    0
                }

                if (savingsPercent > 0) {
                    binding.tvSavings.visibility = View.VISIBLE
                    binding.tvSavings.text = " (Save $savingsPercent%)"
                } else {
                    binding.tvSavings.visibility = View.GONE
                }
            }
        }

        // Seçili planı güncelle
        updateSubscriptionSelection(state.selectedPackageId)
    }

    private fun showLoading(isLoading: Boolean) {
        binding.progressBar.visibility = if (isLoading) View.VISIBLE else View.GONE
        binding.subscriptionOptionsLayout.visibility = View.GONE
        binding.proStatusLayout.visibility = View.GONE
    }

    private fun showSubscriptionOptions() {
        binding.subscriptionOptionsLayout.visibility = View.VISIBLE
        binding.proStatusLayout.visibility = View.GONE
    }

    private fun showActiveSubscription() {
        binding.subscriptionOptionsLayout.visibility = View.GONE
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

    private fun showError(message: String) {
        Toast.makeText(requireContext(), message, Toast.LENGTH_LONG).show()

        // Hata durumunda abonelik seçeneklerini göster
        showSubscriptionOptions()
    }

    override fun onResume() {
        super.onResume()
        // Abonelik durumunu yenile
        viewModel.getExpiryDate { expiryDate ->
            if (expiryDate != null && binding.proStatusLayout.visibility == View.VISIBLE) {
                binding.tvExpiryDate.text = "Your subscription expires on $expiryDate"
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}