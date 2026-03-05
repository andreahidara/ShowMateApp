package com.example.showmateapp.data.repository

import com.example.showmateapp.BuildConfig
import com.example.showmateapp.data.network.TvShow
import com.example.showmateapp.data.network.RetrofitClient
import javax.inject.Inject

class TvShowRepository @Inject constructor() {
    private val apiService = RetrofitClient.apiService
    private val token = BuildConfig.TMDB_API_TOKEN

    suspend fun getTvShowDetails(showId: Int): TvShow {
        return apiService.getTvShowDetails(token, showId)
    }

    suspend fun getTvShowsByGenres(genreIds: String): List<TvShow> {
        return try {
            val response = apiService.getTvShowsByGenre(token, genreIds)
            response.results
        } catch (e: Exception) {
            emptyList()
        }
    }

    suspend fun getPopularTvShows(): List<TvShow> {
        return try {
            val response = apiService.getTvShowsByGenre(token, "") 
            response.results
        } catch (e: Exception) {
            emptyList()
        }
    }

    suspend fun getTrendingTvShows(): List<TvShow> {
        return try {
            val response = apiService.getTvShowsByGenre(token, "", sortBy = "popularity.desc")
            response.results
        } catch (e: Exception) {
            emptyList()
        }
    }

    suspend fun searchTvShows(query: String): List<TvShow> {
        return try {
            val response = apiService.searchTvShows(token, query)
            response.results
        } catch (e: Exception) {
            emptyList()
        }
    }
}