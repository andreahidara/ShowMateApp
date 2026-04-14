package com.andrea.showmateapp.data.model

import com.google.gson.annotations.SerializedName


data class SeasonResponse(
    @SerializedName("_id") val tmdbId: String = "",
    @SerializedName("air_date") val airDate: String? = null,
    val episodes: List<Episode> = emptyList(),
    val name: String = "",
    val overview: String = "",
    val id: Int = 0,
    @SerializedName("poster_path") val posterPath: String? = null,
    @SerializedName("season_number") val seasonNumber: Int = 0
)

data class Episode(
    @SerializedName("air_date") val airDate: String? = null,
    @SerializedName("episode_number") val episodeNumber: Int = 0,
    val id: Int = 0,
    val name: String = "",
    val overview: String = "",
    @SerializedName("production_code") val productionCode: String? = null,
    val runtime: Int? = null,
    @SerializedName("season_number") val seasonNumber: Int = 0,
    @SerializedName("show_id") val showId: Int = 0,
    @SerializedName("still_path") val stillPath: String? = null,
    @SerializedName("vote_average") val voteAverage: Float = 0f,
    @SerializedName("vote_count") val voteCount: Int = 0
)
