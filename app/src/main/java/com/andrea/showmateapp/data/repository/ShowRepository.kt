package com.andrea.showmateapp.data.repository

import com.andrea.showmateapp.data.local.ShowDao
import com.andrea.showmateapp.data.model.toDomain
import com.andrea.showmateapp.data.model.toEntity
import com.andrea.showmateapp.data.network.TmdbApiService
import com.andrea.showmateapp.data.network.MediaContent
import com.andrea.showmateapp.data.network.SeasonResponse
import com.andrea.showmateapp.util.Resource
import com.andrea.showmateapp.util.safeApiCall
import com.andrea.showmateapp.domain.repository.IShowRepository
import com.andrea.showmateapp.di.IoDispatcher
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.withTimeoutOrNull
import timber.log.Timber
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ShowRepository @Inject constructor(
    private val apiService: TmdbApiService,
    private val showDao: ShowDao,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher
) : IShowRepository {

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

    override suspend fun getShowDetails(showId: Int): Resource<MediaContent> {
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
                Timber.e("Error fetching details for %d: %s", showId, networkResource.message)
                val localShow = showDao.getShowById(showId)?.toDomain()
                if (localShow != null) Resource.Success(localShow) else networkResource
            }
            else -> networkResource
        }
    }

    override suspend fun getSeasonDetails(showId: Int, seasonNumber: Int): Resource<SeasonResponse> {
        return safeApiCall {
            withTimeoutOrNull(API_TIMEOUT_MS) { apiService.getSeasonDetails(showId, seasonNumber) }
                ?: throw Exception("Tiempo de espera agotado")
        }
    }

    override suspend fun discoverShows(
        genreId: String?,
        year: Int?,
        minRating: Float?,
        sortBy: String,
        keywords: String?,
        watchRegion: String?,
        withCast: String?,
        withCrew: String?,
        providers: String?,
        firstAirDateGte: String?,
        firstAirDateLte: String?
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

    override suspend fun getPersonDetails(personId: Int): Resource<com.andrea.showmateapp.data.network.PersonResponse> {
        return safeApiCall {
            apiService.getPersonDetails(personId)
        }
    }

    private suspend fun saveAndReturn(category: String, shows: List<MediaContent>): List<MediaContent> {
        if (shows.isEmpty()) return shows
        showDao.replaceCategory(category, shows.map { it.toEntity(category) })
        return shows
    }

    private suspend fun getFreshCachedShows(category: String): List<MediaContent> {
        val threshold = System.currentTimeMillis() - CACHE_TTL_MS
        showDao.deleteStaleByCategory(category, threshold)
        return showDao.getShowsByCategory(category).map { it.toDomain() }
    }

    private suspend fun fetchCached(
        category: String,
        apiCall: suspend () -> List<MediaContent>
    ): Resource<List<MediaContent>> {
        val result = safeApiCall {
            withTimeoutOrNull(API_TIMEOUT_MS) { apiCall() }
                ?: throw Exception("Tiempo de espera agotado")
        }
        return when (result) {
            is Resource.Success -> { saveAndReturn(category, result.data); result }
            is Resource.Error   -> {
                val cached = getFreshCachedShows(category)
                if (cached.isNotEmpty()) Resource.Success(cached) else result
            }
            else -> result
        }
    }

    override suspend fun getShowDetailsInParallel(ids: List<Int>, maxCount: Int): List<MediaContent> =
        coroutineScope {
            ids.take(maxCount)
                .map { id -> async { getShowDetails(id) } }
                .awaitAll()
                .mapNotNull { (it as? Resource.Success)?.data }
        }

    override suspend fun getPopularShows(): Resource<List<MediaContent>> =
        fetchCached(CAT_POPULAR) { apiService.getPopularMedia().results }

    override suspend fun getTrendingThisWeek(): Resource<List<MediaContent>> =
        fetchCached(CAT_TRENDING) { apiService.getTrendingThisWeek().results }

    override suspend fun getTrendingShows(): Resource<List<MediaContent>> =
        fetchCached(CAT_POPULAR) { apiService.discoverMedia(sortBy = "popularity.desc").results }

    override suspend fun getShowsByGenres(genreIds: String): Resource<List<MediaContent>> =
        fetchCached(CAT_RECOMMENDED) { apiService.discoverMedia(genreId = genreIds).results }


    override suspend fun getDetailedRecommendations(genres: String?): List<MediaContent> {
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

    override suspend fun getShowsOnTheAir(
        providers: String
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

    override suspend fun searchShows(query: String): Resource<List<MediaContent>> {
        return safeApiCall {
            val response = apiService.searchMedia(query)
            response.results
        }
    }

    override suspend fun searchByPerson(query: String, isCreator: Boolean): Resource<List<MediaContent>> {
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

    override suspend fun discoverShowsPaged(
        genreId: String?,
        sortBy: String,
        page: Int,
        airDateGte: String?,
        airDateLte: String?
    ): Resource<Pair<List<MediaContent>, Int>> {
        return safeApiCall {
            withTimeoutOrNull(API_TIMEOUT_MS) {
                apiService.discoverMedia(
                    genreId = genreId,
                    sortBy = sortBy,
                    page = page,
                    airDateGte = airDateGte,
                    airDateLte = airDateLte
                )
            }?.let { it.results to it.total_pages }
                ?: throw Exception("Tiempo de espera agotado")
        }
    }

    override suspend fun getSimilarShows(showId: Int): List<MediaContent> {
        val result = safeApiCall {
            val response = apiService.getRecommendationsByShow(showId)
            response.results
        }
        return (result as? Resource.Success)?.data ?: emptyList()
    }
}
