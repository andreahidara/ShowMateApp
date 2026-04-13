package com.andrea.showmateapp.util

sealed class ErrorType {
    open val defaultMessage: String = "Ha ocurrido un error inesperado"
    open val isCritical: Boolean = false
    open val isRetryable: Boolean = true

    object Network : ErrorType() {
        override val defaultMessage = "Sin conexión a internet. Comprueba tu red."
    }

    data class Server(val code: Int) : ErrorType() {
        override val defaultMessage = "Error del servidor ($code). Inténtalo más tarde."
        override val isCritical = code >= 500
        override val isRetryable = code >= 500
    }

    object Auth : ErrorType() {
        override val defaultMessage = "Sesión caducada. Vuelve a iniciar sesión."
        override val isCritical = true
        override val isRetryable = false
    }

    object Data : ErrorType() {
        override val defaultMessage = "Error al cargar el contenido. Comprueba tu conexión."
        override val isRetryable = true
    }

    object Unknown : ErrorType()
}
