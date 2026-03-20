package com.example.showmateapp.domain.usecase

import com.example.showmateapp.data.model.UserProfile
import com.example.showmateapp.data.network.MediaContent
import com.example.showmateapp.data.repository.UserRepository
import com.example.showmateapp.data.repository.ShowRepository
import com.example.showmateapp.util.Resource
import javax.inject.Inject
import kotlin.math.exp
import kotlin.math.log10

class GetRecommendationsUseCase @Inject constructor(
    private val userRepository: UserRepository,
    private val showRepository: ShowRepository
) {
    companion object {
        // Final score = 70 % personal affinity + 30 % Bayesian global rating
        private const val W_PERSONAL = 0.7f
        private const val W_GLOBAL   = 0.3f

        // Personal affinity sub-weights (must sum to 1.0)
        private const val WG = 0.5f   // genres
        private const val WK = 0.3f   // keywords
        private const val WA = 0.2f   // actors

        // Bayesian rating parameters (TMDB-style)
        private const val C  = 6.5f   // prior mean rating across the platform
        private const val M  = 150f   // minimum votes needed to trust the raw average

        // Time-decay: λ = ln(2)/90 ≈ 0.0077 so that a score halves after ~90 days
        private const val DECAY_LAMBDA = 0.0077f

        // Diversity: no single genre may occupy more than this fraction of the results
        private const val MAX_GENRE_FRACTION = 0.35f
    }

    suspend fun execute(): List<MediaContent> {
        return try {
            val userProfile = userRepository.getUserProfile()

            if (userProfile == null || userProfile.genreScores.isEmpty()) {
                val popular = showRepository.getPopularShows()
                return if (popular is Resource.Success) popular.data else emptyList()
            }

            val genres = userProfile.genreScores
                .filter { it.value > 0 }
                .entries
                .sortedByDescending { it.value }
                .take(3)
                .map { it.key }
                .joinToString(",")
                .ifEmpty { null }

            val candidates = showRepository.getDetailedRecommendations(genres)

            val watchedIds   = userRepository.getWatchedMediaIds()
            val excludedIds  = userProfile.dislikedMediaIds.toSet() + watchedIds

            val now = System.currentTimeMillis()
            val decayedProfile = applyTimeDecay(userProfile, now)

            val scored = candidates
                .filter { it.id !in excludedIds }
                .map { scoreShow(it, decayedProfile) }
                .sortedByDescending { it.affinityScore }

            applyDiversityFilter(scored)

        } catch (e: Exception) {
            android.util.Log.e("GetRecommendations", "Error executing recommendations", e)
            val popular = showRepository.getPopularShows()
            if (popular is Resource.Success) popular.data else emptyList()
        }
    }

    suspend fun scoreShows(shows: List<MediaContent>): List<MediaContent> {
        return try {
            val userProfile = userRepository.getUserProfile()
            if (userProfile == null || userProfile.genreScores.isEmpty()) return shows
            val now = System.currentTimeMillis()
            val decayedProfile = applyTimeDecay(userProfile, now)
            shows.map { scoreShow(it, decayedProfile) }.sortedByDescending { it.affinityScore }
        } catch (e: Exception) {
            android.util.Log.e("GetRecommendations", "Error scoring shows", e)
            shows
        }
    }

    // ── Time-decay ────────────────────────────────────────────────────────────

    /**
     * Returns a copy of [profile] with scores multiplied by e^(-λ * daysSinceUpdate).
     * Scores from today are unchanged; a score from 90 days ago is halved.
     */
    private fun applyTimeDecay(profile: UserProfile, now: Long): UserProfile {
        fun decayMap(scores: Map<String, Float>, dates: Map<String, Long>): Map<String, Float> {
            return scores.mapValues { (key, score) ->
                val lastUpdated = dates[key] ?: now
                val daysSince   = ((now - lastUpdated) / 86_400_000L).toFloat().coerceAtLeast(0f)
                score * exp(-DECAY_LAMBDA * daysSince)
            }
        }
        return profile.copy(
            genreScores       = decayMap(profile.genreScores,       profile.genreScoreDates),
            preferredKeywords = decayMap(profile.preferredKeywords, profile.keywordScoreDates),
            preferredActors   = decayMap(profile.preferredActors,   profile.actorScoreDates)
        )
    }

    // ── Diversity filter ──────────────────────────────────────────────────────

    /**
     * Limits any single genre to [MAX_GENRE_FRACTION] of the final list,
     * keeping the top-scoring shows overall.
     */
    private fun applyDiversityFilter(scored: List<MediaContent>): List<MediaContent> {
        val maxPerGenre = (scored.size * MAX_GENRE_FRACTION).toInt().coerceAtLeast(3)
        val genreCount  = mutableMapOf<Int, Int>()
        val result      = mutableListOf<MediaContent>()

        for (show in scored) {
            val dominantGenre = show.safeGenreIds.firstOrNull() ?: -1
            val count         = genreCount.getOrDefault(dominantGenre, 0)
            if (count < maxPerGenre) {
                result.add(show)
                genreCount[dominantGenre] = count + 1
            }
        }
        // Fill remaining slots with the shows that were skipped (diversity overflow)
        if (result.size < scored.size) {
            val included = result.map { it.id }.toSet()
            result.addAll(scored.filter { it.id !in included })
        }
        return result
    }

    // ── Scoring ───────────────────────────────────────────────────────────────

    private fun scoreShow(show: MediaContent, user: UserProfile): MediaContent {
        val personal = calculatePersonalAffinity(show, user)
        val global   = calculateBayesianRating(show)
        val completeness = calculateCompletenessBoost(show)
        return show.copy(
            affinityScore = ((personal * W_PERSONAL) + (global * W_GLOBAL) + completeness)
                .coerceIn(0f, 11f)
        )
    }

    /**
     * Computes a personal affinity score in [0, 10] by combining three min-max normalised
     * signals (genres, keywords, actors) weighted WG/WK/WA.
     *
     * Each signal is normalised against the user's own maximum score to prevent score
     * accumulation over time (a user who has watched many dramas shouldn't push drama to ∞).
     *
     * Raw affinity ∈ [-1, 1] is remapped to [0, 10] via (raw + 1) / 2 * 10,
     * then a log-scaled popularity boost (≤ 1.5 pts) is added to break ties.
     */
    private fun calculatePersonalAffinity(show: MediaContent, user: UserProfile): Float {
        val maxGenreWeight   = user.genreScores.values.maxOrNull()?.coerceAtLeast(1f) ?: 1f
        val maxKeywordWeight = user.preferredKeywords.values.maxOrNull()?.coerceAtLeast(1f) ?: 1f
        val maxActorWeight   = user.preferredActors.values.maxOrNull()?.coerceAtLeast(1f) ?: 1f

        var genreScore = 0f
        show.safeGenreIds.forEach { id ->
            genreScore += ((user.genreScores[id.toString()] ?: 0f) / maxGenreWeight).coerceIn(-1f, 1f)
        }
        val normalizedGenreScore = if (show.safeGenreIds.isNotEmpty())
            (genreScore / show.safeGenreIds.size).coerceIn(-1f, 1f) else 0f

        val keywords = show.keywords?.results ?: emptyList()
        var keywordScore = 0f
        keywords.forEach { kw ->
            keywordScore += ((user.preferredKeywords[kw.name] ?: 0f) / maxKeywordWeight).coerceIn(-1f, 1f)
        }
        // Cap keyword denominator at 5 to avoid diluting the score on keyword-heavy shows
        val normalizedKeywordScore = if (keywords.isNotEmpty())
            (keywordScore / minOf(5f, keywords.size.toFloat())).coerceIn(-1f, 1f) else 0f

        val actors = show.credits?.cast ?: emptyList()
        var actorScore = 0f
        actors.forEach { actor ->
            actorScore += ((user.preferredActors[actor.id.toString()] ?: 0f) / maxActorWeight).coerceIn(-1f, 1f)
        }
        val normalizedActorScore = if (actors.isNotEmpty())
            (actorScore / minOf(5f, actors.size.toFloat())).coerceIn(-1f, 1f) else 0f

        // Weighted combination → [-1, 1], then mapped to [0, 10]
        val rawAffinity = (WG * normalizedGenreScore) + (WK * normalizedKeywordScore) + (WA * normalizedActorScore)
        val baseScore   = ((rawAffinity + 1f) / 2f) * 10f
        val popBoost    = if (show.popularity > 1f)
            (log10(show.popularity.toDouble()).toFloat() * 0.5f).coerceIn(0f, 1.5f) else 0f

        return (baseScore + popBoost).coerceIn(0f, 10f)
    }

    /**
     * Bayesian average: B = (v·R + M·C) / (v + M)
     *   v = vote count, R = raw average, M = confidence threshold, C = prior mean.
     * Shows with few votes are pulled towards the platform average C,
     * preventing obscure high-rated shows from dominating results.
     */
    private fun calculateBayesianRating(show: MediaContent): Float {
        val v = show.voteCount.toFloat()
        val R = show.voteAverage
        return if (v + M > 0) ((v / (v + M)) * R) + ((M / (v + M)) * C) else C
    }

    /**
     * +0.5 for ended shows (user can binge without waiting), +0.3 for short series (≤3 seasons).
     * Never penalises — only adds a small bonus.
     */
    private fun calculateCompletenessBoost(show: MediaContent): Float {
        var boost = 0f
        if (show.status == "Ended" || show.status == "Canceled") boost += 0.5f
        val seasons = show.numberOfSeasons ?: Int.MAX_VALUE
        if (seasons in 1..3) boost += 0.3f
        return boost
    }
}
