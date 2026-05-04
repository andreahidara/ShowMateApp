package com.andrea.showmateapp.ui.screens.main

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.andrea.showmateapp.data.model.Achievement
import com.andrea.showmateapp.domain.repository.IAuthRepository
import com.andrea.showmateapp.domain.repository.ISocialRepository
import com.andrea.showmateapp.domain.usecase.AchievementChecker
import com.andrea.showmateapp.util.NetworkMonitor
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

@HiltViewModel
class MainScreenViewModel @Inject constructor(
    networkMonitor: NetworkMonitor,
    authRepository: IAuthRepository,
    achievementChecker: AchievementChecker,
    private val socialRepository: ISocialRepository
) : ViewModel() {

    val isOnline: StateFlow<Boolean> = networkMonitor.isOnline
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), true)

    val isLoggedIn: StateFlow<Boolean> = authRepository.authState
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), true)

    private val _pendingAchievement = MutableStateFlow<Achievement?>(null)
    val pendingAchievement: StateFlow<Achievement?> = _pendingAchievement.asStateFlow()

    private val _pendingRequestCount = MutableStateFlow(0)
    val pendingRequestCount: StateFlow<Int> = _pendingRequestCount.asStateFlow()

    init {
        viewModelScope.launch {
            achievementChecker.unlockEvents.collect { ach ->
                _pendingAchievement.value = ach
            }
        }
        viewModelScope.launch {
            while (isActive) {
                updateRequestCount()
                delay(30_000L)
            }
        }
    }

    fun refreshRequestCount() {
        viewModelScope.launch { updateRequestCount() }
    }

    fun onAchievementDismissed() {
        _pendingAchievement.value = null
    }

    private suspend fun updateRequestCount() {
        runCatching { socialRepository.getPendingRequestCount() }
            .onSuccess { _pendingRequestCount.value = it }
    }
}
