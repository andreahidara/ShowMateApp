package com.example.showmateapp.ui.screens.home

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
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

@Composable
fun HomeScreen(navController: NavController) {
    Scaffold(
        containerColor = MaterialTheme.colorScheme.background
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            item {
                FeaturedBanner()
            }

            item {
                SectionTitle("Trending Now")
                SeriesRow()
            }

            item {
                SectionTitle("For You")
                SeriesRow()
            }

            item { Spacer(modifier = Modifier.height(40.dp)) }
        }
    }
}

@Composable
fun FeaturedBanner() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(450.dp) // Un poco más alto para que luzca
    ) {
        AsyncImage(
            // --- PARCHE AQUÍ: También añadimos el proxy al Banner ---
            model = "https://images.weserv.nl/?url=https://image.tmdb.org/t/p/original/uKvH5611UiV3s9OQn9S1oHbsKh9.jpg",
            contentDescription = null,
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop
        )

        // Degradado suave para que el fondo negro de la app se funda con la imagen
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(Color.Transparent, MaterialTheme.colorScheme.background),
                        startY = 700f
                    )
                )
        )
    }
}

@Composable
fun SectionTitle(title: String) {
    Text(
        text = title,
        color = Color.White,
        fontSize = 20.sp,
        fontWeight = FontWeight.Bold,
        modifier = Modifier.padding(start = 16.dp, top = 24.dp, bottom = 12.dp)
    )
}

@Composable
fun SeriesRow() {
    val tempPosters = listOf(
        "/uKvH5611UiV3s9OQn9S1oHbsKh9.jpg",
        "/9n21ow6vYfwSRC9uau3FJt6XmXp.jpg",
        "/6L963vT86S2qIosNAsm80u49jN2.jpg",
        "/og9S1oHbsKh9uKvH5611UiV3s9O.jpg"
    )

    LazyRow(
        contentPadding = PaddingValues(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        items(tempPosters) { posterPath ->
            AsyncImage(
                model = "https://images.weserv.nl/?url=https://image.tmdb.org/t/p/w500$posterPath",
                contentDescription = null,
                modifier = Modifier
                    .width(130.dp)
                    .height(190.dp)
                    .clip(RoundedCornerShape(12.dp)),
                contentScale = ContentScale.Crop
            )
        }
    }
}