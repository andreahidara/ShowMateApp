package com.andrea.showmateapp.di

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

object DatabaseMigrations {
    val ALL = arrayOf(
        object : Migration(6, 7) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("ALTER TABLE media_content ADD COLUMN cachedAt INTEGER NOT NULL DEFAULT 0")
            }
        },
        object : Migration(7, 8) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("ALTER TABLE media_interactions ADD COLUMN isInWatchlist INTEGER NOT NULL DEFAULT 0")
            }
        },
        object : Migration(8, 9) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("ALTER TABLE media_interactions ADD COLUMN syncPending INTEGER NOT NULL DEFAULT 0")
            }
        },
        object : Migration(9, 10) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("CREATE INDEX IF NOT EXISTS index_media_content_category ON media_content (category)")
            }
        },
        object : Migration(10, 11) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("CREATE INDEX IF NOT EXISTS index_media_interactions_isWatched ON media_interactions (isWatched)")
                database.execSQL("CREATE INDEX IF NOT EXISTS index_media_interactions_isInWatchlist ON media_interactions (isInWatchlist)")
                database.execSQL("CREATE INDEX IF NOT EXISTS index_media_interactions_syncPending ON media_interactions (syncPending)")
            }
        },
        object : Migration(11, 12) {
            override fun migrate(database: SupportSQLiteDatabase) { }
        },
        object : Migration(12, 13) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("ALTER TABLE media_interactions ADD COLUMN isDisliked INTEGER NOT NULL DEFAULT 0")
                database.execSQL("CREATE INDEX IF NOT EXISTS index_media_interactions_isDisliked ON media_interactions (isDisliked)")
            }
        },
        object : Migration(13, 14) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("""CREATE TABLE IF NOT EXISTS seasons (
                    id INTEGER NOT NULL, showId INTEGER NOT NULL, seasonNumber INTEGER NOT NULL,
                    name TEXT NOT NULL, overview TEXT NOT NULL, posterPath TEXT, airDate TEXT,
                    tmdbId TEXT NOT NULL, cachedAt INTEGER NOT NULL DEFAULT 0, PRIMARY KEY(id))""")
                database.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS index_seasons_showId_seasonNumber ON seasons (showId, seasonNumber)")
            }
        }
    )
}
