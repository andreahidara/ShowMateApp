package com.example.showmateapp.data.service

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.showmateapp.data.local.ShowDao
import com.example.showmateapp.data.model.toDomain
import com.example.showmateapp.data.repository.ShowRepository
import com.example.showmateapp.data.repository.UserRepository
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope

/**
 * Se ejecuta cuando hay red disponible. Lee las cachés "liked" y "watched" de Room y
 * asegura que las subcolecciones de Firestore sean consistentes con ellas.
 * Se registra una vez al inicio con restricción NetworkConnected.
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
            // Parallel reads: Firestore and Room at the same time
            val (firestoreLiked, firestoreWatched) = coroutineScope {
                val likedDef   = async { userRepository.getFavorites().map { it.id }.toSet() }
                val watchedDef = async { userRepository.getWatchedShows().map { it.id }.toSet() }
                likedDef.await() to watchedDef.await()
            }
            val (roomLiked, roomWatched) = coroutineScope {
                val likedDef   = async { showDao.getShowsByCategory(ShowRepository.CAT_LIKED).map { it.id }.toSet() }
                val watchedDef = async { showDao.getShowsByCategory(ShowRepository.CAT_WATCHED).map { it.id }.toSet() }
                likedDef.await() to watchedDef.await()
            }

            // Series en Room pero no en Firestore → sincronizar en paralelo
            val toSyncLike  = roomLiked  - firestoreLiked
            val toSyncWatch = roomWatched - firestoreWatched
            coroutineScope {
                val likeJobs = toSyncLike.map { mediaId ->
                    async {
                        val entity = showDao.getShowById(mediaId) ?: return@async
                        userRepository.toggleFavorite(entity.toDomain(), setLiked = true)
                    }
                }
                val watchJobs = toSyncWatch.map { mediaId ->
                    async {
                        val entity = showDao.getShowById(mediaId) ?: return@async
                        userRepository.toggleWatched(entity.toDomain(), setWatched = true)
                    }
                }
                (likeJobs + watchJobs).awaitAll()
            }

            Result.success()
        } catch (e: Exception) {
            if (runAttemptCount < 3) Result.retry() else Result.failure()
        }
    }
}
