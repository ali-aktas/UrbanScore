package com.aliaktas.urbanscore.ui.onboarding

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.annotation.DrawableRes
import androidx.fragment.app.Fragment
import com.aliaktas.urbanscore.databinding.FragmentOnboardingImagePageBinding

class OnboardingImagePageFragment : Fragment() {

    private var _binding: FragmentOnboardingImagePageBinding? = null
    private val binding get() = _binding!!

    private var imageResId: Int? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            imageResId = it.getInt(ARG_IMAGE_RES_ID)
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentOnboardingImagePageBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        imageResId?.let {
            // Eğer Glide gibi bir kütüphane kullanıyorsanız:
            // Glide.with(this).load(it).into(binding.imageViewOnboardingPage)
            // Direkt drawable resource ID'si ile:
            binding.imageViewOnboardingPage.setImageResource(it)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    companion object {
        private const val ARG_IMAGE_RES_ID = "image_res_id"

        @JvmStatic
        fun newInstance(@DrawableRes imageResId: Int): OnboardingImagePageFragment {
            val fragment = OnboardingImagePageFragment()
            val args = Bundle()
            args.putInt(ARG_IMAGE_RES_ID, imageResId)
            fragment.arguments = args
            return fragment
        }
    }
}
