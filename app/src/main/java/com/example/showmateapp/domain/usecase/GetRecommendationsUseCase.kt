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
    // A. Definición de Variables y Pesos
    private val W_PERSONAL = 0.7f // 70% peso a la afinidad personal
    private val W_GLOBAL = 0.3f   // 30% peso a la nota global de TMDB
    
    // Pesos internos de la Afinidad Personal
    private val WG = 0.5f // Géneros
    private val WK = 0.3f // Keywords
    private val WA = 0.2f // Reparto

    suspend fun execute(): List<MediaContent> {
        return try {
            val userProfile = userRepository.getUserProfile()

            // Si el usuario no tiene datos, devolvemos las populares por defecto
            if (userProfile == null || userProfile.genreScores.isEmpty()) {
                return showRepository.getPopularShows()
            }

            val candidates = showRepository.getDetailedRecommendations()

            // Filtrado Duro (Excluir disliked y vistas)
            val watchedIds = userRepository.getWatchedShows().map { it.id }.toSet()
            val excludedIds = userProfile.dislikedMediaIds + watchedIds

            val filteredCandidates = candidates.filter { it.id !in excludedIds }

            // Calcular y ordenar
            filteredCandidates.map { show ->
                val affinityScore = calculatePersonalAffinity(show, userProfile)
                val globalScore = calculateBayesianRating(show)
                
                // Puntuación Final = Combinación de Afinidad Personal y Calidad Global
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
        // Score Géneros: Suma de los pesos dinámicos de cada género de la serie
        var genreScore = 0f
        show.safeGenreIds.forEach { id ->
            val weight = user.genreScores[id.toString()] ?: 0f
            genreScore += weight
        }

        // Score Keywords: Suma de pesos de keywords
        var keywordScore = 0f
        show.keywords?.results?.forEach { kw ->
            val weight = user.preferredKeywords[kw.name] ?: 0f
            keywordScore += weight
        }

        // Score Actores: Suma de pesos de actores
        var actorScore = 0f
        show.credits?.cast?.forEach { actor ->
            val weight = user.preferredActors[actor.id.toString()] ?: 0f
            actorScore += weight
        }

        // Normalizamos sumando todos los Scores multiplicados por su importancia
        val rawAffinity = (WG * genreScore) + (WK * keywordScore) + (WA * actorScore)

        // Aplicamos la normalización logarítmica de popularidad para que las series desconocidas destaquen si tienen mucho "match"
        val n = if (show.popularity > 0) log10(show.popularity.toDouble()).toFloat() else 1f

        // Retornamos la afinidad ajustada. (Maximizada a 10.0 para alinearla con la escala de nota TMDB)
        return (rawAffinity / n.coerceAtLeast(0.1f)).coerceIn(0f, 10f)
    }

    private fun calculateBayesianRating(show: MediaContent): Float {
        // Nota TMDB viene en escala de 10.
        // Simularemos vote_average a 7.0 y vote_count a la popularidad multiplicada por 10 si no tenemos esos datos en MediaContent
        val voteAverage = 7.0f // Idealmente vendría de `show.voteAverage` 
        val voteCount = show.popularity * 10 // Idealmente vendría de `show.voteCount`

        val C = 6.5f // Media global de todas las series
        val m = 100f // Votos mínimos requeridos para ser listada

        // Fórmula de Bayesian Average: (v / (v+m)) * R + (m / (v+m)) * C
        return ((voteCount / (voteCount + m)) * voteAverage) + ((m / (voteCount + m)) * C)
    }
}
