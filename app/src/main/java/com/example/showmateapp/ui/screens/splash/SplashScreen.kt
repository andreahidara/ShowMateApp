package com.example.showmateapp.ui.screens.splash

import androidx.compose.animation.core.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.showmateapp.R
import com.example.showmateapp.ui.components.premium.AuthBackground
import com.example.showmateapp.ui.theme.PrimaryPurple
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun SplashScreen(onTimeout: () -> Unit) {
    val logoAlpha = remember { Animatable(0f) }
    val titleAlpha = remember { Animatable(0f) }
    val subtitleAlpha = remember { Animatable(0f) }
    
    // Offset for entry animation
    val logoOffsetY = remember { Animatable(50f) }
    val textOffsetY = remember { Animatable(30f) }
    
    val scale = remember { Animatable(0.9f) }

    LaunchedEffect(key1 = true) {
        // Parallel animations
        launch { logoAlpha.animateTo(1f, animationSpec = tween(durationMillis = 800, easing = LinearOutSlowInEasing)) }
        launch { logoOffsetY.animateTo(0f, animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow)) }
        launch { scale.animateTo(1f, animationSpec = tween(durationMillis = 1000, easing = FastOutSlowInEasing)) }
        
        delay(300)
        
        launch { titleAlpha.animateTo(1f, animationSpec = tween(durationMillis = 600, easing = LinearOutSlowInEasing)) }
        launch { subtitleAlpha.animateTo(1f, animationSpec = tween(durationMillis = 600, easing = LinearOutSlowInEasing)) }
        launch { textOffsetY.animateTo(0f, animationSpec = spring(dampingRatio = Spring.DampingRatioNoBouncy, stiffness = Spring.StiffnessLow)) }

        delay(1500)

        onTimeout()
    }

    AuthBackground {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.scale(scale.value)
            ) {
                // LOGO
                Image(
                    painter = painterResource(id = R.drawable.logo),
                    contentDescription = "Logo ShowMate",
                    modifier = Modifier
                        .size(140.dp)
                        .offset(y = logoOffsetY.value.dp)
                        .alpha(logoAlpha.value)
                )

                Spacer(modifier = Modifier.height(32.dp))

                // TÍTULO
                Text(
                    text = "ShowMate",
                    color = Color.White,
                    fontSize = 42.sp,
                    fontWeight = FontWeight.ExtraBold,
                    modifier = Modifier
                        .offset(y = textOffsetY.value.dp)
                        .alpha(titleAlpha.value)
                )

                Spacer(modifier = Modifier.height(8.dp))

                // ESLOGAN / SUBTÍTULO
                Text(
                    text = "PREMIUM SERIES GUIDE",
                    color = PrimaryPurple.copy(alpha = 0.8f),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 6.sp,
                    modifier = Modifier
                        .offset(y = textOffsetY.value.dp)
                        .alpha(subtitleAlpha.value)
                )
            }
        }
    }
}