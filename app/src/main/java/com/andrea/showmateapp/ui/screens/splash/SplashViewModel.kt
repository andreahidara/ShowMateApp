package com.andrea.showmateapp.ui.screens.splash

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.andrea.showmateapp.data.repository.AuthRepository
import com.andrea.showmateapp.data.repository.SessionRepository
import com.andrea.showmateapp.domain.repository.IUserRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

@HiltViewModel
class SplashViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    private val sessionRepository: SessionRepository,
    private val userRepository: IUserRepository
) : ViewModel() {

    private val _authDecision = MutableStateFlow<SplashDestination?>(null)
    val authDecision: StateFlow<SplashDestination?> = _authDecision

    init {
        viewModelScope.launch {
            val sessionValid = sessionRepository.checkSessionAndLogoutIfExpired()
            val user = authRepository.getCurrentUser()
            val loggedIn = sessionValid && user != null

            if (loggedIn) {
                sessionRepository.updateLastActivity()
                val profile = userRepository.getUserProfile()
                if (profile?.onboardingCompleted == true) {
                    _authDecision.value = SplashDestination.HOME
                } else {
                    _authDecision.value = SplashDestination.ONBOARDING
                }
            } else {
                _authDecision.value = SplashDestination.LOGIN
            }
        }
    }

    fun isLoggedIn(): Boolean = authRepository.getCurrentUser() != null
}

enum class SplashDestination {
    HOME,
    ONBOARDING,
    LOGIN
}
