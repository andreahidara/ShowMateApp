package com.example.showmateapp.ui.screens.stats

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.showmateapp.data.repository.UserRepository
import com.example.showmateapp.domain.usecase.GetViewerPersonalityUseCase
import com.example.showmateapp.util.GenreMapper
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import javax.inject.Inject

data class StatsUiState(
    val isLoading: Boolean = true,
    val currentStreak: Int = 0,
    val longestStreak: Int = 0,
    val dailyRecord: Int = 0,
    val totalEpisodesWatched: Int = 0,
    val topGenresByMonth: Map<String, List<Pair<String, Int>>> = emptyMap(),
    val activityByMonth: Map<String, Int> = emptyMap(),
    val personalityProfile: GetViewerPersonalityUseCase.PersonalityProfile? = null
)

@HiltViewModel
class StatsViewModel @Inject constructor(
    private val userRepository: UserRepository,
    private val getViewerPersonalityUseCase: GetViewerPersonalityUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(StatsUiState())
    val uiState: StateFlow<StatsUiState> = _uiState.asStateFlow()

    init {
        loadStats()
    }

    private fun loadStats() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            try {
                val profile = userRepository.getUserProfile()
                val history = profile?.viewingHistory ?: emptyList()

                // Parse history entries "YYYY-MM-DD:showId:count"
                data class Entry(val date: LocalDate, val showId: String, val count: Int)
                val formatter = DateTimeFormatter.ISO_LOCAL_DATE
                val entries = history.mapNotNull { raw ->
                    val parts = raw.split(":")
                    if (parts.size >= 3) {
                        runCatching {
                            Entry(
                                date = LocalDate.parse(parts[0], formatter),
                                showId = parts[1],
                                count = parts[2].toInt()
                            )
                        }.getOrNull()
                    } else null
                }

                // Episodes per day map
                val episodesPerDay = entries.groupBy { it.date }
                    .mapValues { (_, es) -> es.sumOf { it.count } }

                // Total episodes
                val total = episodesPerDay.values.sum()

                // Daily record
                val dailyRecord = episodesPerDay.values.maxOrNull() ?: 0

                // Current streak: empieza desde hoy o ayer (el que tenga datos primero)
                // Así la racha no se rompe si el usuario aún no ha visto nada hoy.
                val today = LocalDate.now()
                val streakStart = if (episodesPerDay.containsKey(today)) today else today.minusDays(1)
                var streak = 0
                var day = streakStart
                while (episodesPerDay.getOrDefault(day, 0) > 0) {
                    streak++
                    day = day.minusDays(1)
                }

                // Longest streak
                val sortedDays = episodesPerDay.keys.sorted()
                var longest = 0
                var current = 0
                var prevDay: LocalDate? = null
                for (d in sortedDays) {
                    current = if (prevDay != null && d == prevDay!!.plusDays(1)) current + 1 else 1
                    if (current > longest) longest = current
                    prevDay = d
                }

                // Activity by month
                val activityByMonth = entries.groupBy { it.date.format(DateTimeFormatter.ofPattern("yyyy-MM")) }
                    .mapValues { (_, es) -> es.sumOf { it.count } }

                // Genre distribution per month (using watchedEpisodes map which holds showId->episodes)
                // We use the profile genreScores as a proxy for top genres per time window
                val watchedByGenre = profile?.genreScores?.entries
                    ?.sortedByDescending { it.value }
                    ?.take(6)
                    ?.map { GenreMapper.getGenreName(it.key) to it.value.toInt() }
                    ?: emptyList()

                // Build topGenresByMonth using current month only (viewingHistory doesn't track genres)
                val currentMonth = today.format(DateTimeFormatter.ofPattern("yyyy-MM"))
                val topGenresByMonth = if (watchedByGenre.isNotEmpty()) {
                    mapOf(currentMonth to watchedByGenre)
                } else emptyMap()

                val personalityProfile = profile?.let { getViewerPersonalityUseCase.execute(it) }

                _uiState.update {
                    it.copy(
                        isLoading = false,
                        currentStreak = streak,
                        longestStreak = longest,
                        dailyRecord = dailyRecord,
                        totalEpisodesWatched = total,
                        topGenresByMonth = topGenresByMonth,
                        activityByMonth = activityByMonth,
                        personalityProfile = personalityProfile
                    )
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(isLoading = false) }
            }
        }
    }
}
