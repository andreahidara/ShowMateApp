package com.example.showmateapp.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.showmateapp.data.model.MediaEntity

@Dao
interface ShowDao {

    @Query("SELECT * FROM media_content WHERE category = :category")
    suspend fun getShowsByCategory(category: String): List<MediaEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertShows(shows: List<MediaEntity>)

    @Query("SELECT * FROM media_content WHERE id = :id LIMIT 1")
    suspend fun getShowById(id: Int): MediaEntity?

    @Query("DELETE FROM media_content WHERE category = :category")
    suspend fun deleteShowsByCategory(category: String)

    // Helper transcation to replace category content
    @androidx.room.Transaction
    suspend fun replaceCategory(category: String, shows: List<MediaEntity>) {
        deleteShowsByCategory(category)
        insertShows(shows)
    }
}
