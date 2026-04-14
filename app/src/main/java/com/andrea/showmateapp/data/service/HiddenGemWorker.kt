package com.andrea.showmateapp.data.service

import android.Manifest
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.andrea.showmateapp.R
import com.andrea.showmateapp.data.model.MediaContent
import com.andrea.showmateapp.domain.repository.IInteractionRepository
import com.andrea.showmateapp.domain.repository.IShowRepository
import com.andrea.showmateapp.domain.repository.IUserRepository
import com.andrea.showmateapp.ui.MainActivity
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject

@HiltWorker
class HiddenGemWorker @AssistedInject constructor(
    @Assisted private val context: Context,
    @Assisted params: WorkerParameters,
    private val userRepository: IUserRepository,
    private val showRepository: IShowRepository,
    private val interactionRepository: IInteractionRepository
) : CoroutineWorker(context, params) {

    companion object {
        const val NOTIFICATION_ID = 2002

        private const val MIN_VOTE_AVERAGE = 7.5f
        private const val MAX_VOTE_COUNT = 5_000
        private const val MIN_VOTE_COUNT = 100
    }

    override suspend fun doWork(): Result {
        if (!hasNotificationPermission()) return Result.success()

        return try {
            val profile = userRepository.getUserProfile() ?: return Result.success()
            val watchedIds = interactionRepository.getWatchedMediaIds()
            val excludedIds = watchedIds +
                profile.likedMediaIds.toSet() +
                profile.essentialMediaIds.toSet() +
                profile.dislikedMediaIds.toSet()

            val topGenres = profile.genreScores.entries
                .sortedByDescending { it.value }
                .take(3)
                .joinToString(",") { it.key }

            val gem = showRepository
                .getDetailedRecommendations(topGenres.ifBlank { null })
                .filter { candidate ->
                    candidate.id !in excludedIds &&
                        candidate.voteAverage >= MIN_VOTE_AVERAGE &&
                        candidate.voteCount in MIN_VOTE_COUNT..MAX_VOTE_COUNT
                }
                .maxByOrNull { it.voteAverage }
                ?: return Result.success()

            sendNotification(gem)
            Result.success()
        } catch (e: Exception) {
            if (runAttemptCount < 2) Result.retry() else Result.failure()
        }
    }

    private fun sendNotification(gem: MediaContent) {
        val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        val intent = Intent(
            Intent.ACTION_VIEW,
            Uri.parse("showmate://detail/${gem.id}"),
            context,
            MainActivity::class.java
        ).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val pi = PendingIntent.getActivity(
            context,
            NOTIFICATION_ID,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val overview = gem.overview.take(180).let { if (gem.overview.length > 180) "$it\u2026" else it }

        val notification = NotificationCompat.Builder(context, NotificationChannels.HIDDEN_GEM)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle(context.getString(R.string.notif_gem_title, gem.name))
            .setContentText(context.getString(R.string.notif_gem_content, gem.voteAverage, gem.voteCount))
            .setStyle(
                NotificationCompat.BigTextStyle()
                    .bigText(context.getString(R.string.notif_gem_big_text, gem.voteAverage, gem.voteCount, overview))
                    .setBigContentTitle(context.getString(R.string.notif_gem_big_title, gem.name))
                    .setSummaryText(context.getString(R.string.notif_gem_summary))
            )
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)
            .setContentIntent(pi)
            .addAction(R.drawable.ic_launcher_foreground, context.getString(R.string.notif_gem_action), pi)
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

