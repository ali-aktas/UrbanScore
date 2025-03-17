package com.aliaktas.urbanscore.ui.detail

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.navArgs
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.aliaktas.urbanscore.MainActivity
import com.aliaktas.urbanscore.R
import com.aliaktas.urbanscore.databinding.FragmentCityDetailBinding
import com.aliaktas.urbanscore.ui.comments.CommentBottomSheet
import com.aliaktas.urbanscore.ui.comments.CommentsAdapter
import com.aliaktas.urbanscore.ui.ratecity.RateCityBottomSheet
import com.google.android.material.snackbar.Snackbar
import com.google.android.material.transition.MaterialContainerTransform
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import javax.inject.Inject

private const val TAG = "CityDetailFragment"

@AndroidEntryPoint
class CityDetailFragment : Fragment() {

    private var _binding: FragmentCityDetailBinding? = null
    private val binding get() = _binding!!

    private val viewModel: CityDetailViewModel by viewModels()
    private val args: CityDetailFragmentArgs by navArgs()

    @Inject
    lateinit var formatter: CityDetailFormatter

    @Inject
    lateinit var radarChartHelper: RadarChartHelper

    private lateinit var uiStateManager: CityDetailUiStateManager

    // Reference to rating bottom sheet for lifecycle management
    private var ratingBottomSheet: RateCityBottomSheet? = null

    private lateinit var commentsAdapter: CommentsAdapter
    private lateinit var commentsSection: View

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Setup shared element transition
        sharedElementEnterTransition = MaterialContainerTransform().apply {
            duration = 300L
            fadeMode = MaterialContainerTransform.FADE_MODE_THROUGH
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentCityDetailBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Setup transition name for shared element transition
        binding.toolbar.transitionName = "city_${args.cityId}"

        // Initialize UI state manager
        initializeUiStateManager()

        // Setup UI components and observers
        setupRadarChart()
        setupClickListeners()
        observeViewModel()
        setupComments()
    }

    private fun initializeUiStateManager() {
        uiStateManager = CityDetailUiStateManager(
            context = requireContext(),
            binding = binding,
            lifecycleScope = lifecycleScope,
            formatter = formatter,
            radarChartHelper = radarChartHelper,
            onRetry = { viewModel.loadCityDetails() },
            onGoBack = { (requireActivity() as MainActivity).handleBackPressed() },
            onRateButtonClick = { viewModel.showRatingSheet() }
        )
    }

    private fun setupRadarChart() {
        lifecycleScope.launch {
            radarChartHelper.setupRadarChart(binding.radarChart)
        }
    }

    private fun setupClickListeners() {
        // Back button
        binding.toolbar.setOnClickListener {
            (requireActivity() as MainActivity).handleBackPressed()
        }

        // Action buttons
        binding.btnAddToWishlist.setOnClickListener {
            viewModel.toggleWishlist()
        }

        // Explore buttons
        binding.btnExploreYouTube.setOnClickListener {
            viewModel.openYouTubeSearch()
        }

        binding.btnExploreGoogle.setOnClickListener {
            viewModel.openGoogleSearch()
        }

        binding.btnShowComments.setOnClickListener {
            viewModel.toggleComments()
        }

        // Add comment button
        binding.btnAddComment.setOnClickListener {
            viewModel.showCommentBottomSheet()
        }


    }

    private fun observeViewModel() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                // Observe state
                launch {
                    viewModel.detailState.collectLatest { state ->
                        uiStateManager.updateUI(state)
                    }
                }

                // Observe events
                launch {
                    viewModel.detailEvents.collectLatest { event ->
                        handleEvent(event)
                    }
                }

