package com.example.showmateapp.ui.screens.login

import com.example.showmateapp.util.UiText

data class LoginUiState(
    val email: String = "",
    val password: String = "",
    val isPasswordVisible: Boolean = false,
    val isLoading: Boolean = false,
    val error: UiText? = null,
    val isSuccess: Boolean = false
)