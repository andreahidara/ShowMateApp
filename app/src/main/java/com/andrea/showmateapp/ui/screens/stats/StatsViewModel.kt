package com.andrea.showmateapp.ui.screens.stats

import androidx.compose.runtime.Immutable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.andrea.showmateapp.data.model.UserProfile
import com.andrea.showmateapp.domain.repository.IUserRepository
import com.andrea.showmateapp.domain.usecase.GetViewerPersonalityUseCase
import com.andrea.showmateapp.util.GenreMapper
import dagger.hilt.android.lifecycle.HiltViewModel
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import javax.inject.Inject
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

@Immutable
data class StatsUiState(
    val isLoading: Boolean = true,
    val error: String? = null,
    val currentStreak: Int = 0,
    val longestStreak: Int = 0,
    val dailyRecord: Int = 0,
    val totalEpisodesWatched: Int = 0,
    val topGenresByMonth: Map<String, List<Pair<String, Int>>> = emptyMap(),
    val personalityProfile: GetViewerPersonalityUseCase.PersonalityProfile? = null
)

@HiltViewModel
class StatsViewModel @Inject constructor(
    private val userRepository: IUserRepository,
    private val getViewerPersonalityUseCase: GetViewerPersonalityUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(StatsUiState())
    val uiState: StateFlow<StatsUiState> = _uiState.asStateFlow()

    init {
        observeProfile()
    }

    private fun observeProfile() {
        viewModelScope.launch {
            try {
                userRepository.getUserProfileFlow().collectLatest { profile ->
                    if (profile != null) {
                        processProfileData(profile)
                    }
                }
            } catch (e: Exception) {
                if (e is CancellationException) throw e
                _uiState.update { it.copy(isLoading = false, error = e.message ?: "Error desconocido") }
            }
        }
    }

    fun retry() {
        _uiState.update { it.copy(isLoading = true, error = null) }
        observeProfile()
    }

    private suspend fun processProfileData(profile: UserProfile) {
        try {
                val history = profile.viewingHistory

                data class Entry(val date: LocalDate, val showId: String, val count: Int)
                val formatter = DateTimeFormatter.ISO_LOCAL_DATE
                val entries = history.mapNotNull { raw ->
                    val parts = raw.split(":")
                    if (parts.size >= 3) {
                        runCatching {
                            Entry(LocalDate.parse(parts[0], formatter), parts[1], parts[2].toInt())
                        }.getOrNull()
                    } else {
                        null
                    }
                }

                val episodesPerDay = entries.groupBy { it.date }
                    .mapValues { (_, es) -> es.sumOf { it.count } }

                val total = profile.watchedEpisodes.values.sumOf { it.size }

                val dailyRecord = episodesPerDay.values.maxOrNull() ?: 0

                val today = LocalDate.now()
                // Si hoy no hay actividad se acepta ayer como inicio de racha para no penalizar al usuario que todavía no ha visto nada hoy
                val streakStart = if (episodesPerDay.containsKey(today)) today else today.minusDays(1)
                var streak = 0
                var day = streakStart
                while (episodesPerDay.getOrDefault(day, 0) > 0) {
                    streak++
                    day = day.minusDays(1)
                }

                val sortedDays = episodesPerDay.keys.sorted()
                var longest = 0
                var current = 0
                var prevDay: LocalDate? = null
                for (d in sortedDays) {
                    current = if (d == prevDay?.plusDays(1)) current + 1 else 1
                    if (current > longest) longest = current
                    prevDay = d
                }

                val watchedByGenre = profile.genreScores.entries
                    .sortedByDescending { it.value }
                    .take(6)
                    .map { GenreMapper.getGenreName(it.key) to it.value.toInt() }

                val currentMonth = today.format(DateTimeFormatter.ofPattern("yyyy-MM"))
                val topGenresByMonth = if (watchedByGenre.isNotEmpty()) {
                    mapOf(currentMonth to watchedByGenre)
                } else {
                    emptyMap()
                }

                val personalityProfile = getViewerPersonalityUseCase.execute(profile)

                _uiState.update {
                    it.copy(
                        isLoading = false,
                        currentStreak = streak,
                        longestStreak = longest,
                        dailyRecord = dailyRecord,
                        totalEpisodesWatched = total,
                        topGenresByMonth = topGenresByMonth,
                        personalityProfile = personalityProfile
                    )
                }
        } catch (e: Exception) {
            if (e is CancellationException) throw e
            _uiState.update { it.copy(isLoading = false, error = e.message ?: "Error desconocido") }
        }
    }
}
