package com.example.showmateapp.di

import android.content.Context
import androidx.room.Room
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.showmateapp.data.local.AppDatabase
import com.example.showmateapp.data.local.MediaInteractionDao
import com.example.showmateapp.data.local.ShowDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    private val MIGRATION_6_7 = object : Migration(6, 7) {
        override fun migrate(database: SupportSQLiteDatabase) {
            database.execSQL(
                "ALTER TABLE media_content ADD COLUMN cachedAt INTEGER NOT NULL DEFAULT 0"
            )
        }
    }

    private val MIGRATION_7_8 = object : Migration(7, 8) {
        override fun migrate(database: SupportSQLiteDatabase) {
            database.execSQL(
                "ALTER TABLE media_interactions ADD COLUMN isInWatchlist INTEGER NOT NULL DEFAULT 0"
            )
        }
    }

    @Provides
    @Singleton
    fun provideAppDatabase(@ApplicationContext context: Context): AppDatabase {
        return Room.databaseBuilder(
            context,
            AppDatabase::class.java,
            "showmate_database"
        )
            .addMigrations(MIGRATION_6_7, MIGRATION_7_8)
            .fallbackToDestructiveMigration()
            .build()
    }

    @Provides
    @Singleton
    fun provideShowDao(appDatabase: AppDatabase): ShowDao {
        return appDatabase.showDao()
    }

    @Provides
    @Singleton
    fun provideMediaInteractionDao(appDatabase: AppDatabase): MediaInteractionDao {
        return appDatabase.mediaInteractionDao()
    }

}
