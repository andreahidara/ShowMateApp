package com.example.showmateapp.ui.screens.profile.about

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.example.showmateapp.R
import com.example.showmateapp.ui.theme.PrimaryPurple
import com.example.showmateapp.ui.theme.PrimaryPurpleDark
import com.example.showmateapp.ui.theme.SurfaceVariantDark
import com.example.showmateapp.ui.theme.TextGray

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AboutScreen(navController: NavController) {
    val infiniteTransition = rememberInfiniteTransition(label = "aboutGlow")
    val orb1Alpha by infiniteTransition.animateFloat(
        initialValue = 0.20f, targetValue = 0.42f,
        animationSpec = infiniteRepeatable(tween(3200, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "orb1"
    )
    val orb2Alpha by infiniteTransition.animateFloat(
        initialValue = 0.15f, targetValue = 0.32f,
        animationSpec = infiniteRepeatable(tween(2600, easing = FastOutSlowInEasing, delayMillis = 900), RepeatMode.Reverse),
        label = "orb2"
    )
    val ringAlpha by infiniteTransition.animateFloat(
        initialValue = 0.20f, targetValue = 0.50f,
        animationSpec = infiniteRepeatable(tween(2000, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "ring"
    )

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Acerca de", color = Color.White, fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Volver", tint = Color.White)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { paddingValues ->
        Box(modifier = Modifier.fillMaxSize().padding(paddingValues)) {
            // Background orbs
            Box(
                modifier = Modifier
                    .size(280.dp)
                    .offset(x = (-80).dp, y = (-40).dp)
                    .alpha(orb1Alpha)
                    .blur(90.dp)
                    .background(PrimaryPurple.copy(alpha = 0.5f), CircleShape)
            )
            Box(
                modifier = Modifier
                    .size(220.dp)
                    .align(Alignment.BottomEnd)
                    .offset(x = 60.dp, y = 60.dp)
                    .alpha(orb2Alpha)
                    .blur(90.dp)
                    .background(PrimaryPurpleDark.copy(alpha = 0.6f), CircleShape)
            )

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Spacer(modifier = Modifier.height(12.dp))

                // Logo with glow ring
                Box(contentAlignment = Alignment.Center) {
                    Box(
                        modifier = Modifier
                            .size(110.dp)
                            .alpha(ringAlpha)
                            .blur(20.dp)
                            .background(PrimaryPurple.copy(alpha = 0.7f), CircleShape)
                    )
                    Image(
                        painter = painterResource(id = R.drawable.logosm),
                        contentDescription = "ShowMate Logo",
                        modifier = Modifier.size(80.dp).clip(CircleShape)
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = stringResource(id = R.string.app_name),
                    color = Color.White,
                    fontWeight = FontWeight.Black,
                    fontSize = 28.sp,
                    letterSpacing = (-0.5).sp
                )
                Spacer(modifier = Modifier.height(4.dp))
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(20.dp))
                        .background(PrimaryPurple.copy(alpha = 0.12f))
                        .padding(horizontal = 12.dp, vertical = 4.dp)
                ) {
                    Text(text = "Versión 1.0.0", color = PrimaryPurple, fontSize = 12.sp, fontWeight = FontWeight.Medium)
                }

                Spacer(modifier = Modifier.height(28.dp))

                // TFG card
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(20.dp))
                        .background(
                            Brush.linearGradient(
                                listOf(PrimaryPurple.copy(alpha = 0.18f), PrimaryPurpleDark.copy(alpha = 0.10f))
                            )
                        )
                ) {
                    Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("Trabajo de Fin de Grado", color = PrimaryPurple, fontWeight = FontWeight.Bold, fontSize = 13.sp, letterSpacing = 0.5.sp)
                        Text(
                            "ShowMate es una aplicación de descubrimiento de series desarrollada como TFG. Utiliza un sistema de recomendación híbrido que combina señales personales del usuario con la valoración global de las series.",
                            color = Color.White, fontSize = 14.sp, lineHeight = 22.sp
                        )
                        HorizontalDivider(color = Color.White.copy(alpha = 0.10f))
                        AboutInfoRow("Autor", "Andrea")
                        AboutInfoRow("Titulación", "Ingeniería Informática")
                        AboutInfoRow("Curso", "2025 - 2026")
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Algorithm explanation
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(20.dp))
                        .background(SurfaceVariantDark)
                ) {
                    Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .width(4.dp)
                                    .height(18.dp)
                                    .clip(RoundedCornerShape(2.dp))
                                    .background(
                                        Brush.verticalGradient(listOf(PrimaryPurple, PrimaryPurpleDark))
                                    )
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Text("Algoritmo de Recomendación", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                        }
                        Text(
                            "Fórmula híbrida: 70% afinidad personal + 30% valoración global bayesiana.",
                            color = TextGray, fontSize = 13.sp, lineHeight = 20.sp
                        )
                        AlgorithmWeightRow("Géneros", "37%", "Géneros favoritos del usuario con decaimiento temporal exponencial (semivida: 90 días)")
                        AlgorithmWeightRow("Estilo narrativo", "19%", "11 clusters narrativos detectados automáticamente a partir de keywords de TMDB")
                        AlgorithmWeightRow("Keywords", "22%", "Temas, ambientaciones y elementos temáticos de cada serie")
                        AlgorithmWeightRow("Actores", "12%", "Actores del reparto con los que el usuario ha interactuado positivamente")
                        AlgorithmWeightRow("Creadores", "10%", "Showrunners y directores de las series mejor valoradas por el usuario")
                        HorizontalDivider(color = Color.White.copy(alpha = 0.06f))
                        Text("Características avanzadas:", color = Color.White, fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                        BulletPoint("Valoración bayesiana: ajusta la nota TMDB penalizando series con pocas votaciones")
                        BulletPoint("Filtrado colaborativo: boost para series populares entre usuarios con gustos similares")
                        BulletPoint("Serendipia: el 15% de resultados son descubrimientos de alta valoración y baja afinidad")
                        BulletPoint("Saturación de género: penaliza géneros que dominen más del 45% del score acumulado")
                        BulletPoint("Diversidad: ningún género ocupa más del 35% de los resultados finales")
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Tech stack
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(20.dp))
                        .background(SurfaceVariantDark)
                ) {
                    Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .width(4.dp)
                                    .height(18.dp)
                                    .clip(RoundedCornerShape(2.dp))
                                    .background(
                                        Brush.verticalGradient(listOf(PrimaryPurple, PrimaryPurpleDark))
                                    )
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Text("Stack Tecnológico", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                        }
                        TechRow("UI", "Jetpack Compose + Material 3")
                        TechRow("Arquitectura", "MVVM + Clean Architecture")
                        TechRow("DI", "Dagger Hilt 2.52")
                        TechRow("Base de datos", "Room 2.6 + Firestore")
                        TechRow("Red", "Retrofit 2.9 + OkHttp")
                        TechRow("Auth", "Firebase Authentication")
                        TechRow("Imágenes", "Coil 2.7")
                        TechRow("API de datos", "TMDB API v3")
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                Text(
                    text = "Este producto utiliza la API de TMDB pero no está avalado ni certificado por TMDB.",
                    color = TextGray,
                    fontSize = 12.sp,
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(32.dp))
            }
        }
    }
}

