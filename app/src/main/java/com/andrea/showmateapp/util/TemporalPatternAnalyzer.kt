package com.andrea.showmateapp.util

import java.time.DayOfWeek
import java.time.LocalDate
import java.time.format.DateTimeParseException

object TemporalPatternAnalyzer {

    data class TemporalPattern(
        val weekendRatio: Float,
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
            val date = try {
                LocalDate.parse(parts[0])
            } catch (e: DateTimeParseException) {
                continue
            }
            val episodeCount = parts[2].toIntOrNull() ?: 1
            totalEpisodes += episodeCount
            if (date.dayOfWeek == DayOfWeek.SATURDAY || date.dayOfWeek == DayOfWeek.SUNDAY) {
                weekendCount++
            } else {
                weekdayCount++
            }
        }
        val total = weekendCount + weekdayCount
        val weekendRatio = if (total > 0) weekendCount.toFloat() / total else 0.5f
        val avgEpisodes = if (total > 0) totalEpisodes.toFloat() / total else 2f
        return TemporalPattern(weekendRatio, avgEpisodes, weekendRatio > 0.65f)
    }

    fun isWeekday(): Boolean {
        val today = LocalDate.now().dayOfWeek
        return today != DayOfWeek.SATURDAY && today != DayOfWeek.SUNDAY
    }

    fun getContextBoost(pattern: TemporalPattern, episodeRuntime: Int?, isWeekday: Boolean): Float {
        if (!isWeekday || !pattern.prefersShortOnWeekdays) return 1.0f
        val runtime = episodeRuntime ?: 45
        return when {
            runtime <= 30 -> 1.0f
            runtime <= 50 -> 0.95f
            else -> 0.88f
        }
    }
}
