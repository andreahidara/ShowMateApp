package com.example.showmateapp.ui.screens.splash

import androidx.compose.animation.core.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.*
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
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun SplashScreen(
    onNavigateToMain: () -> Unit,
    onNavigateToLogin: () -> Unit
) {
    val logoAlpha = remember { Animatable(0f) }
    val titleAlpha = remember { Animatable(0f) }
    val subtitleAlpha = remember { Animatable(0f) }
    
    val logoOffsetY = remember { Animatable(60f) }
    val textOffsetY = remember { Animatable(40f) }
    
    val scale = remember { Animatable(0.8f) }
    
    // Animación de "respiración" para el logo después de entrar
    val infiniteTransition = rememberInfiniteTransition(label = "breathing")
    val breatheScale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.05f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "breatheScale"
    )

    LaunchedEffect(key1 = true) {
        // Secuencia de entrada
        launch { 
            logoAlpha.animateTo(1f, animationSpec = tween(1000, easing = LinearOutSlowInEasing)) 
        }
        launch { 
            logoOffsetY.animateTo(0f, animationSpec = spring(dampingRatio = 0.6f, stiffness = Spring.StiffnessLow)) 
        }
        launch { 
            scale.animateTo(1f, animationSpec = tween(1200, easing = FastOutSlowInEasing)) 
        }
        
        delay(400)
        
        launch { 
            titleAlpha.animateTo(1f, animationSpec = tween(800, easing = LinearOutSlowInEasing)) 
        }
        launch { 
            subtitleAlpha.animateTo(0.6f, animationSpec = tween(800, easing = LinearOutSlowInEasing)) 
        }
        launch { 
            textOffsetY.animateTo(0f, animationSpec = spring(dampingRatio = 0.8f, stiffness = Spring.StiffnessLow)) 
        }

        // Espera mínima para que el usuario vea el logo (2.5 segundos total)
        delay(2500)

        // Verificación de Auth
        val auth = FirebaseAuth.getInstance()
        if (auth.currentUser != null) {
            onNavigateToMain()
        } else {
            onNavigateToLogin()
        }
    }

    AuthBackground {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .scale(scale.value * breatheScale)
                    .padding(bottom = 40.dp)
            ) {
                // Sombra sutil o resplandor se podría añadir aquí con un Box de fondo
                Image(
                    painter = painterResource(id = R.drawable.logo),
                    contentDescription = "Logo ShowMate",
                    modifier = Modifier
                        .size(160.dp)
                        .offset(y = logoOffsetY.value.dp)
                        .alpha(logoAlpha.value)
                )

                Spacer(modifier = Modifier.height(24.dp))

                Text(
                    text = "ShowMate",
                    color = Color.White,
                    fontSize = 48.sp,
                    fontWeight = FontWeight.Black,
                    letterSpacing = (-1).sp,
                    modifier = Modifier
                        .offset(y = textOffsetY.value.dp)
                        .alpha(titleAlpha.value)
                )

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = "TU GUÍA PREMIUM DE SERIES",
                    color = PrimaryPurple,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.ExtraBold,
                    letterSpacing = 5.sp,
                    modifier = Modifier
                        .offset(y = textOffsetY.value.dp)
                        .alpha(subtitleAlpha.value)
                )
            }

            // Indicador de carga muy sutil en la parte inferior
            Box(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 80.dp)
                    .width(150.dp)
                    .alpha(subtitleAlpha.value)
            ) {
                LinearProgressIndicator(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(2.dp),
                    color = PrimaryPurple,
                    trackColor = Color.White.copy(alpha = 0.1f)
                )
            }
            
            // Footer de marca
            Text(
                text = "Powered by TMDB",
                color = Color.White.copy(alpha = 0.3f),
                fontSize = 10.sp,
                fontWeight = FontWeight.Medium,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 32.dp)
                    .alpha(titleAlpha.value)
            )
        }
    }
}
