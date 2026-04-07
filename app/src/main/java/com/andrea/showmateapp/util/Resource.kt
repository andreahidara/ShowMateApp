package com.andrea.showmateapp.util

sealed class Resource<out T> {
    object Loading : Resource<Nothing>()
    data class Success<out T>(val data: T) : Resource<T>()
    data class Empty(val message: String? = null) : Resource<Nothing>()
    data class Error(
        val message: String = "",
        val type: ErrorType = ErrorType.Unknown,
        val throwable: Throwable? = null
    ) : Resource<Nothing>() {
        val effectiveMessage: String
            get() = message.ifBlank { type.defaultMessage }
    }
}
