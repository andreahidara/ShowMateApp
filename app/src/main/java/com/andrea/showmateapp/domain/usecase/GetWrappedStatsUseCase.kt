package com.andrea.showmateapp.domain.usecase

import com.andrea.showmateapp.data.model.UserProfile
import com.andrea.showmateapp.domain.repository.IShowRepository
import com.andrea.showmateapp.util.GenreMapper
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class GetWrappedStatsUseCase @Inject constructor(
    private val showRepository: IShowRepository
) {

    data class WrappedStats(
        val totalHoursWatched: Float,
        val mostActiveMonthLabel: String?,
        val favoriteDayOfWeek: String?,
        val topGenreName: String?,
        val topActorName: String?,
        val topDirectorName: String?,
        val viewerType: ViewerPersonalityType,
        val topRewatchedShows: List<RewatchedShow>,
        val monthlyActivity: List<Pair<String, Int>>,
        val countryDistribution: List<Pair<String, Int>>
    )

    data class RewatchedShow(val showId: Int, val name: String, val sessionCount: Int)

    enum class ViewerPersonalityType(
        val title: String,
        val emoji: String,
        val description: String
    ) {
        MARATHON("El Maratonero", "\uD83C\uDFC3", "Capaz de ver 5 episodios seguidos sin parpadear"),
        ECLECTIC("El Ecléctico", "\uD83C\uDF08", "Un género nunca es suficiente para ti"),
        SPECIALIST("El Especialista", "\uD83C\uDFAF", "Cuando encuentras tu género, no hay marcha atrás"),
        DRAMATIC("El Dramático", "\uD83D\uDE2D", "El drama te llama y tú siempre acudes"),
        DETECTIVE("El Detective", "\uD83D\uDD0D", "Sin crimen ni misterio la vida pierde su gracia"),
        VISIONARY("El Visionario", "\uD83D\uDE80", "La ciencia ficción es tu portal a otros mundos"),
        OPTIMIST("El Optimista", "\uD83D\uDE04", "La comedia te hace la vida mucho más llevadera"),
        EXPLORER("El Explorador", "\uD83D\uDDFA\uFE0F", "Siempre buscando la próxima joya desconocida"),
        COMPLETIST("El Completista", "\u2705", "Si empiezas una serie, la terminas. Sin excusas"),
        CRITIC("El Crítico", "\uD83E\uDDD0", "Tus reseñas son más largas que los propios créditos")
    }

    suspend fun execute(profile: UserProfile): WrappedStats {
        val fmt = DateTimeFormatter.ISO_LOCAL_DATE

        data class HistoryEntry(val date: LocalDate, val showId: Int, val count: Int)
        val entries: List<HistoryEntry> = profile.viewingHistory.mapNotNull { raw ->
            val parts = raw.split(":")
            if (parts.size >= 3) runCatching {
                HistoryEntry(LocalDate.parse(parts[0], fmt), parts[1].toInt(), parts[2].toInt())
            }.getOrNull() else null
        }

        val monthFmt = DateTimeFormatter.ofPattern("MM/yy")
        val monthKey = DateTimeFormatter.ofPattern("yyyy-MM")
        val byMonthKey = entries.groupBy { it.date.format(monthKey) }
            .mapValues { (_, es) -> es.sumOf { it.count } }
        val monthlyActivity = byMonthKey.entries
            .sortedBy { it.key }
            .takeLast(12)
            .map { it.key.let { k ->
                val d = LocalDate.parse("$k-01", DateTimeFormatter.ISO_LOCAL_DATE)
                d.format(monthFmt)
            } to it.value }

        val mostActiveKey = byMonthKey.entries.maxByOrNull { it.value }?.key
        val mostActiveMonthLabel = mostActiveKey?.let {
            val parts = it.split("-")
            val idx = (parts[1].toIntOrNull() ?: 1) - 1
            "${MONTH_NAMES.getOrElse(idx) { it }} ${parts[0]}"
        }

        val dayCount = entries.groupBy { it.date.dayOfWeek }
            .mapValues { (_, es) -> es.sumOf { it.count } }
        val favDayLabel = dayCount.maxByOrNull { it.value }?.key
            ?.getDisplayName(TextStyle.FULL, Locale("es", "ES"))
            ?.replaceFirstChar { it.uppercaseChar() }

        val sessionsByShow = entries.groupBy { it.showId }
            .mapValues { (_, es) -> es.distinctBy { it.date }.size }
            .entries.sortedByDescending { it.value }
            .take(5)

        val topIds = sessionsByShow.map { it.key }
        val showNameMap: Map<Int, String> = if (topIds.isNotEmpty()) {
            runCatching {
                showRepository.getShowDetailsInParallel(topIds)
                    .associate { it.id to (it.name.ifBlank { "Serie #${it.id}" }) }
            }.getOrDefault(emptyMap())
        } else emptyMap()

        val topRewatched = sessionsByShow.take(3).map { (id, count) ->
            RewatchedShow(id, showNameMap[id] ?: "Serie #$id", count)
        }

        val countryMap = mutableMapOf<String, Int>()
        runCatching {
            showRepository.getShowDetailsInParallel(topIds.take(10))
                .forEach { show ->
                    show.originCountry.forEach { code ->
                        val name = COUNTRY_NAMES[code] ?: code
                        countryMap[name] = (countryMap[name] ?: 0) + 1
                    }
                }
        }
        val countryDistribution = countryMap.entries
            .sortedByDescending { it.value }.take(5).map { it.key to it.value }

        val totalEpisodesFromHistory = entries.sumOf { it.count }
        val totalEpisodes = if (totalEpisodesFromHistory > 0) totalEpisodesFromHistory
        else profile.watchedEpisodes.values.sumOf { it.size }
        val totalHours = totalEpisodes * AVG_EPISODE_MINUTES / 60f

        val topGenreName = profile.genreScores.entries.maxByOrNull { it.value }
            ?.let { GenreMapper.getGenreName(it.key) }

        val topActor = profile.preferredActors.entries.maxByOrNull { it.value }?.key
        val topDirector = profile.preferredCreators.entries.maxByOrNull { it.value }?.key

        val viewerType = determineViewerType(
            profile = profile,
            totalSessions = entries.size,
            reviewCount = profile.mediaReviews.size
        )

        return WrappedStats(
            totalHoursWatched = totalHours,
            mostActiveMonthLabel = mostActiveMonthLabel,
            favoriteDayOfWeek = favDayLabel,
            topGenreName = topGenreName,
            topActorName = topActor,
            topDirectorName = topDirector,
            viewerType = viewerType,
            topRewatchedShows = topRewatched,
            monthlyActivity = monthlyActivity,
            countryDistribution = countryDistribution
        )
    }

    private fun determineViewerType(
        profile: UserProfile,
        totalSessions: Int,
        reviewCount: Int
    ): ViewerPersonalityType {
        val sortedGenres = profile.genreScores.entries
            .filter { it.value > 0 }
            .sortedByDescending { it.value }
        val genreCount = sortedGenres.size

        if (reviewCount >= 5) return ViewerPersonalityType.CRITIC

        if (genreCount >= 3) {
            val top3 = sortedGenres.take(3).map { it.value }
            if (top3.last() / top3.first() > 0.70f) return ViewerPersonalityType.ECLECTIC
        }

        val topGenreName = sortedGenres.firstOrNull()?.key
            ?.let { GenreMapper.getGenreName(it) } ?: return ViewerPersonalityType.COMPLETIST

        return when {
            topGenreName.contains("Crimen") || topGenreName.contains("Misterio") ->
                ViewerPersonalityType.DETECTIVE
            topGenreName.contains("Drama") ->
                ViewerPersonalityType.DRAMATIC
            topGenreName.contains("Ciencia") || topGenreName.contains("Fantasía") ->
                ViewerPersonalityType.VISIONARY
            topGenreName.contains("Comedia") ->
                ViewerPersonalityType.OPTIMIST
            genreCount >= 6 -> ViewerPersonalityType.EXPLORER
            genreCount <= 2 -> ViewerPersonalityType.SPECIALIST
            totalSessions > 20 -> ViewerPersonalityType.MARATHON
            else -> ViewerPersonalityType.COMPLETIST
        }
    }

    companion object {
        private const val AVG_EPISODE_MINUTES = 42

        private val MONTH_NAMES = listOf(
            "Enero", "Febrero", "Marzo", "Abril", "Mayo", "Junio",
            "Julio", "Agosto", "Septiembre", "Octubre", "Noviembre", "Diciembre"
        )

        private val COUNTRY_NAMES = mapOf(
            "US" to "EE.UU.", "GB" to "Reino Unido", "JP" to "Japón",
            "KR" to "Corea del Sur", "DE" to "Alemania", "FR" to "Francia",
            "ES" to "España", "IT" to "Italia", "CA" to "Canadá",
            "AU" to "Australia", "SE" to "Suecia", "DK" to "Dinamarca",
            "NO" to "Noruega", "MX" to "México", "BR" to "Brasil",
            "IN" to "India", "CN" to "China", "TH" to "Tailandia"
        )
    }
}
