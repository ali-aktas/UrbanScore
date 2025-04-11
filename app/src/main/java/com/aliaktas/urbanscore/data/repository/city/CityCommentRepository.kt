package com.aliaktas.urbanscore.data.repository.city

import com.aliaktas.urbanscore.data.model.CommentModel
import com.aliaktas.urbanscore.data.model.PaginatedResult
import com.google.firebase.firestore.DocumentSnapshot
import kotlinx.coroutines.flow.Flow

interface CityCommentRepository {
    suspend fun getComments(cityId: String, limit: Long, lastComment: DocumentSnapshot? = null): Flow<PaginatedResult<CommentModel>>
    suspend fun addComment(cityId: String, text: String): Result<String>
    suspend fun deleteComment(cityId: String, commentId: String): Result<Unit>
    suspend fun likeComment(cityId: String, commentId: String, like: Boolean): Result<Unit>
    suspend fun getUserLikedComments(cityId: String): Flow<List<String>>
}