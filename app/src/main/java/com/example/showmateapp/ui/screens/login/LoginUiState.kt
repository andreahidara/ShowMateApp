package com.example.showmateapp.ui.screens.login

data class LoginUiState(
    val email: String = "",
    val password: String = "",
    val isLoading: Boolean = false,
    val error: String? = null,
    val isSuccess: Boolean = false,
    val isPasswordVisible: Boolean = false
)