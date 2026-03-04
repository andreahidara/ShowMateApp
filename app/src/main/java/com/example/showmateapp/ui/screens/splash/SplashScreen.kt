package com.example.showmateapp.ui.screens.splash

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import androidx.navigation.compose.rememberNavController
import com.example.showmateapp.R
import com.example.showmateapp.ui.theme.PrimaryPurple
import com.example.showmateapp.ui.theme.ShowMateAppTheme
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun SplashScreen(navController: NavController) {
    // Entrance animations
    val scale = remember { Animatable(0.8f) }
    val alpha = remember { Animatable(0f) }
    val contentTranslationY = remember { Animatable(20f) }

    // Subtle "breathing" effect for the logo and tagline
    val infiniteTransition = rememberInfiniteTransition(label = "breathing")
    val breathingScale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.05f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "scale"
    )

    LaunchedEffect(key1 = true) {
        launch {
            scale.animateTo(
                targetValue = 1f,
                animationSpec = tween(1200, easing = FastOutSlowInEasing)
            )
        }
        launch {
            alpha.animateTo(
                targetValue = 1f,
                animationSpec = tween(1000)
            )
        }
        launch {
            contentTranslationY.animateTo(
                targetValue = 0f,
                animationSpec = tween(1200, easing = FastOutSlowInEasing)
            )
        }

        delay(3000)

        val currentUser = FirebaseAuth.getInstance().currentUser
        val destination = if (currentUser != null) "main" else "login"
        navController.navigate(destination) {
            popUpTo("splash") { inclusive = true }
        }
    }

    // Professional Background: Deep dark vignette with a subtle purple core
    val backgroundBrush = Brush.radialGradient(
        colors = listOf(
            Color(0xFF1A1A2E), // Rich dark navy/purple
            Color(0xFF08080D)  // Deep cinematic black
        ),
        radius = 1600f
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(brush = backgroundBrush),
        contentAlignment = Alignment.Center
    ) {
        // Decorative background glow for depth
        Box(
            modifier = Modifier
                .size(400.dp)
                .graphicsLayer(alpha = 0.15f)
                .background(
                    brush = Brush.radialGradient(
                        colors = listOf(PrimaryPurple, Color.Transparent)
                    )
                )
                .blur(80.dp)
        )

        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier
                .graphicsLayer(
                    scaleX = scale.value * breathingScale,
                    scaleY = scale.value * breathingScale,
                    alpha = alpha.value,
                    translationY = contentTranslationY.value
                )
        ) {
            // Branded Logo Container
            Box(contentAlignment = Alignment.Center) {
                // Secondary glow behind logo
                Box(
                    modifier = Modifier
                        .size(120.dp)
                        .graphicsLayer(alpha = 0.3f)
                        .background(
                            brush = Brush.radialGradient(
                                colors = listOf(PrimaryPurple, Color.Transparent)
                            )
                        )
                )
                
                Image(
                    painter = painterResource(id = R.drawable.logo),
                    contentDescription = null,
                    modifier = Modifier.size(100.dp)
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Brand Name: Bold, Clean, Modern
            Text(
                text = "ShowMate",
                color = Color.White,
                style = MaterialTheme.typography.displayMedium.copy(
                    fontWeight = FontWeight.ExtraBold,
                    letterSpacing = (-0.5).sp // Tighter kerning for a modern look
                )
            )

            Spacer(modifier = Modifier.height(6.dp))

            // Cinematic Tagline
            Text(
                text = "SERIES GUIDE",
                color = PrimaryPurple.copy(alpha = 0.85f),
                style = MaterialTheme.typography.labelLarge.copy(
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 6.sp,
                    fontSize = 12.sp
                )
            )
        }

        // Refined Footer
        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 40.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "Menos buscar, más disfrutar",
                color = Color.White.copy(alpha = 0.2f),
                style = MaterialTheme.typography.labelSmall.copy(
                    letterSpacing = 2.sp
                )
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "v1.0.0",
                color = Color.White.copy(alpha = 0.15f),
                style = MaterialTheme.typography.labelSmall
            )
        }
    }
}

@Preview(showBackground = true, showSystemUi = true)
@Composable
fun SplashScreenPreview() {
    ShowMateAppTheme {
        SplashScreen(navController = rememberNavController())
    }
}
