package com.andrea.showmateapp.ui.screens.profile.about

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.andrea.showmateapp.R
import com.andrea.showmateapp.ui.theme.PrimaryPurple
import com.andrea.showmateapp.ui.theme.PrimaryPurpleDark
import com.andrea.showmateapp.ui.theme.PrimaryPurpleLight
import com.andrea.showmateapp.ui.theme.SurfaceVariantDark
import com.andrea.showmateapp.ui.theme.TextGray

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AboutScreen(navController: NavController) {
    val infiniteTransition = rememberInfiniteTransition(label = "aboutGlow")
    val orb1Alpha by infiniteTransition.animateFloat(
        initialValue = 0.18f, targetValue = 0.40f,
        animationSpec = infiniteRepeatable(tween(3200, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "orb1"
    )
    val orb2Alpha by infiniteTransition.animateFloat(
        initialValue = 0.12f, targetValue = 0.28f,
        animationSpec = infiniteRepeatable(tween(2600, easing = FastOutSlowInEasing, delayMillis = 900), RepeatMode.Reverse),
        label = "orb2"
    )
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Acerca de ShowMate", color = Color.White, fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Volver", tint = Color.White)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background)
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { paddingValues ->
        Box(modifier = Modifier.fillMaxSize().padding(paddingValues)) {

            Box(
                modifier = Modifier
                    .size(380.dp)
                    .offset(x = (-120).dp, y = (-80).dp)
                    .background(
                        Brush.radialGradient(
                            colors = listOf(
                                PrimaryPurple.copy(alpha = orb1Alpha * 0.8f),
                                Color.Transparent
                            )
                        ),
                        CircleShape
                    )
            )
            Box(
                modifier = Modifier
                    .size(300.dp)
                    .align(Alignment.BottomEnd)
                    .offset(x = 80.dp, y = 80.dp)
                    .background(
                        Brush.radialGradient(
                            colors = listOf(
                                PrimaryPurpleDark.copy(alpha = orb2Alpha * 0.9f),
                                Color.Transparent
                            )
                        ),
                        CircleShape
                    )
            )

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 20.dp)
                    .padding(bottom = 40.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = stringResource(id = R.string.app_name),
                    color = Color.White,
                    fontWeight = FontWeight.Black,
                    fontSize = 30.sp,
                    letterSpacing = (-0.5).sp
                )
                Spacer(modifier = Modifier.height(6.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    VersionChip(text = "v1.0.0")
                    VersionChip(text = "TFG 2025-26", tint = Color(0xFF00BCD4))
                }

                Spacer(modifier = Modifier.height(10.dp))
                Text(
                    text = "Tu compañero inteligente para descubrir series",
                    color = TextGray,
                    fontSize = 14.sp,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(28.dp))

                AboutSectionCard(
                    title = "Características",
                    icon = Icons.Default.AutoAwesome,
                    iconTint = Color(0xFFFFB300)
                ) {
                    FeatureRow(Icons.Default.Recommend, Color(0xFF7C4DFF), "Recomendaciones personalizadas", "Algoritmo híbrido con afinidad personal + valoración bayesiana global")
                    Spacer(Modifier.height(10.dp))
                    FeatureRow(Icons.Default.People, Color(0xFF2196F3), "Sistema social", "Compara gustos con amigos, Group Match y noches de película")
                    Spacer(Modifier.height(10.dp))
                    FeatureRow(Icons.Default.EmojiEvents, Color(0xFFFFB300), "Gamificación", "Logros, niveles de XP y racha de visualización")
                    Spacer(Modifier.height(10.dp))
                    FeatureRow(Icons.Default.BarChart, Color(0xFF4CAF50), "Estadísticas Wrapped", "Resumen anual con géneros, horas y perfil de espectador")
                    Spacer(Modifier.height(10.dp))
                    FeatureRow(Icons.Default.Psychology, Color(0xFFE91E63), "Perfil psicológico", "8 arquetipos de espectador detectados automáticamente")
                }

                Spacer(modifier = Modifier.height(16.dp))

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(20.dp))
                        .background(
                            Brush.linearGradient(listOf(PrimaryPurple.copy(alpha = 0.20f), PrimaryPurpleDark.copy(alpha = 0.10f)))
                        )
                ) {
                    Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(34.dp)
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(PrimaryPurple.copy(alpha = 0.20f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Default.School, contentDescription = null, tint = PrimaryPurpleLight, modifier = Modifier.size(18.dp))
                            }
                            Spacer(Modifier.width(10.dp))
                            Column {
                                Text("Trabajo de Fin de Grado", color = PrimaryPurpleLight, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                Text("Ingeniería Informática", color = TextGray, fontSize = 12.sp)
                            }
                        }
                        HorizontalDivider(color = Color.White.copy(alpha = 0.08f))
                        Text(
                            "ShowMate es una aplicación de descubrimiento de series desarrollada como TFG. Combina un sistema de recomendación híbrido, funciones sociales, gamificación y análisis estadístico para ofrecer una experiencia personalizada.",
                            color = Color.White.copy(alpha = 0.85f),
                            fontSize = 13.sp,
                            lineHeight = 20.sp
                        )
                        HorizontalDivider(color = Color.White.copy(alpha = 0.08f))
                        AboutInfoRow(Icons.Default.Person, "Autora", "Andrea Hidara")
                        AboutInfoRow(Icons.Default.CalendarMonth, "Curso", "2025 – 2026")
                        AboutInfoRow(Icons.Default.LocationOn, "Universidad", "UPM · ETSISI")
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                AboutSectionCard(title = "Algoritmo de Recomendación", icon = Icons.Default.Insights, iconTint = PrimaryPurpleLight) {
                    Text(
                        "Fórmula híbrida: 70% afinidad personal + 30% valoración global bayesiana",
                        color = TextGray,
                        fontSize = 13.sp,
                        lineHeight = 20.sp
                    )
                    Spacer(Modifier.height(14.dp))
                    AlgorithmWeightRow("Géneros", "37%", "Favoritos con decaimiento temporal exponencial (semivida 90 días)", Color(0xFF7C4DFF))
                    Spacer(Modifier.height(8.dp))
                    AlgorithmWeightRow("Keywords", "22%", "Temas, ambientaciones y elementos temáticos de cada serie", Color(0xFF00BCD4))
                    Spacer(Modifier.height(8.dp))
                    AlgorithmWeightRow("Estilo narrativo", "19%", "11 clusters detectados automáticamente a partir de keywords TMDB", Color(0xFFFFB300))
                    Spacer(Modifier.height(8.dp))
                    AlgorithmWeightRow("Actores", "12%", "Reparto con el que el usuario ha interactuado positivamente", Color(0xFF4CAF50))
                    Spacer(Modifier.height(8.dp))
                    AlgorithmWeightRow("Creadores", "10%", "Showrunners y directores de series mejor valoradas", Color(0xFFE91E63))
                    Spacer(Modifier.height(14.dp))
                    HorizontalDivider(color = Color.White.copy(alpha = 0.06f))
                    Spacer(Modifier.height(12.dp))
                    Text("Características avanzadas", color = Color.White, fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                    Spacer(Modifier.height(8.dp))
                    BulletPoint("Valoración bayesiana: penaliza series con pocas votaciones")
                    BulletPoint("Filtrado colaborativo: boost por popularidad entre usuarios similares")
                    BulletPoint("Serendipia: 15% de resultados son descubrimientos de alta valoración")
                    BulletPoint("Saturación de género: penaliza géneros que dominen > 45% del score")
                    BulletPoint("Diversidad: ningún género ocupa > 35% de los resultados finales")
                }

                Spacer(modifier = Modifier.height(16.dp))

                AboutSectionCard(title = "Stack Tecnológico", icon = Icons.Default.Code, iconTint = Color(0xFF4CAF50)) {
                    TechRow(Icons.Default.Brush, Color(0xFFE91E63), "UI", "Jetpack Compose + Material 3")
                    TechRow(Icons.Default.Architecture, Color(0xFF7C4DFF), "Arquitectura", "MVVM + Clean Architecture")
                    TechRow(Icons.Default.Hub, Color(0xFF2196F3), "DI", "Dagger Hilt 2.52")
                    TechRow(Icons.Default.Storage, Color(0xFFFFB300), "Base de datos", "Room 2.6 + Firestore")
                    TechRow(Icons.Default.Cloud, Color(0xFF00BCD4), "Red", "Retrofit 2.9 + OkHttp")
                    TechRow(Icons.Default.Security, Color(0xFF4CAF50), "Auth", "Firebase Authentication")
                    TechRow(Icons.Default.Image, Color(0xFFFF5722), "Imágenes", "Coil 2.7")
                    TechRow(Icons.Default.Movie, Color(0xFFFF9800), "API datos", "TMDB API v3")
                }

                Spacer(modifier = Modifier.height(24.dp))

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color.White.copy(alpha = 0.04f))
                        .padding(16.dp)
                ) {
                    Row(verticalAlignment = Alignment.Top) {
                        Icon(Icons.Default.Info, contentDescription = null, tint = TextGray.copy(alpha = 0.6f), modifier = Modifier.size(14.dp).padding(top = 1.dp))
                        Spacer(Modifier.width(8.dp))
                        Text(
                            text = "Este producto utiliza la API de TMDB pero no está avalado ni certificado por TMDB.",
                            color = TextGray.copy(alpha = 0.7f),
                            fontSize = 11.sp,
                            lineHeight = 16.sp
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun VersionChip(text: String, tint: Color = PrimaryPurple) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(20.dp))
            .background(tint.copy(alpha = 0.14f))
            .padding(horizontal = 12.dp, vertical = 4.dp)
    ) {
        Text(text = text, color = tint, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
private fun AboutSectionCard(
    title: String,
    icon: ImageVector,
    iconTint: Color = PrimaryPurple,
    content: @Composable ColumnScope.() -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(SurfaceVariantDark)
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(bottom = 16.dp)) {
                Box(
                    modifier = Modifier
                        .size(34.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(iconTint.copy(alpha = 0.16f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(icon, contentDescription = null, tint = iconTint, modifier = Modifier.size(18.dp))
                }
                Spacer(Modifier.width(10.dp))
                Text(title, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp)
            }
            content()
        }
    }
}

@Composable
private fun FeatureRow(icon: ImageVector, iconTint: Color, title: String, description: String) {
    Row(verticalAlignment = Alignment.Top) {
        Box(
            modifier = Modifier
                .size(32.dp)
                .clip(RoundedCornerShape(9.dp))
                .background(iconTint.copy(alpha = 0.15f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, contentDescription = null, tint = iconTint, modifier = Modifier.size(17.dp))
        }
        Spacer(Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(title, color = Color.White, fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
            Text(description, color = TextGray, fontSize = 12.sp, lineHeight = 17.sp)
        }
    }
}

@Composable
private fun AboutInfoRow(icon: ImageVector, label: String, value: String) {
    Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Icon(icon, contentDescription = null, tint = TextGray.copy(alpha = 0.7f), modifier = Modifier.size(14.dp))
        Spacer(Modifier.width(8.dp))
        Text(label, color = TextGray, fontSize = 13.sp, modifier = Modifier.weight(1f))
        Text(value, color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
private fun AlgorithmWeightRow(title: String, weight: String, description: String, accentColor: Color = PrimaryPurple) {
    Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.Top) {
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(8.dp))
                .background(accentColor.copy(alpha = 0.18f))
        ) {
            Text(
                text = weight,
                color = accentColor,
                fontSize = 12.sp,
                fontWeight = FontWeight.Black,
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
            )
        }
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(title, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp)
            Text(description, color = TextGray, fontSize = 12.sp, lineHeight = 17.sp)
        }
    }
}

@Composable
private fun BulletPoint(text: String) {
    Row(verticalAlignment = Alignment.Top, modifier = Modifier.padding(bottom = 4.dp)) {
        Box(
            modifier = Modifier
                .size(5.dp)
                .offset(y = 6.dp)
                .clip(CircleShape)
                .background(PrimaryPurple)
        )
        Spacer(Modifier.width(10.dp))
        Text(text, color = TextGray, fontSize = 12.sp, lineHeight = 18.sp)
    }
}

@Composable
private fun TechRow(icon: ImageVector, iconTint: Color, category: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 5.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(28.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(iconTint.copy(alpha = 0.14f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, contentDescription = null, tint = iconTint, modifier = Modifier.size(15.dp))
        }
        Spacer(Modifier.width(10.dp))
        Text(category, color = TextGray, fontSize = 13.sp, modifier = Modifier.weight(1f))
        Text(value, color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Medium, textAlign = TextAlign.End)
    }
}
