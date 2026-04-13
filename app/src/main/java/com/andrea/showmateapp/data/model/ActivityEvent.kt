package com.andrea.showmateapp.data.model

import androidx.compose.runtime.Immutable

@Immutable
data class ActivityEvent(
    val id: String = "",
    val userId: String = "",
    val username: String = "",
    val type: String = "",
    val mediaId: Int = 0,
    val mediaTitle: String = "",
    val mediaPoster: String = "",
    val score: Float = 0f,
    val timestamp: Long = 0L
) {
    companion object {
        const val TYPE_LIKED = "liked"
        const val TYPE_ESSENTIAL = "essential"
        const val TYPE_RATED = "rated"
        const val TYPE_WATCHED = "watched"
        const val TYPE_WATCHLISTED = "watchlisted"
    }
}
