package com.andrea.showmateapp.di

import android.content.Context
import androidx.room.Room
import com.andrea.showmateapp.data.local.AppDatabase
import com.andrea.showmateapp.data.local.MediaInteractionDao
import com.andrea.showmateapp.data.local.ShowDao
import com.andrea.showmateapp.util.DatabaseKeyProvider
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton
import net.sqlcipher.database.SQLiteDatabase
import net.sqlcipher.database.SupportFactory

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideAppDatabase(@ApplicationContext context: Context): AppDatabase {
        SQLiteDatabase.loadLibs(context)
        val passphrase = DatabaseKeyProvider.getOrCreatePassphrase(context)
        val factory = SupportFactory(SQLiteDatabase.getBytes(String(passphrase, Charsets.ISO_8859_1).toCharArray()))
        passphrase.fill(0)

        return Room.databaseBuilder(context, AppDatabase::class.java, "showmate_database_v2")
            .openHelperFactory(factory)
            .addMigrations(*DatabaseMigrations.ALL)
            .fallbackToDestructiveMigrationOnDowngrade(dropAllTables = true)
            .fallbackToDestructiveMigration(dropAllTables = true)
            .build()
    }

    @Provides
    @Singleton
    fun provideShowDao(appDatabase: AppDatabase): ShowDao = appDatabase.showDao()

    @Provides
    @Singleton
    fun provideMediaInteractionDao(appDatabase: AppDatabase): MediaInteractionDao = appDatabase.mediaInteractionDao()
}
