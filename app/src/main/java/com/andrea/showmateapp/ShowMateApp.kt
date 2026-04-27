package com.andrea.showmateapp

import android.app.Application
import androidx.hilt.work.HiltWorkerFactory
import androidx.work.*
import coil.ImageLoader
import coil.ImageLoaderFactory
import coil.disk.DiskCache
import coil.imageLoader
import coil.memory.MemoryCache
import com.andrea.showmateapp.data.service.NotificationScheduler
import com.andrea.showmateapp.util.AppUtils
import com.andrea.showmateapp.util.AppUtils.isDebug
import com.andrea.showmateapp.util.NetworkMonitor
import com.andrea.showmateapp.util.SecurityChecker
import com.google.firebase.Firebase
import com.google.firebase.appcheck.FirebaseAppCheck
import com.google.firebase.appcheck.playintegrity.PlayIntegrityAppCheckProviderFactory
import com.google.firebase.crashlytics.FirebaseCrashlytics
import com.google.firebase.initialize
import com.google.firebase.perf.FirebasePerformance
import dagger.hilt.android.HiltAndroidApp
import javax.inject.Inject
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import timber.log.Timber

@HiltAndroidApp
class ShowMateApp : Application(), Configuration.Provider, ImageLoaderFactory {

    @Inject lateinit var workerFactory: HiltWorkerFactory
    @Inject lateinit var networkMonitor: NetworkMonitor

    private val appScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder().setWorkerFactory(workerFactory).build()

    override fun onCreate() {
        super.onCreate()

        if (isDebug) {
            Timber.plant(Timber.DebugTree())
        } else {
            Timber.plant(AppUtils.CrashlyticsTree())
        }

        Firebase.initialize(this)
        FirebaseCrashlytics.getInstance().setCrashlyticsCollectionEnabled(!isDebug)
        FirebasePerformance.getInstance().isPerformanceCollectionEnabled = !isDebug

        setupAppCheck()

        appScope.launch {
            if (!isDebug && SecurityChecker.isDeviceCompromised()) {
                FirebaseCrashlytics.getInstance().log("Security check failed: rooted=${SecurityChecker.isRooted()} emulator=${SecurityChecker.isEmulator()}")
            }
        }

        NotificationScheduler.scheduleAll(this)
    }

    private fun setupAppCheck() {
        if (!isDebug && AppUtils.isInstalledFromPlayStore(this)) {
            FirebaseAppCheck.getInstance().installAppCheckProviderFactory(
                PlayIntegrityAppCheckProviderFactory.getInstance()
            )
        }
    }

    override fun onTrimMemory(level: Int) {
        super.onTrimMemory(level)
        if (level >= TRIM_MEMORY_RUNNING_LOW) {
            imageLoader.memoryCache?.clear()
        }
    }

    override fun newImageLoader(): ImageLoader {
        val client = okhttp3.OkHttpClient.Builder()
            .connectTimeout(30, java.util.concurrent.TimeUnit.SECONDS)
            .readTimeout(30, java.util.concurrent.TimeUnit.SECONDS)
            .retryOnConnectionFailure(true)
            .build()
        return ImageLoader.Builder(this)
            .okHttpClient(client)
            .memoryCache { MemoryCache.Builder(this).maxSizePercent(0.25).build() }
            .diskCache {
                DiskCache.Builder()
                    .directory(cacheDir.resolve("image_cache"))
                    .maxSizeBytes(100L * 1024 * 1024)
                    .build()
            }
            .crossfade(true)
            .respectCacheHeaders(false)
            .build()
    }
}
