package com.andrea.showmateapp.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import com.andrea.showmateapp.ui.theme.BackgroundDark
import com.andrea.showmateapp.ui.theme.GradientPurpleEnd
import com.andrea.showmateapp.ui.theme.SurfaceDark

@Composable
fun AuthBackground(content: @Composable BoxScope.() -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        SurfaceDark,
                        BackgroundDark,
                        GradientPurpleEnd.copy(alpha = 0.2f)
                    )
                )
            ),
        content = content
    )
}
