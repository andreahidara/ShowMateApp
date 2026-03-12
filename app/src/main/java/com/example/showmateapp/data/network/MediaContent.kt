package com.example.showmateapp.data.network

import com.google.gson.annotations.SerializedName

data class MediaContent(
    val id: Int = 0,
    val name: String = "",
    val overview: String = "",
    @SerializedName("poster_path") val posterPath: String? = null,
    @SerializedName("genre_ids") val genreIds: List<Int>? = emptyList(),
    val genres: List<Genre>? = null,
    val popularity: Float = 0f,
    val keywords: KeywordsResponse? = null,
    val credits: CreditsResponse? = null,
    var affinityScore: Float = 0f,
    @SerializedName("first_air_date") val firstAirDate: String? = null,
    @SerializedName("number_of_seasons") val numberOfSeasons: Int? = null,
    val status: String? = null
) {
    val safeGenreIds: List<Int>
        get() = genreIds ?: genres?.map { it.id } ?: emptyList()
}

data class Genre(val id: Int = 0, val name: String = "")

data class KeywordsResponse(
    val results: List<Keyword> = emptyList()
)

data class Keyword(
    val id: Int = 0,
    val name: String = ""
)

data class CreditsResponse(
    val cast: List<CastMember> = emptyList()
)

data class CastMember(
    val id: Int = 0,
    val name: String = "",
    @SerializedName("profile_path") val profilePath: String? = null
)
