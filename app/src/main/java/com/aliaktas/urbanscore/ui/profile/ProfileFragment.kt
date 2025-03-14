package com.aliaktas.urbanscore.ui.profile

import android.content.Intent
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
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.aliaktas.urbanscore.R
import com.aliaktas.urbanscore.databinding.FragmentProfileBinding
import com.aliaktas.urbanscore.databinding.ItemWishlistCityBinding
import com.aliaktas.urbanscore.ui.auth.AuthViewModel
import com.aliaktas.urbanscore.data.model.CityModel
import com.aliaktas.urbanscore.databinding.ItemVisitedCitiesBinding
import com.bumptech.glide.Glide
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.Snackbar
import com.google.firebase.auth.FirebaseAuth
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

@AndroidEntryPoint
class ProfileFragment : Fragment() {

    private var _binding: FragmentProfileBinding? = null
    private val binding get() = _binding!!

    private val authViewModel: AuthViewModel by viewModels()
    private val profileViewModel: ProfileViewModel by viewModels()

    private lateinit var visitedCitiesAdapter: VisitedCitiesAdapter
    private lateinit var wishlistCitiesAdapter: WishlistCitiesAdapter

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

        setupUI()
        setupRecyclerViews()
        setupClickListeners()
        observeViewModels()
    }

    private fun setupUI() {
        // Display user info (will be populated from AuthViewModel)
        // Initially empty, will be filled from observeViewModels
    }

    private fun setupRecyclerViews() {
        // Setup Visited Cities RecyclerView
        visitedCitiesAdapter = VisitedCitiesAdapter(
            onItemClick = { cityId ->
                navigateToCityDetail(cityId)
            }
        )
        binding.rvVisitedCities.apply {
            layoutManager = LinearLayoutManager(context)
            adapter = visitedCitiesAdapter
            // Önemli: İç içe kaydırmayı etkinleştir
            isNestedScrollingEnabled = true
            // Sabit boyut ayarını kapat (dinamik içerik için)
            setHasFixedSize(false)
        }

        // Setup Wishlist Cities RecyclerView
        wishlistCitiesAdapter = WishlistCitiesAdapter(
            onItemClick = { cityId ->
                navigateToCityDetail(cityId)
            },
            onRemoveClick = { cityId ->
                profileViewModel.removeFromWishlist(cityId)
            }
        )
        binding.rvWishlistCities.apply {
            layoutManager = LinearLayoutManager(context)
            adapter = wishlistCitiesAdapter
            // Önemli: İç içe kaydırmayı etkinleştir
            isNestedScrollingEnabled = true
            // Sabit boyut ayarını kapat (dinamik içerik için)
            setHasFixedSize(false)
        }
    }

    private fun navigateToCityDetail(cityId: String) {
        try {
            val action = ProfileFragmentDirections.actionProfileFragmentToCityDetailFragment(cityId)
            findNavController().navigate(action)
        } catch (e: Exception) {
            Log.e("ProfileFragment", "Navigation error: ${e.message}", e)
            Toast.makeText(context, "Navigation error: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    private fun setupClickListeners() {
        // Logout button
        binding.btnLogout.setOnClickListener {
            showLogoutConfirmationDialog()
        }

        // Share visited cities button
        binding.btnShareVisitedCities.setOnClickListener {
            profileViewModel.shareVisitedCities()
        }

        // Share wishlist button
        binding.btnShareBucketList.setOnClickListener {
            profileViewModel.shareWishlist()
        }
    }

    private fun observeViewModels() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                // Observe Auth State
                launch {
                    authViewModel.state.collect { state ->
                        updateAuthUI(state)
                    }
                }

                // Observe Visited Cities
                launch {
                    profileViewModel.visitedCities.collectLatest { cities ->
                        visitedCitiesAdapter.submitList(cities)
                        binding.txtVisitedCitiesCount.text = "Visited cities: ${cities.size}"

                        // Empty state kontrolü
                        if (cities.isEmpty()) {
                            binding.tvNoVisitedCities.visibility = View.VISIBLE
                            binding.rvVisitedCities.visibility = View.GONE
                        } else {
                            binding.tvNoVisitedCities.visibility = View.GONE
                            binding.rvVisitedCities.visibility = View.VISIBLE
                        }
                    }
                }

                // Observe Wishlist
                launch {
                    profileViewModel.wishlistCities.collectLatest { cities ->
                        wishlistCitiesAdapter.submitList(cities)

                        // Empty state kontrolü
                        if (cities.isEmpty()) {
                            binding.tvNoWishlistCities.visibility = View.VISIBLE
                            binding.rvWishlistCities.visibility = View.GONE
                        } else {
                            binding.tvNoWishlistCities.visibility = View.GONE
                            binding.rvWishlistCities.visibility = View.VISIBLE
                        }
                    }
                }

                // Observe Share Intent
                launch {
                    profileViewModel.shareIntent.collect { intent ->
                        intent?.let {
                            try {
                                startActivity(Intent.createChooser(it, "Share via"))
                                // Intent'i kullandıktan sonra null'a ayarla
                                profileViewModel.clearShareIntent()
                            } catch (e: Exception) {
                                Log.e("ProfileFragment", "Error sharing: ${e.message}")
                                Toast.makeText(context, "Paylaşma hatası: ${e.message}", Toast.LENGTH_SHORT).show()
                            }
                        }
                    }
                }
            }
        }
    }

    private fun updateAuthUI(state: com.aliaktas.urbanscore.ui.auth.AuthState) {
        when (state) {
            is com.aliaktas.urbanscore.ui.auth.AuthState.Authenticated -> {
                binding.txtUsername.text = state.user.displayName.ifEmpty { "Traveler!" }

                // Load profile image if available
                if (state.user.photoUrl.isNotEmpty()) {
                    Glide.with(this)
                        .load(state.user.photoUrl)
                        .circleCrop()
                        .into(binding.imageProfileAvatar)
                }
            }
            is com.aliaktas.urbanscore.ui.auth.AuthState.Unauthenticated -> {
                findNavController().navigate(R.id.action_profileFragment_to_loginFragment)
            }
            else -> {
                // Handle other states if needed
            }
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
                logoutUser()
            }
            .show()
    }

    private fun logoutUser() {
        FirebaseAuth.getInstance().signOut()
        Toast.makeText(context, "Çıkış yapıldı", Toast.LENGTH_SHORT).show()
        findNavController().navigate(R.id.action_profileFragment_to_loginFragment)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}

