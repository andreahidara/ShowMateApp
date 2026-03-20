package com.example.showmateapp.domain.usecase

import com.example.showmateapp.data.model.UserProfile
import com.example.showmateapp.data.network.MediaContent
import com.example.showmateapp.util.GenreMapper
import javax.inject.Inject

class GetProfileStatsUseCase @Inject constructor() {

    fun execute(watchedShows: List<MediaContent>, userProfile: UserProfile?): ProfileStats {
        val watchedEpisodesMap = userProfile?.watchedEpisodes ?: emptyMap()
        var totalMinutes = 0
        watchedShows.forEach { show ->
            val watchedEpCount = watchedEpisodesMap[show.id.toString()]?.size ?: 0
            if (watchedEpCount > 0) {
                // Use actual episode runtime if available, otherwise fall back to 45 min
                val avgRuntime = show.episodeRunTime?.firstOrNull()?.takeIf { it > 0 } ?: 45
                totalMinutes += watchedEpCount * avgRuntime
            }
        }
        val totalHours = totalMinutes / 60

        // Obtener género favorito basado en puntuaciones del perfil
        val topGenreId = userProfile?.genreScores?.maxByOrNull { it.value }?.key
        val topGenreName = topGenreId?.let { GenreMapper.getGenreName(it) } ?: "Ninguno"

        val topActorId = userProfile?.preferredActors?.maxByOrNull { it.value }?.key

        val sortedGenres = userProfile?.genreScores?.entries?.sortedByDescending { it.value } ?: emptyList()
        val totalScore = sortedGenres.sumOf { it.value.toDouble() }.toFloat()
        val topGenresList = if (totalScore > 0f) {
            sortedGenres.take(3).map {
                Pair(GenreMapper.getGenreName(it.key), it.value / totalScore)
            }
        } else emptyList()
        
        return ProfileStats(
            totalWatchedHours = totalHours,
            watchedCount = watchedShows.size,
            topGenre = topGenreName,
            favoriteActorId = topActorId,
            topGenres = topGenresList
        )
    }

    data class ProfileStats(
        val totalWatchedHours: Int,
        val watchedCount: Int,
        val topGenre: String = "N/A",
        val favoriteActorId: String? = null,
        val topGenres: List<Pair<String, Float>> = emptyList()
    )
}
