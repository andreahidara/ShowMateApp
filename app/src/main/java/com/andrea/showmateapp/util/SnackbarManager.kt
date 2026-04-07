package com.andrea.showmateapp.util

import androidx.compose.material3.SnackbarDuration
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow

data class SnackbarEvent(
    val message: String,
    val actionLabel: String? = null,
    val duration: SnackbarDuration = SnackbarDuration.Short
)

object SnackbarManager {

    private val _events = MutableSharedFlow<SnackbarEvent>(
        extraBufferCapacity = 1,
        onBufferOverflow = BufferOverflow.DROP_OLDEST
    )
    val events: SharedFlow<SnackbarEvent> = _events.asSharedFlow()

    fun showError(message: String, actionLabel: String? = null) {
        _events.tryEmit(SnackbarEvent(message, actionLabel, SnackbarDuration.Long))
    }

    fun showMessage(message: String, actionLabel: String? = null) {
        _events.tryEmit(SnackbarEvent(message, actionLabel, SnackbarDuration.Short))
    }

    fun showError(type: ErrorType) = showError(type.defaultMessage)
}
