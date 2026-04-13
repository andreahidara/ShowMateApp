package com.andrea.showmateapp.data.local

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "media_interactions",
    indices = [
        Index(value = ["isWatched"]),
        Index(value = ["isDisliked"]),
        Index(value = ["isInWatchlist"]),
        Index(value = ["syncPending"])
    ]
)
data class MediaInteractionEntity(
    @PrimaryKey val mediaId: Int,
    val isLiked: Boolean = false,
    val isEssential: Boolean = false,
    val isWatched: Boolean = false,
    val isDisliked: Boolean = false,
    val isInWatchlist: Boolean = false,
    val lastKnownSeasons: Int = 0,
    val syncPending: Boolean = false
)
