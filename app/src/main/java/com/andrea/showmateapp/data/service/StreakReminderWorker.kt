package com.andrea.showmateapp.data.service

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.andrea.showmateapp.R
import com.andrea.showmateapp.data.model.UserProfile
import com.andrea.showmateapp.domain.repository.IUserRepository
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import java.time.LocalDate
import java.time.format.DateTimeFormatter

@HiltWorker
class StreakReminderWorker @AssistedInject constructor(
    @Assisted private val context: Context,
    @Assisted workerParams: WorkerParameters,
    private val userRepository: IUserRepository
) : CoroutineWorker(context, workerParams) {

    override suspend fun doWork(): Result {
        val profile = runCatching { userRepository.getUserProfile() }.getOrNull() ?: return Result.success()

        val today = LocalDate.now().format(DateTimeFormatter.ISO_LOCAL_DATE)
        val watchedToday = profile.viewingHistory.any { it.startsWith(today) }

        if (!watchedToday) {
            sendNotification(profile)
        }
        return Result.success()
    }

    private fun sendNotification(profile: UserProfile) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) !=
            PackageManager.PERMISSION_GRANTED
        ) {
            return
        }
        val streakDays = computeStreak(profile)
        val isDanger = streakDays >= DANGER_STREAK_THRESHOLD

        val channelId = if (isDanger) {
            NotificationChannels.STREAK_DANGER
        } else {
            NotificationChannels.GENERAL
        }

        val title = when {
            isDanger -> context.getString(R.string.notif_streak_danger_title, streakDays)
            streakDays > 0 -> context.getString(R.string.notif_streak_active_title)
            else -> context.getString(R.string.notif_streak_inactive_title)
        }
        val body = when {
            isDanger -> context.getString(R.string.notif_streak_danger_body, streakDays)
            streakDays > 0 -> context.getString(R.string.notif_streak_active_body, streakDays, if (streakDays == 1) "" else "s")
            else -> context.getString(R.string.notif_streak_inactive_body)
        }

        val notification = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle(title)
            .setContentText(body)
            .setPriority(
                if (isDanger) {
                    NotificationCompat.PRIORITY_HIGH
                } else {
                    NotificationCompat.PRIORITY_DEFAULT
                }
            )
            .setAutoCancel(true)
            .build()

        NotificationManagerCompat.from(context).notify(NOTIFICATION_ID, notification)
    }

    private fun computeStreak(profile: UserProfile): Int {
        val fmt = DateTimeFormatter.ISO_LOCAL_DATE
        val activeDays = profile.viewingHistory
            .mapNotNull { raw -> runCatching { LocalDate.parse(raw.split(":").first(), fmt) }.getOrNull() }
            .toSet()
        var streak = 0
        var day = LocalDate.now().minusDays(1)
        while (day in activeDays) {
            streak++
            day = day.minusDays(1)
        }
        return streak
    }

    companion object {
        const val NOTIFICATION_ID = 1002
        private const val DANGER_STREAK_THRESHOLD = 6
    }
}
