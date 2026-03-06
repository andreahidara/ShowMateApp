package com.example.showmateapp.data.worker

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.showmateapp.data.repository.TvShowRepository
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import android.util.Log

@HiltWorker
class DailyPicksWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted workerParams: WorkerParameters,
    private val tvShowRepository: TvShowRepository
) : CoroutineWorker(context, workerParams) {

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        try {
            Log.d("DailyPicksWorker", "Starting daily pre-fetch routine...")
            // By fetching these, the repository will automatically cache them in Room
            tvShowRepository.getTrendingTvShows() 
            tvShowRepository.getPopularTvShows()
            
            Log.d("DailyPicksWorker", "Daily pre-fetch complete!")
            Result.success()
        } catch (e: Exception) {
            Log.e("DailyPicksWorker", "Failed to pre-fetch daily picks: ${e.localizedMessage}")
            Result.retry()
        }
    }
}
