package com.example.showmateapp.ui.screens.splash

import androidx.compose.animation.core.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.showmateapp.R
import com.example.showmateapp.ui.components.premium.AuthBackground
import com.example.showmateapp.ui.theme.PrimaryPurple
import com.example.showmateapp.ui.theme.PrimaryPurpleDark
import com.example.showmateapp.ui.theme.PrimaryPurpleLight
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun SplashScreen(
    onNavigateToMain: () -> Unit,
    onNavigateToLogin: () -> Unit,
    viewModel: SplashViewModel = hiltViewModel()
) {
    val logoAlpha = remember { Animatable(0f) }
    val logoScale = remember { Animatable(0.4f) }
    val glowAlpha = remember { Animatable(0f) }
    val titleAlpha = remember { Animatable(0f) }
    val titleOffsetY = remember { Animatable(24f) }
    val subtitleAlpha = remember { Animatable(0f) }
    val bottomAlpha = remember { Animatable(0f) }

    val infiniteTransition = rememberInfiniteTransition(label = "idle")

    val glowPulse by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.18f,
        animationSpec = infiniteRepeatable(
            animation = tween(2400, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "glowPulse"
    )
    val logoBreath by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.035f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "logoBreath"
    )
    val dot1Alpha by infiniteTransition.animateFloat(
        initialValue = 0.25f, targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(550, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse,
            initialStartOffset = StartOffset(0)
        ), label = "d1"
    )
    val dot2Alpha by infiniteTransition.animateFloat(
        initialValue = 0.25f, targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(550, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse,
            initialStartOffset = StartOffset(183)
        ), label = "d2"
    )
    val dot3Alpha by infiniteTransition.animateFloat(
        initialValue = 0.25f, targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(550, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse,
            initialStartOffset = StartOffset(366)
        ), label = "d3"
    )

    LaunchedEffect(Unit) {
        launch { glowAlpha.animateTo(0.55f, tween(1400, easing = LinearOutSlowInEasing)) }
        launch { logoAlpha.animateTo(1f, tween(900, easing = LinearOutSlowInEasing)) }
        launch {
            logoScale.animateTo(
                1f,
                spring(dampingRatio = 0.45f, stiffness = Spring.StiffnessMediumLow)
            )
        }
        delay(520)
        launch { titleAlpha.animateTo(1f, tween(700, easing = LinearOutSlowInEasing)) }
        launch { titleOffsetY.animateTo(0f, spring(dampingRatio = 0.65f, stiffness = Spring.StiffnessLow)) }
        delay(220)
        launch { subtitleAlpha.animateTo(0.75f, tween(600, easing = LinearOutSlowInEasing)) }
        launch { bottomAlpha.animateTo(1f, tween(800, easing = LinearOutSlowInEasing)) }
        delay(2500)
        try {
            if (viewModel.isLoggedIn()) onNavigateToMain() else onNavigateToLogin()
        } catch (e: Exception) {
            onNavigateToLogin()
        }
    }

    AuthBackground {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Box(
                modifier = Modifier
                    .offset(y = (-60).dp)
                    .size(260.dp)
                    .scale(glowPulse)
                    .alpha(glowAlpha.value)
                    .blur(50.dp)
                    .background(
                        Brush.radialGradient(
                            listOf(
                                PrimaryPurple.copy(alpha = 0.55f),
                                PrimaryPurpleDark.copy(alpha = 0.25f),
                                Color.Transparent
                            )
                        ),
                        CircleShape
                    )
            )

            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
                modifier = Modifier.offset(y = (-20).dp)
            ) {
                Image(
                    painter = painterResource(id = R.drawable.logo),
                    contentDescription = "Logo ShowMate",
                    modifier = Modifier
                        .size(164.dp)
                        .scale(logoScale.value * logoBreath)
                        .alpha(logoAlpha.value)
                )

                Spacer(Modifier.height(32.dp))

                Text(
                    text = "ShowMate",
                    color = Color.White,
                    fontSize = 50.sp,
                    fontWeight = FontWeight.Black,
                    letterSpacing = (-1.5).sp,
                    modifier = Modifier
                        .offset(y = titleOffsetY.value.dp)
                        .alpha(titleAlpha.value)
                )

                Spacer(Modifier.height(8.dp))

                Text(
                    text = "TU GUÍA PREMIUM DE SERIES",
                    color = PrimaryPurpleLight,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.ExtraBold,
                    letterSpacing = 4.sp,
                    modifier = Modifier
                        .offset(y = titleOffsetY.value.dp)
                        .alpha(subtitleAlpha.value)
                )
            }

            Row(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 80.dp)
                    .alpha(bottomAlpha.value),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                listOf(dot1Alpha, dot2Alpha, dot3Alpha).forEach { alpha ->
                    Box(
                        modifier = Modifier
                            .size(7.dp)
                            .alpha(alpha)
                            .background(PrimaryPurple, CircleShape)
                    )
                }
            }

            Text(
                text = "Powered by TMDB",
                color = Color.White.copy(alpha = 0.22f),
                fontSize = 10.sp,
                fontWeight = FontWeight.Medium,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 32.dp)
                    .alpha(bottomAlpha.value)
            )
        }
    }
}
