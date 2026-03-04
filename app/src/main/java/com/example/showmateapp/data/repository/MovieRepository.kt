package com.example.showmateapp.data.repository

import com.example.showmateapp.data.network.Movie
import com.example.showmateapp.data.network.RetrofitClient

class MovieRepository {
    private val apiService = RetrofitClient.apiService
    private val token = com.example.showmateapp.BuildConfig.TMDB_API_TOKEN

    suspend fun getTrendingShows(): List<Movie> {
        val response = apiService.getTrendingShows(token)
        return response.results
    }

    suspend fun getPopularShows(): List<Movie> {
        val response = apiService.getPopularShows(token)
        return response.results
    }

    suspend fun getShowsByGenres(genreIds: String): List<Movie> {
        val response = apiService.getShowsByGenres(token, genreIds)
        return response.results
    }
}
