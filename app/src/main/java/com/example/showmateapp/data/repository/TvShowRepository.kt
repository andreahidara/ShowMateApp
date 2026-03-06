package com.example.showmateapp.data.repository

import com.example.showmateapp.BuildConfig
import com.example.showmateapp.data.local.TvShowDao
import com.example.showmateapp.data.model.toDomain
import com.example.showmateapp.data.model.toEntity
import com.example.showmateapp.data.network.RetrofitClient
import com.example.showmateapp.data.network.TvShow
import javax.inject.Inject
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope

class TvShowRepository @Inject constructor(
    private val tvShowDao: TvShowDao
) {
    private val apiService = RetrofitClient.apiService
    private val token = if (BuildConfig.TMDB_API_TOKEN.startsWith("Bearer ")) {
        BuildConfig.TMDB_API_TOKEN
    } else {
        "Bearer ${BuildConfig.TMDB_API_TOKEN}"
    }

    suspend fun getTvShowDetails(showId: Int): TvShow {
        return apiService.getTvShowDetails(token, showId, "credits,keywords")
    }

    suspend fun getTvShowsByGenres(genreIds: String): List<TvShow> {
        return try {
            val response = apiService.getTvShowsByGenre(token, genreIds)
            val fullData = fetchMetadataForList(response.results)
            // Cache para SwipeScreen
            tvShowDao.replaceCategory("recommended", fullData.map { it.toEntity("recommended") })
            fullData
        } catch (e: Exception) {
            tvShowDao.getShowsByCategory("recommended").map { it.toDomain() }
        }
    }

    suspend fun getPopularTvShows(): List<TvShow> {
        return try {
            val response = apiService.getPopularTvShows(token)
            val fullData = fetchMetadataForList(response.results)
            tvShowDao.replaceCategory("popular", fullData.map { it.toEntity("popular") })
            fullData
        } catch (e: Exception) {
            tvShowDao.getShowsByCategory("popular").map { it.toDomain() }
        }
    }

    /**
     * Helper para obtener metadatos (keywords y reparto) de una lista de series en paralelo
     */
    private suspend fun fetchMetadataForList(shows: List<TvShow>): List<TvShow> = coroutineScope {
        shows.map { show ->
            async {
                try {
                    apiService.getTvShowDetails(token, show.id, "credits,keywords")
                } catch (e: Exception) {
                    show
                }
            }
        }.awaitAll()
    }

    suspend fun getTrendingTvShows(): List<TvShow> {
        return try {
            val response = apiService.getTvShowsByGenre(token, "", sortBy = "popularity.desc")
            val fullData = fetchMetadataForList(response.results)
            tvShowDao.replaceCategory("trending", fullData.map { it.toEntity("trending") })
            fullData
        } catch (e: Exception) {
            tvShowDao.getShowsByCategory("trending").map { it.toDomain() }
        }
    }

    suspend fun getDetailedRecommendations(): List<TvShow> {
        return getTrendingTvShows()
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
