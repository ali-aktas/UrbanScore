package com.aliaktas.urbanscore.ui.allcities

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.aliaktas.urbanscore.databinding.FragmentAllCitiesBinding
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class AllCitiesFragment : Fragment() {

    private var _binding: FragmentAllCitiesBinding? = null
    private val binding get() = _binding!!

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
    }

    // AllCitiesFragment.kt içerisindeki setupCategoryCards metodunu güncelleyelim
    private fun setupCategoryCards() {
        // Environment & Aesthetics
        binding.cardEnvironment.setOnClickListener {
            navigateToCategoryList("environment")
        }

        // Safety & Tranquility
        binding.cardSafety.setOnClickListener {
            navigateToCategoryList("safety")
        }

        // Livability
        binding.cardLivability.setOnClickListener {
            navigateToCategoryList("livability")
        }

        // Cost of Living
        binding.cardCost.setOnClickListener {
            navigateToCategoryList("cost")
        }

        // Social & Cultural Life
        binding.cardSocial.setOnClickListener {
            navigateToCategoryList("social")
        }
    }


    private fun navigateToCategoryList(categoryId: String) {
        try {
            val action = AllCitiesFragmentDirections.actionAllCitiesFragmentToCategoryListFragment(categoryId)
            findNavController().navigate(action)
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