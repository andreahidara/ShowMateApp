package com.example.showmateapp.data.network

import retrofit2.http.GET
import retrofit2.http.Query
import retrofit2.http.Header
import retrofit2.http.Path

// Aquí definimos los datos que nos devuelve TMDB
data class MovieResponse(val results: List<Movie>)
data class Movie(
    val id: Int,
    val name: String,
    val poster_path: String?,
    val overview: String
)

interface TmdbApiService {
    @GET("discover/tv")
    suspend fun getShowsByGenres(
        @Header("Authorization") token: String,
        @Query("with_genres") genreIds: String,
        @Query("language") lang: String = "en-US"
    ): MovieResponse

    @GET("trending/tv/day")
    suspend fun getTrendingShows(
        @Header("Authorization") token: String,
        @Query("language") lang: String = "en-US"
    ): MovieResponse

    @GET("tv/popular")
    suspend fun getPopularShows(
        @Header("Authorization") token: String,
        @Query("language") lang: String = "en-US"
    ): MovieResponse

    @GET("search/tv")
    suspend fun searchShows(
        @Header("Authorization") token: String,
        @Query("query") query: String,
        @Query("language") lang: String = "en-US"
    ): MovieResponse

    @GET("tv/{tv_id}")
    suspend fun getTvShowDetails(
        @Header("Authorization") token: String,
        @Path("tv_id") tvId: Int,
        @Query("language") lang: String = "en-US"
    ): Movie
}