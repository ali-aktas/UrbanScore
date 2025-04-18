package com.aliaktas.urbanscore.ui.auth

import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import com.aliaktas.urbanscore.MainActivity
import com.aliaktas.urbanscore.databinding.FragmentCountrySelectionBinding
import com.google.android.material.snackbar.Snackbar
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

@AndroidEntryPoint
class CountrySelectionFragment : Fragment() {

    private var _binding: FragmentCountrySelectionBinding? = null
    private val binding get() = _binding!!
    private val viewModel: CountrySelectionViewModel by viewModels()
    private lateinit var countriesAdapter: CountriesAdapter

    // Arama gecikme işleyicisi
    private val searchHandler = Handler(Looper.getMainLooper())
    private var searchRunnable: Runnable? = null

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
        setupSearchView()
        setupButtons()
        observeViewModel()
    }

    // CountrySelectionFragment.kt içindeki setupRecyclerView metodunu güncelle

    private fun setupRecyclerView() {
        countriesAdapter = CountriesAdapter()
        binding.recyclerViewCountries.apply {
            adapter = countriesAdapter
            layoutManager = LinearLayoutManager(requireContext())
            setHasFixedSize(true)
        }

        // Hızlı kaydırma ayarları - Yeni kütüphane ile
        me.zhanghai.android.fastscroll.FastScrollerBuilder(binding.fastScroller)
            .setPopupTextProvider(countriesAdapter)
            .build()

        // Ülke seçimi callback'i
        countriesAdapter.onItemClick = { country ->
            Log.d("CountrySelection", "Selected country: ${country.name} (${country.id})")
            // Hata mesajını gizle
            binding.tvError.visibility = View.GONE
        }
    }

    private fun setupSearchView() {
        // Arama kutusu değişikliklerini dinle
        binding.etSearch.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                // Mevcut aramayı iptal et
                searchRunnable?.let { searchHandler.removeCallbacks(it) }

                // 300ms sonra filtreleme yap (performans için)
                searchRunnable = Runnable {
                    countriesAdapter.filter(s.toString())
                }
                searchHandler.postDelayed(searchRunnable!!, 300)
            }
        })

        // Klavyeden aramayı yap
        binding.etSearch.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_DONE) {
                // Klavyeyi kapat
                binding.etSearch.clearFocus()
                true
            } else {
                false
            }
        }
    }

    private fun setupButtons() {
        binding.btnContinue.setOnClickListener {
            // Seçili ülke yoksa uyarı göster
            val selectedCountry = countriesAdapter.getSelectedCountry()
            if (selectedCountry == null) {
                binding.tvError.visibility = View.VISIBLE
                return@setOnClickListener
            }

            // Butonu devre dışı bırak
            binding.btnContinue.isEnabled = false
            binding.progressBar.visibility = View.VISIBLE

            // Ülke ID'sini kaydet
            viewModel.saveUserCountry(selectedCountry.id)
        }
    }

    private fun observeViewModel() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                // Ülke listesi
                launch {
                    viewModel.countries.collectLatest { countries ->
                        Log.d("CountrySelection", "Loaded ${countries.size} countries")
                        countriesAdapter.setCountries(countries)
                    }
                }

                // Yükleme durumu
                launch {
                    viewModel.isLoading.collectLatest { isLoading ->
                        binding.progressBar.isVisible = isLoading
                        binding.btnContinue.isEnabled = !isLoading
                    }
                }

                // Hata durumu
                launch {
                    viewModel.error.collectLatest { errorMsg ->
                        if (errorMsg != null) {
                            Snackbar.make(binding.root, errorMsg, Snackbar.LENGTH_LONG).show()
                            viewModel.clearError()
                        }
                    }
                }

                // Ülke kaydedildi
                launch {
                    viewModel.countrySaved.collectLatest { saved ->
                        if (saved) {
                            // Ana sayfaya git
                            (requireActivity() as MainActivity).navigateToHomeAfterLogin()
                        }
                    }
                }
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        // Arama işleyiciyi temizle
        searchRunnable?.let { searchHandler.removeCallbacks(it) }
        _binding = null
    }
}