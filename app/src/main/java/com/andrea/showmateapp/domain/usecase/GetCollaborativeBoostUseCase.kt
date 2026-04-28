package com.andrea.showmateapp.domain.usecase

import com.andrea.showmateapp.data.model.UserProfile
import com.andrea.showmateapp.domain.repository.IUserRepository
import com.andrea.showmateapp.util.ContentEmbeddingEngine
import javax.inject.Inject
import kotlinx.coroutines.CancellationException
import timber.log.Timber

class GetCollaborativeBoostUseCase @Inject constructor(
    private val userRepository: IUserRepository
) {
    companion object {
        // Umbral mínimo de similitud coseno: por debajo de 0.15 el vecino aporta más ruido que señal
        private const val MIN_SIMILARITY = 0.15f
        // El boost máximo (1.2) es intencionalmente conservador para no sobreponderar la señal social
        private const val MAX_BOOST = 1.20f
        private const val MAX_SIMILAR_USERS = 30L
        private const val MAX_SHOWS_PER_RESULT = 50

        // Jerarquía de señales implícitas: "esencial" > "like" > "visto"; el rating explícito las sobreescribe
        private const val SIGNAL_ESSENTIAL = 1.00f
        private const val SIGNAL_LIKED = 0.80f
        private const val SIGNAL_WATCHED = 0.40f
    }

    suspend fun execute(myProfile: UserProfile): Map<Int, Float> {
        return try {
            val similarUsers = userRepository.getSimilarUsers(limit = MAX_SIMILAR_USERS)
            if (similarUsers.isEmpty()) return emptyMap()

            val myRatingVec = buildRatingVector(myProfile)
            val myLiked = myProfile.likedMediaIds.toHashSet()
            val myEssential = myProfile.essentialMediaIds.toHashSet()
            val myWatched = (myRatingVec.keys - myLiked - myEssential).toHashSet()

            val neighbors: List<Pair<UserProfile, Float>> = similarUsers
                .mapNotNull { user ->
                    val userVec = buildRatingVector(user)
                    val sim = ContentEmbeddingEngine.cosineSimilarityRatings(myRatingVec, userVec)
                    if (sim >= MIN_SIMILARITY) user to sim else null
                }
                .sortedByDescending { it.second }

            if (neighbors.isEmpty()) return emptyMap()

            val alreadySeen = myProfile.dislikedMediaIds.toHashSet() +
                myLiked + myEssential + myWatched

            val showAccum = mutableMapOf<Int, Pair<Float, Float>>()

            // Media ponderada por similitud (weighted sum / similarity sum): equivale al filtro colaborativo
            // de usuario a usuario sin necesitar Pearson completo, ya que los vectores están normalizados
            for ((neighbor, similarity) in neighbors) {
                val neighborVec = buildRatingVector(neighbor)
                for ((showId, rating) in neighborVec) {
                    if (showId in alreadySeen || rating <= 0f) continue
                    val (wSum, sSum) = showAccum.getOrDefault(showId, 0f to 0f)
                    showAccum[showId] = (wSum + similarity * rating) to (sSum + similarity)
                }
            }

            if (showAccum.isEmpty()) return emptyMap()

            showAccum
                .mapValues { (_, pair) ->
                    val (weightedSum, weightSum) = pair
                    if (weightSum == 0f) 0f else (weightedSum / weightSum * MAX_BOOST).coerceIn(0f, MAX_BOOST)
                }
                .entries
                .sortedByDescending { it.value }
                .take(MAX_SHOWS_PER_RESULT)
                .associate { it.key to it.value }
        } catch (e: Exception) {
            if (e is CancellationException) throw e
            Timber.e(e, "Error in collaborative boost")
            emptyMap()
        }
    }

    internal fun buildRatingVector(profile: UserProfile): Map<Int, Float> {
        val ratings = mutableMapOf<Int, Float>()

        profile.watchedEpisodes.keys.forEach { idStr ->
            idStr.toIntOrNull()?.let { ratings[it] = SIGNAL_WATCHED }
        }

        profile.ratings.forEach { (idStr, rating) ->
            idStr.toIntOrNull()?.let { ratings[it] = (rating / 10f).coerceIn(0f, 1f) }
        }

        profile.likedMediaIds.forEach { id ->
            ratings[id] = maxOf(ratings.getOrDefault(id, 0f), SIGNAL_LIKED)
        }
        profile.essentialMediaIds.forEach { id ->
            ratings[id] = SIGNAL_ESSENTIAL
        }

        profile.dislikedMediaIds.forEach { ratings.remove(it) }

        return ratings
    }
}
