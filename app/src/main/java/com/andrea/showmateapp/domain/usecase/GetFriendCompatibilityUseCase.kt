package com.andrea.showmateapp.domain.usecase

import com.andrea.showmateapp.domain.repository.ISocialRepository
import com.andrea.showmateapp.domain.repository.IUserRepository
import com.andrea.showmateapp.util.ContentEmbeddingEngine
import com.andrea.showmateapp.util.GenreMapper
import javax.inject.Inject

class GetFriendCompatibilityUseCase @Inject constructor(
    private val userRepository: IUserRepository,
    private val socialRepository: ISocialRepository
) {
    data class CompatibilityResult(
        val overallScore: Int,
        val genreMatchPct: Int,
        val ratingCorrelationPct: Int,
        val sharedFavoriteIds: List<Int>,
        val topSharedGenres: List<String>
    )

    suspend fun execute(friendUid: String): CompatibilityResult {
        val me = userRepository.getUserProfile() ?: return CompatibilityResult(0, 0, 0, emptyList(), emptyList())
        val friend = socialRepository.getFriendProfile(friendUid)
            ?: return CompatibilityResult(0, 0, 0, emptyList(), emptyList())

        val genreJaccard = GenreMapper.jaccardSimilarity(me.genreScores, friend.genreScores)

        val myRatings = me.ratings.mapKeys { it.key.toIntOrNull() ?: 0 }.filterKeys { it > 0 }
        val friendRatings = friend.ratings.mapKeys { it.key.toIntOrNull() ?: 0 }.filterKeys { it > 0 }
        val ratingCorr = ContentEmbeddingEngine
            .cosineSimilarityRatings(myRatings.mapValues { it.value }, friendRatings.mapValues { it.value })
            .coerceIn(0f, 1f)

        val myFavorites = (me.likedMediaIds + me.essentialMediaIds).toSet()
        val friendFavorites = (friend.likedMediaIds + friend.essentialMediaIds).toSet()
        val shared = (myFavorites intersect friendFavorites).toList()

        val overall = ((genreJaccard * 0.5f + ratingCorr * 0.5f) * 100)
            .toInt().coerceIn(0, 100)

        val topSharedGenres = me.genreScores.keys
            .filter { k -> (me.genreScores[k] ?: 0f) > 3f && (friend.genreScores[k] ?: 0f) > 3f }
            .sortedByDescending { k -> (me.genreScores[k] ?: 0f) + (friend.genreScores[k] ?: 0f) }
            .take(3)
            .mapNotNull { k -> k.toIntOrNull()?.let { GenreMapper.getGenreName(k) } }

        return CompatibilityResult(
            overallScore = overall,
            genreMatchPct = (genreJaccard * 100).toInt(),
            ratingCorrelationPct = (ratingCorr * 100).toInt(),
            sharedFavoriteIds = shared,
            topSharedGenres = topSharedGenres
        )
    }
}
