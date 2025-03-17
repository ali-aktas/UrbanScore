package com.aliaktas.urbanscore.ui.comments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import com.aliaktas.urbanscore.databinding.BottomSheetAddCommentBinding
import com.aliaktas.urbanscore.ui.detail.CityDetailViewModel
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class CommentBottomSheet : BottomSheetDialogFragment() {

    private var _binding: BottomSheetAddCommentBinding? = null
    private val binding get() = _binding!!

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
    }

    private fun setupClickListeners() {
        binding.btnSubmitComment.setOnClickListener {
            val commentText = binding.etComment.text.toString().trim()
            if (commentText.isNotEmpty()) {
                binding.btnSubmitComment.isEnabled = false
                binding.progressBar.visibility = View.VISIBLE

                viewModel.addComment(commentText)

                // Comment eklendikten sonra bottom sheet'i kapat
                lifecycleScope.launch {
                    viewModel.detailEvents.collect { event ->
                        if (event is com.aliaktas.urbanscore.ui.detail.CityDetailEvent.AddCommentResult) {
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
            } else {
                Toast.makeText(requireContext(), "Please enter a comment", Toast.LENGTH_SHORT).show()
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}