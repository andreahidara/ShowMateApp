package com.andrea.showmateapp.di

import android.content.Context
import com.andrea.showmateapp.BuildConfig
import com.andrea.showmateapp.data.network.TmdbApiService
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import java.io.File
import java.util.concurrent.TimeUnit
import javax.inject.Singleton
import okhttp3.Cache
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {

    private const val BASE_URL = "https://api.themoviedb.org/3/"

    @Provides
    @Singleton
    fun provideAuthInterceptor(): Interceptor = Interceptor { chain ->
        val token = BuildConfig.TMDB_API_TOKEN.removeSurrounding("\"").trim()

        val request = chain.request().newBuilder()
            .header("Authorization", if (token.startsWith("Bearer ", true)) token else "Bearer $token")
            .header("Accept", "application/json")
            .build()
        chain.proceed(request)
    }

    @Provides
    @Singleton
    fun provideHttpCache(@ApplicationContext context: Context): Cache {
        return Cache(File(context.cacheDir, "http_cache"), 30L * 1024 * 1024)
    }

    @Provides
    @Singleton
    fun provideOkHttpClient(authInterceptor: Interceptor, cache: Cache): OkHttpClient {
        val logging = HttpLoggingInterceptor().apply {
            level = if (BuildConfig.DEBUG) HttpLoggingInterceptor.Level.HEADERS else HttpLoggingInterceptor.Level.NONE
        }
        val retryInterceptor = Interceptor { chain ->
            var attempt = 0
            var lastException: Exception? = null
            while (attempt < 3) {
                try {
                    val response = chain.proceed(chain.request())
                    if (response.isSuccessful || attempt == 2) return@Interceptor response
                    response.close()
                } catch (e: Exception) {
                    lastException = e
                }
                attempt++
            }
            throw lastException ?: Exception("Max retries exceeded")
        }
        return OkHttpClient.Builder()
            .addInterceptor(authInterceptor)
            .addInterceptor(retryInterceptor)
            .addInterceptor(logging)
            .cache(cache)
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .retryOnConnectionFailure(true)
            .build()
    }

    @Provides
    @Singleton
    fun provideRetrofit(okHttpClient: OkHttpClient): Retrofit = Retrofit.Builder()
        .baseUrl(BASE_URL)
        .client(okHttpClient)
        .addConverterFactory(GsonConverterFactory.create())
        .build()

    @Provides
    @Singleton
    fun provideTmdbApiService(retrofit: Retrofit): TmdbApiService = retrofit.create(TmdbApiService::class.java)
}
