package com.andrea.showmateapp.ui.screens.profile.settings

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.andrea.showmateapp.data.local.ThemePreference
import com.andrea.showmateapp.data.repository.AuthRepository
import com.andrea.showmateapp.di.AppPrefsDataStore
import com.andrea.showmateapp.domain.repository.IUserRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject
import kotlinx.coroutines.CancellationException

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val themePreference: ThemePreference,
    private val authRepository: AuthRepository,
    private val userRepository: IUserRepository,
    @AppPrefsDataStore private val dataStore: DataStore<Preferences>
) : ViewModel() {

    companion object {
        val KEY_NOTIF_ENABLED       = booleanPreferencesKey("notif_enabled")
        val KEY_NOTIF_NEW_EPISODES  = booleanPreferencesKey("notif_new_episodes")
        val KEY_NOTIF_FRIENDS       = booleanPreferencesKey("notif_friends")
        val KEY_NOTIF_RECOMMENDATIONS = booleanPreferencesKey("notif_recommendations")
    }

    val isDarkTheme = themePreference.isDarkTheme
        .stateIn(viewModelScope, SharingStarted.Eagerly, true)

    val notifEnabled = dataStore.data.map { it[KEY_NOTIF_ENABLED] != false }
        .stateIn(viewModelScope, SharingStarted.Eagerly, true)

    val notifNewEpisodes = dataStore.data.map { it[KEY_NOTIF_NEW_EPISODES] != false }
        .stateIn(viewModelScope, SharingStarted.Eagerly, true)

    val notifFriends = dataStore.data.map { it[KEY_NOTIF_FRIENDS] != false }
        .stateIn(viewModelScope, SharingStarted.Eagerly, true)

    val notifRecommendations = dataStore.data.map { it[KEY_NOTIF_RECOMMENDATIONS] == true }
        .stateIn(viewModelScope, SharingStarted.Eagerly, false)

    private val _loggedOut = MutableStateFlow(false)
    val loggedOut: StateFlow<Boolean> = _loggedOut.asStateFlow()

    private val _accountDeleted = MutableStateFlow(false)
    val accountDeleted: StateFlow<Boolean> = _accountDeleted.asStateFlow()

    private val _currentEmail = MutableStateFlow("")
    val currentEmail: StateFlow<String> = _currentEmail.asStateFlow()

    init {
        viewModelScope.launch {
            _currentEmail.value = userRepository.getCurrentUserEmail() ?: ""
        }
    }

    fun setDarkTheme(enabled: Boolean) {
        viewModelScope.launch { themePreference.setDarkTheme(enabled) }
    }

    fun setNotifEnabled(enabled: Boolean) {
        viewModelScope.launch { dataStore.edit { it[KEY_NOTIF_ENABLED] = enabled } }
    }

    fun setNotifNewEpisodes(enabled: Boolean) {
        viewModelScope.launch { dataStore.edit { it[KEY_NOTIF_NEW_EPISODES] = enabled } }
    }

    fun setNotifFriends(enabled: Boolean) {
        viewModelScope.launch { dataStore.edit { it[KEY_NOTIF_FRIENDS] = enabled } }
    }

    fun setNotifRecommendations(enabled: Boolean) {
        viewModelScope.launch { dataStore.edit { it[KEY_NOTIF_RECOMMENDATIONS] = enabled } }
    }

    fun logout() {
        authRepository.signOut()
        _loggedOut.value = true
    }

    fun deleteAccount(onComplete: (Boolean) -> Unit) {
        viewModelScope.launch {
            try {
                userRepository.deleteAccount()
                authRepository.signOut()
                onComplete(true)
                _accountDeleted.value = true
            } catch (e: Exception) {
                if (e is CancellationException) throw e
                onComplete(false)
            }
        }
    }

    fun updateDisplayName(name: String, onComplete: (Boolean) -> Unit) {
        viewModelScope.launch {
            try {
                userRepository.updateProfile(name)
                onComplete(true)
            } catch (e: Exception) {
                if (e is CancellationException) throw e
                onComplete(false)
            }
        }
    }

    fun sendPasswordReset(onComplete: (Boolean) -> Unit) {
        val email = _currentEmail.value.takeIf { it.isNotBlank() }
            ?: userRepository.getCurrentUserEmail()
        if (email.isNullOrBlank()) { onComplete(false); return }
        viewModelScope.launch {
            val result = authRepository.sendPasswordResetEmail(email)
            onComplete(result.isSuccess)
        }
    }
}
