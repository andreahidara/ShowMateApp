package com.example.showmateapp.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable

private val DarkColorScheme = darkColorScheme(
    primary = PrimaryPurple,
    secondary = AccentBlue,
    background = BackgroundDark,
    surface = SurfaceDark,
    surfaceVariant = SurfaceVariantDark,
    onPrimary = TextWhite,
    onBackground = TextWhite,
    onSurface = TextWhite,
    onSurfaceVariant = TextGray
)

// Navy/indigo variant — keeps dark palette so all Color.White text stays readable
private val NavyColorScheme = darkColorScheme(
    primary = PrimaryPurple,
    secondary = AccentBlue,
    background = BackgroundLight,
    surface = SurfaceLight,
    surfaceVariant = SurfaceVariantLight,
    onPrimary = TextWhite,
    onBackground = TextDark,
    onSurface = TextDark,
    onSurfaceVariant = TextGrayLight
)

@Composable
fun ShowMateAppTheme(
    darkTheme: Boolean = true,
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = if (darkTheme) DarkColorScheme else NavyColorScheme,
        typography = Typography,
        content = content
    )
}
