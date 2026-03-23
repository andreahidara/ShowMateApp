package com.example.showmateapp.ui.screens.profile.about

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
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
import com.example.showmateapp.ui.theme.TextGray

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AboutScreen(navController: NavController) {
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
                    containerColor = MaterialTheme.colorScheme.background,
                    titleContentColor = Color.White,
                    navigationIconContentColor = Color.White
                )
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Image(
                painter = painterResource(id = R.drawable.logosm),
                contentDescription = "ShowMate Logo",
                modifier = Modifier.size(100.dp).clip(CircleShape)
            )
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = stringResource(id = R.string.app_name),
                style = MaterialTheme.typography.headlineMedium,
                color = PrimaryPurple,
                fontWeight = FontWeight.Black
            )
            Text(text = "Versión 1.0.0", style = MaterialTheme.typography.bodyMedium, color = TextGray)

            Spacer(modifier = Modifier.height(24.dp))

            // TFG card
            Surface(
                color = PrimaryPurple.copy(alpha = 0.08f),
                shape = RoundedCornerShape(20.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Trabajo de Fin de Grado", color = PrimaryPurple, fontWeight = FontWeight.Bold, fontSize = 13.sp, letterSpacing = 0.5.sp)
                    Text("ShowMate es una aplicación de descubrimiento de series desarrollada como TFG. Utiliza un sistema de recomendación híbrido que combina señales personales del usuario con la valoración global de las series.", color = Color.White, fontSize = 14.sp, lineHeight = 22.sp)
                    HorizontalDivider(color = Color.White.copy(alpha = 0.08f))
                    AboutInfoRow("Autor", "Andrea")
                    AboutInfoRow("Titulación", "Ingeniería Informática")
                    AboutInfoRow("Curso", "2025 - 2026")
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Algorithm explanation
            Surface(
                color = Color.White.copy(alpha = 0.04f),
                shape = RoundedCornerShape(20.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("Algoritmo de Recomendación", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    Text(
                        "Fórmula híbrida: 70% afinidad personal + 30% valoración global bayesiana.",
                        color = TextGray, fontSize = 13.sp, lineHeight = 20.sp
                    )
                    AlgorithmWeightRow("Géneros", "37%", "Géneros favoritos del usuario con decaimiento temporal exponencial (semivida: 90 días)")
                    AlgorithmWeightRow("Estilo narrativo", "19%", "11 clusters narrativos detectados automáticamente a partir de keywords de TMDB")
                    AlgorithmWeightRow("Keywords", "22%", "Temas, ambientaciones y elementos temáticos de cada serie")
                    AlgorithmWeightRow("Actores", "12%", "Actores del reparto con los que el usuario ha interactuado positivamente")
                    AlgorithmWeightRow("Creadores", "10%", "Showrunners y directores de las series mejor valoradas por el usuario")
                    HorizontalDivider(color = Color.White.copy(alpha = 0.08f))
                    Text("Características avanzadas:", color = Color.White, fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                    BulletPoint("Valoración bayesiana: ajusta la nota TMDB penalizando series con pocas votaciones")
                    BulletPoint("Filtrado colaborativo: boost para series populares entre usuarios con gustos similares")
                    BulletPoint("Serendipia: el 15% de resultados son descubrimientos de alta valoración y baja afinidad")
                    BulletPoint("Saturación de género: penaliza géneros que dominen más del 45% del score acumulado")
                    BulletPoint("Diversidad: ningún género ocupa más del 35% de los resultados finales")
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Tech stack
            Surface(
                color = Color.White.copy(alpha = 0.04f),
                shape = RoundedCornerShape(20.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("Stack Tecnológico", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp)
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

            Spacer(modifier = Modifier.height(20.dp))

            Text(
                text = "Este producto utiliza la API de TMDB pero no está avalado ni certificado por TMDB.",
                style = MaterialTheme.typography.bodySmall,
                color = TextGray,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(32.dp))
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
        Surface(
            color = PrimaryPurple.copy(alpha = 0.15f),
            shape = RoundedCornerShape(8.dp)
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
