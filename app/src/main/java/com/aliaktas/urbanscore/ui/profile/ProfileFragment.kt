package com.aliaktas.urbanscore.ui.profile

import android.content.Intent
import android.content.res.ColorStateList
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.PopupMenu
import android.widget.Toast
import androidx.core.content.ContextCompat
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
    private var isMyTopListSelected = true
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
        updateTabButtonStyles()

        // ViewModel'in Flow'larının düzgün çalıştığından emin olmak için
        Log.d("ProfileFragment", "onViewCreated: Starting to observe ViewModel")
    }

    // Bu yeni metodu ekleyin - Fragment'ın görünürlüğünü izlemek için
    override fun onStart() {
        super.onStart()
        Log.d("ProfileFragment", "onStart: Fragment is becoming visible")
    }

    override fun onResume() {
        super.onResume()
        Log.d("ProfileFragment", "onResume: Fragment is now in foreground")

        // artık burada refresh çağrısı yapmayacağız çünkü Flow'lar zaten güncel verileri gönderiyor
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

        // Sekme butonları
        binding.btnMyTopList.setOnClickListener {
            if (!isMyTopListSelected) {
                isMyTopListSelected = true
                updateTabButtonStyles()
                updateTabContent()
            }
        }

        binding.profileButton.setOnClickListener {
            showProfileMenu(it)
        }

        binding.btnBucketList.setOnClickListener {
            if (isMyTopListSelected) {
                isMyTopListSelected = false
                updateTabButtonStyles()
                updateTabContent()
            }
        }

        // Logout button
        binding.btnLogout.setOnClickListener {
            showLogoutConfirmationDialog()
        }

        // Delete account butonunu ekle
        binding.btnDeleteAccount.setOnClickListener {
            showDeleteAccountConfirmationDialog()
        }
    }

    // Tab butonlarının stillerini güncelleyen yeni metod
    private fun updateTabButtonStyles() {
        val context = requireContext()

        if (isMyTopListSelected) {
            binding.btnMyTopList.backgroundTintList = ColorStateList.valueOf(ContextCompat.getColor(context, R.color.primary_purple))
            binding.btnBucketList.backgroundTintList = ColorStateList.valueOf(ContextCompat.getColor(context, R.color.primary_gray))
        } else {
            binding.btnMyTopList.backgroundTintList = ColorStateList.valueOf(ContextCompat.getColor(context, R.color.primary_gray))
            binding.btnBucketList.backgroundTintList = ColorStateList.valueOf(ContextCompat.getColor(context, R.color.primary_purple))
        }
    }

    // Sekme içeriğini güncelleyen yeni metod
    private fun updateTabContent() {
        val currentState = viewModel.profileState.value as? ProfileState.Success ?: return

        if (isMyTopListSelected) {
            // My Top List sekmesi aktif
            binding.rvVisitedCities.visibility = View.VISIBLE
            binding.rvWishlistCities.visibility = View.GONE
            binding.tvNoVisitedCities.visibility = if (currentState.visitedCities.isEmpty()) View.VISIBLE else View.GONE
            binding.tvNoWishlistCities.visibility = View.GONE
        } else {
            // Bucket List sekmesi aktif
            binding.rvVisitedCities.visibility = View.GONE
            binding.rvWishlistCities.visibility = View.VISIBLE
            binding.tvNoVisitedCities.visibility = View.GONE
            binding.tvNoWishlistCities.visibility = if (currentState.wishlistCities.isEmpty()) View.VISIBLE else View.GONE
        }
    }


    private fun observeViewModel() {
        Log.d("ProfileFragment", "Starting to observe ViewModel flows")

        viewLifecycleOwner.lifecycleScope.launch {
            // State flow'u gözlemle
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                // Profile state
                launch {
                    viewModel.profileState.collect { state ->
                        Log.d("ProfileFragment", "Received new profile state: ${state.javaClass.simpleName}")
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

    // updateVisitedCitiesList ve updateWishlistCitiesList metodlarını güncelle
    private fun updateVisitedCitiesList(cities: List<VisitedCityItem>) {
        // Sadece adapter'ı güncelle, görünürlük updateTabContent'te kontrol edilecek
        visitedCitiesAdapter.submitList(cities)
    }

    private fun updateWishlistCitiesList(cities: List<WishlistCityItem>) {
        // Sadece adapter'ı güncelle, görünürlük updateTabContent'te kontrol edilecek
        wishlistCitiesAdapter.submitList(cities)
    }

    // updateUI metodunu güncelle
    private fun updateUI(state: ProfileState) {
        when (state) {
            is ProfileState.Loading -> {
                showLoading(true)
            }

            is ProfileState.Success -> {
                showLoading(false)

                // Kullanıcı bilgilerini göster
                binding.txtUsername.text = state.displayName

                // Sayaçları güncelle
                binding.txtVisitedCitiesCount.text = "${state.visitedCities.size}"
                binding.txtBucketListCount.text = "${state.wishlistCities.size}"

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

                // Aktif sekmeye göre içeriği güncelle
                updateTabContent()
            }

            is ProfileState.Error -> {
                showLoading(false)
                showMessage(state.message)
            }
        }
    }

    private fun showProfileMenu(view: View) {
        val popup = PopupMenu(requireContext(), view)
        popup.menuInflater.inflate(R.menu.menu_profile, popup.menu)

        // Menu öğesi tıklama olayını ayarla
        popup.setOnMenuItemClickListener { menuItem ->
            when (menuItem.itemId) {
                R.id.menu_pro_subscription -> {
                    // Pro abonelik ekranına yönlendir
                    (requireActivity() as MainActivity).navigateToProSubscription()
                    true
                }
                R.id.menu_logout -> {
                    // Çıkış yap - mevcut dialog'u kullan
                    showLogoutConfirmationDialog()
                    true
                }
                else -> false
            }
        }

        // Material tasarıma uygun icon tint rengini ayarla
        try {
            val menuHelper = PopupMenu::class.java.getDeclaredField("mPopup")
            menuHelper.isAccessible = true
            val menuPopupHelper = menuHelper.get(popup)
            menuPopupHelper.javaClass
                .getDeclaredMethod("setForceShowIcon", Boolean::class.java)
                .invoke(menuPopupHelper, true)
        } catch (e: Exception) {
            Log.e("ProfileFragment", "Menu icon force show error", e)
        }

        // Popup menu'yü göster
        popup.show()
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


    private fun showDeleteAccountConfirmationDialog() {
        val dialogView = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_delete_account, null)
        val reasonEditText = dialogView.findViewById<EditText>(R.id.editTextReason)

        MaterialAlertDialogBuilder(requireContext())
            .setTitle(R.string.delete_account)
            .setView(dialogView)
            .setMessage(R.string.delete_account_confirmation)
            .setNegativeButton(R.string.cancel) { dialog, _ ->
                dialog.dismiss()
            }
            .setPositiveButton(R.string.yes) { _, _ ->
                val reason = reasonEditText.text.toString()
                viewModel.requestAccountDeletion(reason)
            }
            .show()
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