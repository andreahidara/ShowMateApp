package com.example.showmateapp.data.repository

import com.example.showmateapp.data.local.ShowDao
import com.example.showmateapp.data.model.toDomain
import com.example.showmateapp.data.model.toEntity
import com.example.showmateapp.data.network.TmdbApiService
import com.example.showmateapp.data.network.MediaContent
import com.example.showmateapp.data.network.SeasonResponse
import com.example.showmateapp.util.Resource
import com.example.showmateapp.util.safeApiCall
import javax.inject.Inject
import android.util.Log
import kotlinx.coroutines.withTimeoutOrNull
import java.time.LocalDate
import java.time.format.DateTimeFormatter

class ShowRepository @Inject constructor(
    private val apiService: TmdbApiService,
    private val showDao: ShowDao
) {

    companion object {
        private const val API_TIMEOUT_MS = 15_000L
        // Cache entries older than 24 h are considered stale and replaced on next successful fetch
        private const val CACHE_TTL_MS = 24 * 60 * 60 * 1000L
    }

    suspend fun getShowDetails(showId: Int): Resource<MediaContent> {
        val networkResource = safeApiCall {
            withTimeoutOrNull(API_TIMEOUT_MS) { apiService.getMediaDetails(showId) }
                ?: throw Exception("Tiempo de espera agotado")
        }
        
        return when (networkResource) {
            is Resource.Success -> {
                showDao.insertShows(listOf(networkResource.data.toEntity("details")))
                networkResource
            }
            is Resource.Error -> {
                Log.e("ShowRepository", "Error fetching details for $showId: ${networkResource.message}")
                val localShow = showDao.getShowById(showId)?.toDomain()
                if (localShow != null) Resource.Success(localShow) else networkResource
            }
            else -> networkResource
        }
    }

    suspend fun getSeasonDetails(showId: Int, seasonNumber: Int): SeasonResponse {
        return apiService.getSeasonDetails(showId, seasonNumber)
    }

    suspend fun discoverShows(
        genreId: String? = null,
        year: Int? = null,
        minRating: Float? = null,
        sortBy: String = "popularity.desc",
        keywords: String? = null,
        watchRegion: String? = null,
        withCast: String? = null,
        providers: String? = null,
        firstAirDateGte: String? = null,
        firstAirDateLte: String? = null
    ): Resource<List<MediaContent>> {
        return safeApiCall {
            withTimeoutOrNull(API_TIMEOUT_MS) {
                apiService.discoverMedia(
                    genreId = genreId,
                    year = year,
                    minRating = minRating,
                    sortBy = sortBy,
                    keywords = keywords,
                    watchRegion = watchRegion,
                    withCast = withCast,
                    watchProviders = providers,
                    firstAirDateGte = firstAirDateGte,
                    firstAirDateLte = firstAirDateLte
                )
            }?.results ?: throw Exception("Tiempo de espera agotado")
        }
    }

    suspend fun getPersonDetails(personId: Int): Resource<com.example.showmateapp.data.network.PersonResponse> {
        return safeApiCall {
            apiService.getPersonDetails(personId)
        }
    }

    private suspend fun saveAndReturn(category: String, shows: List<MediaContent>): List<MediaContent> {
        val staleThreshold = System.currentTimeMillis() - CACHE_TTL_MS
        showDao.deleteStaleByCategory(category, staleThreshold)
        showDao.replaceCategory(category, shows.map { it.toEntity(category) })
        return shows
    }

    suspend fun getPopularShows(): Resource<List<MediaContent>> {
        val result = safeApiCall {
            withTimeoutOrNull(API_TIMEOUT_MS) { apiService.getPopularMedia() }?.results
                ?: throw Exception("Tiempo de espera agotado")
        }
        
        return when (result) {
            is Resource.Success -> {
                saveAndReturn("popular", result.data)
                result
            }
            is Resource.Error -> {
                val localShows = showDao.getShowsByCategory("popular").map { it.toDomain() }
                if (localShows.isNotEmpty()) Resource.Success(localShows) else result
            }
            else -> result
        }
    }

    suspend fun getTrendingShows(): Resource<List<MediaContent>> {
        val result = safeApiCall {
            withTimeoutOrNull(API_TIMEOUT_MS) { apiService.discoverMedia(sortBy = "popularity.desc") }?.results
                ?: throw Exception("Tiempo de espera agotado")
        }
        
        return when (result) {
            is Resource.Success -> {
                saveAndReturn("trending", result.data)
                result
            }
            is Resource.Error -> {
                val localShows = showDao.getShowsByCategory("trending").map { it.toDomain() }
                if (localShows.isNotEmpty()) Resource.Success(localShows) else result
            }
            else -> result
        }
    }

    suspend fun getShowsByGenres(genreIds: String): Resource<List<MediaContent>> {
        val result = safeApiCall {
            withTimeoutOrNull(API_TIMEOUT_MS) { apiService.discoverMedia(genreId = genreIds) }?.results
                ?: throw Exception("Tiempo de espera agotado")
        }
        
        return when (result) {
            is Resource.Success -> {
                saveAndReturn("recommended", result.data)
                result
            }
            is Resource.Error -> {
                val localShows = showDao.getShowsByCategory("recommended").map { it.toDomain() }
                if (localShows.isNotEmpty()) Resource.Success(localShows) else result
            }
            else -> result
        }
    }


    suspend fun getDetailedRecommendations(genres: String?): List<MediaContent> {
        if (genres.isNullOrEmpty()) {
            val trending = getTrendingShows()
            return if (trending is Resource.Success) trending.data else emptyList()
        }
        val result = safeApiCall {
            apiService.discoverMedia(genreId = genres, sortBy = "popularity.desc").results
        }
        val list = (result as? Resource.Success)?.data ?: emptyList()
        return if (list.isEmpty()) {
            val trending = getTrendingShows()
            if (trending is Resource.Success) trending.data else emptyList()
        } else {
            list
        }
    }

    /**
     * Returns shows airing in the next 7 days on the given provider IDs (pipe-separated, e.g. "8|9|337").
     * Uses discover/tv so that with_watch_providers is properly supported.
     */
    suspend fun getShowsOnTheAir(
        providers: String = "8|9|337|384|531"
    ): Resource<List<MediaContent>> {
        val today = LocalDate.now()
        val weekLater = today.plusDays(7)
        val fmt = DateTimeFormatter.ISO_LOCAL_DATE
        return safeApiCall {
            withTimeoutOrNull(API_TIMEOUT_MS) {
                apiService.discoverMedia(
                    airDateGte = today.format(fmt),
                    airDateLte = weekLater.format(fmt),
                    watchProviders = providers,
                    watchRegion = "ES",
                    sortBy = "popularity.desc"
                )
            }?.results ?: throw Exception("Tiempo de espera agotado")
        }
    }

    suspend fun searchShows(query: String): Resource<List<MediaContent>> {
        return safeApiCall {
            val response = apiService.searchMedia(query)
            response.results
        }
    }

    suspend fun getSimilarShows(showId: Int): List<MediaContent> {
        val result = safeApiCall {
            val response = apiService.getRecommendationsByShow(showId)
            response.results
        }
        return (result as? Resource.Success)?.data ?: emptyList()
    }
}
