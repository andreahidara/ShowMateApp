package com.example.showmateapp.data.network

import com.example.showmateapp.data.model.TvShowResponse
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.Path
import retrofit2.http.Query

interface TmdbApiService {

    @GET("tv/{tv_id}")
    suspend fun getTvShowDetails(
        @Header("Authorization") token: String,
        @Path("tv_id") tvId: Int
    ): TvShow

    @GET("discover/tv")
    suspend fun getTvShowsByGenre(
        @Header("Authorization") token: String,
        @Query("with_genres") genreId: String,
        @Query("language") language: String = "es-ES",
        @Query("sort_by") sortBy: String = "popularity.desc"
    ): TvShowResponse

    @GET("search/tv")
    suspend fun searchTvShows(
        @Header("Authorization") token: String,
        @Query("query") query: String,
        @Query("language") language: String = "es-ES"
    ): TvShowResponse
}