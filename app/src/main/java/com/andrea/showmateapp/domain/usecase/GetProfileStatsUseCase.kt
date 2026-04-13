package com.andrea.showmateapp.domain.usecase

import com.andrea.showmateapp.data.model.UserProfile
import com.andrea.showmateapp.data.network.MediaContent
import com.andrea.showmateapp.domain.repository.IInteractionRepository
import com.andrea.showmateapp.domain.repository.IUserRepository
import com.andrea.showmateapp.util.GenreMapper
import javax.inject.Inject
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map

class GetProfileStatsUseCase @Inject constructor(
    private val userRepo: IUserRepository,
    private val interactionRepo: IInteractionRepository
) {
    fun observeStats() = combine(userRepo.getUserProfileFlow(), interactionRepo.getWatchedShowsFlow()) { profile, watched ->
        computeStats(watched.size, profile)
    }.distinctUntilChanged()

    fun execute(watched: List<MediaContent>, profile: UserProfile?) = computeStats(watched.size, profile)

    private fun computeStats(watchedCount: Int, profile: UserProfile?): ProfileStats {
        val p = profile ?: return ProfileStats()
        val totalEpisodes = p.watchedEpisodes.values.sumOf { it.size }
        val ratings = p.ratings.values
        val genres = p.genreScores.filter { it.value > 0 }.entries.sortedByDescending { it.value }.take(5)
        val maxScore = genres.firstOrNull()?.value?.coerceAtLeast(1f) ?: 1f
        val topGenres = genres.map { GenreMapper.getGenreName(it.key) to (it.value / maxScore) }
        val liked = p.likedMediaIds.size
        val disliked = p.dislikedMediaIds.size

        return ProfileStats(
            totalHours = (totalEpisodes * 45) / 60,
            watchedCount = watchedCount,
            totalEpisodes = totalEpisodes,
            topGenre = topGenres.firstOrNull()?.first ?: "Ninguno",
            favoriteActorId = p.preferredActors.maxByOrNull { it.value }?.key,
            topGenres = topGenres,
            likedCount = liked,
            essentialCount = p.essentialMediaIds.size,
            ratingsCount = ratings.size,
            avgRating = if (ratings.isEmpty()) 0f else ratings.average().toFloat(),
            likeRate = if (liked + disliked > 0) liked.toFloat() / (liked + disliked) else 0f
        )
    }

    data class ProfileStats(
        val totalHours: Int = 0,
        val watchedCount: Int = 0,
        val totalEpisodes: Int = 0,
        val topGenre: String = "N/A",
        val favoriteActorId: String? = null,
        val topGenres: List<Pair<String, Float>> = emptyList(),
        val likedCount: Int = 0,
        val essentialCount: Int = 0,
        val ratingsCount: Int = 0,
        val avgRating: Float = 0f,
        val likeRate: Float = 0f
    )
}
