package com.andrea.showmateapp.di

import com.andrea.showmateapp.data.repository.GroupSessionRepository
import com.andrea.showmateapp.data.repository.ReviewRepository
import com.andrea.showmateapp.data.repository.SharedListRepository
import com.andrea.showmateapp.data.repository.SocialRepository
import com.andrea.showmateapp.domain.repository.IGroupSessionRepository
import com.andrea.showmateapp.domain.repository.IReviewRepository
import com.andrea.showmateapp.domain.repository.ISharedListRepository
import com.andrea.showmateapp.domain.repository.ISocialRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class SocialModule {

    @Binds
    @Singleton
    abstract fun bindSocialRepository(socialRepository: SocialRepository): ISocialRepository

    @Binds
    @Singleton
    abstract fun bindReviewRepository(reviewRepository: ReviewRepository): IReviewRepository

    @Binds
    @Singleton
    abstract fun bindGroupSessionRepository(groupSessionRepository: GroupSessionRepository): IGroupSessionRepository

    @Binds
    @Singleton
    abstract fun bindSharedListRepository(sharedListRepository: SharedListRepository): ISharedListRepository
}
