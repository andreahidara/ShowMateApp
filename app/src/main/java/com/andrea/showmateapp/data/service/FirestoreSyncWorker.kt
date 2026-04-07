package com.andrea.showmateapp.data.service

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.andrea.showmateapp.data.local.MediaInteractionDao
import com.andrea.showmateapp.data.local.ShowDao
import com.andrea.showmateapp.data.model.toDomain
import com.andrea.showmateapp.domain.repository.IInteractionRepository
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope

@HiltWorker
class FirestoreSyncWorker @AssistedInject constructor(
    @Assisted ctx: Context,
    @Assisted params: WorkerParameters,
    private val showDao: ShowDao,
    private val interactionDao: MediaInteractionDao,
    private val interactionRepository: IInteractionRepository
) : CoroutineWorker(ctx, params) {

    override suspend fun doWork(): Result {
        return try {
            val pendings = interactionDao.getPendingSyncInteractions()
            if (pendings.isEmpty()) return Result.success()

            coroutineScope {
                val syncJobs = pendings.map { entity ->
                    async {
                        val media = showDao.getShowById(entity.mediaId)?.toDomain()
                            ?: return@async

                        try {
                            interactionRepository.toggleFavorite(media, entity.isLiked)
                            interactionRepository.toggleWatched(media, entity.isWatched)
                            interactionRepository.toggleEssential(media, entity.isEssential)
                            interactionRepository.toggleWatchlist(media, entity.isInWatchlist)
                        } catch (_: Exception) {
                        }
                    }
                }
                syncJobs.awaitAll()
            }

            Result.success()
        } catch (e: Exception) {
            if (runAttemptCount < 3) Result.retry() else Result.failure()
        }
    }
}
