package com.example.showmateapp.ui.screens.signup

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.showmateapp.data.repository.AuthRepository
import com.example.showmateapp.data.repository.UserRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

    @HiltViewModel
    class SignUpViewModel @Inject constructor(
        private val authRepository: AuthRepository,
        private val userRepository: UserRepository
    ) : ViewModel() {

        private val _uiState = MutableStateFlow(SignUpUiState())
        val uiState: StateFlow<SignUpUiState> = _uiState.asStateFlow()

        fun onUsernameChanged(name: String) {
            _uiState.value = _uiState.value.copy(username = name, error = null)
        }

        fun onEmailChanged(email: String) {
            _uiState.value = _uiState.value.copy(email = email, error = null)
        }

        fun onPasswordChanged(pass: String) {
            _uiState.value = _uiState.value.copy(password = pass, error = null)
        }
        
        fun onConfirmPasswordChanged(confPass: String) {
            _uiState.value = _uiState.value.copy(confirmPassword = confPass, error = null)
        }

        fun togglePasswordVisibility() {
            _uiState.value = _uiState.value.copy(isPasswordVisible = !_uiState.value.isPasswordVisible)
        }
        
        fun toggleConfirmPasswordVisibility() {
            _uiState.value = _uiState.value.copy(isConfirmPasswordVisible = !_uiState.value.isConfirmPasswordVisible)
        }

        // Quita el "(onSuccess: () -> Unit)" de aquí:
        fun onSignUpClick() {
            val state = _uiState.value
            
            // Validation
            if (state.email.isBlank() || state.password.isBlank() || state.username.isBlank() || state.confirmPassword.isBlank()) {
                _uiState.value = _uiState.value.copy(error = "Completa todos los campos")
                return
            }
            if (!android.util.Patterns.EMAIL_ADDRESS.matcher(state.email).matches()) {
                _uiState.value = _uiState.value.copy(error = "Formato de correo inválido")
                return
            }
            if (state.password.length < 6) {
                _uiState.value = _uiState.value.copy(error = "La contraseña debe tener al menos 6 caracteres")
                return
            }
            if (state.password != state.confirmPassword) {
                _uiState.value = _uiState.value.copy(error = "Las contraseñas no coinciden")
                return
            }

            viewModelScope.launch {
                _uiState.value = _uiState.value.copy(isLoading = true, error = null)
                val authResult = authRepository.signUp(state.email, state.password)

                if (authResult.isSuccess) {
                    // 2. Aquí es donde marcamos el éxito
                    _uiState.value = _uiState.value.copy(isLoading = false, isSuccess = true)
                } else {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        error = authResult.exceptionOrNull()?.message ?: "Error al registrarse"
                    )
                }
            }
        }

        data class SignUpUiState(
            val username: String = "",
            val email: String = "",
            val password: String = "",
            val confirmPassword: String = "",
            val isPasswordVisible: Boolean = false,
            val isConfirmPasswordVisible: Boolean = false,
            val isLoading: Boolean = false,
            val error: String? = null,
            val isSuccess: Boolean = false
        )
    }