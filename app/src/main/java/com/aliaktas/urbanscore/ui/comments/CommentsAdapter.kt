package com.aliaktas.urbanscore.ui.comments

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.aliaktas.urbanscore.R
import com.aliaktas.urbanscore.data.model.CommentModel
import com.aliaktas.urbanscore.databinding.ItemCommentBinding
import com.bumptech.glide.Glide
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class CommentsAdapter(
    private val onLikeClick: (CommentModel, Boolean) -> Unit,
    private val onDeleteClick: (String) -> Unit
) : ListAdapter<CommentModel, CommentsAdapter.CommentViewHolder>(CommentDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CommentViewHolder {
        val binding = ItemCommentBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return CommentViewHolder(binding)
    }

    override fun onBindViewHolder(holder: CommentViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class CommentViewHolder(
        private val binding: ItemCommentBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        // CommentsAdapter.kt içindeki bind metodunda
        fun bind(comment: CommentModel) {
            binding.apply {
                txtUsername.text = comment.userName
                txtComment.text = comment.text
                txtLikeCount.text = comment.likeCount.toString()
                txtTimeAgo.text = getTimeAgo(comment.timestamp)

                // Kullanıcı resmi
                if (comment.userPhotoUrl.isNotEmpty()) {
                    Glide.with(root.context)
                        .load(comment.userPhotoUrl)
                        .circleCrop()
                        .into(imgUserAvatar)
                } else {
                    imgUserAvatar.setImageResource(R.drawable.loginicon2)
                }

                // Beğeni durumuna göre butonu güncelle
                if (comment.isLikedByUser) {
                    btnLike.setImageResource(R.drawable.ic_liked) // Beğenilmiş ikon
                } else {
                    btnLike.setImageResource(R.drawable.ic_like) // Normal beğeni ikonu
                }

                btnLike.setOnClickListener {
                    onLikeClick(comment, !comment.isLikedByUser)
                    // Optimistik UI güncelleme - hemen görünümü değiştir
                    // İşlem başarısız olursa ViewModel tarafından geri alınacak
                    comment.isLikedByUser = !comment.isLikedByUser
                    if (comment.isLikedByUser) {
                        comment.likeCount++
                        btnLike.setImageResource(R.drawable.ic_liked)
                    } else {
                        if (comment.likeCount > 0) comment.likeCount--
                        btnLike.setImageResource(R.drawable.ic_like)
                    }
                    txtLikeCount.text = comment.likeCount.toString()
                }

                // Kullanıcının kendi yorumu ise silme butonu göster
                val currentUserId = FirebaseAuth.getInstance().currentUser?.uid
                btnDelete.visibility = if (currentUserId == comment.userId) View.VISIBLE else View.GONE
                btnDelete.setOnClickListener {
                    onDeleteClick(comment.id)
                }

                // Düzenlenmiş yorum işareti
                txtEdited.visibility = if (comment.isEdited) View.VISIBLE else View.GONE
            }
        }

        private fun getTimeAgo(timestamp: Timestamp): String {
            val dateFormat = SimpleDateFormat("dd MMM yyyy", Locale.ENGLISH)
            return dateFormat.format(Date(timestamp.seconds * 1000))
        }
    }

    private class CommentDiffCallback : DiffUtil.ItemCallback<CommentModel>() {
        override fun areItemsTheSame(oldItem: CommentModel, newItem: CommentModel): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: CommentModel, newItem: CommentModel): Boolean {
            return oldItem == newItem
        }
    }
}