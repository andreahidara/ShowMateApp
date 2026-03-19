package com.example.showmateapp.data.service

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import androidx.core.app.NotificationCompat
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.showmateapp.R
import com.example.showmateapp.data.repository.ShowRepository
import com.example.showmateapp.data.repository.UserRepository
import com.example.showmateapp.util.Resource
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject

/**
 * Periodic worker (daily) that checks if any watched show has added a new season
 * since the user last watched it, then sends a local notification.
 */
@HiltWorker
class SeasonCheckWorker @AssistedInject constructor(
    @Assisted private val ctx: Context,
    @Assisted params: WorkerParameters,
    private val userRepository: UserRepository,
    private val showRepository: ShowRepository
) : CoroutineWorker(ctx, params) {

    companion object {
        const val CHANNEL_ID = "season_notifications"
    }

    override suspend fun doWork(): Result {
        return try {
            createNotificationChannel()

            val watchedWithCount = userRepository.getWatchedShowsWithSeasonCount()

            watchedWithCount.forEach { entity ->
                val result = showRepository.getShowDetails(entity.mediaId)
                if (result is Resource.Success) {
                    val currentSeasons = result.data.numberOfSeasons ?: entity.lastKnownSeasons
                    if (currentSeasons > entity.lastKnownSeasons && entity.lastKnownSeasons > 0) {
                        sendNotification(result.data.name, currentSeasons)
                        userRepository.updateLastKnownSeasons(entity.mediaId, currentSeasons)
                    }
                }
            }
            Result.success()
        } catch (e: Exception) {
            Result.retry()
        }
    }

    private fun createNotificationChannel() {
        val channel = NotificationChannel(
            CHANNEL_ID,
            "Nuevas temporadas",
            NotificationManager.IMPORTANCE_DEFAULT
        ).apply { description = "Notifica cuando una serie que has visto tiene nueva temporada" }
        val nm = ctx.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        nm.createNotificationChannel(channel)
    }

    private fun sendNotification(showName: String, newSeasonCount: Int) {
        val nm = ctx.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val notification = NotificationCompat.Builder(ctx, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_logo_placeholder)
            .setContentTitle("¡Nueva temporada disponible!")
            .setContentText("$showName tiene ahora $newSeasonCount temporadas")
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)
            .build()
        nm.notify(showName.hashCode(), notification)
    }
}
