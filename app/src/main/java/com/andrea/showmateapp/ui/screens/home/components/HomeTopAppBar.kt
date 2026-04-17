package com.andrea.showmateapp.ui.screens.home.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.andrea.showmateapp.R
import com.andrea.showmateapp.ui.components.premium.ShowMateSpacing
import com.andrea.showmateapp.ui.theme.PrimaryMagenta
import com.andrea.showmateapp.ui.theme.PrimaryPurple
import com.andrea.showmateapp.ui.theme.PrimaryPurpleLight
import java.util.Calendar

@Composable
fun greeting(): String {
    val hour by remember { derivedStateOf { Calendar.getInstance().get(Calendar.HOUR_OF_DAY) } }
    return when {
        hour < 12 -> stringResource(R.string.home_greeting_morning)
        hour < 20 -> stringResource(R.string.home_greeting_afternoon)
        else -> stringResource(R.string.home_greeting_night)
    }
}

@Composable
fun HomeTopAppBar(userName: String = "", onPickWhatToWatch: () -> Unit = {}, onAvatarClick: () -> Unit = {}) {
    val gradientColors = listOf(PrimaryPurple, PrimaryMagenta)

    val infiniteTransition = rememberInfiniteTransition(label = "wtwGlow")
    val glowAlpha by infiniteTransition.animateFloat(
        initialValue = 0.12f,
        targetValue = 0.28f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "glowAlpha"
    )

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .statusBarsPadding()
            .padding(horizontal = ShowMateSpacing.m, vertical = ShowMateSpacing.s),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "ShowMate",
                style = TextStyle(brush = Brush.linearGradient(colors = gradientColors)),
                fontSize = 28.sp,
                fontWeight = FontWeight.Black,
                letterSpacing = (-1.5).sp
            )
            Box(
                modifier = Modifier
                    .padding(top = 2.dp)
                    .width(42.dp)
                    .height(3.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(Brush.linearGradient(colors = gradientColors))
            )
        }

        Box(
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .size(38.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(
                    Brush.linearGradient(
                        listOf(
                            PrimaryPurple.copy(alpha = glowAlpha + 0.15f),
                            PrimaryMagenta.copy(alpha = glowAlpha + 0.05f)
                        )
                    )
                )
                .border(
                    1.dp,
                    Brush.linearGradient(
                        listOf(PrimaryPurpleLight.copy(alpha = 0.45f), Color.Transparent)
                    ),
                    RoundedCornerShape(12.dp)
                )
                .clickable(
                    onClickLabel = stringResource(R.string.home_what_to_watch),
                    role = Role.Button,
                    onClick = onPickWhatToWatch
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.AutoAwesome,
                contentDescription = stringResource(R.string.home_what_to_watch),
                tint = Color.White,
                modifier = Modifier.size(18.dp)
            )
        }
    }
}
