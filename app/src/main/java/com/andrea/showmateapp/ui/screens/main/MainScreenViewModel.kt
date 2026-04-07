package com.andrea.showmateapp.ui.screens.main

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.andrea.showmateapp.data.model.Achievement
import com.andrea.showmateapp.data.repository.AuthRepository
import com.andrea.showmateapp.domain.usecase.AchievementChecker
import com.andrea.showmateapp.util.NetworkMonitor
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class MainScreenViewModel @Inject constructor(
    val networkMonitor: NetworkMonitor,
    val authRepository: AuthRepository,
    achievementChecker: AchievementChecker
) : ViewModel() {

    private val _pendingAchievement = MutableStateFlow<Achievement?>(null)
    val pendingAchievement: StateFlow<Achievement?> = _pendingAchievement.asStateFlow()

    init {
        viewModelScope.launch {
            achievementChecker.unlockEvents.collect { ach ->
                _pendingAchievement.value = ach
            }
        }
    }

    fun onAchievementDismissed() { _pendingAchievement.value = null }
}
