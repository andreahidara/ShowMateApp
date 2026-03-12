package com.example.showmateapp.data.network

import com.example.showmateapp.data.model.MediaResponse
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query

interface TmdbApiService {

    @GET("tv/{tv_id}")
    suspend fun getMediaDetails(
        @Path("tv_id") tvId: Int,
        @Query("append_to_response") appendToResponse: String? = null
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
        @Query("language") language: String = "es-ES",
        @Query("sort_by") sortBy: String = "popularity.desc",
        @Query("page") page: Int = 1
    ): MediaResponse

    @GET("search/tv")
    suspend fun searchMedia(
        @Query("query") query: String,
        @Query("language") language: String = "es-ES"
    ): MediaResponse
}