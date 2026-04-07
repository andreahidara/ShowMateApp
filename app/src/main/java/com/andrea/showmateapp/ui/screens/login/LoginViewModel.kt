package com.andrea.showmateapp.ui.screens.login

import android.util.Patterns
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.andrea.showmateapp.data.repository.AuthRepository
import com.andrea.showmateapp.domain.repository.IUserRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject
import com.andrea.showmateapp.util.UiText
import com.andrea.showmateapp.R

@HiltViewModel
class LoginViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    private val userRepository: IUserRepository
) : ViewModel() {

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
            _uiState.update { it.copy(resetError = "Introduce tu email primero") }
            return
        }
        viewModelScope.launch {
            authRepository.sendPasswordResetEmail(email.trim())
                .onSuccess { _uiState.update { it.copy(resetEmailSent = true, resetError = null) } }
                .onFailure { e -> _uiState.update { it.copy(resetError = e.message ?: "No se pudo enviar el correo") } }
        }
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
                .onFailure { e -> _uiState.update { it.copy(isGoogleLoading = false, error = UiText.DynamicString(e.message ?: "Error con Google")) } }
        }
    }

    fun onLoginClick() {
        val state = _uiState.value

        if (state.email.isBlank() || state.password.isBlank()) {
            _uiState.update { it.copy(error = UiText.StringResource(R.string.error_empty_fields)) }
            return
        }

        if (!Patterns.EMAIL_ADDRESS.matcher(state.email).matches()) {
            _uiState.update { it.copy(error = UiText.DynamicString("Formato de correo inválido")) }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }

            authRepository.login(state.email, state.password)
                .onSuccess {
                    _uiState.update { it.copy(isLoading = false, isSuccess = true) }
                }
                .onFailure { throwable ->
                    _uiState.update { it.copy(
                        isLoading = false,
                        error = UiText.DynamicString(throwable.message ?: "Error desconocido")
                    ) }
                }
        }
    }
}
