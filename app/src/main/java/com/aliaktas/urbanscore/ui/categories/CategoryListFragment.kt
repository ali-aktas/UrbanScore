package com.aliaktas.urbanscore.ui.categories

import android.graphics.Color
import android.graphics.Color.*
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.graphics.ColorUtils
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.interpolator.view.animation.FastOutSlowInInterpolator
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import com.aliaktas.urbanscore.R
import com.aliaktas.urbanscore.databinding.FragmentCategoryListBinding
import com.aliaktas.urbanscore.ui.home.CitiesAdapter
import com.aliaktas.urbanscore.ui.home.HomeState
import com.aliaktas.urbanscore.ui.home.HomeViewModel
import com.google.android.material.transition.MaterialContainerTransform
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class CategoryListFragment : Fragment() {

    private var _binding: FragmentCategoryListBinding? = null
    private val binding get() = _binding!!

    private val viewModel: HomeViewModel by viewModels()
    private val citiesAdapter = CitiesAdapter()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Daha yumuşak bir paylaşılan öğe geçiş animasyonu - düzeltilmiş
        sharedElementEnterTransition = MaterialContainerTransform().apply {
            // Daha uzun süre (milisaniye cinsinden)
            duration = 500L

            // Daha yumuşak geçiş eğrisi
            interpolator = FastOutSlowInInterpolator()

            // Sayfa arka planını karartan hafif transparant renk (scrim)
            scrimColor = ColorUtils.setAlphaComponent(
                requireContext().getColor(android.R.color.black), 60
            )

            // Daha yumuşak bir "fade through" efekti
            fadeMode = MaterialContainerTransform.FADE_MODE_THROUGH
            fadeProgressThresholds = MaterialContainerTransform.ProgressThresholds(0.2f, 0.9f)

            // Arka plan rengi - ya activity'nin arka plan rengini alır ya da sabit bir değer kullanır
            // Transparanlık için:
            setAllContainerColors(TRANSPARENT)

            // VEYA sabit bir renk için (örn. beyaz):
            // setAllContainerColors(Color.WHITE)

            // Fragment'ın çizileceği view ID'si
            drawingViewId = R.id.nav_host_fragment
        }

        // ÇIKIŞ animasyonu da özelleştirelim
        sharedElementReturnTransition = MaterialContainerTransform().apply {
            duration = 450L
            interpolator = FastOutSlowInInterpolator()
            fadeMode = MaterialContainerTransform.FADE_MODE_THROUGH
            fadeProgressThresholds = MaterialContainerTransform.ProgressThresholds(0.2f, 0.9f)
            scrimColor = ColorUtils.setAlphaComponent(
                requireContext().getColor(android.R.color.black), 60
            )
            setAllContainerColors(TRANSPARENT) // Transparanlık için
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentCategoryListBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupRecyclerView()
        setupBackButton()
        observeViewModel()
    }

    private fun setupRecyclerView() {
        binding.recyclerViewCategoryList.apply {
            adapter = citiesAdapter
        }

        citiesAdapter.onItemClick = { city ->
            // Şehir detayına yönlendirme
            val action = CategoryListFragmentDirections.actionCategoryListFragmentToCityDetailFragment(city.id)
            findNavController().navigate(action)
        }
    }

    private fun setupBackButton() {
        binding.btnBack.setOnClickListener {
            findNavController().navigateUp()
        }
    }

    private fun observeViewModel() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.state.collect { state ->
                    updateUI(state)
                }
            }
        }
    }

    private fun updateUI(state: HomeState) {
        when (state) {
            is HomeState.Success -> {
                binding.progressBar.visibility = View.GONE
                citiesAdapter.submitList(state.cities)
            }
            is HomeState.Loading -> {
                binding.progressBar.visibility = View.VISIBLE
            }
            is HomeState.Error -> {
                binding.progressBar.visibility = View.GONE
                // Hata durumunu göster
            }
            else -> { /* Initial state - do nothing */ }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}