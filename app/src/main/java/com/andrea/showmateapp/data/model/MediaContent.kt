package com.andrea.showmateapp.data.model

import androidx.compose.runtime.Immutable
import com.andrea.showmateapp.data.model.RecommendationReason
import com.google.firebase.firestore.PropertyName
import com.google.gson.annotations.SerializedName

data class MediaContent(
    @field:PropertyName("id")
    @SerializedName("id") var id: Int = 0,

    @field:PropertyName("name")
    @SerializedName("name") var name: String = "",

    @field:PropertyName("overview")
    @SerializedName("overview") var overview: String = "",

    @field:PropertyName("poster_path")
    @SerializedName("poster_path") var posterPath: String? = null,

    @field:PropertyName("genre_ids")
    @SerializedName("genre_ids") var genreIds: List<Int>? = emptyList(),

    @field:PropertyName("genres")
    @SerializedName("genres") var genres: List<Genre>? = null,

    @field:PropertyName("popularity")
    @SerializedName("popularity") var popularity: Float = 0f,

    @field:PropertyName("keywords")
    @SerializedName("keywords") var keywords: KeywordsResponse? = null,

    @field:PropertyName("credits")
    @SerializedName("credits") var credits: CreditsResponse? = null,

    val affinityScore: Float = 0f,
    val reasons: List<RecommendationReason> = emptyList(),

    @field:PropertyName("first_air_date")
    @SerializedName("first_air_date") var firstAirDate: String? = null,

    @field:PropertyName("number_of_seasons")
    @SerializedName("number_of_seasons") var numberOfSeasons: Int? = null,

    @field:PropertyName("status")
    @SerializedName("status") var status: String? = null,

    @field:PropertyName("vote_average")
    @SerializedName("vote_average") var voteAverage: Float = 0f,

    @field:PropertyName("vote_count")
    @SerializedName("vote_count") var voteCount: Int = 0,

    @field:PropertyName("episode_run_time")
    @SerializedName("episode_run_time") var episodeRunTime: List<Int>? = null,

    @field:PropertyName("backdrop_path")
    @SerializedName("backdrop_path") var backdropPath: String? = null,

    @field:PropertyName("watch_providers")
    @SerializedName("watch/providers") var watchProviders: WatchProvidersResponse? = null,

    @field:PropertyName("seasons")
    @SerializedName("seasons") var seasons: List<Season>? = null,

    @field:PropertyName("videos")
    @SerializedName("videos") var videos: VideosResponse? = null,

    @field:PropertyName("origin_country")
    @SerializedName("origin_country") var originCountry: List<String> = emptyList()
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

@Immutable
data class Genre(
    @field:PropertyName("id")
    @SerializedName("id") var id: Int = 0,
    @field:PropertyName("name")
    @SerializedName("name") var name: String = ""
)

data class KeywordsResponse(
    @field:PropertyName("results")
    @SerializedName("results") var results: List<Keyword> = emptyList()
)

data class Keyword(
    @field:PropertyName("id")
    @SerializedName("id") var id: Int = 0,
    @field:PropertyName("name")
    @SerializedName("name") var name: String = ""
)

data class CreditsResponse(
    @field:PropertyName("cast")
    @SerializedName("cast") var cast: List<CastMember> = emptyList(),
    @field:PropertyName("crew")
    @SerializedName("crew") var crew: List<CrewMember> = emptyList()
)

data class CastMember(
    @field:PropertyName("id")
    @SerializedName("id") var id: Int = 0,
    @field:PropertyName("name")
    @SerializedName("name") var name: String = "",
    @field:PropertyName("profile_path")
    @SerializedName("profile_path") var profilePath: String? = null,
    @field:PropertyName("character")
    @SerializedName("character") var character: String = ""
)

data class CrewMember(
    @field:PropertyName("id")
    @SerializedName("id") var id: Int = 0,
    @field:PropertyName("name")
    @SerializedName("name") var name: String = "",
    @field:PropertyName("job")
    @SerializedName("job") var job: String = "",
    @field:PropertyName("profile_path")
    @SerializedName("profile_path") var profilePath: String? = null
)

data class WatchProvidersResponse(
    @field:PropertyName("results")
    var results: Map<String, CountryProviders>? = null
)

data class CountryProviders(
    @field:PropertyName("link")
    var link: String? = null,
    @field:PropertyName("flatrate")
    var flatrate: List<Provider>? = null,
    @field:PropertyName("rent")
    var rent: List<Provider>? = null,
    @field:PropertyName("buy")
    var buy: List<Provider>? = null
)

data class Provider(
    @field:PropertyName("provider_id")
    @SerializedName("provider_id") var providerId: Int = 0,
    @field:PropertyName("provider_name")
    @SerializedName("provider_name") var providerName: String = "",
    @field:PropertyName("logo_path")
    @SerializedName("logo_path") var logoPath: String? = null
)

data class Season(
    @field:PropertyName("id")
    var id: Int = 0,
    @field:PropertyName("name")
    var name: String = "",
    @field:PropertyName("season_number")
    @SerializedName("season_number") var seasonNumber: Int = 0,
    @field:PropertyName("episode_count")
    @SerializedName("episode_count") var episodeCount: Int = 0,
    @field:PropertyName("poster_path")
    @SerializedName("poster_path") var posterPath: String? = null
)

data class VideosResponse(
    @field:PropertyName("results")
    var results: List<Video>? = null
)

data class Video(
    @field:PropertyName("id")
    var id: String = "",
    @field:PropertyName("key")
    var key: String = "",
    @field:PropertyName("site")
    var site: String = "",
    @field:PropertyName("type")
    var type: String = ""
)

data class PersonSearchResponse(
    @SerializedName("results") var results: List<PersonSearchResult> = emptyList()
)

data class PersonSearchResult(
    @field:PropertyName("id")
    @SerializedName("id") var id: Int = 0,
    @field:PropertyName("name")
    @SerializedName("name") var name: String = "",
    @field:PropertyName("profile_path")
    @SerializedName("profile_path") var profilePath: String? = null,
    @field:PropertyName("known_for_department")
    @SerializedName("known_for_department") var knownForDepartment: String? = null
)

data class PersonTvCreditsResponse(
    @SerializedName("cast") var cast: List<MediaContent> = emptyList(),
    @SerializedName("crew") var crew: List<PersonTvCrewEntry> = emptyList()
)

data class PersonTvCrewEntry(
    @field:PropertyName("id")
    var id: Int = 0,
    @field:PropertyName("name")
    var name: String = "",
    @field:PropertyName("poster_path")
    @SerializedName("poster_path") var posterPath: String? = null,
    @field:PropertyName("vote_average")
    @SerializedName("vote_average") var voteAverage: Float = 0f,
    @field:PropertyName("vote_count")
    @SerializedName("vote_count") var voteCount: Int = 0,
    @field:PropertyName("first_air_date")
    @SerializedName("first_air_date") var firstAirDate: String? = null,
    @field:PropertyName("overview")
    var overview: String = "",
    @field:PropertyName("popularity")
    var popularity: Float = 0f,
    @field:PropertyName("job")
    var job: String = "",
    @field:PropertyName("genre_ids")
    @SerializedName("genre_ids") var genreIds: List<Int>? = emptyList()
) {
    fun toMediaContent() = MediaContent(
        id = id, name = name, posterPath = posterPath,
        voteAverage = voteAverage, voteCount = voteCount,
        firstAirDate = firstAirDate, overview = overview,
        popularity = popularity, genreIds = genreIds
    )
}
