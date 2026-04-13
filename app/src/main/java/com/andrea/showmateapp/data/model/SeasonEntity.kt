package com.andrea.showmateapp.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.andrea.showmateapp.data.network.SeasonResponse

@Entity(
    tableName = "seasons",
    indices = [
        androidx.room.Index(value = ["showId", "seasonNumber"], unique = true)
    ]
)
data class SeasonEntity(
    @PrimaryKey val id: Int,
    val showId: Int,
    val seasonNumber: Int,
    val name: String,
    val overview: String,
    val posterPath: String?,
    val airDate: String?,
    val tmdbId: String,
    val cachedAt: Long = System.currentTimeMillis()
)

fun SeasonResponse.toEntity(showId: Int): SeasonEntity = SeasonEntity(
    id = id,
    showId = showId,
    seasonNumber = seasonNumber,
    name = name,
    overview = overview,
    posterPath = posterPath,
    airDate = airDate,
    tmdbId = tmdbId
)

fun SeasonEntity.toDomain(): SeasonResponse = SeasonResponse(
    id = id,
    seasonNumber = seasonNumber,
    name = name,
    overview = overview,
    posterPath = posterPath,
    airDate = airDate,
    episodes = emptyList(),
    tmdbId = tmdbId
)
