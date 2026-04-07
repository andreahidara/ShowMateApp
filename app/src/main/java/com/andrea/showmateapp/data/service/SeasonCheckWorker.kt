package com.andrea.showmateapp.data.service

import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.andrea.showmateapp.R
import com.andrea.showmateapp.data.repository.ShowRepository
import com.andrea.showmateapp.di.AppPrefsDataStore
import com.andrea.showmateapp.domain.repository.IInteractionRepository
import com.andrea.showmateapp.ui.MainActivity
import com.andrea.showmateapp.ui.screens.profile.settings.SettingsViewModel
import com.andrea.showmateapp.util.Resource
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.first

@HiltWorker
class SeasonCheckWorker @AssistedInject constructor(
    @Assisted private val ctx: Context,
    @Assisted params: WorkerParameters,
    private val interactionRepository: IInteractionRepository,
    private val showRepository: ShowRepository,
    @AppPrefsDataStore private val dataStore: DataStore<Preferences>
) : CoroutineWorker(ctx, params) {

    companion object {
        val CHANNEL_ID = NotificationChannels.SEASON
    }

    override suspend fun doWork(): Result {
        val prefs = dataStore.data.first()
        val notifEnabled = prefs[SettingsViewModel.KEY_NOTIF_ENABLED] != false
        val notifNewEpisodes = prefs[SettingsViewModel.KEY_NOTIF_NEW_EPISODES] != false
        if (!notifEnabled || !notifNewEpisodes) return Result.success()

        return try {

            val watchedWithCount = interactionRepository.getWatchedShowsWithSeasonCount()

            coroutineScope {
                watchedWithCount.map { entity ->
                    async {
                        val result = showRepository.getShowDetails(entity.mediaId)
                        if (result is Resource.Success) {
                            val currentSeasons = result.data.numberOfSeasons ?: entity.lastKnownSeasons
                            if (currentSeasons > entity.lastKnownSeasons && entity.lastKnownSeasons > 0) {
                                sendNotification(entity.mediaId, result.data.name, currentSeasons)
                            }
                            if (currentSeasons != entity.lastKnownSeasons) {
                                interactionRepository.updateLastKnownSeasons(entity.mediaId, currentSeasons)
                            }
                        }
                    }
                }.awaitAll()
            }
            Result.success()
        } catch (e: Exception) {
            if (runAttemptCount < 3) Result.retry() else Result.failure()
        }
    }

    private fun sendNotification(showId: Int, showName: String, newSeasonCount: Int) {
        val nm = ctx.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        val intent = Intent(ctx, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra("open_show_name", showName)
        }
        val pendingIntent = PendingIntent.getActivity(
            ctx,
            showId,
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

        nm.notify(showId, notification)
    }
}
