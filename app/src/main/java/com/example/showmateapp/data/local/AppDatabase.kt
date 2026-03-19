package com.example.showmateapp.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import com.example.showmateapp.data.model.MediaEntity

@Database(entities = [MediaEntity::class, MediaInteractionEntity::class], version = 6, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {
    abstract fun showDao(): ShowDao
    abstract fun mediaInteractionDao(): MediaInteractionDao
}
