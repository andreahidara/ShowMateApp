package com.andrea.showmateapp.ui.screens.signup

import android.util.Patterns
import androidx.compose.runtime.Immutable
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

@Immutable
data class SignUpUiState(
    val username: String = "",
    val email: String = "",
    val password: String = "",
    val confirmPassword: String = "",
    val isPasswordVisible: Boolean = false,
    val isConfirmPasswordVisible: Boolean = false,
    val isLoading: Boolean = false,
    val error: UiText? = null,
    val isSuccess: Boolean = false
)

@HiltViewModel
class SignUpViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    private val userRepository: IUserRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(SignUpUiState())
    val uiState: StateFlow<SignUpUiState> = _uiState.asStateFlow()

    fun onUsernameChanged(name: String) {
        _uiState.update { it.copy(username = name, error = null) }
    }

    fun onEmailChanged(email: String) {
        _uiState.update { it.copy(email = email, error = null) }
    }

    fun onPasswordChanged(pass: String) {
        _uiState.update { it.copy(password = pass, error = null) }
    }

    fun onConfirmPasswordChanged(confPass: String) {
        _uiState.update { it.copy(confirmPassword = confPass, error = null) }
    }

    fun togglePasswordVisibility() {
        _uiState.update { it.copy(isPasswordVisible = !it.isPasswordVisible) }
    }

    fun toggleConfirmPasswordVisibility() {
        _uiState.update { it.copy(isConfirmPasswordVisible = !it.isConfirmPasswordVisible) }
    }

    private fun mapFirebaseAuthError(throwable: Throwable): UiText {
        val msg = throwable.message?.lowercase() ?: ""
        return when {
            "email_already_in_use" in msg || "already in use" in msg ->
                UiText.StringResource(R.string.error_signup_email_in_use)
            "weak_password" in msg ->
                UiText.StringResource(R.string.error_signup_weak_password)
            "invalid_email" in msg ->
                UiText.StringResource(R.string.error_invalid_email)
            "network" in msg ->
                UiText.StringResource(R.string.error_no_connection)
            "too_many_requests" in msg || "too many" in msg ->
                UiText.StringResource(R.string.error_login_too_many_attempts)
            else -> UiText.StringResource(R.string.error_signup_failed)
        }
    }

    fun onSignUpClick() {
        val state = _uiState.value

        if (state.email.isBlank() || state.password.isBlank() ||
            state.username.isBlank() || state.confirmPassword.isBlank()
        ) {
            _uiState.update { it.copy(error = UiText.StringResource(R.string.error_empty_fields)) }
            return
        }
        if (state.username.length < 3 || state.username.length > 20) {
            _uiState.update { it.copy(error = UiText.StringResource(R.string.error_username_length)) }
            return
        }
        if (!state.username.matches(Regex("^[a-zA-Z0-9_]+$"))) {
            _uiState.update { it.copy(error = UiText.StringResource(R.string.error_username_format)) }
            return
        }
        if (!Patterns.EMAIL_ADDRESS.matcher(state.email).matches()) {
            _uiState.update { it.copy(error = UiText.StringResource(R.string.error_invalid_email)) }
            return
        }
        if (state.password.length < 8) {
            _uiState.update { it.copy(error = UiText.StringResource(R.string.error_password_length)) }
            return
        }
        if (!state.password.any { it.isUpperCase() }) {
            _uiState.update { it.copy(error = UiText.StringResource(R.string.error_password_uppercase)) }
            return
        }
        if (!state.password.any { it.isDigit() }) {
            _uiState.update { it.copy(error = UiText.StringResource(R.string.error_password_digit)) }
            return
        }
        if (state.password != state.confirmPassword) {
            _uiState.update { it.copy(error = UiText.StringResource(R.string.error_passwords_dont_match)) }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }

            authRepository.signUp(state.email, state.password)
                .onSuccess {
                    runCatching { userRepository.initUserProfile(state.username) }
                        .onFailure {
                            _uiState.update {
                                it.copy(isLoading = false, isSuccess = true,
                                    error = UiText.StringResource(R.string.error_profile_init))
                            }
                            return@launch
                        }
                    _uiState.update { it.copy(isLoading = false, isSuccess = true) }
                }
                .onFailure { throwable ->
                    _uiState.update {
                        it.copy(isLoading = false, error = mapFirebaseAuthError(throwable))
                    }
                }
        }
    }
}
