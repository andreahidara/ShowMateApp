package com.andrea.showmateapp.data.repository

import com.andrea.showmateapp.data.local.ShowDao
import com.andrea.showmateapp.data.model.PersonResponse
import com.andrea.showmateapp.data.model.SeasonResponse
import com.andrea.showmateapp.data.model.toDomain
import com.andrea.showmateapp.data.model.toEntity
import com.andrea.showmateapp.data.model.MediaContent
import com.andrea.showmateapp.data.network.TmdbApiService
import com.andrea.showmateapp.di.IoDispatcher
import com.andrea.showmateapp.domain.repository.IShowRepository
import com.andrea.showmateapp.util.Resource
import com.andrea.showmateapp.util.safeApiCall
import java.io.IOException
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull

@Singleton
class ShowRepository @Inject constructor(
    private val api: TmdbApiService,
    private val dao: ShowDao,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher
) : IShowRepository {

    private companion object {
        const val TIMEOUT = 10_000L
        const val CACHE_TTL = 24 * 60 * 60 * 1000L
        const val CAT_DETAILS = "details"
        const val CAT_POPULAR = "popular"
        const val CAT_TRENDING = "trending"
        const val CAT_RECOMMENDED = "recommended"
    }

    override suspend fun getShowDetails(showId: Int): Resource<MediaContent> = withContext(ioDispatcher) {
        val network = safeApiCall {
            withTimeoutOrNull(TIMEOUT) { api.getMediaDetails(showId) } ?: throw IOException("Timeout")
        }
        when (network) {
            is Resource.Success -> {
                dao.insertShows(listOf(network.data.toEntity(CAT_DETAILS)))
                network
            }
            is Resource.Error -> {
                dao.getLikedShowById(showId)?.toDomain()?.let { Resource.Success(it) }
                    ?: dao.getWatchedShowById(showId)?.toDomain()?.let { Resource.Success(it) }
                    ?: network
            }
            else -> network
        }
    }

    override suspend fun getSeasonDetails(showId: Int, seasonNumber: Int): Resource<SeasonResponse> = withContext(ioDispatcher) {
        dao.getSeason(showId, seasonNumber)?.takeIf { System.currentTimeMillis() - it.cachedAt < CACHE_TTL }?.let {
            return@withContext Resource.Success(it.toDomain())
        }
        safeApiCall {
            withTimeoutOrNull(TIMEOUT) { api.getSeasonDetails(showId, seasonNumber) }?.also {
                dao.insertSeason(it.toEntity(showId))
            } ?: throw IOException("Timeout")
        }
    }

    override suspend fun discoverShows(
        genreId: String?, year: Int?, minRating: Float?, sortBy: String, keywords: String?,
        watchRegion: String?, withCast: String?, withCrew: String?, providers: String?,
        firstAirDateGte: String?, firstAirDateLte: String?, airDateGte: String?, airDateLte: String?,
        excludedIds: List<Int>
    ): Resource<List<MediaContent>> = safeApiCall(retries = 1) {
        val results = api.discoverMedia(
            genreId = genreId, year = year, minRating = minRating,
            minVoteCount = if (firstAirDateGte != null || airDateGte != null) 0 else 50,
            sortBy = sortBy, keywords = keywords, watchRegion = watchRegion,
            withCast = withCast, withCrew = withCrew, watchProviders = providers,
            firstAirDateGte = firstAirDateGte, firstAirDateLte = firstAirDateLte,
            airDateGte = airDateGte, airDateLte = airDateLte
        ).results
        if (excludedIds.isEmpty()) results else results.filter { it.id !in excludedIds }
    }

    override suspend fun getPersonDetails(personId: Int): Resource<PersonResponse> = safeApiCall { api.getPersonDetails(personId) }

    override suspend fun getPersonTvCredits(personId: Int): Resource<List<MediaContent>> = safeApiCall {
        val credits = withTimeoutOrNull(TIMEOUT) { api.getPersonTvCredits(personId) } ?: throw IOException("Timeout")
        val cast = credits.cast.filter { it.voteAverage > 0f && it.name.isNotBlank() }
        val crew = credits.crew.filter { it.voteAverage > 0f }.map { it.toMediaContent() }
        (cast + crew).distinctBy { it.id }.sortedByDescending { it.voteAverage }.take(40)
    }

    private suspend fun fetchCached(cat: String, excluded: List<Int>, apiCall: suspend () -> List<MediaContent>): Resource<List<MediaContent>> = withContext(ioDispatcher) {
        val result = safeApiCall(retries = 1) { withTimeoutOrNull(TIMEOUT) { apiCall() } ?: throw IOException("Timeout") }
        when (result) {
            is Resource.Success -> {
                dao.replaceCategory(cat, result.data.map { it.toEntity(cat) })
                if (excluded.isNotEmpty()) Resource.Success(result.data.filter { it.id !in excluded }) else result
            }
            is Resource.Error -> {
                dao.deleteStaleByCategory(cat, System.currentTimeMillis() - CACHE_TTL)
                val cached = if (excluded.isEmpty()) dao.getShowsByCategory(cat) else dao.getShowsByCategoryExcluding(cat, excluded)
                if (cached.isNotEmpty()) Resource.Success(cached.map { it.toDomain() }) else result
            }
            else -> result
        }
    }

    override suspend fun getShowDetailsInParallel(ids: List<Int>, maxCount: Int): List<MediaContent> = coroutineScope {
        ids.take(maxCount).map { async { getShowDetails(it) } }.awaitAll().mapNotNull { (it as? Resource.Success)?.data }
    }

    override suspend fun getPopularShows(excludedIds: List<Int>) = fetchCached(CAT_POPULAR, excludedIds) { api.getPopularMedia().results }
    override suspend fun getTrendingThisWeek(excludedIds: List<Int>) = fetchCached(CAT_TRENDING, excludedIds) { api.getTrendingThisWeek().results }
    override suspend fun getTrendingShows(excludedIds: List<Int>) = fetchCached(CAT_POPULAR, excludedIds) { api.discoverMedia(sortBy = "popularity.desc").results }
    override suspend fun getShowsByGenres(genreIds: String, excludedIds: List<Int>) = fetchCached(CAT_RECOMMENDED, excludedIds) { api.discoverMedia(genreId = genreIds).results }

    override suspend fun getDetailedRecommendations(genres: String?, excludedIds: List<Int>): List<MediaContent> {
        if (genres.isNullOrEmpty()) return (getTrendingShows(excludedIds) as? Resource.Success)?.data ?: emptyList()
        val result = safeApiCall {
            val res = api.discoverMedia(genreId = genres, sortBy = "popularity.desc", minRating = 6f, minVoteCount = 100).results
            if (excludedIds.isEmpty()) res else res.filter { it.id !in excludedIds }
        }
        return (result as? Resource.Success)?.data.takeIf { !it.isNullOrEmpty() } ?: (getTrendingShows(excludedIds) as? Resource.Success)?.data ?: emptyList()
    }

    override suspend fun getShowsOnTheAir(providers: String): Resource<List<MediaContent>> {
        val today = LocalDate.now().toString()
        val nextWeek = LocalDate.now().plusDays(7).toString()
        return discoverShows(
            providers = providers,
            watchRegion = "ES",
            sortBy = "popularity.desc",
            airDateGte = today,
            airDateLte = nextWeek
        )
    }

    override suspend fun searchShows(query: String) = safeApiCall { api.searchMedia(query).results }

    override suspend fun searchPerson(query: String) = safeApiCall { api.searchPerson(query).results }

    override suspend fun searchByPerson(query: String, isCreator: Boolean): Resource<List<MediaContent>> = safeApiCall {
        val people = api.searchPerson(query).results
        if (people.isEmpty()) return@safeApiCall emptyList()
        val jobs = setOf("Creator", "Executive Producer", "Showrunner", "Series Director")
        people.take(3).flatMap { p ->
            val c = api.getPersonTvCredits(p.id)
            if (isCreator) c.crew.filter { it.job in jobs }.map { it.toMediaContent() } else c.cast
        }.distinctBy { it.id }.filter { it.posterPath != null }.sortedByDescending { it.popularity }
    }

    override suspend fun discoverShowsPaged(genreId: String?, sortBy: String, page: Int, airDateGte: String?, airDateLte: String?): Resource<Pair<List<MediaContent>, Int>> = safeApiCall {
        api.discoverMedia(genreId = genreId, sortBy = sortBy, page = page, airDateGte = airDateGte, airDateLte = airDateLte).let { it.results to it.total_pages }
    }

    override suspend fun getSimilarShows(showId: Int): List<MediaContent> = (safeApiCall { api.getRecommendationsByShow(showId).results } as? Resource.Success)?.data ?: emptyList()
}

