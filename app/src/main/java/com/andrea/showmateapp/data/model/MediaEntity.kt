package com.andrea.showmateapp.data.model

import androidx.compose.runtime.Immutable
import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import com.andrea.showmateapp.data.network.MediaContent

@Entity(
    tableName = "media_content",
    primaryKeys = ["id", "category"],
    indices = [Index(value = ["category"])]
)
@Immutable
data class MediaEntity(
    val id: Int,
    val name: String,
    val overview: String,
    val posterPath: String,
    val voteAverage: Float = 0f,
    val backdropPath: String? = null,
    val category: String,
    val genreIds: String = "",
    @ColumnInfo(defaultValue = "0")
    val cachedAt: Long = 0L
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
