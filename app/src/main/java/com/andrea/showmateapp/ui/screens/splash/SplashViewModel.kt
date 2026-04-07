package com.andrea.showmateapp.ui.screens.splash

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.andrea.showmateapp.data.repository.AuthRepository
import com.andrea.showmateapp.data.repository.SessionRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SplashViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    private val sessionRepository: SessionRepository
) : ViewModel() {

    private val _authDecision = MutableStateFlow<Boolean?>(null)
    val authDecision: StateFlow<Boolean?> = _authDecision

    init {
        viewModelScope.launch {
            val sessionValid = sessionRepository.checkSessionAndLogoutIfExpired()
            val loggedIn = sessionValid && authRepository.getCurrentUser() != null
            if (loggedIn) sessionRepository.updateLastActivity()
            _authDecision.value = loggedIn
        }
    }

    fun isLoggedIn(): Boolean = authRepository.getCurrentUser() != null
}
