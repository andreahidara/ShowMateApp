package com.andrea.showmateapp.ui.screens.login

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.andrea.showmateapp.R
import com.andrea.showmateapp.data.repository.AuthRepository
import com.andrea.showmateapp.domain.repository.IUserRepository
import com.andrea.showmateapp.util.UiText
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

@HiltViewModel
class LoginViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    private val userRepository: IUserRepository
) : ViewModel() {

    private val emailRegex = Regex("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$")

    private val _uiState = MutableStateFlow(LoginUiState())
    val uiState: StateFlow<LoginUiState> = _uiState.asStateFlow()

    fun onEmailChanged(email: String) {
        _uiState.update { it.copy(email = email, error = null) }
    }

    fun onPasswordChanged(password: String) {
        _uiState.update { it.copy(password = password, error = null) }
    }

    fun togglePasswordVisibility() {
        _uiState.update { it.copy(isPasswordVisible = !it.isPasswordVisible) }
    }

    fun sendPasswordReset(email: String) {
        if (email.isBlank()) {
            _uiState.update { it.copy(resetError = UiText.StringResource(R.string.error_reset_blank_email)) }
            return
        }
        viewModelScope.launch {
            authRepository.sendPasswordResetEmail(email.trim())
                .onSuccess { _uiState.update { it.copy(resetEmailSent = true, resetError = null) } }
                .onFailure { e ->
                    _uiState.update { it.copy(resetError = mapFirebaseAuthError(e, forLogin = true)) }
                }
        }
    }

    fun onGoogleSignInFailed() {
        _uiState.update { it.copy(error = UiText.StringResource(R.string.error_google_signin)) }
    }

    fun dismissResetDialog() {
        _uiState.update { it.copy(resetEmailSent = false, resetError = null) }
    }

    fun signInWithGoogle(idToken: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isGoogleLoading = true, error = null) }
            authRepository.signInWithGoogle(idToken)
                .onSuccess { isNewUser ->
                    if (isNewUser) {
                        val displayName = authRepository.getCurrentUser()?.displayName
                            ?: authRepository.getCurrentUser()?.email?.substringBefore("@")
                            ?: "Usuario"
                        runCatching { userRepository.initUserProfile(displayName) }
                    }
                    _uiState.update { it.copy(isGoogleLoading = false, isSuccess = true, isNewGoogleUser = isNewUser) }
                }
                .onFailure { e ->
                    _uiState.update {
                        it.copy(isGoogleLoading = false, error = UiText.StringResource(R.string.error_google_signin))
                    }
                }
        }
    }

    fun onLoginClick() {
        val state = _uiState.value

        if (state.email.isBlank() || state.password.isBlank()) {
            _uiState.update { it.copy(error = UiText.StringResource(R.string.error_empty_fields)) }
            return
        }

        if (!emailRegex.matches(state.email)) {
            _uiState.update { it.copy(error = UiText.StringResource(R.string.error_invalid_email_format)) }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }

            authRepository.login(state.email, state.password)
                .onSuccess {
                    val profile = userRepository.getUserProfile()
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            isSuccess = true,
                            isOnboardingCompleted = profile?.onboardingCompleted == true
                        )
                    }
                }
                .onFailure { throwable ->
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            error = mapFirebaseAuthError(throwable, forLogin = true)
                        )
                    }
                }
        }
    }

    private fun mapFirebaseAuthError(throwable: Throwable, forLogin: Boolean): UiText {
        val msg = throwable.message?.lowercase() ?: ""
        return when {
            "wrong_password" in msg || "invalid_credential" in msg || "invalid credential" in msg ->
                UiText.StringResource(R.string.error_login_wrong_credentials)
            "user_not_found" in msg || "no user record" in msg ->
                UiText.StringResource(R.string.error_login_user_not_found)
            "user_disabled" in msg ->
                UiText.StringResource(R.string.error_login_user_disabled)
            "too_many_requests" in msg || "too many" in msg ->
                UiText.StringResource(R.string.error_login_too_many_attempts)
            "network" in msg ->
                UiText.StringResource(R.string.error_no_connection)
            forLogin -> UiText.StringResource(R.string.error_login_wrong_credentials)
            else -> UiText.StringResource(R.string.error_reset_failed)
        }
    }
}
