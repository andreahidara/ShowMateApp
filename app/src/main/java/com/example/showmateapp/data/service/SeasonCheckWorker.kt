package com.example.showmateapp.data.service

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.showmateapp.R
import com.example.showmateapp.data.repository.ShowRepository
import com.example.showmateapp.data.repository.UserRepository
import com.example.showmateapp.ui.MainActivity
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
                    }
                    if (currentSeasons != entity.lastKnownSeasons) {
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
            NotificationManager.IMPORTANCE_HIGH
        ).apply {
            description = "Notifica cuando una serie que has visto estrena nueva temporada"
            enableLights(true)
            enableVibration(true)
        }
        val nm = ctx.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        nm.createNotificationChannel(channel)
    }

    private fun sendNotification(showName: String, newSeasonCount: Int) {
        val nm = ctx.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        // Tap → abre la app en MainActivity
        val intent = Intent(ctx, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra("open_show_name", showName)
        }
        val pendingIntent = PendingIntent.getActivity(
            ctx,
            showName.hashCode(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val bigText = "La temporada $newSeasonCount de $showName ya está disponible. ¡Es el momento de ponerte al día!"

        val notification = NotificationCompat.Builder(ctx, CHANNEL_ID)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle("🎬 ¡Nueva temporada de $showName!")
            .setContentText("Temporada $newSeasonCount disponible")
            .setStyle(
                NotificationCompat.BigTextStyle()
                    .bigText(bigText)
                    .setBigContentTitle("¡Nueva temporada de $showName!")
                    .setSummaryText("ShowMate")
            )
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .addAction(
                R.mipmap.ic_launcher,
                "Ver ahora",
                pendingIntent
            )
            .build()

        nm.notify(showName.hashCode(), notification)
    }
}
