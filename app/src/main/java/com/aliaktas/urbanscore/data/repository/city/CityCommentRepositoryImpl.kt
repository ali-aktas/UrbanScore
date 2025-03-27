package com.aliaktas.urbanscore.data.repository.city

import android.util.Log
import com.aliaktas.urbanscore.data.model.CommentModel
import com.aliaktas.urbanscore.data.model.PaginatedResult
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CityCommentRepositoryImpl @Inject constructor(
    private val firestore: FirebaseFirestore,
    private val auth: FirebaseAuth
) : CityCommentRepository {

    companion object {
        private const val CITIES_COLLECTION = "cities"
        private const val USERS_COLLECTION = "users"
        private const val COMMENTS_COLLECTION = "comments"
        private const val LIKED_COMMENTS_COLLECTION = "liked_comments"
        private const val TAG = "CityCommentRepository"
    }

    override suspend fun getComments(
        cityId: String,
        limit: Long,
        lastComment: DocumentSnapshot?
    ): Flow<PaginatedResult<CommentModel>> = callbackFlow {
        try {
            // Önce beğeni sayısına göre, sonra zamana göre sıralayarak yorumları al
            var query = firestore.collection(CITIES_COLLECTION)
                .document(cityId)
                .collection(COMMENTS_COLLECTION)
                .orderBy("likeCount", Query.Direction.DESCENDING)
                .orderBy("timestamp", Query.Direction.DESCENDING)
                .limit(limit)

            // Pagination için son yorum varsa, ondan sonrasını al
            if (lastComment != null) {
                query = query.startAfter(lastComment)
            }

            val querySnapshot = query.get().await()

            // Kullanıcının beğendiği yorumları almak için
            val currentUser = auth.currentUser
            val likedComments = if (currentUser != null) {
                getUserLikedComments(cityId).first()
            } else {
                emptyList()
            }

            val comments = querySnapshot.documents.mapNotNull { doc ->
                try {
                    val comment = doc.toObject(CommentModel::class.java)?.copy(id = doc.id)
                    // Kullanıcının bu yorumu beğenip beğenmediğini işaretle
                    comment?.copy(isLikedByUser = likedComments.contains(doc.id))
                } catch (e: Exception) {
                    Log.e(TAG, "Error converting comment document", e)
                    null
                }
            }

            val lastVisible = querySnapshot.documents.lastOrNull()
            val hasMore = comments.size == limit.toInt() && comments.isNotEmpty()

            trySend(PaginatedResult(comments, lastVisible, hasMore))
        } catch (e: Exception) {
            Log.e(TAG, "Error getting comments", e)
            close(e)
        }

        awaitClose()
    }

    override suspend fun addComment(cityId: String, text: String): Result<String> {
        return try {
            val currentUser = auth.currentUser
                ?: return Result.failure(Exception("User not logged in"))

            val userRef = firestore.collection(USERS_COLLECTION).document(currentUser.uid)
            val userDoc = userRef.get().await()

            val comment = hashMapOf(
                "text" to text,
                "timestamp" to FieldValue.serverTimestamp(),
                "userId" to currentUser.uid,
                "userName" to (userDoc.getString("display_name") ?: currentUser.displayName ?: "Anonymous"),
                "userPhotoUrl" to (userDoc.getString("photo_url") ?: currentUser.photoUrl?.toString() ?: ""),
                "likeCount" to 0,
                "isEdited" to false
            )

            val result = firestore.collection(CITIES_COLLECTION)
                .document(cityId)
                .collection(COMMENTS_COLLECTION)
                .add(comment)
                .await()

            Result.success(result.id)
        } catch (e: Exception) {
            Log.e(TAG, "Error adding comment", e)
            Result.failure(e)
        }
    }

    override suspend fun deleteComment(cityId: String, commentId: String): Result<Unit> {
        return try {
            val currentUser = auth.currentUser
                ?: return Result.failure(Exception("User not logged in"))

            // Önce yorumu al ve kullanıcının kendi yorumu mu kontrol et
            val commentRef = firestore.collection(CITIES_COLLECTION)
                .document(cityId)
                .collection(COMMENTS_COLLECTION)
                .document(commentId)

            val comment = commentRef.get().await()

            if (comment.getString("userId") != currentUser.uid) {
                return Result.failure(Exception("You can only delete your own comments"))
            }

            // Yorumu sil
            commentRef.delete().await()

            // Kullanıcının beğendiği yorumlar listesinden de sil
            try {
                firestore.collection(USERS_COLLECTION)
                    .document(currentUser.uid)
                    .collection(LIKED_COMMENTS_COLLECTION)
                    .document(cityId)
                    .update("commentIds", FieldValue.arrayRemove(commentId))
                    .await()
            } catch (e: Exception) {
                // Beğeniler listesi bulunamadıysa hata verme
                Log.w(TAG, "No liked_comments document found for deletion", e)
            }

            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Error deleting comment", e)
            Result.failure(e)
        }
    }

    override suspend fun likeComment(
        cityId: String,
        commentId: String,
        like: Boolean
    ): Result<Unit> {
        return try {
            val currentUser = auth.currentUser
                ?: return Result.failure(Exception("User not logged in"))

            firestore.runTransaction { transaction ->
                // Yorum referansı
                val commentRef = firestore.collection(CITIES_COLLECTION)
                    .document(cityId)
                    .collection(COMMENTS_COLLECTION)
                    .document(commentId)

                // Kullanıcının beğendiği yorumlar listesi referansı
                val userLikesRef = firestore.collection(USERS_COLLECTION)
                    .document(currentUser.uid)
                    .collection(LIKED_COMMENTS_COLLECTION)
                    .document(cityId)

                val comment = transaction.get(commentRef)
                // Yorum bulunamadığında kontrol ekle
                if (!comment.exists()) {
                    throw Exception("Comment not found")
                }

                val currentLikes = comment.getLong("likeCount") ?: 0
                // Kullanıcı beğeni listesi belgesini al
                val userLikesDoc = transaction.get(userLikesRef)

                if (like) {
                    // Beğeni ekle
                    transaction.update(commentRef, "likeCount", currentLikes + 1)

                    // Kullanıcının beğendiği yorumlar listesine ekle
                    if (!userLikesDoc.exists()) {
                        // Belge yoksa oluştur
                        transaction.set(userLikesRef, hashMapOf("commentIds" to listOf(commentId)))
                    } else {
                        // Varsa güncelle
                        transaction.update(userLikesRef, "commentIds", FieldValue.arrayUnion(commentId))
                    }
                } else {
                    // Beğeni çıkar
                    if (currentLikes > 0) {
                        transaction.update(commentRef, "likeCount", currentLikes - 1)
                    }

                    // Belge yoksa update yapma
                    if (userLikesDoc.exists()) {
                        transaction.update(userLikesRef, "commentIds", FieldValue.arrayRemove(commentId))
                    } else {
                        //TODO
                    }
                }
            }.await()

            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Error liking comment", e)
            Result.failure(e)
        }
    }

    override suspend fun getUserLikedComments(cityId: String): Flow<List<String>> = callbackFlow {
        val currentUser = auth.currentUser

        if (currentUser == null) {
            trySend(emptyList())
            close()
            return@callbackFlow
        }

        val listener = firestore.collection(USERS_COLLECTION)
            .document(currentUser.uid)
            .collection(LIKED_COMMENTS_COLLECTION)
            .document(cityId)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.e(TAG, "Error getting liked comments", error)
                    trySend(emptyList())
                    return@addSnapshotListener
                }

                if (snapshot != null && snapshot.exists()) {
                    @Suppress("UNCHECKED_CAST")
                    val commentIds = snapshot.get("commentIds") as? List<String> ?: emptyList()
                    trySend(commentIds)
                } else {
                    trySend(emptyList())
                }
            }

        awaitClose { listener.remove() }
    }
}