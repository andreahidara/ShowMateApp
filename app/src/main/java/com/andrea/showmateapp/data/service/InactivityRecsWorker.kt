package com.andrea.showmateapp.data.service

import android.Manifest
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.andrea.showmateapp.R
import com.andrea.showmateapp.data.network.MediaContent
import com.andrea.showmateapp.domain.repository.IInteractionRepository
import com.andrea.showmateapp.domain.repository.IShowRepository
import com.andrea.showmateapp.domain.repository.IUserRepository
import com.andrea.showmateapp.ui.MainActivity
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit

@HiltWorker
class InactivityRecsWorker @AssistedInject constructor(
    @Assisted private val context: Context,
    @Assisted params: WorkerParameters,
    private val userRepository: IUserRepository,
    private val showRepository: IShowRepository,
    private val interactionRepository: IInteractionRepository
) : CoroutineWorker(context, params) {

    companion object {
        const val NOTIFICATION_ID   = 2001
        private const val INACTIVITY_THRESHOLD_DAYS = 3L
        private const val MAX_RECS  = 3
    }

    override suspend fun doWork(): Result {
        if (!hasNotificationPermission()) return Result.success()

        return try {
            val profile = userRepository.getUserProfile() ?: return Result.success()

            val daysSinceWatch = daysSinceLastWatch(profile.viewingHistory)
            if (daysSinceWatch < INACTIVITY_THRESHOLD_DAYS) return Result.success()

            val watchedIds  = interactionRepository.getWatchedMediaIds()
            val dislikedIds = profile.dislikedMediaIds.toSet()
            val excludedIds = watchedIds + dislikedIds

            val topGenres = profile.genreScores.entries
                .sortedByDescending { it.value }
                .take(3)
                .joinToString(",") { it.key }

            val recommendations = showRepository
                .getDetailedRecommendations(topGenres.ifBlank { null })
                .filter { it.id !in excludedIds }
                .take(MAX_RECS)

            if (recommendations.isEmpty()) return Result.success()

            sendNotification(recommendations, daysSinceWatch)
            Result.success()
        } catch (e: Exception) {
            if (runAttemptCount < 2) Result.retry() else Result.failure()
        }
    }

    private fun daysSinceLastWatch(viewingHistory: List<String>): Long {
        val fmt = DateTimeFormatter.ISO_LOCAL_DATE
        val lastWatchDate = viewingHistory
            .mapNotNull { raw -> runCatching { LocalDate.parse(raw.split(":")[0], fmt) }.getOrNull() }
            .maxOrNull()
            ?: return Long.MAX_VALUE
        return ChronoUnit.DAYS.between(lastWatchDate, LocalDate.now())
    }

    private fun sendNotification(shows: List<MediaContent>, daysSince: Long) {
        val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val pi = PendingIntent.getActivity(
            context, NOTIFICATION_ID, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val shortList = shows.joinToString("  \u00b7  ") { it.name }
        val bigText   = "Aqu\u00ed tienes 3 recomendaciones r\u00e1pidas para esta noche:\n\n" +
            shows.mapIndexed { i, s -> "${i + 1}. ${s.name} (\u2605\u202f${s.voteAverage})" }
                .joinToString("\n")

        val notification = NotificationCompat.Builder(context, NotificationChannels.INACTIVITY_RECS)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle("Llevas $daysSince d\u00edas sin ver series \uD83C\uDF7F")
            .setContentText(shortList)
            .setStyle(
                NotificationCompat.BigTextStyle()
                    .bigText(bigText)
                    .setBigContentTitle("\u00a1Es hora de una buena serie!")
                    .setSummaryText("ShowMate \u00b7 Recomendaciones")
            )
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)
            .setContentIntent(pi)
            .addAction(R.drawable.ic_launcher_foreground, "Ver recomendaciones", pi)
            .build()

        nm.notify(NOTIFICATION_ID, notification)
    }

    private fun hasNotificationPermission(): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return true
        return ContextCompat.checkSelfPermission(
            context, Manifest.permission.POST_NOTIFICATIONS
        ) == PackageManager.PERMISSION_GRANTED
    }
}
