package com.andrea.showmateapp.di

import android.content.Context
import androidx.room.Room
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.andrea.showmateapp.BuildConfig
import com.andrea.showmateapp.data.local.AppDatabase
import com.andrea.showmateapp.data.local.MediaInteractionDao
import com.andrea.showmateapp.data.local.ShowDao
import com.andrea.showmateapp.util.DatabaseKeyProvider
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import net.sqlcipher.database.SQLiteDatabase
import net.sqlcipher.database.SupportFactory
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

    private val MIGRATION_8_9 = object : Migration(8, 9) {
        override fun migrate(database: SupportSQLiteDatabase) {
            database.execSQL(
                "ALTER TABLE media_interactions ADD COLUMN syncPending INTEGER NOT NULL DEFAULT 0"
            )
        }
    }

    private val MIGRATION_9_10 = object : Migration(9, 10) {
        override fun migrate(database: SupportSQLiteDatabase) {
            database.execSQL(
                "CREATE INDEX IF NOT EXISTS index_media_content_category ON media_content (category)"
            )
        }
    }

    private val MIGRATION_10_11 = object : Migration(10, 11) {
        override fun migrate(database: SupportSQLiteDatabase) {
            database.execSQL("CREATE INDEX IF NOT EXISTS index_media_interactions_isWatched ON media_interactions (isWatched)")
            database.execSQL("CREATE INDEX IF NOT EXISTS index_media_interactions_isInWatchlist ON media_interactions (isInWatchlist)")
            database.execSQL("CREATE INDEX IF NOT EXISTS index_media_interactions_syncPending ON media_interactions (syncPending)")
        }
    }

    @Provides
    @Singleton
    fun provideAppDatabase(@ApplicationContext context: Context): AppDatabase {
        SQLiteDatabase.loadLibs(context)
        val passphrase = DatabaseKeyProvider.getOrCreatePassphrase(context)
        val factory = SupportFactory(SQLiteDatabase.getBytes(
            String(passphrase, Charsets.ISO_8859_1).toCharArray()
        ))
        passphrase.fill(0)

        return Room.databaseBuilder(
            context,
            AppDatabase::class.java,
            "showmate_database_v2"
        )
            .openHelperFactory(factory)
            .addMigrations(MIGRATION_6_7, MIGRATION_7_8, MIGRATION_8_9, MIGRATION_9_10, MIGRATION_10_11)
            .fallbackToDestructiveMigrationOnDowngrade()
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
