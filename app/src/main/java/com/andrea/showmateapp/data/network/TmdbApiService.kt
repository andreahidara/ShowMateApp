package com.andrea.showmateapp.data.network

import com.andrea.showmateapp.data.model.*
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
            value = "append_to_response",
            encoded = true
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

    @GET("tv/{tv_id}/similar")
    suspend fun getSimilarMediaByShow(
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
