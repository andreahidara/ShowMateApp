package com.example.showmateapp.data.service

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.showmateapp.data.local.ShowDao
import com.example.showmateapp.data.model.toDomain
import com.example.showmateapp.data.repository.UserRepository
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject

/**
 * Runs when network is available. Reads the Room "liked" and "watched" caches and
 * ensures Firestore sub-collections are consistent with them.
 * Registered once at startup with a NetworkConnected constraint.
 */
@HiltWorker
class FirestoreSyncWorker @AssistedInject constructor(
    @Assisted ctx: Context,
    @Assisted params: WorkerParameters,
    private val showDao: ShowDao,
    private val userRepository: UserRepository
) : CoroutineWorker(ctx, params) {

    override suspend fun doWork(): Result {
        return try {
            val firestoreLiked = userRepository.getFavorites().map { it.id }.toSet()
            val roomLiked      = showDao.getShowsByCategory("liked").map { it.id }.toSet()

            // Shows in Room but not in Firestore → sync up
            val toSyncLike = roomLiked - firestoreLiked
            toSyncLike.forEach { mediaId ->
                val entity = showDao.getShowById(mediaId) ?: return@forEach
                userRepository.toggleFavorite(entity.toDomain(), setLiked = true)
            }

            val firestoreWatched = userRepository.getWatchedShows().map { it.id }.toSet()
            val roomWatched      = showDao.getShowsByCategory("watched").map { it.id }.toSet()

            val toSyncWatch = roomWatched - firestoreWatched
            toSyncWatch.forEach { mediaId ->
                val entity = showDao.getShowById(mediaId) ?: return@forEach
                userRepository.toggleWatched(entity.toDomain(), setWatched = true)
            }

            Result.success()
        } catch (e: Exception) {
            if (runAttemptCount < 3) Result.retry() else Result.failure()
        }
    }
}
