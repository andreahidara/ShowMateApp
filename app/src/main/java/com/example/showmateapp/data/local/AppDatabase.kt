package com.example.showmateapp.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import com.example.showmateapp.data.model.TvShowEntity

@Database(entities = [TvShowEntity::class], version = 1, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {
    abstract fun tvShowDao(): TvShowDao
}
