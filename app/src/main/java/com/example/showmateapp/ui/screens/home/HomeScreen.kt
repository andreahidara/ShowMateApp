package com.example.showmateapp.ui.screens.home

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.example.showmateapp.data.network.Movie
import java.net.URLEncoder
import java.nio.charset.StandardCharsets

@Composable
fun HomeScreen(navController: NavController, viewModel: HomeViewModel = viewModel()) {
    val trendingShows by viewModel.trendingShows.collectAsState()
    val popularShows by viewModel.popularShows.collectAsState()
    val errorMessage by viewModel.errorMessage.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()

    if (isLoading) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background),
            contentAlignment = Alignment.Center
        ) {
            CircularProgressIndicator(color = com.example.showmateapp.ui.theme.PrimaryPurple)
        }
        return
    }

    if (errorMessage != null) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background),
            contentAlignment = Alignment.Center
        ) {
            androidx.compose.material3.Text(
                text = errorMessage!!,
                color = androidx.compose.ui.graphics.Color.Red,
                fontWeight = androidx.compose.ui.text.font.FontWeight.Bold,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                modifier = Modifier.padding(16.dp)
            )
        }
        return
    }

    // Usamos el padding que viene de AppNavigation's Scaffold
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        item {
            if (trendingShows.isNotEmpty()) {
                FeaturedBanner(trendingShows.first()) { movie ->
                    navigateToDetail(navController, movie)
                }
            }
        }

        item {
            SectionTitle("Trending Now")
            SeriesRow(trendingShows) { movie ->
                navigateToDetail(navController, movie)
            }
        }

        item {
            SectionTitle("Popular")
            SeriesRow(popularShows) { movie ->
                navigateToDetail(navController, movie)
            }
        }

        item { Spacer(modifier = Modifier.height(80.dp)) }
    }
}

private fun navigateToDetail(navController: NavController, movie: Movie) {
    navController.navigate("detail/${movie.id}")
}

@Composable
fun FeaturedBanner(movie: Movie, onClick: (Movie) -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(450.dp)
            .clickable { onClick(movie) }
    ) {
        AsyncImage(
            model = "https://images.weserv.nl/?url=https://image.tmdb.org/t/p/original${movie.poster_path}",
            contentDescription = movie.name,
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop
        )

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
fun SeriesRow(shows: List<Movie>, onMovieClick: (Movie) -> Unit) {
    LazyRow(
        contentPadding = PaddingValues(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        items(shows) { movie ->
            AsyncImage(
                model = "https://images.weserv.nl/?url=https://image.tmdb.org/t/p/w500${movie.poster_path}",
                contentDescription = movie.name,
                modifier = Modifier
                    .width(130.dp)
                    .height(190.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .clickable { onMovieClick(movie) },
                contentScale = ContentScale.Crop
            )
        }
    }
}