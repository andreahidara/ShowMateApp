package com.example.showmateapp.data.repository

import com.example.showmateapp.data.network.Movie
import com.example.showmateapp.data.network.RetrofitClient

class MovieRepository {
    private val apiService = RetrofitClient.apiService
    private val token = "Bearer eyJhbGciOiJIUzI1NiJ9.eyJhdWQiOiJjYzFhY2NmNTU5Mzk4YTNmNDdiMWZhMzYyNTIwY2UyYiIsIm5iZiI6MTc3MTk1MDk1OC4xMjYwMDAyLCJzdWIiOiI2OTlkZDM2ZWJjM2YzZDFkNjUwNjYwMjYiLCJzY29wZXMiOlsiYXBpX3JlYWQiXSwidmVyc2lvbiI6MX0.Deix25uQqXJ623nkHJJWhLBz2Ga4ouCv1iK9PT5iSM8"

    suspend fun getTrendingShows(): List<Movie> {
        return try {
            val response = apiService.getTrendingShows(token)
            response.results
        } catch (e: Exception) {
            emptyList()
        }
    }

    suspend fun getPopularShows(): List<Movie> {
        return try {
            val response = apiService.getPopularShows(token)
            response.results
        } catch (e: Exception) {
            emptyList()
        }
    }

    suspend fun getShowsByGenres(genreIds: String): List<Movie> {
        return try {
            val response = apiService.getShowsByGenres(token, genreIds)
            response.results
        } catch (e: Exception) {
            emptyList()
        }
    }
}
