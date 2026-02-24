package com.example.showmateapp.ui.screens.onboarding

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.example.showmateapp.ui.theme.*

// Asegúrate de que el modelo GenreItem esté definido
data class GenreItem(val name: String, val imageUrl: String)

@Composable
fun OnboardingScreen(navController: NavController) {
    val genres = remember {
        listOf(
            GenreItem("Sci-Fi", "https://picsum.photos/id/903/500/500"),
            GenreItem("Detective", "https://picsum.photos/id/609/500/500"),
            GenreItem("Comedy", "https://picsum.photos/id/1012/500/500"),
            GenreItem("Thriller", "https://picsum.photos/id/1024/500/500"),
            GenreItem("Documentary", "https://picsum.photos/id/1036/500/500"),
            GenreItem("Animation", "https://picsum.photos/id/1081/500/500"),
            GenreItem("Fantasy", "https://picsum.photos/id/1043/500/500"),
            GenreItem("Drama", "https://picsum.photos/id/64/500/500")
        )
    }

    // Esta es la lista que guarda tus selecciones
    val selectedGenres = remember { mutableStateListOf<String>() }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(horizontal = 24.dp)
    ) {
        Spacer(modifier = Modifier.height(48.dp))
        Text("Choose Your Favorites", color = Color.White, fontSize = 28.sp, fontWeight = FontWeight.Bold)
        Text("Select at least 3 genres.", color = TextGray, fontSize = 14.sp)
        Spacer(modifier = Modifier.height(24.dp))

        LazyVerticalGrid(
            columns = GridCells.Fixed(2),
            modifier = Modifier.weight(1f),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            items(genres) { genre ->
                val isSelected = selectedGenres.contains(genre.name)
                GenreCard(
                    genre = genre,
                    isSelected = isSelected,
                    onClick = {
                        if (isSelected) selectedGenres.remove(genre.name)
                        else selectedGenres.add(genre.name)
                    }
                )
            }
        }

        // --- EL BOTÓN CLAVE ---
        Button(
            onClick = {
                // 1. Convertimos la lista en texto separado por comas
                val genresString = selectedGenres.joinToString(",")

                // 2. Lo enviamos por la ruta
                navController.navigate("swipe/$genresString") {
                    popUpTo("onboarding") { inclusive = true }
                }
            },
            modifier = Modifier.fillMaxWidth().padding(vertical = 24.dp).height(56.dp),
            colors = ButtonDefaults.buttonColors(containerColor = PrimaryPurple),
            shape = RoundedCornerShape(16.dp),
            enabled = selectedGenres.size >= 3 // Solo activa si hay 3 o más
        ) {
            Text("Continue", color = Color.White, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
fun GenreCard(genre: GenreItem, isSelected: Boolean, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .aspectRatio(1f)
            .clip(RoundedCornerShape(20.dp))
            .background(SurfaceDark)
            .border(
                width = if (isSelected) 3.dp else 0.dp,
                color = if (isSelected) PrimaryPurple else Color.Transparent,
                shape = RoundedCornerShape(20.dp)
            )
            .clickable { onClick() }
    ) {
        AsyncImage(
            model = genre.imageUrl,
            contentDescription = null,
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop,
            alpha = if (isSelected) 1f else 0.5f
        )
        Text(
            text = genre.name,
            color = Color.White,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.align(Alignment.BottomStart).padding(16.dp)
        )
    }
}