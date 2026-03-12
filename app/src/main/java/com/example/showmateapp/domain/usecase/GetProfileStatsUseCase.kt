package com.example.showmateapp.domain.usecase

import com.example.showmateapp.data.network.MediaContent
import javax.inject.Inject

class GetProfileStatsUseCase @Inject constructor() {

    fun execute(watchedShows: List<MediaContent>): ProfileStats {
        var totalHours = 0
        watchedShows.forEach { show ->
            // Estimating: 10 episodes per season, 1 hour per episode avg
            val seasons = show.numberOfSeasons ?: 1
            totalHours += (seasons * 10)
        }
        
        return ProfileStats(
            totalWatchedHours = totalHours,
            watchedCount = watchedShows.size
        )
    }

    data class ProfileStats(
        val totalWatchedHours: Int,
        val watchedCount: Int
    )
}
