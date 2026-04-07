package com.andrea.showmateapp.di

import com.andrea.showmateapp.BuildConfig
import com.andrea.showmateapp.data.network.TmdbApiService
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import okhttp3.CertificatePinner
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {

    private const val BASE_URL = "https://api.themoviedb.org/3/"
    private const val TMDB_HOST = "api.themoviedb.org"

    private val certificatePinner = CertificatePinner.Builder()
        .add(TMDB_HOST, "sha256/RQeZkB42znUfsDIIFWIRiYEcKl7nHwNFwWCrnMMJbi0=")
        .add(TMDB_HOST, "sha256/r/mIkG3eEpVdm+u/ko/cwxzOMo1bk4TyHIlByibiA5E=")
        .build()

    @Provides
    @Singleton
    fun provideAuthInterceptor(): Interceptor = Interceptor { chain ->
        val token = if (BuildConfig.TMDB_API_TOKEN.startsWith("Bearer ")) {
            BuildConfig.TMDB_API_TOKEN
        } else {
            "Bearer ${BuildConfig.TMDB_API_TOKEN}"
        }
        val request = chain.request().newBuilder()
            .addHeader("Authorization", token)
            .build()
        chain.proceed(request)
    }

    @Provides
    @Singleton
    fun provideOkHttpClient(authInterceptor: Interceptor): OkHttpClient {
        return OkHttpClient.Builder()
            .addInterceptor(authInterceptor)
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .build()
    }

    @Provides
    @Singleton
    fun provideRetrofit(okHttpClient: OkHttpClient): Retrofit {
        return Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
    }

    @Provides
    @Singleton
    fun provideTmdbApiService(retrofit: Retrofit): TmdbApiService {
        return retrofit.create(TmdbApiService::class.java)
    }
}
