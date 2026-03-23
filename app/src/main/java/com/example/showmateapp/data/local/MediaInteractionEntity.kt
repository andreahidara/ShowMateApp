package com.example.showmateapp.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "media_interactions")
data class MediaInteractionEntity(
    @PrimaryKey val mediaId: Int,
    val isLiked: Boolean = false,
    val isEssential: Boolean = false,
    val isWatched: Boolean = false,
    // Se guarda cuando el usuario marca como visto — usado para detectar nuevas temporadas
    val lastKnownSeasons: Int = 0
)
