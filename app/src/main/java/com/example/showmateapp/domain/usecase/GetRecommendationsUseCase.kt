package com.example.showmateapp.domain.usecase

import com.example.showmateapp.data.model.UserProfile
import com.example.showmateapp.data.network.TvShow
import com.example.showmateapp.data.repository.FirestoreRepository
import com.example.showmateapp.data.repository.TvShowRepository
import javax.inject.Inject
import kotlin.math.log10

class GetRecommendationsUseCase @Inject constructor(
    private val firestoreRepository: FirestoreRepository,
    private val tvShowRepository: TvShowRepository
) {
    // A. Definición de Variables y Pesos según tu diseño
    private val WG = 0.5f // Géneros
    private val WK = 0.3f // Keywords
    private val WA = 0.2f // Reparto

    suspend fun execute(): List<TvShow> {
        return try {
            // 1. Obtenemos el perfil completo del usuario (Gu, Ku, Au)
            val userProfile = firestoreRepository.getUserProfile()

            // Si el usuario no tiene datos, devolvemos las populares por defecto
            if (userProfile == null || userProfile.genreScores.isEmpty()) {
                return tvShowRepository.getPopularTvShows()
            }

            // 2. Traemos las series candidatas con sus metadatos (Gx, Kx, Ax)
            val candidates = tvShowRepository.getDetailedRecommendations()

            // 3. Aplicamos el Modelo Matemático de Afinidad S(x)
            candidates.map { show ->
                val score = calculateAffinityScore(show, userProfile)
                show.affinityScore = score // Asegúrate de que 'affinityScore' sea 'var' en TvShow.kt
                show
            }.sortedByDescending { it.affinityScore } // Ranking de mayor a menor

        } catch (e: Exception) {
            tvShowRepository.getPopularTvShows()
        }
    }

    private fun calculateAffinityScore(show: TvShow, user: UserProfile): Float {
        // Coincidencia de Géneros (Gx ∩ Gu)
        val genreMatch = show.safeGenreIds.count { id ->
            user.genreScores[id.toString()]?.let { it > 0 } ?: false
        }

        // Coincidencias de Keywords y Actores
        val keywordMatch = show.keywords?.results?.count { it.name in user.preferredKeywords } ?: 0
        val actorMatch = show.credits?.cast?.count { it.id in user.preferredActors } ?: 0

        // Fórmula S(x) = (Σ W * Match) / N
        val numerator = (WG * genreMatch) + (WK * keywordMatch) + (WA * actorMatch)

        // Normalización N basada en popularidad (logarítmica para equilibrio)
        val n = if (show.popularity > 0) log10(show.popularity.toDouble()).toFloat() else 1f

        return numerator / n.coerceAtLeast(0.1f)
    }
}