// Adapter for Visited Cities (with ratings)
class VisitedCitiesAdapter(
    private val onItemClick: (String) -> Unit
) : RecyclerView.Adapter<VisitedCitiesAdapter.ViewHolder>() {

    private var cities: List<VisitedCityItem> = emptyList()

    fun submitList(newList: List<VisitedCityItem>) {
        cities = newList
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemVisitedCitiesBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(cities[position])
    }

    override fun getItemCount() = cities.size

    inner class ViewHolder(private val binding: ItemVisitedCitiesBinding) :
        RecyclerView.ViewHolder(binding.root) {

        init {
            binding.root.setOnClickListener {
                val position = bindingAdapterPosition
                if (position != RecyclerView.NO_POSITION) {
                    onItemClick(cities[position].id)
                }
            }
        }

        fun bind(city: VisitedCityItem) {
            binding.textCityName.text = "${city.name}, ${city.country}"
            binding.textRating.text = String.format("%.2f", city.userRating)
            binding.textRatingCount.text = (position + 1).toString()

            // Load flag image
            Glide.with(binding.root)
                .load(city.flagUrl)
                .into(binding.imageFlag)
        }
    }

    data class VisitedCityItem(
        val id: String,
        val name: String,
        val country: String,
        val flagUrl: String,
        val userRating: Double
    )
}

// Adapter for Wishlist Cities
class WishlistCitiesAdapter(
    private val onItemClick: (String) -> Unit,
    private val onRemoveClick: ((String) -> Unit)? = null
) : RecyclerView.Adapter<WishlistCitiesAdapter.ViewHolder>() {

    private var cities: List<WishlistCityItem> = emptyList()

    fun submitList(newList: List<WishlistCityItem>) {
        cities = newList
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemWishlistCityBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(cities[position])
    }

    override fun getItemCount() = cities.size

    inner class ViewHolder(private val binding: ItemWishlistCityBinding) :
        RecyclerView.ViewHolder(binding.root) {

        init {
            binding.root.setOnClickListener {
                val position = bindingAdapterPosition
                if (position != RecyclerView.NO_POSITION) {
                    onItemClick(cities[position].id)
                }
            }

            binding.btnRemove.setOnClickListener {
                val position = bindingAdapterPosition
                if (position != RecyclerView.NO_POSITION) {
                    onRemoveClick?.invoke(cities[position].id)
                }
            }
        }

        fun bind(city: WishlistCityItem) {
            binding.textCityName.text = "${city.name}, ${city.country}"

            // Load flag image
            Glide.with(binding.root)
                .load(city.flagUrl)
                .into(binding.imageFlag)
        }
    }

    data class WishlistCityItem(
        val id: String,
        val name: String,
        val country: String,
        val flagUrl: String
    )
}