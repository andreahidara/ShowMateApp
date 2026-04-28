package com.andrea.showmateapp.util

import com.andrea.showmateapp.data.model.MediaContent
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalTime

object MoodContextEngine {

    enum class TimeSlot { MORNING, AFTERNOON, EVENING, LATE_NIGHT }
    enum class DayType { WEEKDAY, FRIDAY, WEEKEND }

    data class MoodContext(val timeSlot: TimeSlot, val dayType: DayType)

    fun currentContext(): MoodContext {
        val hour = LocalTime.now().hour
        val dayOfWeek = LocalDate.now().dayOfWeek
        val timeSlot = when (hour) {
            in 6..11 -> TimeSlot.MORNING
            in 12..17 -> TimeSlot.AFTERNOON
            in 18..21 -> TimeSlot.EVENING
            else -> TimeSlot.LATE_NIGHT
        }
        val dayType = when (dayOfWeek) {
            DayOfWeek.FRIDAY -> DayType.FRIDAY
            DayOfWeek.SATURDAY, DayOfWeek.SUNDAY -> DayType.WEEKEND
            else -> DayType.WEEKDAY
        }
        return MoodContext(timeSlot, dayType)
    }

    // Multiplicadores calibrados por contexto: entre semana/noche favorece comedia y familia
    // (sesiones cortas de descanso), viernes/noche potencia acción/sci-fi (inicio de fin de semana),
    // madrugada prioriza misterio y drama de ritmo lento que no requiere alta atención sostenida
    private fun genreMultipliers(context: MoodContext): Map<Int, Float> = when (context) {
        MoodContext(TimeSlot.EVENING, DayType.WEEKDAY) -> mapOf(
            35 to 1.28f,
            10751 to 1.20f,
            9648 to 1.12f,
            99 to 1.10f,
            18 to 0.92f,
            10759 to 0.90f
        )
        MoodContext(TimeSlot.EVENING, DayType.FRIDAY) -> mapOf(
            10759 to 1.28f,
            10765 to 1.22f,
            80 to 1.15f,
            9648 to 1.10f,
            35 to 1.08f
        )
        MoodContext(TimeSlot.AFTERNOON, DayType.WEEKEND) -> mapOf(
            18 to 1.15f,
            80 to 1.10f,
            10765 to 1.10f,
            10759 to 1.08f
        )
        MoodContext(TimeSlot.EVENING, DayType.WEEKEND) -> mapOf(
            18 to 1.18f,
            10765 to 1.15f,
            80 to 1.12f,
            10759 to 1.10f
        )
        MoodContext(TimeSlot.LATE_NIGHT, DayType.WEEKDAY),
        MoodContext(TimeSlot.LATE_NIGHT, DayType.FRIDAY),
        MoodContext(TimeSlot.LATE_NIGHT, DayType.WEEKEND) -> mapOf(
            9648 to 1.25f,
            18 to 1.15f,
            99 to 1.12f,
            10759 to 0.85f,
            10765 to 0.92f
        )
        else -> emptyMap()
    }

    // Entre semana por la noche y en madrugada se penalizan episodios largos (>50 min)
    // porque el usuario tiene menor disponibilidad de tiempo o menor energía
    private fun runtimeMultiplier(episodeRuntime: Int?, context: MoodContext): Float {
        val runtime = episodeRuntime ?: 45
        return when {
            context.timeSlot == TimeSlot.EVENING && context.dayType == DayType.WEEKDAY -> when {
                runtime <= 30 -> 1.15f
                runtime <= 50 -> 1.00f
                else -> 0.90f
            }
            context.timeSlot == TimeSlot.LATE_NIGHT -> when {
                runtime <= 45 -> 1.10f
                runtime > 60 -> 0.92f
                else -> 1.00f
            }
            else -> 1.00f
        }
    }

    fun getMoodMultiplier(show: MediaContent, context: MoodContext): Float {
        val genreMults = genreMultipliers(context)
        val showGenres = show.safeGenreIds.toHashSet()
        val genreMultiplier = genreMults.entries
            .filter { (genreId, _) -> genreId in showGenres }
            .maxOfOrNull { it.value } ?: 1.0f
        val genrePenalty = genreMults.entries
            .filter { (genreId, mult) -> genreId in showGenres && mult < 1.0f }
            .minOfOrNull { it.value } ?: 1.0f
        // Si el show tiene géneros tanto con boost como con penalización, prevalece el boost (señal positiva gana)
        val finalGenre = if (genreMultiplier >= 1.0f) genreMultiplier else genrePenalty
        val rtMult = runtimeMultiplier(show.episodeRunTime?.firstOrNull(), context)
        return (finalGenre * rtMult).coerceIn(0.80f, 1.35f)
    }
}
