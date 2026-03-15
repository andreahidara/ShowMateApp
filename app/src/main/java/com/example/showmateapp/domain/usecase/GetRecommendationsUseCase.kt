package com.example.showmateapp.domain.usecase

import com.example.showmateapp.data.model.UserProfile
import com.example.showmateapp.data.network.MediaContent
import com.example.showmateapp.data.repository.UserRepository
import com.example.showmateapp.data.repository.ShowRepository
import javax.inject.Inject
import kotlin.math.log10

class GetRecommendationsUseCase @Inject constructor(
    private val userRepository: UserRepository,
    private val showRepository: ShowRepository
) {
    private val W_PERSONAL = 0.7f
    private val W_GLOBAL = 0.3f
    
    private val WG = 0.5f
    private val WK = 0.3f
    private val WA = 0.2f

    suspend fun execute(): List<MediaContent> {
        return try {
            val userProfile = userRepository.getUserProfile()

            if (userProfile == null || userProfile.genreScores.isEmpty()) {
                return showRepository.getPopularShows()
            }

            val candidates = showRepository.getDetailedRecommendations()

            val watchedIds = userRepository.getWatchedShows().map { it.id }.toSet()
            val excludedIds = userProfile.dislikedMediaIds + watchedIds

            val filteredCandidates = candidates.filter { it.id !in excludedIds }

            filteredCandidates.map { show ->
                val affinityScore = calculatePersonalAffinity(show, userProfile)
                val globalScore = calculateBayesianRating(show)
                
                show.affinityScore = (affinityScore * W_PERSONAL) + (globalScore * W_GLOBAL)
                show
            }.sortedByDescending { it.affinityScore }

        } catch (e: Exception) {
            showRepository.getPopularShows()
        }
    }

    suspend fun scoreShows(shows: List<MediaContent>): List<MediaContent> {
        val userProfile = userRepository.getUserProfile()
        if (userProfile == null || userProfile.genreScores.isEmpty()) return shows

        return shows.map { show ->
            val affinityScore = calculatePersonalAffinity(show, userProfile)
            val globalScore = calculateBayesianRating(show)
            show.affinityScore = (affinityScore * W_PERSONAL) + (globalScore * W_GLOBAL)
            show
        }
    }

    private fun calculatePersonalAffinity(show: MediaContent, user: UserProfile): Float {
        var genreScore = 0f
        show.safeGenreIds.forEach { id ->
            val weight = user.genreScores[id.toString()] ?: 0f
            genreScore += weight
        }

        var keywordScore = 0f
        show.keywords?.results?.forEach { kw ->
            val weight = user.preferredKeywords[kw.name] ?: 0f
            keywordScore += weight
        }

        var actorScore = 0f
        show.credits?.cast?.forEach { actor ->
            val weight = user.preferredActors[actor.id.toString()] ?: 0f
            actorScore += weight
        }

        val rawAffinity = (WG * genreScore) + (WK * keywordScore) + (WA * actorScore)

        val n = if (show.popularity > 0) log10(show.popularity.toDouble()).toFloat() else 1f

        return (rawAffinity / n.coerceAtLeast(0.1f)).coerceIn(0f, 10f)
    }

    private fun calculateBayesianRating(show: MediaContent): Float {
        val voteAverage = 7.0f
        val voteCount = show.popularity * 10

        val C = 6.5f
        val m = 100f

        return ((voteCount / (voteCount + m)) * voteAverage) + ((m / (voteCount + m)) * C)
    }
}
