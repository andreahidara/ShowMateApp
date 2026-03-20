package com.example.showmateapp.data.model

import androidx.room.Entity
import com.example.showmateapp.data.network.MediaContent

@Entity(tableName = "media_content", primaryKeys = ["id", "category"])
data class MediaEntity(
    val id: Int,
    val name: String,
    val overview: String,
    val posterPath: String,
    val voteAverage: Float = 0f,
    val backdropPath: String? = null,
    val category: String,
    val genreIds: String = "",   // comma-separated list of genre IDs
    val cachedAt: Long = 0L      // epoch ms; used for TTL-based cache invalidation
)

fun MediaContent.toEntity(category: String): MediaEntity {
    return MediaEntity(
        id = this.id,
        name = this.name,
        overview = this.overview,
        posterPath = this.posterPath ?: "",
        voteAverage = this.voteAverage,
        backdropPath = this.backdropPath,
        category = category,
        genreIds = this.safeGenreIds.joinToString(","),
        cachedAt = System.currentTimeMillis()
    )
}

fun MediaEntity.toDomain(): MediaContent {
    val parsedGenreIds = genreIds
        .split(",")
        .filter { it.isNotBlank() }
        .mapNotNull { it.toIntOrNull() }
    return MediaContent(
        id = this.id,
        name = this.name,
        overview = this.overview,
        posterPath = this.posterPath.ifEmpty { null },
        voteAverage = this.voteAverage,
        backdropPath = this.backdropPath,
        genreIds = parsedGenreIds
    )
}
