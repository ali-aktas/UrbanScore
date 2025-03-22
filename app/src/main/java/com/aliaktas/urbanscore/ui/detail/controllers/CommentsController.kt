package com.aliaktas.urbanscore.ui.detail.controllers

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ProgressBar
import android.widget.TextView
import androidx.lifecycle.LifecycleOwner
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.aliaktas.urbanscore.R
import com.aliaktas.urbanscore.databinding.FragmentCityDetailBinding
import com.aliaktas.urbanscore.ui.comments.CommentsAdapter
import com.aliaktas.urbanscore.ui.detail.CityDetailState
import com.aliaktas.urbanscore.ui.detail.CityDetailViewModel

/**
 * Yorum bölümünü yöneten controller
 */
class CommentsController(
    private val binding: FragmentCityDetailBinding,
    private val viewModel: CityDetailViewModel,
    private val lifecycleOwner: LifecycleOwner
) : UiController {

    private lateinit var commentsAdapter: CommentsAdapter
    private lateinit var commentsSection: View

    override fun bind(view: View) {
        setupComments(view)

        binding.btnShowComments.setOnClickListener {
            viewModel.toggleComments()
        }

        binding.btnAddComment.setOnClickListener {
            viewModel.showCommentBottomSheet()
        }
    }

    private fun setupComments(rootView: View) {
        // Yorum adaptörünü başlat
        commentsAdapter = CommentsAdapter(
            onLikeClick = { comment, like ->
                viewModel.likeComment(comment.id, like)
            },
            onDeleteClick = { commentId ->
                viewModel.deleteComment(commentId)
            }
        )

        // Yorum bölümünü inflate et
        commentsSection = LayoutInflater.from(rootView.context).inflate(
            R.layout.layout_comments_section,
            binding.nestedScrollView.getChildAt(0) as ViewGroup,
            false
        )

        // RecyclerView'i ayarla
        commentsSection.findViewById<RecyclerView>(R.id.recyclerViewComments).apply {
            adapter = commentsAdapter
            layoutManager = LinearLayoutManager(rootView.context)
        }

        // Daha fazla yorum butonu tıklaması
        commentsSection.findViewById<Button>(R.id.btnLoadMoreComments).setOnClickListener {
            viewModel.loadMoreComments()
        }

        // Yorum bölümünü düzene ekle
        val commentsButtonsContainer = binding.btnShowComments.parent as ViewGroup
        val parentLayout = commentsButtonsContainer.parent as ViewGroup
        val containerIndex = parentLayout.indexOfChild(commentsButtonsContainer)
        parentLayout.addView(commentsSection, containerIndex + 1)

        // Başlangıçta gizle
        commentsSection.visibility = View.GONE
    }

    override fun update(state: CityDetailState) {
        if (state !is CityDetailState.Success) return

        // Yorum görünürlüğünü güncelle
        commentsSection.visibility = if (state.showComments) View.VISIBLE else View.GONE

        // Değiştirme butonu metnini güncelle
        binding.btnShowComments.text = if (state.showComments)
            binding.root.context.getString(R.string.hide_comments)
        else
            binding.root.context.getString(R.string.show_comments, state.commentsCount)

        // Yorum listesini güncelle
        commentsAdapter.submitList(state.comments)

        // İlerleme ve daha fazla butonunu güncelle
        val progressBar = commentsSection.findViewById<ProgressBar>(R.id.progressBarComments)
        val loadMoreButton = commentsSection.findViewById<Button>(R.id.btnLoadMoreComments)
        val noCommentsText = commentsSection.findViewById<TextView>(R.id.txtNoComments)

        progressBar.visibility = if (state.isLoadingComments) View.VISIBLE else View.GONE
        loadMoreButton.visibility = if (!state.isLoadingComments && state.hasMoreComments)
            View.VISIBLE
        else
            View.GONE

        noCommentsText.visibility = if (!state.isLoadingComments && state.comments.isEmpty())
            View.VISIBLE
        else
            View.GONE
    }
}