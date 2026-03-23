package com.example.showmateapp.domain.usecase

import com.example.showmateapp.data.model.UserProfile
import com.example.showmateapp.data.repository.UserRepository
import com.example.showmateapp.util.GenreMapper
import javax.inject.Inject

/**
 * Filtrado colaborativo ligero: encuentra usuarios con preferencias de género similares
 * y devuelve un mapa de showId -> puntuación de boost colaborativo.
 *
 * Estrategia:
 *  1. Obtiene los géneros principales del usuario actual
 *  2. Consulta una muestra pequeña de otros usuarios en Firestore
 *  3. Filtra los que tienen similitud Jaccard de género > umbral
 *  4. Cuenta cuántos usuarios similares dieron like a cada serie
 *  5. Devuelve puntuaciones de boost normalizadas
 */
class GetCollaborativeBoostUseCase @Inject constructor(
    private val userRepository: UserRepository
) {
    companion object {
        private const val MIN_SIMILARITY = 0.25f
        private const val MAX_BOOST = 1.2f
    }

    suspend fun execute(myProfile: UserProfile): Map<Int, Float> {
        return try {
            val similarUsers = userRepository.getSimilarUsers(limit = 30)
            val likeCounts = mutableMapOf<Int, Int>()
            val myLiked = myProfile.likedMediaIds.toSet()

            for (user in similarUsers) {
                val similarity = calculateJaccard(myProfile, user)
                if (similarity < MIN_SIMILARITY) continue
                for (showId in user.likedMediaIds) {
                    if (showId !in myLiked) {
                        likeCounts[showId] = (likeCounts[showId] ?: 0) + 1
                    }
                }
            }

            if (likeCounts.isEmpty()) return emptyMap()

            val maxCount = likeCounts.values.maxOrNull()?.toFloat() ?: 1f
            likeCounts.mapValues { (_, count) ->
                (count.toFloat() / maxCount * MAX_BOOST).coerceIn(0f, MAX_BOOST)
            }
        } catch (e: Exception) {
            emptyMap()
        }
    }

    private fun calculateJaccard(a: UserProfile, b: UserProfile): Float =
        GenreMapper.jaccardSimilarity(a.genreScores, b.genreScores)
}
