package com.example.showmateapp.di

import android.content.Context
import androidx.room.Room
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

    @Provides
    @Singleton
    fun provideAppDatabase(@ApplicationContext context: Context): AppDatabase {
        return Room.databaseBuilder(
            context,
            AppDatabase::class.java,
            "showmate_database"
        ).fallbackToDestructiveMigration().build()
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
