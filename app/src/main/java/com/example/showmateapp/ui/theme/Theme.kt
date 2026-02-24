package com.example.showmateapp.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable

// Tu paleta de colores oscuros
private val DarkColorScheme = darkColorScheme(
    primary = PrimaryPurple,
    secondary = AccentBlue,
    background = BackgroundDark,
    surface = SurfaceDark,
    onPrimary = TextWhite,
    onBackground = TextWhite,
    onSurface = TextWhite
)

@Composable
fun ShowMateAppTheme(
    // Quitamos la lógica dinámica y forzamos directamente tu tema oscuro
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = DarkColorScheme,
        typography = Typography, // Asegúrate de tener el archivo Type.kt por defecto en tu carpeta theme
        content = content
    )
}