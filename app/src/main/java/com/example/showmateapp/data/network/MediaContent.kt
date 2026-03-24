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
    val affinityScore: Float = 0f,
    @SerializedName("first_air_date") val firstAirDate: String? = null,
    @SerializedName("number_of_seasons") val numberOfSeasons: Int? = null,
    val status: String? = null,
    @SerializedName("vote_average") val voteAverage: Float = 0f,
    @SerializedName("vote_count") val voteCount: Int = 0,
    @SerializedName("episode_run_time") val episodeRunTime: List<Int>? = null,
    @SerializedName("backdrop_path") val backdropPath: String? = null,
    @SerializedName("watch/providers") val watchProviders: WatchProvidersResponse? = null,
    val seasons: List<Season>? = null,
    val videos: VideosResponse? = null
) {
    val safeGenreIds: List<Int>
        get() = genreIds ?: genres?.map { it.id } ?: emptyList()

    val creatorIds: List<Int>
        get() = credits?.crew
            ?.filter { it.job in CREATOR_JOBS }
            ?.map { it.id } ?: emptyList()

    val keywordNames: List<String>
        get() = keywords?.results?.map { it.name } ?: emptyList()

    companion object {
        val CREATOR_JOBS = setOf("Creator", "Executive Producer", "Showrunner", "Series Director")
    }
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
    val cast: List<CastMember> = emptyList(),
    val crew: List<CrewMember> = emptyList()
)

data class CastMember(
    val id: Int = 0,
    val name: String = "",
    @SerializedName("profile_path") val profilePath: String? = null
)

data class CrewMember(
    val id: Int = 0,
    val name: String = "",
    val job: String = "",
    @SerializedName("profile_path") val profilePath: String? = null
)

data class WatchProvidersResponse(
    val results: Map<String, CountryProviders>? = null
)

data class CountryProviders(
    val link: String? = null,
    val flatrate: List<Provider>? = null,
    val rent: List<Provider>? = null,
    val buy: List<Provider>? = null
)

data class Provider(
    @SerializedName("provider_id") val providerId: Int = 0,
    @SerializedName("provider_name") val providerName: String = "",
    @SerializedName("logo_path") val logoPath: String? = null
)

data class Season(
    val id: Int = 0,
    val name: String = "",
    @SerializedName("season_number") val seasonNumber: Int = 0,
    @SerializedName("episode_count") val episodeCount: Int = 0,
    @SerializedName("poster_path") val posterPath: String? = null
)

data class VideosResponse(
    val results: List<Video>? = null
)

data class Video(
    val id: String = "",
    val key: String = "",
    val site: String = "",
    val type: String = ""
)

data class PersonSearchResponse(
    val results: List<PersonSearchResult> = emptyList()
)

data class PersonSearchResult(
    val id: Int = 0,
    val name: String = "",
    @SerializedName("profile_path") val profilePath: String? = null,
    @SerializedName("known_for_department") val knownForDepartment: String? = null
)

data class PersonTvCreditsResponse(
    val cast: List<MediaContent> = emptyList(),
    val crew: List<PersonTvCrewEntry> = emptyList()
)

data class PersonTvCrewEntry(
    val id: Int = 0,
    val name: String = "",
    @SerializedName("poster_path") val posterPath: String? = null,
    @SerializedName("vote_average") val voteAverage: Float = 0f,
    @SerializedName("vote_count") val voteCount: Int = 0,
    @SerializedName("first_air_date") val firstAirDate: String? = null,
    val overview: String = "",
    val popularity: Float = 0f,
    val job: String = "",
    @SerializedName("genre_ids") val genreIds: List<Int>? = emptyList()
) {
    fun toMediaContent() = MediaContent(
        id = id, name = name, posterPath = posterPath,
        voteAverage = voteAverage, voteCount = voteCount,
        firstAirDate = firstAirDate, overview = overview,
        popularity = popularity, genreIds = genreIds
    )
}
