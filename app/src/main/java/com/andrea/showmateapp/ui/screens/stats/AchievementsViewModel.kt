package com.andrea.showmateapp.ui.screens.stats

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.andrea.showmateapp.data.model.Achievement
import com.andrea.showmateapp.domain.repository.IAchievementRepository
import com.andrea.showmateapp.domain.repository.ISocialRepository
import com.andrea.showmateapp.domain.usecase.AchievementDefs
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class AchievementsUiState(
    val isLoading: Boolean = true,
    val xp: Int = 0,
    val achievements: List<Achievement> = emptyList(),
    val leaderboard: List<IAchievementRepository.LeaderboardEntry> = emptyList()
)

@HiltViewModel
class AchievementsViewModel @Inject constructor(
    private val achievementRepository: IAchievementRepository,
    private val socialRepository: ISocialRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(AchievementsUiState())
    val uiState: StateFlow<AchievementsUiState> = _uiState.asStateFlow()

    init { load() }

    private fun load() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            try {
                coroutineScope {
                    val xpDef          = async { achievementRepository.getXp() }
                    val unlockedDef    = async { achievementRepository.getUnlockedIds() }
                    val friendsDef     = async {
                        runCatching { socialRepository.getFriends().map { it.email } }.getOrDefault(emptyList())
                    }
                    val xp         = xpDef.await()
                    val unlockedIds = unlockedDef.await().toSet()
                    val friendEmails = friendsDef.await()
                    val leaderboard = runCatching {
                        achievementRepository.getFriendLeaderboard(friendEmails)
                    }.getOrDefault(emptyList())

                    val now = System.currentTimeMillis()
                    val achievements = AchievementDefs.all.map { def ->
                        if (def.id in unlockedIds) def.copy(unlockedAt = now) else def
                    }

                    _uiState.update {
                        it.copy(
                            isLoading    = false,
                            xp           = xp,
                            achievements = achievements,
                            leaderboard  = leaderboard
                        )
                    }
                }
            } catch (e: Exception) {
                if (e is CancellationException) throw e
                _uiState.update { it.copy(isLoading = false) }
            }
        }
    }
}
