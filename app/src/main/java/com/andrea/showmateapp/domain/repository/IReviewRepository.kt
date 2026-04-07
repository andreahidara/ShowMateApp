package com.andrea.showmateapp.domain.repository

import com.andrea.showmateapp.data.model.Review
import com.andrea.showmateapp.data.model.ReviewPage

interface IReviewRepository {

    suspend fun submitReview(review: Review): String

    suspend fun updateReview(
        reviewId: String,
        rating: Int,
        text: String,
        hasSpoiler: Boolean,
        seasonNumber: Int
    )

    suspend fun deleteReview(reviewId: String)

    suspend fun getMyReview(mediaId: Int, seasonNumber: Int): Review?

    suspend fun getPublicReviews(
        mediaId: Int,
        seasonNumber: Int,
        pageSize: Int = 10,
        cursorId: String? = null
    ): ReviewPage

    suspend fun getFriendReviews(
        mediaId: Int,
        seasonNumber: Int,
        friendEmails: List<String>
    ): List<Review>

    suspend fun toggleLike(reviewId: String): Boolean

    suspend fun reportReview(reviewId: String)
}
