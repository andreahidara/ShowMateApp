package com.andrea.showmateapp.di

import com.andrea.showmateapp.data.repository.AchievementRepository
import com.andrea.showmateapp.data.repository.ShowRepository
import com.andrea.showmateapp.data.repository.UserInteractionRepository
import com.andrea.showmateapp.domain.repository.IAchievementRepository
import com.andrea.showmateapp.domain.repository.IInteractionRepository
import com.andrea.showmateapp.domain.repository.IShowRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class MediaModule {

    @Binds
    @Singleton
    abstract fun bindShowRepository(showRepository: ShowRepository): IShowRepository

    @Binds
    @Singleton
    abstract fun bindUserInteractionRepository(
        userInteractionRepository: UserInteractionRepository
    ): IInteractionRepository

    @Binds
    @Singleton
    abstract fun bindAchievementRepository(achievementRepository: AchievementRepository): IAchievementRepository
}
