package com.andrea.showmateapp.di

import com.andrea.showmateapp.data.repository.AuthRepository
import com.andrea.showmateapp.data.repository.UserRepository
import com.andrea.showmateapp.domain.repository.IAuthRepository
import com.andrea.showmateapp.domain.repository.IUserRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class UserModule {

    @Binds
    @Singleton
    abstract fun bindUserRepository(userRepository: UserRepository): IUserRepository

    @Binds
    @Singleton
    abstract fun bindAuthRepository(authRepository: AuthRepository): IAuthRepository
}
