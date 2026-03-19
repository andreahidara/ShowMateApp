package com.example.showmateapp.domain.usecase

import com.example.showmateapp.data.model.UserProfile
import com.example.showmateapp.data.network.MediaContent
import com.example.showmateapp.util.GenreMapper
import javax.inject.Inject

class GetProfileStatsUseCase @Inject constructor() {

    fun execute(watchedShows: List<MediaContent>, userProfile: UserProfile?): ProfileStats {
        var totalHours = 0
        watchedShows.forEach { show ->
            // Estimación: 10 episodios por temporada, 45 min por episodio
            val seasons = show.numberOfSeasons ?: 1
            totalHours += (seasons * 10 * 45) / 60
        }

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
