package com.andrea.showmateapp.domain.usecase

import com.andrea.showmateapp.data.model.UserProfile
import com.andrea.showmateapp.util.GenreMapper
import com.andrea.showmateapp.util.NarrativeStyleMapper
import javax.inject.Inject

class GetViewerPersonalityUseCase @Inject constructor() {

    data class PersonalityProfile(
        val label: String,
        val topGenres: List<Pair<String, Float>>,
        val topKeywords: List<String>,
        val topNarrativeStyles: List<Pair<String, Float>> = emptyList()
    )

    fun execute(profile: UserProfile): PersonalityProfile? {
        if (profile.genreScores.isEmpty()) return null
        val maxScore = profile.genreScores.values.filter { it > 0 }.maxOrNull() ?: return null

        // Se normaliza por el score máximo para obtener pesos relativos comparables entre usuarios
        val topGenres = profile.genreScores
            .filter { it.value > 0 }
            .entries
            .sortedByDescending { it.value }
            .take(5)
            .map { (id, score) ->
                GenreMapper.getGenreName(id) to (score / maxScore).coerceIn(0f, 1f)
            }

        val topKeywords = profile.preferredKeywords
            .filter { it.value > 0 }
            .entries
            .sortedByDescending { it.value }
            .take(3)
            .map { it.key.replaceFirstChar { c -> c.uppercaseChar() } }

        val nsMax = profile.narrativeStyleScores.values.maxOrNull() ?: 1f
        val topNarrativeStyles = profile.narrativeStyleScores
            .filter { it.value > 0 }
            .entries
            .sortedByDescending { it.value }
            .take(3)
            .map { (key, score) ->
                NarrativeStyleMapper.getStyleLabel(key) to (score / nsMax).coerceIn(0f, 1f)
            }

        return PersonalityProfile(
            label = buildLabel(topGenres.map { it.first }),
            topGenres = topGenres,
            topKeywords = topKeywords,
            topNarrativeStyles = topNarrativeStyles
        )
    }

    private fun buildLabel(genres: List<String>): String {
        // Los 8 tipos se derivan del género dominante: el orden de los `when` es la jerarquía de prioridad
        // (Crimen > Misterio > ... > Ecléctico) cuando varios géneros tienen puntuaciones similares
        val primary = when {
            genres.any { it.contains("Crimen") } -> "Analítico"
            genres.any { it.contains("Misterio") } -> "Intuitivo"
            genres.any { it.contains("Comedia") } -> "Optimista"
            genres.any { it.contains("Drama") } -> "Empático"
            genres.any { it.contains("Sci-Fi") || it.contains("Fantasía") } -> "Imaginativo"
            genres.any { it.contains("Acción") } -> "Apasionado"
            genres.any { it.contains("Documental") } -> "Curioso"
            else -> "Ecléctico"
        }
        val topGenre = genres.firstOrNull() ?: return primary
        return "$primary · Amante de $topGenre"
    }
}
