package com.andrea.showmateapp.di

import com.andrea.showmateapp.data.repository.AchievementRepository
import com.andrea.showmateapp.data.repository.AuthRepository
import com.andrea.showmateapp.data.repository.GroupSessionRepository
import com.andrea.showmateapp.data.repository.ReviewRepository
import com.andrea.showmateapp.data.repository.ShowRepository
import com.andrea.showmateapp.data.repository.SocialRepository
import com.andrea.showmateapp.data.repository.UserInteractionRepository
import com.andrea.showmateapp.data.repository.UserRepository
import com.andrea.showmateapp.domain.repository.IAchievementRepository
import com.andrea.showmateapp.domain.repository.IAuthRepository
import com.andrea.showmateapp.domain.repository.IGroupSessionRepository
import com.andrea.showmateapp.domain.repository.IInteractionRepository
import com.andrea.showmateapp.domain.repository.IReviewRepository
import com.andrea.showmateapp.domain.repository.IShowRepository
import com.andrea.showmateapp.domain.repository.ISocialRepository
import com.andrea.showmateapp.domain.repository.IUserRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {

    @Binds
    @Singleton
    abstract fun bindShowRepository(
        showRepository: ShowRepository
    ): IShowRepository

    @Binds
    @Singleton
    abstract fun bindUserRepository(
        userRepository: UserRepository
    ): IUserRepository

    @Binds
    @Singleton
    abstract fun bindAuthRepository(
        authRepository: AuthRepository
    ): IAuthRepository

    @Binds
    @Singleton
    abstract fun bindUserInteractionRepository(
        userInteractionRepository: UserInteractionRepository
    ): IInteractionRepository

    @Binds
    @Singleton
    abstract fun bindSocialRepository(
        socialRepository: SocialRepository
    ): ISocialRepository

    @Binds
    @Singleton
    abstract fun bindReviewRepository(
        reviewRepository: ReviewRepository
    ): IReviewRepository

    @Binds
    @Singleton
    abstract fun bindAchievementRepository(
        achievementRepository: AchievementRepository
    ): IAchievementRepository

    @Binds
    @Singleton
    abstract fun bindGroupSessionRepository(
        groupSessionRepository: GroupSessionRepository
    ): IGroupSessionRepository
}
