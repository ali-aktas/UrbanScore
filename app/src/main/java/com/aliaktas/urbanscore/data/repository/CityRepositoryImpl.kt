package com.aliaktas.urbanscore.data.repository

import android.content.ContentValues.TAG
import android.util.Log
import com.aliaktas.urbanscore.data.model.CategoryRatings
import com.aliaktas.urbanscore.data.model.CityModel
import com.aliaktas.urbanscore.data.model.CommentModel
import com.aliaktas.urbanscore.data.model.CuratedCityItem
import com.aliaktas.urbanscore.data.model.PaginatedResult
import com.aliaktas.urbanscore.data.model.UserRatingModel
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import com.google.firebase.firestore.Query
import kotlinx.coroutines.flow.first

class CityRepositoryImpl @Inject constructor(
    private val firestore: FirebaseFirestore
) : CityRepository {

    companion object {
        private const val CITIES_COLLECTION = "cities"
        //private const val USER_RATINGS_COLLECTION = "user_ratings"
    }

    override suspend fun getAllCities(): Flow<List<CityModel>> = callbackFlow {
        val subscription = firestore.collection(CITIES_COLLECTION)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }

                val cities = snapshot?.documents?.mapNotNull { doc ->
                    try {
                        val model = doc.toObject(CityModel::class.java)
                        // Assign the document ID to the model ID
                        model?.copy(id = doc.id)
                    } catch (e: Exception) {
                        Log.e("CityRepository", "Error converting document", e)
                        null
                    }
                } ?: emptyList()

                trySend(cities)
            }

        awaitClose { subscription.remove() }
    }

    suspend fun getTopCities(limit: Int): Flow<List<CityModel>> = callbackFlow {
        val subscription = firestore.collection(CITIES_COLLECTION)
            .orderBy("averageRating", Query.Direction.DESCENDING)
            .limit(limit.toLong())
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }

                val cities = snapshot?.documents?.mapNotNull { doc ->
                    try {
                        val model = doc.toObject(CityModel::class.java)
                        model?.copy(id = doc.id)
                    } catch (e: Exception) {
                        Log.e("CityRepository", "Error converting document", e)
                        null
                    }
                } ?: emptyList()

                trySend(cities)
            }

        awaitClose { subscription.remove() }
    }

    // CityRepositoryImpl.kt içinde
    override suspend fun getCityById(cityId: String): Flow<CityModel?> = callbackFlow {
        try {
            // ÖNEMLİ: Doğru şekilde belge referansı oluştur
            val documentRef = firestore.collection(CITIES_COLLECTION).document(cityId)

            val subscription = documentRef.addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }

                if (snapshot != null && snapshot.exists()) {
                    try {
                        val city = snapshot.toObject(CityModel::class.java)?.copy(id = snapshot.id)
                        trySend(city)
                    } catch (e: Exception) {
                        Log.e("CityRepository", "Error converting document", e)
                        trySend(null)
                    }
                } else {
                    trySend(null)
                }
            }

            awaitClose { subscription.remove() }
        } catch (e: Exception) {
            Log.e("CityRepository", "Error in getCityById", e)
            close(e)
        }
    }

    override suspend fun getUserRatings(userId: String): Flow<List<UserRatingModel>> = callbackFlow {
        // Artık user_ratings koleksiyonu kullanmadığımız için,
        // boş liste döndürebiliriz veya alternatif bir implementasyon yapabiliriz
        trySend(emptyList())
        awaitClose { }
    }

    // CityRepositoryImpl.kt içine yeni metot ekle
    override suspend fun getCitiesByCategoryPaginated(
        categoryName: String,
        limit: Int,
        lastVisible: DocumentSnapshot?
    ): Flow<PaginatedResult<CityModel>> = callbackFlow {
        // Kategori adını doğrulayalım
        val validCategories = setOf("environment", "safety", "livability", "cost", "social")
        val category = if (categoryName in validCategories) categoryName else "averageRating"

        // Kategori puanlamasına göre alanı ayarlama
        val fieldPath = if (category == "averageRating") "averageRating" else "ratings.$category"

        // Sorgu oluştur
        var query = firestore.collection(CITIES_COLLECTION)
            .orderBy(fieldPath, Query.Direction.DESCENDING)

        // Eğer son görünen belge varsa, ondan sonrasını al
        if (lastVisible != null) {
            query = query.startAfter(lastVisible)
        }

        // Limit uygula
        query = query.limit(limit.toLong())

        try {
            // Verileri al
            val querySnapshot = query.get().await()

            // Şehir listesini dönüştür
            val cities = querySnapshot.documents.mapNotNull { document ->
                try {
                    val city = document.toObject(CityModel::class.java)
                    city?.copy(id = document.id)
                } catch (e: Exception) {
                    Log.e("CityRepository", "Error converting document: ${e.message}")
                    null
                }
            }

            // Son belgeyi al
            val lastDoc = querySnapshot.documents.lastOrNull()

            // Daha fazla öğe olup olmadığını kontrol et
            val hasMore = cities.size == limit && cities.isNotEmpty()

            // Sonucu gönder
            trySend(PaginatedResult(cities, lastDoc, hasMore))
        } catch (e: Exception) {
            Log.e("CityRepository", "Error getting paginated cities: ${e.message}")
            close(e)
        }

        awaitClose { }
    }

    override suspend fun getCitiesByCategoryRating(categoryName: String, limit: Int): Flow<List<CityModel>> = callbackFlow {
        // Kategori adını doğrulayalım
        val validCategories = setOf("environment", "safety", "livability", "cost", "social")
        val category = if (categoryName in validCategories) categoryName else "averageRating"

        // Kategori puanlamasına göre alanı ayarlama
        val fieldPath = if (category == "averageRating") "averageRating" else "ratings.$category"

        val subscription = firestore.collection(CITIES_COLLECTION)
            .orderBy(fieldPath, Query.Direction.DESCENDING)
            .limit(limit.toLong())
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }

                val cities = snapshot?.documents?.mapNotNull { doc ->
                    try {
                        val model = doc.toObject(CityModel::class.java)
                        // Döküman ID'sini modele atayalım
                        model?.copy(id = doc.id)
                    } catch (e: Exception) {
                        Log.e("CityRepository", "Error converting document", e)
                        null
                    }
                } ?: emptyList()

                trySend(cities)
            }

        awaitClose { subscription.remove() }
    }

    override suspend fun rateCity(cityId: String, userId: String, ratings: CategoryRatings): Result<Unit> = try {
        // Öndalık basamakları düzenle
        val formattedRatings = CategoryRatings(
            environment = (ratings.environment * 100).toInt() / 100.0,
            safety = (ratings.safety * 100).toInt() / 100.0,
            livability = (ratings.livability * 100).toInt() / 100.0,
            cost = (ratings.cost * 100).toInt() / 100.0,
            social = (ratings.social * 100).toInt() / 100.0
        )

        // İşlemi bir transaction içinde gerçekleştir
        firestore.runTransaction { transaction ->
            val cityRef = firestore.collection(CITIES_COLLECTION).document(cityId)
            val cityDoc = transaction.get(cityRef)

            // Şehir verilerini al
            val city = cityDoc.toObject(CityModel::class.java)
                ?: throw Exception("City not found")

            // Kullanıcının önceki puanını users koleksiyonundan al
            val userRef = firestore.collection("users").document(userId)
            val userDoc = transaction.get(userRef)

            @Suppress("UNCHECKED_CAST")
            val visitedCities = userDoc.get("visited_cities") as? Map<String, Double> ?: emptyMap()
            val oldRating = visitedCities[cityId]
            val wasRatedBefore = oldRating != null

            // Şehrin ortalama puanını güncelle
            if (wasRatedBefore) {
                // Bu bir güncelleme - eski puanlama etkisini kaldır
                val newRatings = CategoryRatings(
                    environment = updateRatingForEdit(city.ratings.environment, oldRating!!,
                        formattedRatings.environment, city.ratingCount),
                    safety = updateRatingForEdit(city.ratings.safety, oldRating,
                        formattedRatings.safety, city.ratingCount),
                    livability = updateRatingForEdit(city.ratings.livability, oldRating,
                        formattedRatings.livability, city.ratingCount),
                    cost = updateRatingForEdit(city.ratings.cost, oldRating,
                        formattedRatings.cost, city.ratingCount),
                    social = updateRatingForEdit(city.ratings.social, oldRating,
                        formattedRatings.social, city.ratingCount)
                )

                // Yeni genel ortalamayı ağırlıklı formül kullanarak hesapla
                val newAverageRating = calculateWeightedAverage(newRatings)

                // 2 ondalık basamağa yuvarla
                val formattedAverage = (newAverageRating * 100).toInt() / 100.0

                // Şehir belgesini güncelle - puanlama sayısını artırmadan
                transaction.update(cityRef, mapOf(
                    "ratings" to newRatings,
                    "averageRating" to formattedAverage
                ))
            } else {
                // Bu yeni bir puanlama
                val newRatingCount = city.ratingCount + 1
                val newRatings = CategoryRatings(
                    environment = updateAverage(city.ratings.environment, formattedRatings.environment, city.ratingCount),
                    safety = updateAverage(city.ratings.safety, formattedRatings.safety, city.ratingCount),
                    livability = updateAverage(city.ratings.livability, formattedRatings.livability, city.ratingCount),
                    cost = updateAverage(city.ratings.cost, formattedRatings.cost, city.ratingCount),
                    social = updateAverage(city.ratings.social, formattedRatings.social, city.ratingCount)
                )

                // Yeni genel ortalamayı ağırlıklı formül kullanarak hesapla
                val newAverageRating = calculateWeightedAverage(newRatings)

                // 2 ondalık basamağa yuvarla
                val formattedAverage = (newAverageRating * 100).toInt() / 100.0

                // Şehir belgesini güncelle ve puanlama sayısını artır
                transaction.update(cityRef, mapOf(
                    "ratings" to newRatings,
                    "averageRating" to formattedAverage,
                    "ratingCount" to newRatingCount
                ))
            }
        }.await()

        Result.success(Unit)
    } catch (e: Exception) {
        Log.e(TAG, "rateCity failed", e)
        Result.failure(e)
    }

    // Calculate weighted average based on your formula
    private fun calculateWeightedAverage(ratings: CategoryRatings): Double {
        return ((ratings.environment * 1.3) +
                (ratings.safety * 1.1) +
                (ratings.livability * 1.0) +
                (ratings.cost * 1.0) +
                (ratings.social * 1.2)) / 5.6
    }

    // Update average for a new rating
    private fun updateAverage(oldAvg: Double, newValue: Double, oldCount: Int): Double {
        val newAvg = ((oldAvg * oldCount) + newValue) / (oldCount + 1)
        // Format to 2 decimal places
        return (newAvg * 100).toInt() / 100.0
    }

    // Update average when editing an existing rating
    private fun updateRatingForEdit(currentAvg: Double, oldValue: Double, newValue: Double, totalCount: Int): Double {
        // Formula: ((currentAvg * totalCount) - oldValue + newValue) / totalCount
        val newAvg = ((currentAvg * totalCount) - oldValue + newValue) / totalCount
        // Format to 2 decimal places
        return (newAvg * 100).toInt() / 100.0
    }

    // CityRepositoryImpl.kt içindeki getCuratedCities metodunu değiştir
    override suspend fun getCuratedCities(listType: String): Flow<List<CuratedCityItem>> = callbackFlow {
        Log.d("CityRepository", "Loading curated cities for type: $listType")

        val subscription = firestore.collection("curated_lists")
            .whereEqualTo("listType", listType)
            // position'a göre sıralamayı kaldır
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.e("CityRepository", "Error getting curated cities", error)
                    trySend(emptyList())
                    return@addSnapshotListener
                }

                val cities = snapshot?.documents?.mapNotNull { doc ->
                    try {
                        Log.d("CityRepository", "Found curated document: ${doc.id}")
                        val model = doc.toObject(CuratedCityItem::class.java)
                        model?.copy(id = doc.id)
                    } catch (e: Exception) {
                        Log.e("CityRepository", "Error converting document", e)
                        null
                    }
                } ?: emptyList()

                // Burada manüel sıralama yap
                val sortedCities = cities.sortedBy { it.position }

                Log.d("CityRepository", "Loaded ${sortedCities.size} curated cities")
                trySend(sortedCities)
            }

        awaitClose { subscription.remove() }
    }



    // COMMENTS


    // app/src/main/java/com/aliaktas/urbanscore/data/repository/CityRepositoryImpl.kt
