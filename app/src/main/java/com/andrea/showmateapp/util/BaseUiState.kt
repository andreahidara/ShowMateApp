package com.andrea.showmateapp.util

import androidx.compose.runtime.Immutable

@Immutable
interface BaseUiState {
    val isLoading: Boolean
    val errorMessage: UiText?

    val hasError: Boolean get() = errorMessage != null

    val isReady: Boolean get() = !isLoading && !hasError
}
