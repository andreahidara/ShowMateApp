package com.andrea.showmateapp.di

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.preferencesDataStore
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Qualifier
import javax.inject.Singleton

@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class SecurityDataStore

@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class AppPrefsDataStore

private val Context.searchDataStore: DataStore<Preferences> by preferencesDataStore(name = "search_prefs")
private val Context.securityDataStore: DataStore<Preferences> by preferencesDataStore(name = "security_prefs")
private val Context.appPrefsDataStore: DataStore<Preferences> by preferencesDataStore(name = "app_prefs")

@Module
@InstallIn(SingletonComponent::class)
object DataStoreModule {

    @Provides
    @Singleton
    fun provideSearchDataStore(@ApplicationContext context: Context): DataStore<Preferences> =
        context.searchDataStore

    @SecurityDataStore
    @Provides
    @Singleton
    fun provideSecurityDataStore(@ApplicationContext context: Context): DataStore<Preferences> =
        context.securityDataStore

    @AppPrefsDataStore
    @Provides
    @Singleton
    fun provideAppPrefsDataStore(@ApplicationContext context: Context): DataStore<Preferences> =
        context.appPrefsDataStore

}
