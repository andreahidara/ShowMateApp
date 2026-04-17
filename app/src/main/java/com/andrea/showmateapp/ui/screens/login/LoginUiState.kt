package com.andrea.showmateapp.ui.screens.login

import androidx.compose.runtime.Immutable
import com.andrea.showmateapp.util.UiText

@Immutable
data class LoginUiState(
    val email: String = "",
    val password: String = "",
    val isPasswordVisible: Boolean = false,
    val isLoading: Boolean = false,
    val isGoogleLoading: Boolean = false,
    val error: UiText? = null,
    val isSuccess: Boolean = false,
    val isNewGoogleUser: Boolean = false,
    val isOnboardingCompleted: Boolean = false,
    val resetEmailSent: Boolean = false,
    val resetError: UiText? = null
)
