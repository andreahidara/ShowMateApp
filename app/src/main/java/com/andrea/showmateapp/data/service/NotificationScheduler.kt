package com.andrea.showmateapp.data.service

import android.content.Context
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import java.util.concurrent.TimeUnit

object NotificationScheduler {

    fun scheduleAll(context: Context) {
        NotificationChannels.createAll(context)

        val wm = WorkManager.getInstance(context)
        val networkConstraint = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()

        wm.enqueueUniquePeriodicWork(
            "season_check",
            ExistingPeriodicWorkPolicy.KEEP,
            PeriodicWorkRequestBuilder<SeasonCheckWorker>(1, TimeUnit.DAYS)
                .setConstraints(networkConstraint)
                .build()
        )

        wm.enqueueUniquePeriodicWork(
            "friend_activity",
            ExistingPeriodicWorkPolicy.KEEP,
            PeriodicWorkRequestBuilder<FriendActivityWorker>(6, TimeUnit.HOURS)
                .setConstraints(networkConstraint)
                .build()
        )

        wm.enqueueUniquePeriodicWork(
            "inactivity_recs",
            ExistingPeriodicWorkPolicy.KEEP,
            PeriodicWorkRequestBuilder<InactivityRecsWorker>(1, TimeUnit.DAYS)
                .setConstraints(networkConstraint)
                .setInitialDelay(18, TimeUnit.HOURS)
                .build()
        )

        wm.enqueueUniquePeriodicWork(
            "hidden_gem",
            ExistingPeriodicWorkPolicy.KEEP,
            PeriodicWorkRequestBuilder<HiddenGemWorker>(1, TimeUnit.DAYS)
                .setConstraints(networkConstraint)
                .setInitialDelay(12, TimeUnit.HOURS)
                .build()
        )

        wm.enqueueUniquePeriodicWork(
            "streak_reminder",
            ExistingPeriodicWorkPolicy.KEEP,
            PeriodicWorkRequestBuilder<StreakReminderWorker>(1, TimeUnit.DAYS)
                .setInitialDelay(20, TimeUnit.HOURS)
                .build()
        )
    }
}