// Sınıfa eklenecek yeni metotlar

    override suspend fun getComments(
        cityId: String,
        limit: Long,
        lastComment: DocumentSnapshot?
    ): Flow<PaginatedResult<CommentModel>> = callbackFlow {
        try {
            // Önce beğeni sayısına göre, sonra zamana göre sıralayarak yorumları al
            var query = firestore.collection(CITIES_COLLECTION)
                .document(cityId)
                .collection("comments")
                .orderBy("likeCount", Query.Direction.DESCENDING)
                .orderBy("timestamp", Query.Direction.DESCENDING)
                .limit(limit)

            // Pagination için son yorum varsa, ondan sonrasını al
            if (lastComment != null) {
                query = query.startAfter(lastComment)
            }

            val querySnapshot = query.get().await()

            // Kullanıcının beğendiği yorumları almak için
            val currentUser = FirebaseAuth.getInstance().currentUser
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
            val currentUser = FirebaseAuth.getInstance().currentUser
                ?: return Result.failure(Exception("User not logged in"))

            val userRef = firestore.collection("users").document(currentUser.uid)
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
                .collection("comments")
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
            val currentUser = FirebaseAuth.getInstance().currentUser
                ?: return Result.failure(Exception("User not logged in"))

            // Önce yorumu al ve kullanıcının kendi yorumu mu kontrol et
            val commentRef = firestore.collection(CITIES_COLLECTION)
                .document(cityId)
                .collection("comments")
                .document(commentId)

            val comment = commentRef.get().await()

            if (comment.getString("userId") != currentUser.uid) {
                return Result.failure(Exception("You can only delete your own comments"))
            }

            // Yorumu sil
            commentRef.delete().await()

            // Kullanıcının beğendiği yorumlar listesinden de sil
            try {
                firestore.collection("users")
                    .document(currentUser.uid)
                    .collection("liked_comments")
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

    // CityRepositoryImpl.kt içinde likeComment metodunda güncellemeler
    override suspend fun likeComment(
        cityId: String,
        commentId: String,
        like: Boolean
    ): Result<Unit> {
        return try {
            val currentUser = FirebaseAuth.getInstance().currentUser
                ?: return Result.failure(Exception("User not logged in"))

            firestore.runTransaction { transaction ->
                // Yorum referansı
                val commentRef = firestore.collection(CITIES_COLLECTION)
                    .document(cityId)
                    .collection("comments")
                    .document(commentId)

                // Kullanıcının beğendiği yorumlar listesi referansı
                val userLikesRef = firestore.collection("users")
                    .document(currentUser.uid)
                    .collection("liked_comments")
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
        val currentUser = FirebaseAuth.getInstance().currentUser

        if (currentUser == null) {
            trySend(emptyList())
            close()
            return@callbackFlow
        }

        val listener = firestore.collection("users")
            .document(currentUser.uid)
            .collection("liked_comments")
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