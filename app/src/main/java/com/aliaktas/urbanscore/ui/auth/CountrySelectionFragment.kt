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
import androidx.activity.OnBackPressedCallback
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
import me.zhanghai.android.fastscroll.FastScrollerBuilder
import me.zhanghai.android.fastscroll.FastScroller

@AndroidEntryPoint
class CountrySelectionFragment : Fragment() {

    private var _binding: FragmentCountrySelectionBinding? = null
    private val binding get() = _binding!!
    private val viewModel: CountrySelectionViewModel by viewModels()
    private lateinit var countriesAdapter: CountriesAdapter
    private var fastScroller: FastScroller? = null

    private var backPressedCallback: OnBackPressedCallback? = null

    // Arama gecikmesi için handler
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
        setupBackNavigation()
    }

    private fun setupBackNavigation() {
        // Önceki callback'i temizle
        backPressedCallback?.remove()

        // Yeni callback oluştur
        backPressedCallback = object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                // Geri tuşu işlemini engelleyin
                // İsteğe bağlı olarak kullanıcıya bir mesaj gösterebilirsiniz
                Snackbar.make(binding.root, "Please select a country to continue", Snackbar.LENGTH_SHORT).show()
            }
        }

        requireActivity().onBackPressedDispatcher.addCallback(viewLifecycleOwner, backPressedCallback!!)
    }

    private fun setupRecyclerView() {
        // Adapter'ı yapılandır
        countriesAdapter = CountriesAdapter()

        // RecyclerView'ı yapılandır
        binding.recyclerViewCountries.apply {
            adapter = countriesAdapter
            layoutManager = LinearLayoutManager(requireContext())
            setHasFixedSize(true)
        }

        // FastScroller oluştur
        try {
            fastScroller = FastScrollerBuilder(binding.recyclerViewCountries)
                .useMd2Style() // Material Design 2 stili
                .setPopupTextProvider(countriesAdapter)
                .build()

            Log.d("CountrySelection", "FastScroller successfully initialized")
        } catch (e: Exception) {
            Log.e("CountrySelection", "Error initializing FastScroller", e)
            // FastScroller olmadan devam et
        }

        // Ülke seçim callback'i
        countriesAdapter.onItemClick = { country ->
            Log.d("CountrySelection", "Selected country: ${country.name} (${country.id})")
            // Hata mesajını gizle (eğer görünüyorsa)
            binding.tvError.visibility = View.GONE
        }
    }

    private fun setupSearchView() {
        // Arama kutusuna metin değişikliği dinleyicisi ekle
        binding.etSearch.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                // Mevcut aramayı iptal et
                searchRunnable?.let { searchHandler.removeCallbacks(it) }

                // 300ms gecikme ile filtreleme yap (performans için)
                searchRunnable = Runnable {
                    countriesAdapter.filter(s.toString())
                }
                searchHandler.postDelayed(searchRunnable!!, 300)
            }
        })

        // Klavyeden arama yapıldığında
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
            // Seçili ülke var mı kontrol et
            val selectedCountry = countriesAdapter.getSelectedCountry()
            if (selectedCountry == null) {
                binding.tvError.visibility = View.VISIBLE
                return@setOnClickListener
            }

            // Buton ve UI durum güncellemesi
            binding.btnContinue.isEnabled = false
            binding.progressBar.visibility = View.VISIBLE

            // Ülke bilgisini kaydet
            viewModel.saveUserCountry(selectedCountry.id)
        }
    }

    private fun observeViewModel() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                // Ülke listesini izle
                launch {
                    viewModel.countries.collectLatest { countries ->
                        Log.d("CountrySelection", "Loaded ${countries.size} countries")
                        countriesAdapter.setCountries(countries)
                    }
                }

                // Yükleme durumunu izle
                launch {
                    viewModel.isLoading.collectLatest { isLoading ->
                        binding.progressBar.isVisible = isLoading
                        binding.btnContinue.isEnabled = !isLoading
                    }
                }

                // Hata durumunu izle
                launch {
                    viewModel.error.collectLatest { errorMsg ->
                        if (errorMsg != null) {
                            Snackbar.make(binding.root, errorMsg, Snackbar.LENGTH_LONG).show()
                            viewModel.clearError()
                        }
                    }
                }

                // Ülke kaydedildi durumunu izle
                launch {
                    viewModel.countrySaved.collectLatest { saved ->
                        if (saved) {
                            // Ana sayfaya yönlendir
                            (requireActivity() as MainActivity).navigateToHomeAfterLogin()
                        }
                    }
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()

        // Fragment görünür olduğunda callback'i yeniden ayarla
        setupBackNavigation()
    }

    override fun onDestroyView() {
        super.onDestroyView()

        // Handler'ı temizle
        searchRunnable?.let { searchHandler.removeCallbacks(it) }

        backPressedCallback?.remove()
        backPressedCallback = null

        // Binding'i temizle
        _binding = null

        // FastScroller referansını temizle
        fastScroller = null
    }
}