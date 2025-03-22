package com.aliaktas.urbanscore.ui.subscription

import android.os.Bundle
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
import com.aliaktas.urbanscore.R
import com.aliaktas.urbanscore.databinding.FragmentProSubscriptionBinding
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@AndroidEntryPoint
class ProSubscriptionFragment : Fragment() {

    private var _binding: FragmentProSubscriptionBinding? = null
    private val binding get() = _binding!!

    private val viewModel: ProSubscriptionViewModel by viewModels()

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

        // Aylık abonelik butonu
        binding.btnMonthlySubscription.setOnClickListener {
            viewModel.purchaseMonthlySubscription(requireActivity())
        }

        // Yıllık abonelik butonu (şimdilik pasif)
        binding.btnYearlySubscription.setOnClickListener {
            Toast.makeText(
                requireContext(),
                "Yearly subscription coming soon!",
                Toast.LENGTH_SHORT
            ).show()
        }

        // Abonelik yenileme butonu
        binding.btnRenewSubscription.setOnClickListener {
            viewModel.purchaseMonthlySubscription(requireActivity())
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

        // 30 gün sonrası için geçici bir bitiş tarihi göster
        // Gerçek implementasyonda, RevenueCat'den gerçek bitiş tarihini almalısınız
        val expiryDate = SimpleDateFormat("dd MMM yyyy", Locale.getDefault())
            .format(Date(System.currentTimeMillis() + (30 * 24 * 60 * 60 * 1000L)))

        binding.tvExpiryDate.text = "Your subscription expires on $expiryDate"
    }

    private fun showError(message: String) {
        Toast.makeText(requireContext(), message, Toast.LENGTH_LONG).show()
        // Hata sonrası abonelik seçeneklerini göster
        showSubscriptionOptions()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}