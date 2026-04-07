package com.andrea.showmateapp.data.repository

import com.andrea.showmateapp.data.model.Review
import com.andrea.showmateapp.data.model.ReviewPage
import com.andrea.showmateapp.di.IoDispatcher
import com.andrea.showmateapp.domain.repository.IReviewRepository
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ReviewRepository @Inject constructor(
    private val db: FirebaseFirestore,
    private val auth: FirebaseAuth,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher
) : IReviewRepository {

    private val reviews = db.collection("reviews")

    override suspend fun submitReview(review: Review): String = withContext(ioDispatcher) {
        val docRef = reviews.document()
        val withId = review.copy(id = docRef.id)
        docRef.set(withId).await()
        docRef.id
    }

    override suspend fun updateReview(
        reviewId: String,
        rating: Int,
        text: String,
        hasSpoiler: Boolean,
        seasonNumber: Int
    ) = withContext(ioDispatcher) {
        try {
            reviews.document(reviewId).update(
                mapOf(
                    "rating"       to rating,
                    "text"         to text,
                    "hasSpoiler"   to hasSpoiler,
                    "seasonNumber" to seasonNumber,
                    "updatedAt"    to System.currentTimeMillis()
                )
            ).await()
        } catch (e: Exception) {
            if (e is CancellationException) throw e
        }
        Unit
    }

    override suspend fun deleteReview(reviewId: String) = withContext(ioDispatcher) {
        try { reviews.document(reviewId).delete().await() }
        catch (e: Exception) { if (e is CancellationException) throw e }
        Unit
    }

    override suspend fun getMyReview(mediaId: Int, seasonNumber: Int): Review? =
        withContext(ioDispatcher) {
            val uid = auth.currentUser?.uid ?: return@withContext null
            try {
                reviews
                    .whereEqualTo("userId", uid)
                    .whereEqualTo("mediaId", mediaId)
                    .whereEqualTo("seasonNumber", seasonNumber)
                    .limit(1)
                    .get().await()
                    .documents.firstOrNull()?.toObject(Review::class.java)
            } catch (e: Exception) {
                if (e is CancellationException) throw e
                null
            }
        }

    override suspend fun getPublicReviews(
        mediaId: Int,
        seasonNumber: Int,
        pageSize: Int,
        cursorId: String?
    ): ReviewPage = withContext(ioDispatcher) {
        try {
            val base = reviews
                .whereEqualTo("mediaId", mediaId)
                .whereEqualTo("seasonNumber", seasonNumber)
                .orderBy("likeCount", Query.Direction.DESCENDING)
                .orderBy("createdAt", Query.Direction.DESCENDING)
                .limit((pageSize + 1).toLong())

            val query = if (cursorId != null) {
                val cursor = reviews.document(cursorId).get().await()
                base.startAfter(cursor)
            } else {
                base
            }

            val snap = query.get().await()
            val docs = snap.documents
            val hasMore = docs.size > pageSize
            val pageDocs = if (hasMore) docs.dropLast(1) else docs

            val reviewList = pageDocs
                .mapNotNull { it.toObject(Review::class.java) }
                .filter { it.reportCount < REPORT_HIDE_THRESHOLD }

            ReviewPage(
                reviews      = reviewList,
                lastCursorId = pageDocs.lastOrNull()?.id,
                hasMore      = hasMore
            )
        } catch (e: Exception) {
            if (e is CancellationException) throw e
            ReviewPage()
        }
    }

    override suspend fun getFriendReviews(
        mediaId: Int,
        seasonNumber: Int,
        friendEmails: List<String>
    ): List<Review> = withContext(ioDispatcher) {
        if (friendEmails.isEmpty()) return@withContext emptyList()
        try {
            val chunks = friendEmails.distinct().chunked(30)
            chunks.flatMap { chunk ->
                reviews
                    .whereEqualTo("mediaId", mediaId)
                    .whereEqualTo("seasonNumber", seasonNumber)
                    .whereIn("userEmail", chunk)
                    .get().await()
                    .toObjects(Review::class.java)
            }.sortedByDescending { it.createdAt }
        } catch (e: Exception) {
            if (e is CancellationException) throw e
            emptyList()
        }
    }

    override suspend fun toggleLike(reviewId: String): Boolean = withContext(ioDispatcher) {
        val uid = auth.currentUser?.uid ?: return@withContext false
        try {
            var nowLiked = false
            db.runTransaction { tx ->
                val ref  = reviews.document(reviewId)
                val doc  = tx.get(ref)
                val rv   = doc.toObject(Review::class.java) ?: return@runTransaction
                val liked = rv.likedByIds.toMutableList()
                if (uid in liked) {
                    liked.remove(uid)
                    nowLiked = false
                } else {
                    liked.add(uid)
                    nowLiked = true
                }
                tx.update(ref, mapOf("likedByIds" to liked, "likeCount" to liked.size))
            }.await()
            nowLiked
        } catch (e: Exception) {
            if (e is CancellationException) throw e
            false
        }
    }

    override suspend fun reportReview(reviewId: String) = withContext(ioDispatcher) {
        try {
            reviews.document(reviewId)
                .update("reportCount", FieldValue.increment(1))
                .await()
        } catch (e: Exception) {
            if (e is CancellationException) throw e
        }
        Unit
    }

    companion object {
        private const val REPORT_HIDE_THRESHOLD = 3
    }
}
