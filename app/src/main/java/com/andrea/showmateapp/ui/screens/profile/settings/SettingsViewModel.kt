package com.andrea.showmateapp.ui.screens.profile.settings

import android.content.Context
import android.net.Uri
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.andrea.showmateapp.R
import com.andrea.showmateapp.data.local.ThemePreference
import com.andrea.showmateapp.di.AppPrefsDataStore
import com.andrea.showmateapp.domain.repository.IAuthRepository
import com.andrea.showmateapp.domain.repository.IUserRepository
import com.andrea.showmateapp.util.AppPrefsKeys
import com.andrea.showmateapp.util.DataExportManager
import com.andrea.showmateapp.util.NotificationPrefsKeys
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val themePreference: ThemePreference,
    private val authRepository: IAuthRepository,
    private val userRepository: IUserRepository,
    @AppPrefsDataStore private val dataStore: DataStore<Preferences>,
    private val exportManager: DataExportManager,
    @ApplicationContext private val context: Context
) : ViewModel() {

    val isDarkTheme = themePreference.isDarkTheme
        .distinctUntilChanged()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), true)

    val notifEnabled = dataStore.data.map { it[NotificationPrefsKeys.NOTIF_ENABLED] != false }
        .distinctUntilChanged()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), true)

    val notifNewEpisodes = dataStore.data.map { it[NotificationPrefsKeys.NOTIF_NEW_EPISODES] != false }
        .distinctUntilChanged()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), true)

    val notifFriends = dataStore.data.map { it[NotificationPrefsKeys.NOTIF_FRIENDS] != false }
        .distinctUntilChanged()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), true)

    val notifRecommendations = dataStore.data.map { it[NotificationPrefsKeys.NOTIF_RECOMMENDATIONS] == true }
        .distinctUntilChanged()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), false)

    private val _loggedOut = MutableStateFlow(false)
    val loggedOut: StateFlow<Boolean> = _loggedOut.asStateFlow()

    private val _accountDeleted = MutableStateFlow(false)
    val accountDeleted: StateFlow<Boolean> = _accountDeleted.asStateFlow()

    private val _currentEmail = MutableStateFlow("")
    val currentEmail: StateFlow<String> = _currentEmail.asStateFlow()

    init {
        _currentEmail.value = userRepository.getCurrentUserEmail() ?: ""
    }

    fun setDarkTheme(enabled: Boolean) {
        viewModelScope.launch { themePreference.setDarkTheme(enabled) }
    }

    fun setNotifEnabled(enabled: Boolean) {
        viewModelScope.launch { dataStore.edit { it[NotificationPrefsKeys.NOTIF_ENABLED] = enabled } }
    }

    fun setNotifNewEpisodes(enabled: Boolean) {
        viewModelScope.launch { dataStore.edit { it[NotificationPrefsKeys.NOTIF_NEW_EPISODES] = enabled } }
    }

    fun setNotifFriends(enabled: Boolean) {
        viewModelScope.launch { dataStore.edit { it[NotificationPrefsKeys.NOTIF_FRIENDS] = enabled } }
    }

    fun setNotifRecommendations(enabled: Boolean) {
        viewModelScope.launch { dataStore.edit { it[NotificationPrefsKeys.NOTIF_RECOMMENDATIONS] = enabled } }
    }

    fun logout() {
        viewModelScope.launch {
            runCatching { userRepository.clearUserCache() }
            authRepository.signOut()
            _loggedOut.value = true
        }
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

    private val _isResetting = MutableStateFlow(false)
    val isResetting: StateFlow<Boolean> = _isResetting.asStateFlow()

    fun resetAlgorithmData(onComplete: (Boolean) -> Unit) {
        viewModelScope.launch {
            _isResetting.value = true
            try {
                userRepository.resetAlgorithmData()
                dataStore.edit {
                    it[AppPrefsKeys.KEY_ONBOARDING] = false
                    it[AppPrefsKeys.KEY_CALIBRATION] = false
                }
                onComplete(true)
            } catch (e: Exception) {
                if (e is CancellationException) throw e
                onComplete(false)
            } finally {
                _isResetting.value = false
            }
        }
    }

    fun sendPasswordReset(onComplete: (Boolean) -> Unit) {
        val email = _currentEmail.value.takeIf { it.isNotBlank() }
            ?: userRepository.getCurrentUserEmail()
        if (email.isNullOrBlank()) {
            onComplete(false)
            return
        }
        viewModelScope.launch {
            val result = authRepository.sendPasswordResetEmail(email)
            onComplete(result.isSuccess)
        }
    }

    fun restoreData(uri: Uri, onComplete: (Boolean, String) -> Unit) {
        viewModelScope.launch {
            try {
                val content = exportManager.readUriContent(uri)
                    ?: run {
                        onComplete(false, context.getString(R.string.settings_restore_error_read))
                        return@launch
                    }
                val partial = exportManager.parseJsonBackup(content)
                    ?: run {
                        onComplete(false, context.getString(R.string.settings_restore_error_invalid))
                        return@launch
                    }
                userRepository.restoreBackup(partial)
                onComplete(true, context.getString(R.string.settings_restore_success))
            } catch (e: Exception) {
                if (e is CancellationException) throw e
                onComplete(false, context.getString(R.string.settings_restore_error_generic))
            }
        }
    }

    fun exportData(format: String, onComplete: (Boolean, Uri?) -> Unit) {
        viewModelScope.launch {
            try {
                val profile = userRepository.getUserProfile() ?: run {
                    onComplete(false, null)
                    return@launch
                }
                val (content, mimeType, ext) = if (format == "csv") {
                    Triple(exportManager.buildCsvExport(profile), "text/csv", "csv")
                } else {
                    Triple(exportManager.buildJsonExport(profile), "application/json", "json")
                }
                val filename = "showmate_export_${System.currentTimeMillis()}.$ext"
                val uri = exportManager.saveToDownloads(content, filename, mimeType)
                onComplete(uri != null, uri)
            } catch (e: Exception) {
                if (e is CancellationException) throw e
                onComplete(false, null)
            }
        }
    }
}
