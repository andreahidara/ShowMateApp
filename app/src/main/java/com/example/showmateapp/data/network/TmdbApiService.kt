package com.example.showmateapp.data.network

import com.example.showmateapp.data.model.MediaResponse
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query

interface TmdbApiService {

    @GET("tv/{tv_id}")
    suspend fun getMediaDetails(
        @Path("tv_id") tvId: Int,
        @Query("append_to_response") appendToResponse: String? = "credits,keywords,watch/providers,content_ratings,videos"
    ): MediaContent

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
        @Query("with_cast") withCast: String? = null
    ): MediaResponse

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
        @Query("language") language: String = "es-ES"
    ): MediaResponse

    @GET("tv/{tv_id}/season/{season_number}")
    suspend fun getSeasonDetails(
        @Path("tv_id") tvId: Int,
        @Path("season_number") seasonNumber: Int,
        @Query("language") language: String = "es-ES"
    ): SeasonResponse
}

data class SeasonResponse(
    val _id: String,
    val air_date: String?,
    val episodes: List<Episode>,
    val name: String,
    val overview: String,
    val id: Int,
    val poster_path: String?,
    val season_number: Int
)

data class Episode(
    val air_date: String?,
    val episode_number: Int,
    val id: Int,
    val name: String,
    val overview: String,
    val production_code: String?,
    val runtime: Int?,
    val season_number: Int,
    val show_id: Int,
    val still_path: String?,
    val vote_average: Float,
    val vote_count: Int
)

data class PersonResponse(
    val id: Int,
    val name: String,
    val profile_path: String?
)
