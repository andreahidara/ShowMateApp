package com.andrea.showmateapp.data.model

data class Review(
    val id: String = "",
    val mediaId: Int = 0,
    val seasonNumber: Int = 0,
    val userId: String = "",
    val userEmail: String = "",
    val username: String = "",
    val rating: Int = 0,
    val text: String = "",
    val hasSpoiler: Boolean = false,
    val likedByIds: List<String> = emptyList(),
    val likeCount: Int = 0,
    val reportCount: Int = 0,
    val createdAt: Long = 0L,
    val updatedAt: Long = 0L
)

data class ReviewPage(
    val reviews: List<Review> = emptyList(),
    val lastCursorId: String? = null,
    val hasMore: Boolean = false
)
