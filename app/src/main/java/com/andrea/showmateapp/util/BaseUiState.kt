package com.andrea.showmateapp.util

import androidx.compose.runtime.Immutable

/**
 * Contrato base para los UiState de todos los ViewModels.
 *
 * Implementar esta interfaz homogeneiza el handling de loading/error
 * entre pantallas y facilita la creación de componentes genéricos.
 *
 * Uso:
 * ```kotlin
 * @Immutable
 * data class HomeUiState(
 *     override val isLoading: Boolean = true,
 *     override val errorMessage: UiText? = null,
 *     val shows: List<MediaContent> = emptyList()
 * ) : BaseUiState
 * ```
 */
@Immutable
interface BaseUiState {
    val isLoading: Boolean
    val errorMessage: UiText?

    /** true si errorMessage != null */
    val hasError: Boolean get() = errorMessage != null

    /** true si no está cargando ni tiene error — contenido listo para mostrar */
    val isReady: Boolean get() = !isLoading && !hasError
}
