package com.andrea.showmateapp.domain.repository

import com.andrea.showmateapp.data.local.MediaInteractionEntity
import com.andrea.showmateapp.data.model.MediaEntity
import com.andrea.showmateapp.data.model.MediaContent
import kotlinx.coroutines.flow.Flow

interface IInteractionRepository {
    sealed class InteractionType {
        object Like : InteractionType()
        object Essential : InteractionType()
        object Watched : InteractionType()
        object Watchlist : InteractionType()
        object Dislike : InteractionType()
        data class Rate(val score: Int) : InteractionType()
    }

    suspend fun getWatchedShows(): List<MediaContent>
    suspend fun getWatchlist(): List<MediaContent>
    fun getLikedShowsFlow(): Flow<List<MediaEntity>>
    fun getWatchedShowsFlow(): Flow<List<MediaEntity>>
    fun getWatchlistShowsFlow(): Flow<List<MediaEntity>>
    suspend fun getFavorites(): List<MediaContent>
    suspend fun getEssentials(): List<MediaContent>
    suspend fun getWatchedMediaIds(): Set<Int>
    fun getWatchedMediaIdsFlow(): Flow<Set<Int>>
    suspend fun getExcludedMediaIds(): Set<Int>
    fun getExcludedMediaIdsFlow(): Flow<Set<Int>>
    fun getInteractedMediaIdsFlow(): Flow<Set<Int>>
    suspend fun getLocalInteractionState(mediaId: Int): MediaInteractionEntity?
    suspend fun toggleWatched(media: MediaContent, setWatched: Boolean): Boolean
    suspend fun toggleDislike(media: MediaContent, setDisliked: Boolean): Boolean
    suspend fun toggleFavorite(media: MediaContent, setLiked: Boolean): Boolean
    suspend fun toggleEssential(media: MediaContent, setEssential: Boolean): Boolean
    suspend fun toggleWatchlist(media: MediaContent, setInWatchlist: Boolean): Boolean
    suspend fun isInWatchlist(mediaId: Int): Boolean
    suspend fun cacheInteractionState(mediaId: Int, isLiked: Boolean, isEssential: Boolean, isWatched: Boolean)
    suspend fun toggleEpisodeWatched(showId: Int, episodeId: Int): Boolean
    suspend fun setAllEpisodesWatched(showId: Int, episodeIds: List<Int>)
    suspend fun trackMediaInteraction(
        mediaId: Int,
        genres: List<String>,
        keywords: List<String>,
        actors: List<Int>,
        narrativeStyles: Map<String, Float>,
        creators: List<Int>,
        interactionType: InteractionType
    )
    suspend fun updateRating(mediaId: Int, rating: Int)
    suspend fun deleteRating(mediaId: Int)
    suspend fun getAllRatings(): Map<Int, Int>
    suspend fun getUserRating(mediaId: Int): Int?
    suspend fun saveReview(mediaId: Int, text: String)
    suspend fun getReview(mediaId: Int): String?
    suspend fun addToCustomList(listName: String, mediaId: Int)
    suspend fun removeFromCustomList(listName: String, mediaId: Int)
    suspend fun createCustomList(listName: String)
    suspend fun deleteCustomList(listName: String)
    suspend fun getCustomLists(): Map<String, List<Int>>
    fun getCustomListsFlow(): Flow<Map<String, List<Int>>>
    suspend fun getWatchedShowsWithSeasonCount(): List<MediaInteractionEntity>
    suspend fun updateLastKnownSeasons(mediaId: Int, seasons: Int)
    suspend fun syncFavoritesAndWatchedToRoom()
}
