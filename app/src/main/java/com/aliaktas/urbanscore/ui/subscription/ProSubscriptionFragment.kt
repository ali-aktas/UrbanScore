package com.aliaktas.urbanscore.ui.subscription

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.aliaktas.urbanscore.MainActivity
import com.aliaktas.urbanscore.databinding.FragmentProSubscriptionBinding
import com.revenuecat.purchases.ui.revenuecatui.ExperimentalPreviewRevenueCatUIPurchasesAPI
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

        // RevenueCat Paywall gösterme butonu
        binding.btnShowPaywall.setOnClickListener {
            showRevenueCatPaywall()
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

    @OptIn(ExperimentalPreviewRevenueCatUIPurchasesAPI::class)
    private fun showRevenueCatPaywall() {
        try {
            viewModel.showPaywall(requireActivity())
        } catch (e: Exception) {
            Log.e(TAG, "Error showing RevenueCat paywall", e)
            Toast.makeText(requireContext(), "Could not show subscription options", Toast.LENGTH_SHORT).show()
        }
    }

    private fun observeViewModel() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.uiState.collect { state ->
                    updateUI(state)
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

                // Bitiş tarihi
                val expiryDate = viewModel.getExpiryDate()
                binding.tvExpiryDate.text = "Your subscription expires on $expiryDate"
            }
            is ProSubscriptionState.Error -> {
                showLoading(false)
                showError(state.message)
            }
        }
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
    }

    private fun showError(message: String) {
        Toast.makeText(requireContext(), message, Toast.LENGTH_LONG).show()
        // Hata sonrası abonelik seçeneklerini göster
        showSubscriptionOptions()
    }

    override fun onResume() {
        super.onResume()
        // Premium durumunu yenile
        viewModel.getOfferings { /* Offerings'leri alıp, UI'ı güncellemek isterseniz kullanabilirsiniz */ }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}