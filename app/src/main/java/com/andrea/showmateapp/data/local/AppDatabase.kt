package com.andrea.showmateapp.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import com.andrea.showmateapp.data.model.MediaEntity
import com.andrea.showmateapp.data.model.SeasonEntity

@Database(
    entities = [MediaEntity::class, MediaInteractionEntity::class, SeasonEntity::class],
    version = 15,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun showDao(): ShowDao
    abstract fun mediaInteractionDao(): MediaInteractionDao

    suspend fun clearAllUserData() {
        showDao().clearUserData()
        mediaInteractionDao().deleteAll()
    }
}
