package com.aliaktas.urbanscore.ui.allcities

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.aliaktas.urbanscore.MainActivity
import com.aliaktas.urbanscore.R
import com.aliaktas.urbanscore.databinding.FragmentAllCitiesBinding
import com.aliaktas.urbanscore.util.NetworkUtil
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class AllCitiesFragment : Fragment() {

    private var _binding: FragmentAllCitiesBinding? = null
    private val binding get() = _binding!!

    @Inject
    lateinit var networkUtil: NetworkUtil

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentAllCitiesBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupCategoryCards()
        setupErrorHandling()
        setupAnimations()
        observeNetworkState()

        // İlk açılışta internet kontrolü yap
        checkInternetConnection()
    }

    private fun setupAnimations() {
        binding.animationError.apply {
            setAnimation(R.raw.error_animation)
        }
    }

    private fun setupErrorHandling() {
        binding.btnRetry.setOnClickListener {
            if (networkUtil.isNetworkAvailable()) {
                hideErrorUI()
            } else {
                // Bağlantı hala yoksa animasyonu yeniden oynat
                binding.animationError.playAnimation()
            }
        }
    }

    private fun observeNetworkState() {
        viewLifecycleOwner.lifecycleScope.launch {
            networkUtil.observeNetworkState().collect { isConnected ->
                if (isConnected) {
                    hideErrorUI()
                } else {
                    showErrorUI(getString(R.string.no_internet_connection))
                }
            }
        }
    }

    private fun checkInternetConnection(): Boolean {
        val isConnected = networkUtil.isNetworkAvailable()
        if (!isConnected) {
            showErrorUI(getString(R.string.no_internet_connection))
        }
        return isConnected
    }

    private fun showErrorUI(errorMessage: String) {
        binding.errorContainer.visibility = View.VISIBLE
        binding.textError.text = errorMessage
        binding.animationError.playAnimation()

        // Ana içeriği gizle
        binding.contentScrollView.visibility = View.GONE
    }

    private fun hideErrorUI() {
        binding.errorContainer.visibility = View.GONE

        // Ana içeriği göster
        binding.contentScrollView.visibility = View.VISIBLE
    }

    private fun setupCategoryCards() {
        // Gastronomy & Diversity
        binding.cardGastronomy.setOnClickListener {
            navigateWithCheck("gastronomy")
        }

        // City Aesthetics
        binding.cardAesthetics.setOnClickListener {
            navigateWithCheck("aesthetics")
        }

        // Safety & Peace
        binding.cardSafety.setOnClickListener {
            navigateWithCheck("safety")
        }

        // Cultural Heritage
        binding.cardCulture.setOnClickListener {
            navigateWithCheck("culture")
        }

        // Livability & Nature
        binding.cardLivability.setOnClickListener {
            navigateWithCheck("livability")
        }

        // Social Life & Affordability
        binding.cardSocial.setOnClickListener {
            navigateWithCheck("social")
        }

        // Local Hospitality
        binding.cardHospitality.setOnClickListener {
            navigateWithCheck("hospitality")
        }
    }

    private fun navigateWithCheck(categoryId: String) {
        if (checkInternetConnection()) {
            navigateToCategoryList(categoryId)
        } else {
            Toast.makeText(context, getString(R.string.no_internet_connection), Toast.LENGTH_SHORT).show()
        }
    }

    private fun navigateToCategoryList(categoryId: String) {
        try {
            (requireActivity() as MainActivity).navigateToCategoryList(categoryId)
        } catch (e: Exception) {
            Log.e("AllCitiesFragment", "Navigation error: ${e.message}", e)
            Toast.makeText(context, "Navigation error: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}