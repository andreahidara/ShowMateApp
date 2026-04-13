package com.andrea.showmateapp.data.network

import com.andrea.showmateapp.data.model.MediaResponse
import com.google.gson.annotations.SerializedName
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query

interface TmdbApiService {

    @GET("tv/{tv_id}")
    suspend fun getMediaDetails(
        @Path("tv_id") tvId: Int,
        @Query("language") language: String = "es-ES",
        @Query(
            "append_to_response"
        ) appendToResponse: String? = "credits,keywords,watch/providers,content_ratings,videos"
    ): MediaContent

    @GET("trending/tv/week")
    suspend fun getTrendingThisWeek(@Query("language") language: String = "es-ES"): MediaResponse

    @GET("tv/popular")
    suspend fun getPopularMedia(
        @Query("language") language: String = "es-ES",
        @Query("page") page: Int = 1
    ): MediaResponse

    @GET("tv/{tv_id}/recommendations")
    suspend fun getRecommendationsByShow(
        @Path("tv_id") tvId: Int,
        @Query("language") language: String = "es-ES",
        @Query("page") page: Int = 1
    ): MediaResponse

    @GET("discover/tv")
    suspend fun discoverMedia(
        @Query("with_genres") genreId: String? = null,
        @Query("first_air_date_year") year: Int? = null,
        @Query("vote_average.gte") minRating: Float? = null,
        @Query("vote_count.gte") minVoteCount: Int = 50,
        @Query("language") language: String = "es-ES",
        @Query("sort_by") sortBy: String = "popularity.desc",
        @Query("page") page: Int = 1,
        @Query("watch_region") watchRegion: String? = null,
        @Query("with_watch_providers") watchProviders: String? = null,
        @Query("with_keywords") keywords: String? = null,
        @Query("with_cast") withCast: String? = null,
        @Query("with_crew") withCrew: String? = null,
        @Query("air_date.gte") airDateGte: String? = null,
        @Query("air_date.lte") airDateLte: String? = null,
        @Query("first_air_date.gte") firstAirDateGte: String? = null,
        @Query("first_air_date.lte") firstAirDateLte: String? = null
    ): MediaResponse

    @GET("trending/person/week")
    suspend fun getTrendingPeople(@Query("language") language: String = "es-ES"): PersonSearchResponse

    @GET("person/{person_id}")
    suspend fun getPersonDetails(
        @Path("person_id") personId: Int,
        @Query("language") language: String = "es-ES"
    ): PersonResponse

    @GET("tv/on_the_air")
    suspend fun getOnTheAirShows(
        @Query("language") language: String = "es-ES",
        @Query("page") page: Int = 1,
        @Query("watch_region") watchRegion: String = "ES",
        @Query("with_watch_providers") withProviders: String? = null
    ): MediaResponse

    @GET("search/tv")
    suspend fun searchMedia(
        @Query("query") query: String,
        @Query("language") language: String = "es-ES",
        @Query("page") page: Int = 1
    ): MediaResponse

    @GET("search/person")
    suspend fun searchPerson(
        @Query("query") query: String,
        @Query("language") language: String = "es-ES"
    ): PersonSearchResponse

    @GET("person/{person_id}/tv_credits")
    suspend fun getPersonTvCredits(
        @Path("person_id") personId: Int,
        @Query("language") language: String = "es-ES"
    ): PersonTvCreditsResponse

    @GET("tv/{tv_id}/season/{season_number}")
    suspend fun getSeasonDetails(
        @Path("tv_id") tvId: Int,
        @Path("season_number") seasonNumber: Int,
        @Query("language") language: String = "es-ES"
    ): SeasonResponse
}

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

data class PersonResponse(
    val id: Int = 0,
    val name: String = "",
    @SerializedName("profile_path") val profilePath: String? = null,
    val biography: String = "",
    val birthday: String? = null,
    @SerializedName("place_of_birth") val placeOfBirth: String? = null,
    @SerializedName("known_for_department") val knownForDepartment: String? = null,
    val popularity: Float = 0f
)
