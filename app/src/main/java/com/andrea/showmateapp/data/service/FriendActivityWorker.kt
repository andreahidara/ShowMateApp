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
import com.andrea.showmateapp.data.model.ActivityEvent
import com.andrea.showmateapp.domain.repository.ISocialRepository
import com.andrea.showmateapp.ui.MainActivity
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject

@HiltWorker
class FriendActivityWorker @AssistedInject constructor(
    @Assisted private val context: Context,
    @Assisted params: WorkerParameters,
    private val socialRepository: ISocialRepository
) : CoroutineWorker(context, params) {

    companion object {
        private const val LOOK_BACK_MS = 6 * 60 * 60 * 1_000L
    }

    override suspend fun doWork(): Result {
        if (!hasNotificationPermission()) return Result.success()

        return try {
            val friends = socialRepository.getFriends()
            val highCompatFriends = friends.filter { it.compatibilityScore >= 70 }
            if (highCompatFriends.isEmpty()) return Result.success()

            val since = System.currentTimeMillis() - LOOK_BACK_MS
            val recentEvents = socialRepository
                .getFriendActivityFeed(highCompatFriends.map { it.uid }, limit = 30)
                .filter { it.timestamp >= since && it.type == ActivityEvent.TYPE_WATCHED }

            recentEvents.forEach { event ->
                val friend = highCompatFriends.find { it.uid == event.userId } ?: return@forEach
                sendNotification(friend.username, event.mediaTitle, friend.compatibilityScore, event.mediaId)
            }

            Result.success()
        } catch (e: Exception) {
            if (runAttemptCount < 2) Result.retry() else Result.failure()
        }
    }

    private fun sendNotification(
        friendName: String,
        showTitle: String,
        compatibility: Int,
        showId: Int
    ) {
        val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra("open_show_id", showId)
        }
        val pi = PendingIntent.getActivity(
            context,
            (friendName + showTitle).hashCode(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, NotificationChannels.FRIEND_ACTIVITY)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle("Tu amigo $friendName acaba de terminar $showTitle")
            .setContentText("Coincidís en un $compatibility% \u2014 \u00bfla ves t\u00fa tambi\u00e9n?")
            .setStyle(
                NotificationCompat.BigTextStyle()
                    .bigText(
                        "Tu amigo $friendName acaba de terminar \u201c$showTitle\u201d y vuestra " +
                        "compatibilidad es del $compatibility\u202f%. \u00a1Puede ser tu pr\u00f3xima serie!"
                    )
            )
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)
            .setContentIntent(pi)
            .addAction(R.drawable.ic_launcher_foreground, "Ver detalles", pi)
            .build()

        nm.notify((friendName + showTitle).hashCode(), notification)
    }

    private fun hasNotificationPermission(): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return true
        return ContextCompat.checkSelfPermission(
            context, Manifest.permission.POST_NOTIFICATIONS
        ) == PackageManager.PERMISSION_GRANTED
    }
}
