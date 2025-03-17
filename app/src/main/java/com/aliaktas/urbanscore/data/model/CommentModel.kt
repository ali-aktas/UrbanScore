package com.aliaktas.urbanscore.data.model

import com.google.firebase.Timestamp

data class CommentModel(
    val id: String = "",
    val text: String = "",
    val timestamp: Timestamp = Timestamp.now(),
    val userId: String = "",
    val userName: String = "",
    val userPhotoUrl: String = "",
    var likeCount: Int = 0,
    val isEdited: Boolean = false,
    // Kullanıcının bu yorumu beğenip beğenmediğini takip etmek için
    var isLikedByUser: Boolean = false
)