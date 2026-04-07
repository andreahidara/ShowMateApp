package com.andrea.showmateapp.util

import com.andrea.showmateapp.data.model.UserProfile
import java.time.LocalDate
import java.time.format.DateTimeParseException
import java.time.temporal.ChronoUnit

object ExplorationEngine {

    const val MIN_EXPLORATION = 0.10f
    const val MAX_EXPLORATION = 0.85f

    private const val W_CONCENTRATION = 0.40f
    private const val W_STAGNATION    = 0.35f
    private const val W_DISCOVERY     = 0.25f
    private const val STAGNATION_DAYS_FULL = 30f
    private const val RECENT_WINDOW = 30

    fun calculateFactor(profile: UserProfile): Float {
        val concentration = genreConcentration(profile.genreScores)
        val stagnation    = stagnationFactor(profile.viewingHistory)
        val discovery     = 1f - discoveryRate(profile.viewingHistory)
        val raw = W_CONCENTRATION * concentration +
                  W_STAGNATION    * stagnation    +
                  W_DISCOVERY     * discovery
        return raw.coerceIn(MIN_EXPLORATION, MAX_EXPLORATION)
    }

    fun unexploredGenres(profile: UserProfile, allGenreIds: Set<Int>): Set<Int> {
        val explored = profile.genreScores
            .filter { it.value > 2f }
            .keys
            .mapNotNull { it.toIntOrNull() }
            .toSet()
        return allGenreIds - explored
    }

    private fun genreConcentration(genreScores: Map<String, Float>): Float {
        val positives = genreScores.values.filter { it > 0f }
        if (positives.size < 2) return 0.50f
        val total    = positives.sum()
        val maxScore = positives.max()
        return (maxScore / total).coerceIn(0f, 1f)
    }

    private fun stagnationFactor(viewingHistory: List<String>): Float {
        if (viewingHistory.isEmpty()) return 0.60f
        val lastEntry = viewingHistory.lastOrNull() ?: return 0.60f
        val dateStr   = lastEntry.split(":").getOrNull(0) ?: return 0.50f
        return try {
            val lastDate  = LocalDate.parse(dateStr)
            val daysSince = ChronoUnit.DAYS.between(lastDate, LocalDate.now()).toFloat()
            (daysSince / STAGNATION_DAYS_FULL).coerceIn(0f, 1f)
        } catch (e: DateTimeParseException) { 0.50f }
    }

    private fun discoveryRate(viewingHistory: List<String>): Float {
        if (viewingHistory.isEmpty()) return 0.50f
        val recent      = viewingHistory.takeLast(RECENT_WINDOW)
        val showIds     = recent.mapNotNull { it.split(":").getOrNull(1)?.takeIf { id -> id.isNotBlank() } }
        if (showIds.isEmpty()) return 0.50f
        val uniqueShows = showIds.distinct().size
        return (uniqueShows.toFloat() / showIds.size.toFloat()).coerceIn(0f, 1f)
    }
}