                // Observe comments state
                launch {
                    viewModel.detailState.collectLatest { state ->
                        if (state is CityDetailState.Success) {
                            updateCommentsUI(state)
                        }
                    }
                }
            }
        }

    }

    private fun handleEvent(event: CityDetailEvent) {
        when (event) {
            is CityDetailEvent.OpenUrl -> openUrl(event.url)
            is CityDetailEvent.ShowRatingSheet -> showRatingBottomSheet(event.cityId)
            is CityDetailEvent.ShowMessage -> showMessage(event.message)
            is CityDetailEvent.ShareCity -> startActivity(Intent.createChooser(event.shareIntent, "Share via"))
            is CityDetailEvent.DismissRatingSheet -> dismissRatingSheet()
            is CityDetailEvent.AddCommentResult -> TODO()
            is CityDetailEvent.LikeCommentResult -> TODO()
            is CityDetailEvent.ShowCommentBottomSheet -> {
                val commentBottomSheet = CommentBottomSheet.newInstance(event.cityId)
                commentBottomSheet.show(childFragmentManager, "CommentBottomSheet")
            }
        }
    }

    private fun openUrl(url: String) {
        try {
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
            startActivity(intent)
        } catch (e: Exception) {
            Log.e(TAG, "Error opening URL: ${e.message}", e)
            showMessage("Could not open link: ${e.message}")
        }
    }

    private fun showRatingBottomSheet(cityId: String) {
        try {
            Log.d(TAG, "Creating rating sheet for cityId: $cityId")
            val bottomSheet = RateCityBottomSheet.newInstance(cityId)

            bottomSheet.show(parentFragmentManager, "RateCityBottomSheet")
            ratingBottomSheet = bottomSheet

            Log.d(TAG, "Rating sheet shown successfully")
        } catch (e: Exception) {
            Log.e(TAG, "Error showing rating sheet: ${e.message}", e)
            Toast.makeText(
                requireContext(),
                "Could not open rating screen: ${e.message}",
                Toast.LENGTH_SHORT
            ).show()
        }
    }



    private fun setupComments() {
        // Comments adapter
        commentsAdapter = CommentsAdapter(
            onLikeClick = { comment, like ->
                viewModel.likeComment(comment.id, like)
            },
            onDeleteClick = { commentId ->
                // Silme işlemi onaylama diyaloğu gösterilebilir
                viewModel.deleteComment(commentId)
            }
        )

        // Comments section layout'unu inflate et
        commentsSection = layoutInflater.inflate(
            R.layout.layout_comments_section,
            binding.nestedScrollView.getChildAt(0) as ViewGroup,
            false
        )

        // RecyclerView'i ayarla
        commentsSection.findViewById<RecyclerView>(R.id.recyclerViewComments).apply {
            adapter = commentsAdapter
            layoutManager = LinearLayoutManager(requireContext())
        }

        // Load more button click
        commentsSection.findViewById<Button>(R.id.btnLoadMoreComments).setOnClickListener {
            viewModel.loadMoreComments()
        }

        // Layout'a ekle (comments butonu altına)
        val linearLayout = binding.nestedScrollView.getChildAt(0) as ViewGroup

        // Buttons container index'ini bul ve sonrasına ekle
        val btnExploreGoogleIndex = linearLayout.indexOfChild(binding.btnExploreGoogle)
        linearLayout.addView(commentsSection, btnExploreGoogleIndex + 1)

        // Başlangıçta gizle
        commentsSection.visibility = View.GONE
    }


    private fun updateCommentsUI(state: CityDetailState.Success) {
        // Comments visibility
        commentsSection.visibility = if (state.showComments) View.VISIBLE else View.GONE

        // Toggle button text
        binding.btnShowComments.text = if (state.showComments)
            getString(R.string.hide_comments)
        else
            getString(R.string.show_comments, state.comments.size)

        // Update comments list
        commentsAdapter.submitList(state.comments)

        // Show progress or load more button
        val progressBar = commentsSection.findViewById<ProgressBar>(R.id.progressBarComments)
        val loadMoreButton = commentsSection.findViewById<Button>(R.id.btnLoadMoreComments)
        val noCommentsText = commentsSection.findViewById<TextView>(R.id.txtNoComments)

        progressBar.visibility = if (state.isLoadingComments) View.VISIBLE else View.GONE
        loadMoreButton.visibility = if (!state.isLoadingComments && state.hasMoreComments)
            View.VISIBLE
        else
            View.GONE

        // Show no comments text if needed
        noCommentsText.visibility = if (!state.isLoadingComments && state.comments.isEmpty())
            View.VISIBLE
        else
            View.GONE
    }




    private fun dismissRatingSheet() {
        ratingBottomSheet?.dismiss()
        ratingBottomSheet = null
    }

    private fun showMessage(message: String) {
        Snackbar.make(binding.root, message, Snackbar.LENGTH_SHORT).show()
    }

    override fun onResume() {
        super.onResume()
        // Refresh data if returning from rating bottom sheet
        if (ratingBottomSheet?.isAdded == true && !ratingBottomSheet?.isVisible!!) {
            viewModel.refreshAfterRating()
            ratingBottomSheet = null
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
        ratingBottomSheet = null
    }
}