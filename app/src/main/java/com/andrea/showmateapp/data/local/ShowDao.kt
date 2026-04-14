package com.andrea.showmateapp.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import com.andrea.showmateapp.data.model.MediaEntity
import com.andrea.showmateapp.data.model.SeasonEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ShowDao {

    @Query("SELECT * FROM media_content WHERE category = :category")
    suspend fun getShowsByCategory(category: String): List<MediaEntity>

    @Query("SELECT * FROM media_content WHERE category = :category AND id NOT IN (:excludedIds)")
    suspend fun getShowsByCategoryExcluding(category: String, excludedIds: List<Int>): List<MediaEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertShows(shows: List<MediaEntity>)

    @Query("SELECT * FROM media_content WHERE id = :id AND category = 'liked' LIMIT 1")
    suspend fun getLikedShowById(id: Int): MediaEntity?

    @Query("SELECT * FROM media_content WHERE id = :id AND category = 'watched' LIMIT 1")
    suspend fun getWatchedShowById(id: Int): MediaEntity?

    @Query("DELETE FROM media_content WHERE category = :category")
    suspend fun deleteShowsByCategory(category: String)

    @Query("DELETE FROM media_content WHERE category = :category AND cachedAt > 0 AND cachedAt < :threshold")
    suspend fun deleteStaleByCategory(category: String, threshold: Long)

    @Query("SELECT * FROM media_content WHERE category = 'liked'")
    fun getLikedShowsFlow(): Flow<List<MediaEntity>>

    @Query("DELETE FROM media_content WHERE id = :id AND category = 'liked'")
    suspend fun deleteLikedShow(id: Int)

    @Query("SELECT * FROM media_content WHERE category = 'watched'")
    fun getWatchedShowsFlow(): Flow<List<MediaEntity>>

    @Query("DELETE FROM media_content WHERE id = :id AND category = 'watched'")
    suspend fun deleteWatchedShow(id: Int)

    @Query("SELECT * FROM media_content WHERE category = 'watchlist'")
    fun getWatchlistShowsFlow(): Flow<List<MediaEntity>>

    @Query("DELETE FROM media_content WHERE id = :id AND category = 'watchlist'")
    suspend fun deleteWatchlistShow(id: Int)

    @Query("DELETE FROM media_content WHERE category IN ('liked', 'watched', 'watchlist')")
    suspend fun clearUserData()

    @Query("DELETE FROM media_content WHERE id = :id AND category = :category")
    suspend fun deleteShowById(id: Int, category: String)

    @Transaction
    suspend fun replaceCategory(category: String, shows: List<MediaEntity>) {
        deleteShowsByCategory(category)
        insertShows(shows)
    }

    @Transaction
    suspend fun syncCategory(category: String, shows: List<MediaEntity>) {
        val incomingIds = shows.map { it.id }.toSet()
        val existing = getShowsByCategory(category)
        val toDelete = existing.filter { it.id !in incomingIds }
        toDelete.forEach { deleteShowById(it.id, category) }
        if (shows.isNotEmpty()) {
            insertShows(shows)
        }
    }

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSeason(season: SeasonEntity)

    @Query("SELECT * FROM seasons WHERE showId = :showId AND seasonNumber = :seasonNumber")
    suspend fun getSeason(showId: Int, seasonNumber: Int): SeasonEntity?
}
