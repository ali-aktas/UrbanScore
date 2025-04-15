package com.aliaktas.urbanscore.ui.auth

import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.LinearLayoutManager
import com.aliaktas.urbanscore.MainActivity
import com.aliaktas.urbanscore.databinding.FragmentCountrySelectionBinding
import com.aliaktas.urbanscore.data.model.CountryModel
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class CountrySelectionFragment : Fragment() {

    private var _binding: FragmentCountrySelectionBinding? = null
    private val binding get() = _binding!!
    private val viewModel: AuthViewModel by viewModels()
    private lateinit var countriesAdapter: CountriesAdapter

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentCountrySelectionBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupRecyclerView()

        binding.btnContinue.setOnClickListener {
            // Seçili ülke yoksa işlemi engelle
            if (countriesAdapter.getSelectedCountry() == null) {
                binding.tvError.visibility = View.VISIBLE
                return@setOnClickListener
            }

            // Butonu devre dışı bırak ve bir yükleme indicator göster
            binding.btnContinue.isEnabled = false
            binding.progressBar.visibility = View.VISIBLE  // XML'e ProgressBar ekleyin

            // Ülke seçimini kaydet
            val selectedCountry = countriesAdapter.getSelectedCountry()!!
            viewModel.saveUserCountry(selectedCountry.id)

            // Biraz gecikme ekle (yükleme göstermek için)
            Handler(Looper.getMainLooper()).postDelayed({
                // Fragment hala attached mı kontrol et
                if (isAdded && !isDetached && !isRemoving) {
                    // Ana sayfaya git
                    (requireActivity() as MainActivity).navigateToHomeAfterLogin()
                }
            }, 1000)
        }
    }

    private fun setupRecyclerView() {
        countriesAdapter = CountriesAdapter(CountryModel.getAll())
        binding.recyclerViewCountries.apply {
            adapter = countriesAdapter
            layoutManager = LinearLayoutManager(requireContext())
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}