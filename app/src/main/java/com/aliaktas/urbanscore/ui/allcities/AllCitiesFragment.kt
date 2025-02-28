package com.aliaktas.urbanscore.ui.allcities

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
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

        // Burada fragment başlatma işlemlerini yapacağız
        setupUI()
    }

    private fun setupUI() {
        // Geçici olarak bir başlık gösterelim, sonra detaylı içerikle dolduracağız
        binding.tvCategoriesTitle.text = "Categories"
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}