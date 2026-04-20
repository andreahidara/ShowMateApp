package com.andrea.showmateapp.data.repository

import com.andrea.showmateapp.data.model.Review
import com.andrea.showmateapp.data.model.ReviewPage
import com.andrea.showmateapp.di.IoDispatcher
import com.andrea.showmateapp.domain.repository.IReviewRepository
import com.andrea.showmateapp.util.safeFirestoreCall
import com.andrea.showmateapp.util.safeFirestoreRun
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.CancellationException
import timber.log.Timber
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext

@Singleton
class ReviewRepository @Inject constructor(
    private val db: FirebaseFirestore,
    private val auth: FirebaseAuth,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher
) : IReviewRepository {

    private val reviews = db.collection("reviews")

    override suspend fun submitReview(review: Review): String = withContext(ioDispatcher) {
        try {
            val docRef = reviews.document()
            docRef.set(review.copy(id = docRef.id)).await()
            docRef.id
        } catch (e: Exception) {
            if (e is CancellationException) throw e
            Timber.e(e, "submitReview failed")
            throw e
        }
    }

    override suspend fun updateReview(
        reviewId: String,
        rating: Int,
        text: String,
        hasSpoiler: Boolean,
        seasonNumber: Int
    ) = withContext(ioDispatcher) {
        safeFirestoreRun {
            reviews.document(reviewId).update(
                mapOf(
                    "rating" to rating,
                    "text" to text,
                    "hasSpoiler" to hasSpoiler,
                    "seasonNumber" to seasonNumber,
                    "updatedAt" to System.currentTimeMillis()
                )
            ).await()
        }
    }

    override suspend fun deleteReview(reviewId: String) = withContext(ioDispatcher) {
        safeFirestoreRun { reviews.document(reviewId).delete().await() }
    }

    override suspend fun getMyReview(mediaId: Int, seasonNumber: Int): Review? = withContext(ioDispatcher) {
        val uid = auth.currentUser?.uid ?: return@withContext null
        safeFirestoreCall(null) {
            reviews
                .whereEqualTo("userId", uid)
                .whereEqualTo("mediaId", mediaId)
                .whereEqualTo("seasonNumber", seasonNumber)
                .limit(1)
                .get().await()
                .documents.firstOrNull()?.toObject(Review::class.java)
        }
    }

    override suspend fun getPublicReviews(
        mediaId: Int,
        seasonNumber: Int,
        pageSize: Int,
        cursorId: String?
    ): ReviewPage = withContext(ioDispatcher) {
        safeFirestoreCall(ReviewPage()) {
            val base = reviews
                .whereEqualTo("mediaId", mediaId)
                .whereEqualTo("seasonNumber", seasonNumber)
                .orderBy("likeCount", Query.Direction.DESCENDING)
                .orderBy("createdAt", Query.Direction.DESCENDING)
                .limit((pageSize + 1).toLong())

            val query = if (cursorId != null) base.startAfter(reviews.document(cursorId).get().await()) else base
            val docs = query.get().await().documents
            val hasMore = docs.size > pageSize
            val pageDocs = if (hasMore) docs.dropLast(1) else docs

            ReviewPage(
                reviews = pageDocs.mapNotNull { it.toObject(Review::class.java) }
                    .filter { it.reportCount < REPORT_HIDE_THRESHOLD },
                lastCursorId = pageDocs.lastOrNull()?.id,
                hasMore = hasMore
            )
        }
    }

    override suspend fun getFriendReviews(mediaId: Int, seasonNumber: Int, friendEmails: List<String>): List<Review> =
        withContext(ioDispatcher) {
            if (friendEmails.isEmpty()) return@withContext emptyList()
            safeFirestoreCall(emptyList()) {
                friendEmails.distinct().chunked(30).flatMap { chunk ->
                    reviews
                        .whereEqualTo("mediaId", mediaId)
                        .whereEqualTo("seasonNumber", seasonNumber)
                        .whereIn("userEmail", chunk)
                        .get().await()
                        .toObjects(Review::class.java)
                }.sortedByDescending { it.createdAt }
            }
        }

    override suspend fun toggleLike(reviewId: String): Boolean = withContext(ioDispatcher) {
        val uid = auth.currentUser?.uid ?: return@withContext false
        safeFirestoreCall(false) {
            var nowLiked = false
            db.runTransaction { tx ->
                val ref = reviews.document(reviewId)
                val rv = tx.get(ref).toObject(Review::class.java) ?: return@runTransaction
                val liked = rv.likedByIds.toMutableList()
                if (uid in liked) { liked.remove(uid); nowLiked = false }
                else { liked.add(uid); nowLiked = true }
                tx.update(ref, mapOf("likedByIds" to liked, "likeCount" to liked.size))
            }.await()
            nowLiked
        }
    }

    override suspend fun reportReview(reviewId: String) = withContext(ioDispatcher) {
        safeFirestoreRun {
            reviews.document(reviewId).update("reportCount", FieldValue.increment(1)).await()
        }
    }

    companion object {
        private const val REPORT_HIDE_THRESHOLD = 3
    }
}
