package com.andrea.showmateapp.data.model

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

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
    val cachedAt: Long = System.currentTimeMillis(),
    @ColumnInfo(defaultValue = "[]")
    val episodesJson: String = "[]"
)

private val gson = Gson()
private val episodeListType = object : TypeToken<List<Episode>>() {}.type

fun SeasonResponse.toEntity(showId: Int): SeasonEntity = SeasonEntity(
    id = id,
    showId = showId,
    seasonNumber = seasonNumber,
    name = name,
    overview = overview,
    posterPath = posterPath,
    airDate = airDate,
    tmdbId = tmdbId,
    episodesJson = gson.toJson(episodes)
)

fun SeasonEntity.toDomain(): SeasonResponse = SeasonResponse(
    id = id,
    seasonNumber = seasonNumber,
    name = name,
    overview = overview,
    posterPath = posterPath,
    airDate = airDate,
    tmdbId = tmdbId,
    episodes = runCatching<List<Episode>> { gson.fromJson(episodesJson, episodeListType) }.getOrElse { emptyList() }
)
