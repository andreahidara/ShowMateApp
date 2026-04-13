package com.andrea.showmateapp.domain.repository

import com.andrea.showmateapp.data.network.MediaContent
import com.andrea.showmateapp.data.network.PersonResponse
import com.andrea.showmateapp.data.network.SeasonResponse
import com.andrea.showmateapp.util.Resource

interface IShowRepository {
    suspend fun getShowDetails(showId: Int): Resource<MediaContent>
    suspend fun getSeasonDetails(showId: Int, seasonNumber: Int): Resource<SeasonResponse>
    suspend fun getPopularShows(excludedIds: List<Int> = emptyList()): Resource<List<MediaContent>>
    suspend fun getTrendingThisWeek(excludedIds: List<Int> = emptyList()): Resource<List<MediaContent>>
    suspend fun getTrendingShows(excludedIds: List<Int> = emptyList()): Resource<List<MediaContent>>
    suspend fun getShowsByGenres(genreIds: String, excludedIds: List<Int> = emptyList()): Resource<List<MediaContent>>
    suspend fun getDetailedRecommendations(genres: String?, excludedIds: List<Int> = emptyList()): List<MediaContent>
    suspend fun getShowsOnTheAir(providers: String = "8|119|337|1899|531"): Resource<List<MediaContent>>
    suspend fun searchShows(query: String): Resource<List<MediaContent>>
    suspend fun searchByPerson(query: String, isCreator: Boolean): Resource<List<MediaContent>>
    suspend fun getSimilarShows(showId: Int): List<MediaContent>
    suspend fun getShowDetailsInParallel(ids: List<Int>, maxCount: Int = 20): List<MediaContent>
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
        firstAirDateLte: String? = null,
        excludedIds: List<Int> = emptyList()
    ): Resource<List<MediaContent>>
    suspend fun getPersonDetails(personId: Int): Resource<PersonResponse>
    suspend fun getPersonTvCredits(personId: Int): Resource<List<MediaContent>>
    suspend fun discoverShowsPaged(
        genreId: String? = null,
        sortBy: String = "popularity.desc",
        page: Int = 1,
        airDateGte: String? = null,
        airDateLte: String? = null
    ): Resource<Pair<List<MediaContent>, Int>>
}