@Composable
private fun AboutInfoRow(label: String, value: String) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, color = TextGray, fontSize = 13.sp)
        Text(value, color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
private fun AlgorithmWeightRow(title: String, weight: String, description: String) {
    Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.Top) {
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(8.dp))
                .background(PrimaryPurple.copy(alpha = 0.15f))
        ) {
            Text(
                text = weight,
                color = PrimaryPurple,
                fontSize = 12.sp,
                fontWeight = FontWeight.Black,
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
            )
        }
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(title, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp)
            Text(description, color = TextGray, fontSize = 12.sp, lineHeight = 18.sp)
        }
    }
}

@Composable
private fun BulletPoint(text: String) {
    Row(verticalAlignment = Alignment.Top) {
        Text("•", color = PrimaryPurple, fontSize = 13.sp, modifier = Modifier.padding(top = 1.dp, end = 8.dp))
        Text(text, color = TextGray, fontSize = 12.sp, lineHeight = 18.sp)
    }
}

@Composable
private fun TechRow(category: String, value: String) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.Top) {
        Text(category, color = TextGray, fontSize = 13.sp, modifier = Modifier.width(110.dp))
        Text(value, color = Color.White, fontSize = 13.sp, modifier = Modifier.weight(1f), textAlign = TextAlign.End)
    }
}
