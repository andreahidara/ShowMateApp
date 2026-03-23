package com.example.showmateapp.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface MediaInteractionDao {

    @Query("SELECT * FROM media_interactions WHERE mediaId = :mediaId")
    suspend fun getById(mediaId: Int): MediaInteractionEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(entity: MediaInteractionEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(entities: List<MediaInteractionEntity>)

    @Query("SELECT * FROM media_interactions WHERE mediaId IN (:ids)")
    suspend fun getByIds(ids: List<Int>): List<MediaInteractionEntity>

    @Query("SELECT mediaId FROM media_interactions WHERE isWatched = 1")
    suspend fun getWatchedMediaIds(): List<Int>

    @Query("SELECT * FROM media_interactions WHERE isWatched = 1 AND lastKnownSeasons > 0")
    suspend fun getWatchedWithSeasonCount(): List<MediaInteractionEntity>
}
