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
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.withTimeoutOrNull
import java.time.LocalDate
import java.time.format.DateTimeFormatter

class ShowRepository @Inject constructor(
    private val apiService: TmdbApiService,
    private val showDao: ShowDao
) {

    companion object {
        private const val API_TIMEOUT_MS = 15_000L
        private const val CACHE_TTL_MS = 24 * 60 * 60 * 1000L

        const val CAT_DETAILS     = "details"
        const val CAT_POPULAR     = "popular"
        const val CAT_TRENDING    = "trending"
        const val CAT_RECOMMENDED = "recommended"
        const val CAT_LIKED       = "liked"
        const val CAT_WATCHED     = "watched"
    }

    suspend fun getShowDetails(showId: Int): Resource<MediaContent> {
        val networkResource = safeApiCall {
            withTimeoutOrNull(API_TIMEOUT_MS) { apiService.getMediaDetails(showId) }
                ?: throw Exception("Tiempo de espera agotado")
        }

        return when (networkResource) {
            is Resource.Success -> {
                showDao.insertShows(listOf(networkResource.data.toEntity(CAT_DETAILS)))
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

    suspend fun getSeasonDetails(showId: Int, seasonNumber: Int): Resource<SeasonResponse> {
        return safeApiCall {
            withTimeoutOrNull(API_TIMEOUT_MS) { apiService.getSeasonDetails(showId, seasonNumber) }
                ?: throw Exception("Tiempo de espera agotado")
        }
    }

    suspend fun discoverShows(
        genreId: String? = null,
        year: Int? = null,
        minRating: Float? = null,
        sortBy: String = "popularity.desc",
        keywords: String? = null,
        watchRegion: String? = null,
        withCast: String? = null,
        withCrew: String? = null,
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
                    withCrew = withCrew,
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
        if (shows.isEmpty()) return shows
        showDao.replaceCategory(category, shows.map { it.toEntity(category) })
        return shows
    }

    suspend fun getShowDetailsInParallel(ids: List<Int>, maxCount: Int = 20): List<MediaContent> =
        coroutineScope {
            ids.take(maxCount)
                .map { id -> async { getShowDetails(id) } }
                .awaitAll()
                .mapNotNull { (it as? Resource.Success)?.data }
        }

    suspend fun getPopularShows(): Resource<List<MediaContent>> {
        val result = safeApiCall {
            withTimeoutOrNull(API_TIMEOUT_MS) { apiService.getPopularMedia() }?.results
                ?: throw Exception("Tiempo de espera agotado")
        }
        
        return when (result) {
            is Resource.Success -> {
                saveAndReturn(CAT_POPULAR, result.data)
                result
            }
            is Resource.Error -> {
                val localShows = showDao.getShowsByCategory(CAT_POPULAR).map { it.toDomain() }
                if (localShows.isNotEmpty()) Resource.Success(localShows) else result
            }
            else -> result
        }
    }

    suspend fun getTrendingThisWeek(): Resource<List<MediaContent>> {
        val result = safeApiCall {
            withTimeoutOrNull(API_TIMEOUT_MS) { apiService.getTrendingThisWeek() }?.results
                ?: throw Exception("Tiempo de espera agotado")
        }
        return when (result) {
            is Resource.Success -> {
                saveAndReturn(CAT_TRENDING, result.data)
                result
            }
            is Resource.Error -> {
                val localShows = showDao.getShowsByCategory(CAT_TRENDING).map { it.toDomain() }
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
                saveAndReturn(CAT_TRENDING, result.data)
                result
            }
            is Resource.Error -> {
                val localShows = showDao.getShowsByCategory(CAT_TRENDING).map { it.toDomain() }
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
                saveAndReturn(CAT_RECOMMENDED, result.data)
                result
            }
            is Resource.Error -> {
                val localShows = showDao.getShowsByCategory(CAT_RECOMMENDED).map { it.toDomain() }
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
            withTimeoutOrNull(API_TIMEOUT_MS) {
                apiService.discoverMedia(genreId = genres, sortBy = "popularity.desc")
            }?.results ?: throw Exception("Tiempo de espera agotado")
        }
        val list = (result as? Resource.Success)?.data ?: emptyList()
        return if (list.isEmpty()) {
            val trending = getTrendingShows()
            if (trending is Resource.Success) trending.data else emptyList()
        } else {
            list
        }
    }

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

    suspend fun searchByPerson(query: String, isCreator: Boolean): Resource<List<MediaContent>> {
        return safeApiCall {
            val people = apiService.searchPerson(query).results
            if (people.isEmpty()) return@safeApiCall emptyList()

            val creatorJobs = setOf("Creator", "Executive Producer", "Showrunner", "Series Director")

            people.take(3)
                .flatMap { person ->
                    val credits = apiService.getPersonTvCredits(person.id)
                    if (isCreator) {
                        credits.crew
                            .filter { it.job in creatorJobs }
                            .map { it.toMediaContent() }
                    } else {
                        credits.cast
                    }
                }
                .distinctBy { it.id }
                .filter { it.posterPath != null }
                .sortedByDescending { it.popularity }
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
