package com.andrea.showmateapp.ui.screens.splash

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.andrea.showmateapp.data.repository.AuthRepository
import com.andrea.showmateapp.data.repository.SessionRepository
import com.andrea.showmateapp.di.AppPrefsDataStore
import com.andrea.showmateapp.domain.repository.IUserRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

@HiltViewModel
class SplashViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    private val sessionRepository: SessionRepository,
    private val userRepository: IUserRepository,
    @AppPrefsDataStore private val dataStore: DataStore<Preferences>
) : ViewModel() {

    companion object {
        private val KEY_CONSENT = booleanPreferencesKey("consent_given")
        private val KEY_ONBOARDING = booleanPreferencesKey("onboarding_completed")
    }

    private val _authDecision = MutableStateFlow<SplashDestination?>(null)
    val authDecision: StateFlow<SplashDestination?> = _authDecision

    init {
        viewModelScope.launch {
            try {
                val sessionValid = sessionRepository.checkSessionAndLogoutIfExpired()
                val user = authRepository.getCurrentUser()
                val loggedIn = sessionValid && user != null

                val prefs = dataStore.data.first()
                val consentGiven = prefs[KEY_CONSENT] == true
                val onboardingLocal = prefs[KEY_ONBOARDING] == true

                if (!consentGiven) {
                    _authDecision.value = SplashDestination.WELCOME_NEW
                } else if (loggedIn) {

                    sessionRepository.updateLastActivity()

                    if (onboardingLocal) {

                        _authDecision.value = SplashDestination.HOME
                    } else {

                        val profile = userRepository.getUserProfile()
                        if (profile?.onboardingCompleted == true) {
                            runCatching {
                                dataStore.edit { prefs -> prefs[KEY_ONBOARDING] = true }
                            }
                            _authDecision.value = SplashDestination.HOME
                        } else {
                            _authDecision.value = SplashDestination.ONBOARDING
                        }
                    }
                } else {

                    _authDecision.value = SplashDestination.LOGIN
                }
            } catch (e: Exception) {
                if (e is CancellationException) throw e
                _authDecision.value = SplashDestination.LOGIN
            }
        }
    }

    fun isLoggedIn(): Boolean = authRepository.getCurrentUser() != null
}

enum class SplashDestination {
    HOME,
    ONBOARDING,
    LOGIN,
    WELCOME_NEW  
}
