package com.aliaktas.urbanscore.ui.comments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.aliaktas.urbanscore.ads.AdManager
import com.aliaktas.urbanscore.databinding.BottomSheetAddCommentBinding
import com.aliaktas.urbanscore.ui.detail.CityDetailEvent
import com.aliaktas.urbanscore.ui.detail.CityDetailViewModel
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class CommentBottomSheet : BottomSheetDialogFragment() {

    @Inject
    lateinit var adManager: AdManager

    private var _binding: BottomSheetAddCommentBinding? = null
    private val binding get() = _binding!!
    private var eventCollectorJob: Job? = null
    private val viewModel: CityDetailViewModel by viewModels({ requireParentFragment() })

    companion object {
        const val ARG_CITY_ID = "cityId"
        fun newInstance(cityId: String): CommentBottomSheet {
            return CommentBottomSheet().apply {
                arguments = Bundle().apply {
                    putString(ARG_CITY_ID, cityId)
                }
            }
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = BottomSheetAddCommentBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupClickListeners()

        lifecycleScope.launch {
            viewLifecycleOwner.lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.detailEvents.collect { event ->
                    if (event is CityDetailEvent.AddCommentResult) {
                        if (event.success) {
                            dismiss()
                        } else {
                            Toast.makeText(requireContext(), event.message, Toast.LENGTH_SHORT).show()
                            binding.btnSubmitComment.isEnabled = true
                            binding.progressBar.visibility = View.GONE
                        }
                    }
                }
            }
        }
    }

    private fun setupClickListeners() {
        binding.btnSubmitComment.setOnClickListener {
            val commentText = binding.etComment.text.toString().trim()
            if (commentText.isNotEmpty()) {
                // Önce ödüllü reklam göster, sonra yorumu gönder
                binding.btnSubmitComment.isEnabled = false

                adManager.showRewardedAd(
                    requireActivity(),
                    onRewarded = {
                        // Kullanıcı ödülü aldı, yorumu gönder
                        submitComment(commentText)
                    },
                    onAdClosed = {
                        // Reklam gösterilemedi veya kapatıldı durumunda
                        // Pro kullanıcılar için reklam gösterilmez ve direkt onRewarded çağrılır
                        // Eğer buraya gelindiyse ama isEnabled hala false ise
                        // yorum gönderilmemiş olabilir, butonu aktifleştir
                        if (!binding.btnSubmitComment.isEnabled) {
                            binding.btnSubmitComment.isEnabled = true
                        }
                    }
                )
            }
        }
    }

    private fun submitComment(commentText: String) {
        binding.btnSubmitComment.isEnabled = false
        binding.progressBar.visibility = View.VISIBLE
        viewModel.addComment(commentText)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        eventCollectorJob?.cancel()
        _binding = null
    }
}