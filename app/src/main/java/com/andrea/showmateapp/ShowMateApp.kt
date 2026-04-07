package com.andrea.showmateapp

import android.app.Application
import android.util.Log
import androidx.hilt.work.HiltWorkerFactory
import androidx.work.Constraints
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.Configuration
import coil.ImageLoader
import coil.ImageLoaderFactory
import coil.disk.DiskCache
import coil.imageLoader
import coil.memory.MemoryCache
import com.google.firebase.Firebase
import com.google.firebase.appcheck.appCheck
import com.google.firebase.appcheck.playintegrity.PlayIntegrityAppCheckProviderFactory
import com.google.firebase.crashlytics.FirebaseCrashlytics
import com.google.firebase.initialize
import com.andrea.showmateapp.util.SecurityChecker
import com.andrea.showmateapp.data.service.FirestoreSyncWorker
import com.andrea.showmateapp.data.service.NotificationScheduler
import com.andrea.showmateapp.BuildConfig
import dagger.hilt.android.HiltAndroidApp
import timber.log.Timber
import javax.inject.Inject

@HiltAndroidApp
class ShowMateApp : Application(), Configuration.Provider, ImageLoaderFactory {

    @Inject
    lateinit var workerFactory: HiltWorkerFactory

    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder()
            .setWorkerFactory(workerFactory)
            .build()

    override fun onCreate() {
        super.onCreate()
        if (BuildConfig.DEBUG) {
            Timber.plant(Timber.DebugTree())
        } else {
            Timber.plant(CrashlyticsTree())
        }
        Firebase.initialize(this)
        /* Comentado temporalmente para que funcione en APKs compartidos manualmente
        Firebase.appCheck.installAppCheckProviderFactory(
            PlayIntegrityAppCheckProviderFactory.getInstance()
        )
        */
        FirebaseCrashlytics.getInstance().setCrashlyticsCollectionEnabled(!BuildConfig.DEBUG)

        if (!BuildConfig.DEBUG && SecurityChecker.isDeviceCompromised()) {
            FirebaseCrashlytics.getInstance().log("Launch on compromised device: rooted=${SecurityChecker.isRooted()} emulator=${SecurityChecker.isEmulator()}")
            Timber.w("Device security check failed: rooted or emulator detected")
        }

        if (BuildConfig.DEBUG && BuildConfig.TMDB_API_TOKEN.isBlank()) {
            error(
                "TMDB_API_TOKEN no está configurado.\n" +
                "Añade la siguiente línea a local.properties:\n" +
                "TMDB_API_TOKEN=tu_token_aqui"
            )
        }
        scheduleWorkers()
    }

    private fun scheduleWorkers() {
        val networkConstraint = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()

        WorkManager.getInstance(this).enqueue(
            OneTimeWorkRequestBuilder<FirestoreSyncWorker>()
                .setConstraints(networkConstraint)
                .build()
        )

        NotificationScheduler.scheduleAll(this)
    }

    private inner class CrashlyticsTree : Timber.Tree() {
        override fun log(priority: Int, tag: String?, message: String, t: Throwable?) {
            if (priority < Log.WARN) return
            val crashlytics = FirebaseCrashlytics.getInstance()
            tag?.let { crashlytics.setCustomKey("timber_tag", it) }
            crashlytics.log(message)
            if (t != null && priority == Log.ERROR) crashlytics.recordException(t)
        }
    }

    override fun onTrimMemory(level: Int) {
        super.onTrimMemory(level)
        if (level >= TRIM_MEMORY_RUNNING_LOW) {
            imageLoader.memoryCache?.clear()
        }
    }

    override fun newImageLoader(): ImageLoader =
        ImageLoader.Builder(this)
            .memoryCache {
                MemoryCache.Builder(this)
                    .maxSizePercent(0.15)
                    .build()
            }
            .diskCache {
                DiskCache.Builder()
                    .directory(cacheDir.resolve("image_cache"))
                    .maxSizeBytes(75L * 1024 * 1024)
                    .build()
            }
            .crossfade(true)
            .respectCacheHeaders(false)
            .build()
}
