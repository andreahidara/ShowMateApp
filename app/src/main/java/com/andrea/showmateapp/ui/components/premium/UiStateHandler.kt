package com.andrea.showmateapp.ui.components.premium

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.*
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.andrea.showmateapp.ui.theme.*
import com.andrea.showmateapp.util.ErrorType
import com.andrea.showmateapp.util.Resource

@Composable
fun ErrorContent(
    type: ErrorType,
    onRetry: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val (icon, tint, title) = errorUiConfig(type)

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        AnimatedErrorIcon(icon = icon, tint = tint)

        Spacer(Modifier.height(20.dp))

        Text(
            text = title,
            color = Color.White,
            fontSize = 20.sp,
            fontWeight = FontWeight.ExtraBold,
            textAlign = TextAlign.Center
        )

        Spacer(Modifier.height(8.dp))

        Text(
            text = type.defaultMessage,
            color = TextGray,
            fontSize = 14.sp,
            textAlign = TextAlign.Center,
            lineHeight = 20.sp
        )

        if (onRetry != null && type.isRetryable) {
            Spacer(Modifier.height(28.dp))
            Button(
                onClick = onRetry,
                colors = ButtonDefaults.buttonColors(containerColor = PrimaryPurple),
                shape = RoundedCornerShape(12.dp),
                contentPadding = PaddingValues(horizontal = 32.dp, vertical = 14.dp)
            ) {
                Icon(
                    imageVector = Icons.Outlined.Refresh,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(Modifier.width(8.dp))
                Text("Reintentar", fontWeight = FontWeight.Bold, fontSize = 15.sp)
            }
        }
    }
}

@Composable
private fun AnimatedErrorIcon(icon: ImageVector, tint: Color) {
    val infiniteTransition = rememberInfiniteTransition(label = "error_icon")
    val scale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.08f,
        animationSpec = infiniteRepeatable(
            tween(900, easing = EaseInOutSine),
            RepeatMode.Reverse
        ),
        label = "icon_pulse"
    )

    Box(
        modifier = Modifier
            .size(80.dp)
            .scale(scale)
            .clip(CircleShape)
            .background(tint.copy(alpha = 0.12f)),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = tint,
            modifier = Modifier.size(40.dp)
        )
    }
}

private data class ErrorUiConfig(val icon: ImageVector, val tint: Color, val title: String)

private fun errorUiConfig(type: ErrorType): ErrorUiConfig = when (type) {
    is ErrorType.Network -> ErrorUiConfig(
        icon = Icons.Outlined.WifiOff,
        tint = AccentBlue,
        title = "Sin conexión"
    )
    is ErrorType.Server -> ErrorUiConfig(
        icon = Icons.Outlined.CloudOff,
        tint = ErrorRed,
        title = "Error del servidor"
    )
    is ErrorType.Auth -> ErrorUiConfig(
        icon = Icons.Outlined.LockPerson,
        tint = StarYellow,
        title = "Sesión caducada"
    )
    is ErrorType.Data -> ErrorUiConfig(
        icon = Icons.Outlined.BrokenImage,
        tint = PrimaryPurple,
        title = "Error al cargar"
    )
    else -> ErrorUiConfig(
        icon = Icons.Outlined.ErrorOutline,
        tint = PrimaryPurple,
        title = "¡Ups! Algo salió mal"
    )
}

@Composable
fun EmptyStateContent(
    message: String = "No hay contenido disponible",
    icon: ImageVector = Icons.Outlined.Inbox,
    actionLabel: String? = null,
    onAction: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        val infiniteTransition = rememberInfiniteTransition(label = "empty_float")
        val offsetY by infiniteTransition.animateFloat(
            initialValue = 0f,
            targetValue = -10f,
            animationSpec = infiniteRepeatable(
                tween(1400, easing = EaseInOutSine),
                RepeatMode.Reverse
            ),
            label = "float_y"
        )

        Box(
            modifier = Modifier
                .size(80.dp)
                .offset(y = offsetY.dp)
                .clip(CircleShape)
                .background(PrimaryPurple.copy(alpha = 0.12f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = PrimaryPurpleLight,
                modifier = Modifier.size(40.dp)
            )
        }

        Spacer(Modifier.height(20.dp))

        Text(
            text = message,
            color = TextGray,
            fontSize = 15.sp,
            textAlign = TextAlign.Center,
            lineHeight = 22.sp
        )

        if (actionLabel != null && onAction != null) {
            Spacer(Modifier.height(20.dp))
            OutlinedButton(
                onClick = onAction,
                border = androidx.compose.foundation.BorderStroke(1.dp, PrimaryPurple),
                shape = RoundedCornerShape(10.dp)
            ) {
                Text(actionLabel, color = PrimaryPurpleLight, fontWeight = FontWeight.SemiBold)
            }
        }
    }
}

@Composable
fun <T> UiStateHandler(
    resource: Resource<T>,
    onRetry: (() -> Unit)? = null,
    loadingContent: @Composable () -> Unit = { DefaultLoadingContent() },
    emptyContent: @Composable () -> Unit = { EmptyStateContent() },
    content: @Composable (T) -> Unit
) {
    AnimatedContent(
        targetState = resource,
        transitionSpec = {
            fadeIn(tween(300)) + scaleIn(tween(300), initialScale = 0.97f) togetherWith
                fadeOut(tween(200))
        },
        contentKey = { it::class },
        label = "ui_state_handler"
    ) { state ->
        when (state) {
            is Resource.Loading -> loadingContent()
            is Resource.Empty   -> emptyContent()
            is Resource.Error   -> ErrorContent(type = state.type, onRetry = onRetry)
            is Resource.Success -> content(state.data)
        }
    }
}

@Composable
private fun DefaultLoadingContent() {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        CircularProgressIndicator(color = PrimaryPurple)
    }
}
