package com.example.showmateapp.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.showmateapp.data.model.TvShowEntity

@Dao
interface TvShowDao {

    @Query("SELECT * FROM tv_shows WHERE category = :category")
    suspend fun getShowsByCategory(category: String): List<TvShowEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertShows(shows: List<TvShowEntity>)

    @Query("DELETE FROM tv_shows WHERE category = :category")
    suspend fun deleteShowsByCategory(category: String)

    // Helper transcation to replace category content
    @androidx.room.Transaction
    suspend fun replaceCategory(category: String, shows: List<TvShowEntity>) {
        deleteShowsByCategory(category)
        insertShows(shows)
    }
}
