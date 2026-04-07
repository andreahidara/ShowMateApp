package com.andrea.showmateapp.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import com.andrea.showmateapp.data.model.MediaEntity

@Database(entities = [MediaEntity::class, MediaInteractionEntity::class], version = 11, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {
    abstract fun showDao(): ShowDao
    abstract fun mediaInteractionDao(): MediaInteractionDao
}
