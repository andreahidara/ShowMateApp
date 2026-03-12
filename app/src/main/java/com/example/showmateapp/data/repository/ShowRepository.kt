package com.example.showmateapp.data.repository

import com.example.showmateapp.data.local.ShowDao
import com.example.showmateapp.data.model.toDomain
import com.example.showmateapp.data.model.toEntity
import com.example.showmateapp.data.network.TmdbApiService
import com.example.showmateapp.data.network.MediaContent
import com.example.showmateapp.util.Resource
import com.example.showmateapp.util.safeApiCall
import javax.inject.Inject
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope

class ShowRepository @Inject constructor(
    private val apiService: TmdbApiService,
    private val showDao: ShowDao
) {

    suspend fun getShowDetails(showId: Int): Resource<MediaContent> {
        val networkResource = safeApiCall { 
            apiService.getMediaDetails(showId, "credits,keywords")
        }
        
        return if (networkResource is Resource.Error) {
            val localShow = showDao.getShowById(showId)?.toDomain()
            if (localShow != null) Resource.Success(localShow) else networkResource
        } else {
            networkResource
        }
    }

    suspend fun discoverShows(
        genreId: String? = null,
        year: Int? = null,
        minRating: Float? = null,
        sortBy: String = "popularity.desc"
    ): Resource<List<MediaContent>> {
        return safeApiCall {
            val response = apiService.discoverMedia(genreId, year, minRating, sortBy = sortBy)
            response.results
        }
    }

    private suspend fun saveAndReturn(category: String, shows: List<MediaContent>): List<MediaContent> {
        showDao.replaceCategory(category, shows.map { it.toEntity(category) })
        return shows
    }

    suspend fun getPopularShows(): List<MediaContent> {
        val result = safeApiCall {
            val response = apiService.getPopularMedia()
            response.results
        }
        
        return when (result) {
            is Resource.Success -> saveAndReturn("popular", result.data)
            else -> showDao.getShowsByCategory("popular").map { it.toDomain() }
        }
    }

    suspend fun getTrendingShows(): List<MediaContent> {
        val result = safeApiCall {
            val response = apiService.discoverMedia(sortBy = "popularity.desc")
            response.results
        }
        
        return when (result) {
            is Resource.Success -> saveAndReturn("trending", result.data)
            else -> showDao.getShowsByCategory("trending").map { it.toDomain() }
        }
    }

    suspend fun getShowsByGenres(genreIds: String): List<MediaContent> {
        val result = safeApiCall {
            val response = apiService.discoverMedia(genreId = genreIds)
            response.results
        }
        
        return when (result) {
            is Resource.Success -> {
                saveAndReturn("recommended", result.data)
            }
            else -> showDao.getShowsByCategory("recommended").map { it.toDomain() }
        }
    }


    suspend fun getDetailedRecommendations(): List<MediaContent> = getTrendingShows()

    suspend fun searchShows(query: String): List<MediaContent> {
        val result = safeApiCall {
            val response = apiService.searchMedia(query)
            response.results
        }
        return (result as? Resource.Success)?.data ?: emptyList()
    }

    suspend fun getSimilarShows(showId: Int): List<MediaContent> {
        val result = safeApiCall {
            val response = apiService.getRecommendationsByShow(showId)
            response.results
        }
        return (result as? Resource.Success)?.data ?: emptyList()
    }
}
