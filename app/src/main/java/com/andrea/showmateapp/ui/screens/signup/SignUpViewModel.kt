package com.andrea.showmateapp.ui.screens.signup

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

    fun onSignUpClick() {
        val state = _uiState.value
        
        if (state.email.isBlank() || state.password.isBlank() || state.username.isBlank() || state.confirmPassword.isBlank()) {
            _uiState.update { it.copy(error = UiText.StringResource(R.string.error_empty_fields)) }
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
                    userRepository.initUserProfile(state.username)
                    _uiState.update { it.copy(isLoading = false, isSuccess = true) }
                }
                .onFailure { throwable ->
                    _uiState.update { it.copy(
                        isLoading = false,
                        error = throwable.message?.let { UiText.DynamicString(it) } 
                            ?: UiText.StringResource(R.string.error_signup_failed)
                    ) }
                }
        }
    }
}
