package com.example.showmateapp.ui.screens.profile.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.showmateapp.data.local.ThemePreference
import com.example.showmateapp.data.repository.AuthRepository
import com.example.showmateapp.data.repository.UserRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val themePreference: ThemePreference,
    private val authRepository: AuthRepository,
    private val userRepository: UserRepository
) : ViewModel() {

    val isDarkTheme = themePreference.isDarkTheme
        .stateIn(viewModelScope, SharingStarted.Eagerly, true)

    private val _loggedOut = MutableStateFlow(false)
    val loggedOut: StateFlow<Boolean> = _loggedOut.asStateFlow()

    private val _currentEmail = MutableStateFlow("")
    val currentEmail: StateFlow<String> = _currentEmail.asStateFlow()

    init {
        viewModelScope.launch {
            val profile = userRepository.getUserProfile()
            _currentEmail.value = profile?.username?.takeIf { it.isNotBlank() }
                ?: userRepository.getCurrentUserEmail() ?: ""
        }
    }

    fun setDarkTheme(enabled: Boolean) {
        viewModelScope.launch {
            themePreference.setDarkTheme(enabled)
        }
    }

    fun logout() {
        authRepository.signOut()
        _loggedOut.value = true
    }

    fun updateDisplayName(name: String, onComplete: (Boolean) -> Unit) {
        viewModelScope.launch {
            try {
                userRepository.updateProfile(name)
                onComplete(true)
            } catch (_: Exception) {
                onComplete(false)
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
}
