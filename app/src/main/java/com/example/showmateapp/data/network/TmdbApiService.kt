package com.example.showmateapp.data.network

import retrofit2.http.GET
import retrofit2.http.Query
import retrofit2.http.Header

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
}