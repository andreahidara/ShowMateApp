package com.example.showmateapp.domain.usecase

import com.example.showmateapp.data.model.UserProfile
import com.example.showmateapp.data.network.MediaContent
import com.example.showmateapp.util.GenreMapper
import javax.inject.Inject

class GetProfileStatsUseCase @Inject constructor() {

    fun execute(watchedShows: List<MediaContent>, userProfile: UserProfile?): ProfileStats {
        val watchedEpisodesMap = userProfile?.watchedEpisodes ?: emptyMap()
        var totalMinutes = 0
        var totalEpisodes = 0
        watchedShows.forEach { show ->
            val episodesForShow = watchedEpisodesMap[show.id.toString()]
            // If the show has an entry in watchedEpisodes, use that count (even if 0).
            // Only fall back to the season-based estimate when there is no entry at all.
            val effectiveEpCount = when {
                episodesForShow != null && episodesForShow.isNotEmpty() -> episodesForShow.size
                episodesForShow != null -> 0
                else -> (show.numberOfSeasons ?: 1) * 10
            }
            val avgRuntime = show.episodeRunTime?.firstOrNull()?.takeIf { it > 0 } ?: 45
            totalMinutes += effectiveEpCount * avgRuntime
            totalEpisodes += effectiveEpCount
        }
        val totalHours = totalMinutes / 60

        val topGenreId = userProfile?.genreScores?.maxByOrNull { it.value }?.key
        val topGenreName = topGenreId?.let { GenreMapper.getGenreName(it) } ?: "Ninguno"

        val topActorId = userProfile?.preferredActors?.maxByOrNull { it.value }?.key

        val sortedGenres = userProfile?.genreScores
            ?.filter { it.value > 0 }
            ?.entries
            ?.sortedByDescending { it.value }
            ?: emptyList()
        val maxScore = sortedGenres.firstOrNull()?.value?.coerceAtLeast(1f) ?: 1f
        val topGenresList = sortedGenres.take(5).map {
            Pair(GenreMapper.getGenreName(it.key), it.value / maxScore)
        }

        val ratingsValues = userProfile?.ratings?.values ?: emptyList()
        val avgRating = if (ratingsValues.isNotEmpty()) ratingsValues.average().toFloat() else 0f
        val likedCount = userProfile?.likedMediaIds?.size ?: 0
        val dislikedCount = userProfile?.dislikedMediaIds?.size ?: 0
        val likeRate = if (likedCount + dislikedCount > 0)
            likedCount.toFloat() / (likedCount + dislikedCount)
        else 0f

        return ProfileStats(
            totalWatchedHours = totalHours,
            watchedCount = watchedShows.size,
            totalEpisodes = totalEpisodes,
            topGenre = topGenreName,
            favoriteActorId = topActorId,
            topGenres = topGenresList,
            likedCount = likedCount,
            essentialCount = userProfile?.essentialMediaIds?.size ?: 0,
            ratingsCount = ratingsValues.size,
            avgRating = avgRating,
            likeRate = likeRate
        )
    }

    data class ProfileStats(
        val totalWatchedHours: Int,
        val watchedCount: Int,
        val totalEpisodes: Int = 0,
        val topGenre: String = "N/A",
        val favoriteActorId: String? = null,
        val topGenres: List<Pair<String, Float>> = emptyList(),
        // Extended stats for dashboard
        val likedCount: Int = 0,
        val essentialCount: Int = 0,
        val ratingsCount: Int = 0,
        val avgRating: Float = 0f,
        val likeRate: Float = 0f   // fracción de interacciones positivas sobre el total valorado
    )
}
