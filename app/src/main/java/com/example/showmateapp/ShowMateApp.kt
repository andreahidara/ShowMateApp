package com.example.showmateapp

import android.app.Application
import androidx.hilt.work.HiltWorkerFactory
import androidx.work.Configuration
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import dagger.hilt.android.HiltAndroidApp
import java.util.concurrent.TimeUnit
import javax.inject.Inject

@HiltAndroidApp
class ShowMateApp : Application(), Configuration.Provider {

    @Inject
    lateinit var workerFactory: HiltWorkerFactory

    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder()
            .setWorkerFactory(workerFactory)
            .build()

    override fun onCreate() {
        super.onCreate()
        setupWorkManager()
    }

    private fun setupWorkManager() {
        // TODO: The DailyPicksWorker class doesn't exist yet, commenting out to fix build.
        /*
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.UNMETERED) // Only Wi-Fi
            .setRequiresCharging(true) // Only when charging
            .build()

        val dailyPicksRequest = PeriodicWorkRequestBuilder<com.example.showmateapp.data.worker.DailyPicksWorker>(
            24, TimeUnit.HOURS
        )
            .setConstraints(constraints)
            .build()

        WorkManager.getInstance(this).enqueueUniquePeriodicWork(
            "daily_picks_fetch",
            ExistingPeriodicWorkPolicy.KEEP,
            dailyPicksRequest
        )
        */
    }
}
