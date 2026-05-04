package com.andrea.showmateapp.ui.screens.splash

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.andrea.showmateapp.data.repository.SessionRepository
import com.andrea.showmateapp.domain.repository.IAuthRepository
import com.andrea.showmateapp.di.AppPrefsDataStore
import com.andrea.showmateapp.domain.repository.IInteractionRepository
import com.andrea.showmateapp.domain.repository.IUserRepository
import com.andrea.showmateapp.util.AppPrefsKeys
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

@HiltViewModel
class SplashViewModel @Inject constructor(
    private val authRepository: IAuthRepository,
    private val sessionRepository: SessionRepository,
    private val userRepository: IUserRepository,
    private val interactionRepository: IInteractionRepository,
    @AppPrefsDataStore private val dataStore: DataStore<Preferences>
) : ViewModel() {

    private val _authDecision = MutableStateFlow<SplashDestination?>(null)
    val authDecision: StateFlow<SplashDestination?> = _authDecision

    init {
        viewModelScope.launch {
            try {
                val sessionValid = sessionRepository.checkSessionAndLogoutIfExpired()
                val user = authRepository.getCurrentUser()
                val loggedIn = sessionValid && user != null

                val prefs = dataStore.data.first()
                val consentGiven = prefs[AppPrefsKeys.KEY_CONSENT] == true
                val onboardingLocal = prefs[AppPrefsKeys.KEY_ONBOARDING] == true
                val calibrationLocal = prefs[AppPrefsKeys.KEY_CALIBRATION] == true

                if (!consentGiven) {
                    _authDecision.value = SplashDestination.WELCOME_NEW
                } else if (loggedIn) {

                    sessionRepository.updateLastActivity()

                    // Always verify Firestore: DataStore alone can be stale after an account switch
                    val profile = userRepository.getUserProfile()
                    runCatching { interactionRepository.syncFavoritesAndWatchedToRoom() }

                    if (profile == null) {
                        // Fallback: If Firestore fails (e.g., offline), trust local DataStore to avoid loops
                        if (calibrationLocal) {
                            _authDecision.value = SplashDestination.HOME
                        } else if (onboardingLocal) {
                            _authDecision.value = SplashDestination.SWIPE
                        } else {
                            _authDecision.value = SplashDestination.ONBOARDING
                        }
                    } else if (profile.calibrationCompleted) {
                        if (!calibrationLocal) {
                            runCatching { dataStore.edit { p -> p[AppPrefsKeys.KEY_CALIBRATION] = true } }
                        }
                        _authDecision.value = SplashDestination.HOME
                    } else if (profile.onboardingCompleted) {
                        if (!onboardingLocal) {
                            runCatching { dataStore.edit { p -> p[AppPrefsKeys.KEY_ONBOARDING] = true } }
                        }
                        _authDecision.value = SplashDestination.SWIPE
                    } else {
                        if (onboardingLocal || calibrationLocal) {
                            // DataStore was stale — reset it so the next launch is consistent
                            runCatching {
                                dataStore.edit { p ->
                                    p[AppPrefsKeys.KEY_ONBOARDING] = false
                                    p[AppPrefsKeys.KEY_CALIBRATION] = false
                                }
                            }
                        }
                        _authDecision.value = SplashDestination.ONBOARDING
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
}

enum class SplashDestination {
    HOME,
    ONBOARDING,
    SWIPE,
    LOGIN,
    WELCOME_NEW
}
