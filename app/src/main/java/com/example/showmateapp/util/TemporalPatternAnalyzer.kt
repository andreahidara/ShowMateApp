package com.example.showmateapp.util

import java.time.DayOfWeek
import java.time.LocalDate
import java.time.format.DateTimeParseException

/**
 * Analiza el historial de visionado del usuario para detectar patrones temporales.
 * Formato de viewingHistory: "YYYY-MM-DD:showId:episodeCount"
 */
object TemporalPatternAnalyzer {

    data class TemporalPattern(
        val weekendRatio: Float,   // 0..1, fraction of views on weekends
        val avgEpisodesPerSession: Float,
        val prefersShortOnWeekdays: Boolean
    )

    fun analyze(viewingHistory: List<String>): TemporalPattern {
        if (viewingHistory.isEmpty()) return TemporalPattern(0.5f, 2f, false)

        var weekendCount = 0
        var weekdayCount = 0
        var totalEpisodes = 0

        for (entry in viewingHistory) {
            val parts = entry.split(":")
            if (parts.size < 3) continue
            val date = try { LocalDate.parse(parts[0]) } catch (e: DateTimeParseException) { continue }
            val episodeCount = parts[2].toIntOrNull() ?: 1
            totalEpisodes += episodeCount
            if (date.dayOfWeek == DayOfWeek.SATURDAY || date.dayOfWeek == DayOfWeek.SUNDAY) weekendCount++
            else weekdayCount++
        }

        val total = weekendCount + weekdayCount
        val weekendRatio = if (total > 0) weekendCount.toFloat() / total else 0.5f
        val avgEpisodes = if (total > 0) totalEpisodes.toFloat() / total else 2f

        // Prefiere episodios cortos entre semana si ve mayormente en fin de semana (= menos tiempo entre semana)
        val prefersShortOnWeekdays = weekendRatio > 0.65f

        return TemporalPattern(weekendRatio, avgEpisodes, prefersShortOnWeekdays)
    }

    /** Devuelve true si hoy es día laborable. Calcular una sola vez por pasada de scoring. */
    fun isWeekday(): Boolean {
        val today = LocalDate.now().dayOfWeek
        return today != DayOfWeek.SATURDAY && today != DayOfWeek.SUNDAY
    }

    /** Devuelve un multiplicador de impulso [0.8, 1.0] para una serie según el contexto del día.
     *  [isWeekday] debe calcularse una sola vez antes del bucle de scoring y pasarse aquí. */
    fun getContextBoost(pattern: TemporalPattern, episodeRuntime: Int?, isWeekday: Boolean): Float {
        if (!isWeekday || !pattern.prefersShortOnWeekdays) return 1.0f
        // Entre semana, si el usuario suele tener menos tiempo, favorece episodios cortos
        val runtime = episodeRuntime ?: 45
        return when {
            runtime <= 30 -> 1.0f
            runtime <= 50 -> 0.95f
            else          -> 0.88f
        }
    }
}
