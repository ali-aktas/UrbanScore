package com.aliaktas.urbanscore.ui.profile

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.aliaktas.urbanscore.MainActivity
import com.aliaktas.urbanscore.R
import com.aliaktas.urbanscore.base.BaseViewModel
import com.aliaktas.urbanscore.databinding.FragmentProfileBinding
import com.bumptech.glide.Glide
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.Snackbar
import com.google.firebase.auth.FirebaseAuth
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class ProfileFragment : Fragment() {

    private var _binding: FragmentProfileBinding? = null
    private val binding get() = _binding!!

    private val viewModel: ProfileViewModel by viewModels()

    // Adaptörleri lazy olarak başlat - sadece kullanıldığında oluşturulur
    private val visitedCitiesAdapter by lazy { VisitedCitiesAdapter(this::navigateToCityDetail) }
    private val wishlistCitiesAdapter by lazy {
        WishlistCitiesAdapter(
            onItemClick = this::navigateToCityDetail,
            onRemoveClick = viewModel::removeFromWishlist
        )
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentProfileBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupRecyclerViews()
        setupClickListeners()
        observeViewModel()
    }


    private fun setupRecyclerViews() {
        // Visited Cities RecyclerView
        binding.rvVisitedCities.apply {
            adapter = visitedCitiesAdapter
            layoutManager = LinearLayoutManager(context)
            // RecyclerView optimizasyonları
            setHasFixedSize(true)
            itemAnimator = null // İlk yükleme animasyonunu devre dışı bırak
        }

        // Wishlist Cities RecyclerView
        binding.rvWishlistCities.apply {
            adapter = wishlistCitiesAdapter
            layoutManager = LinearLayoutManager(context)
            setHasFixedSize(true)
            itemAnimator = null
        }
    }

    private fun setupClickListeners() {
        // Share visited cities
        binding.btnShareVisitedCities.setOnClickListener {
            viewModel.shareVisitedCities()
        }

        // Share wishlist
        binding.btnShareBucketList.setOnClickListener {
            viewModel.shareWishlist()
        }

        // Logout button
        binding.btnLogout.setOnClickListener {
            showLogoutConfirmationDialog()
        }
    }



    private fun observeViewModel() {
        viewLifecycleOwner.lifecycleScope.launch {
            // State flow'u gözlemle
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                // Profile state
                launch {
                    viewModel.profileState.collect { state ->
                        updateUI(state)
                    }
                }

                // Share intent (tek seferlik event)
                launch {
                    viewModel.shareIntent.collect { intent ->
                        intent?.let {
                            try {
                                startActivity(Intent.createChooser(it, "Share via"))
                            } catch (e: Exception) {
                                showMessage("Error sharing: ${e.localizedMessage}")
                            }
                        }
                    }
                }

                // UI Event'leri gözlemle
                launch {
                    viewModel.events.collect { event ->
                        handleEvent(event)
                    }
                }
            }
        }
    }

    private fun updateUI(state: ProfileState) {
        when (state) {
            is ProfileState.Loading -> {
                showLoading(true)
            }

            is ProfileState.Success -> {
                showLoading(false)

                // Kullanıcı bilgilerini göster
                binding.txtUsername.text = state.displayName
                binding.txtVisitedCitiesCount.text =
                    "Visited cities: ${state.visitedCities.size}"

                // Profil fotoğrafını yükle
                if (state.photoUrl.isNotEmpty()) {
                    Glide.with(this)
                        .load(state.photoUrl)
                        .circleCrop()
                        .into(binding.imageProfileAvatar)
                }

                // Listeleri güncelle
                updateVisitedCitiesList(state.visitedCities)
                updateWishlistCitiesList(state.wishlistCities)
            }

            is ProfileState.Error -> {
                showLoading(false)
                showMessage(state.message)
            }
        }
    }

    private fun updateVisitedCitiesList(cities: List<VisitedCityItem>) {
        // Boş liste kontrolü - UI'da gösterilecek metin
        binding.tvNoVisitedCities.isVisible = cities.isEmpty()
        binding.rvVisitedCities.isVisible = cities.isNotEmpty()

        // Listeyi güncelle (DiffUtil adapter içinde çalışacak)
        visitedCitiesAdapter.submitList(cities)
    }

    private fun updateWishlistCitiesList(cities: List<WishlistCityItem>) {
        // Boş liste kontrolü - UI'da gösterilecek metin
        binding.tvNoWishlistCities.isVisible = cities.isEmpty()
        binding.rvWishlistCities.isVisible = cities.isNotEmpty()

        // Listeyi güncelle (DiffUtil adapter içinde çalışacak)
        wishlistCitiesAdapter.submitList(cities)
    }

    private fun showLoading(isLoading: Boolean) {
        // İlerde progress bar eklenirse burada gösterilebilir
    }

    private fun handleEvent(event: BaseViewModel.UiEvent) {
        when (event) {
            is BaseViewModel.UiEvent.Error -> showMessage(event.message)
            is BaseViewModel.UiEvent.Success -> showMessage(event.message)
            else -> { /* Diğer event tipleri */ }
        }
    }

    private fun showMessage(message: String) {
        Snackbar.make(binding.root, message, Snackbar.LENGTH_SHORT).show()
    }

    private fun navigateToCityDetail(cityId: String) {
        try {
            (requireActivity() as MainActivity).navigateToCityDetail(cityId)
        } catch (e: Exception) {
            Log.e("ProfileFragment", "Navigation error: ${e.message}", e)
            showMessage("Navigation error")
        }
    }

    private fun showLogoutConfirmationDialog() {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle(R.string.logout)
            .setMessage(R.string.logout_confirmation)
            .setNegativeButton(R.string.cancel) { dialog, _ ->
                dialog.dismiss()
            }
            .setPositiveButton(R.string.yes) { _, _ ->
                FirebaseAuth.getInstance().signOut()
                showMessage("Logged out successfully")

                // Navigation Component yerine doğrudan Activity'ye yönlendirme
                (requireActivity() as MainActivity).showLoginFragment()
            }
            .show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}