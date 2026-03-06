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
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.example.showmateapp.data.network.TvShow
import com.example.showmateapp.ui.theme.PrimaryPurple

@Composable
fun HomeScreen(
    navController: NavController,
    viewModel: HomeViewModel = hiltViewModel()
) {
    val trendingShows by viewModel.trendingShows.collectAsState()
    val popularShows by viewModel.popularShows.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val errorMessage by viewModel.errorMessage.collectAsState()

    HomeScreenContent(
        trendingShows = trendingShows,
        popularShows = popularShows,
        isLoading = isLoading,
        errorMessage = errorMessage,
        onTvShowClick = { tvShow -> navigateToDetail(navController, tvShow) }
    )
}

@Composable
fun HomeScreenContent(
    trendingShows: List<TvShow>,
    popularShows: List<TvShow>,
    isLoading: Boolean,
    errorMessage: String?,
    onTvShowClick: (TvShow) -> Unit
) {
    if (isLoading) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background),
            contentAlignment = Alignment.Center
        ) {
            CircularProgressIndicator(color = PrimaryPurple)
        }
    } else if (errorMessage != null) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background),
            contentAlignment = Alignment.Center
        ) {
            Text(text = errorMessage, color = Color.Red)
        }
    } else {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
        ) {
            // Header
            item {
                Text(
                    text = "ShowMate",
                    color = PrimaryPurple,
                    fontSize = 32.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(16.dp)
                )
            }

            // Featured Section (First Trending Show)
            if (trendingShows.isNotEmpty()) {
                item {
                    FeaturedBanner(trendingShows.first()) { tvShow ->
                        onTvShowClick(tvShow)
                    }
                }
            }

            // Trending Row
            item {
                SectionHeader("Tendencias")
                SeriesRow(trendingShows) { tvShow ->
                    onTvShowClick(tvShow)
                }
            }

            // Popular Row
            item {
                SectionHeader("Populares")
                SeriesRow(popularShows) { tvShow ->
                    onTvShowClick(tvShow)
                }
            }

            item { Spacer(modifier = Modifier.height(80.dp)) }
        }
    }
}

private fun navigateToDetail(navController: NavController, tvShow: TvShow) {
    navController.navigate("detail/${tvShow.id}")
}

@Composable
fun SectionHeader(title: String) {
    Text(
        text = title,
        color = Color.White,
        fontSize = 20.sp,
        fontWeight = FontWeight.Bold,
        modifier = Modifier.padding(16.dp)
    )
}

@Composable
fun FeaturedBanner(tvShow: TvShow, onClick: (TvShow) -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(400.dp)
            .clickable { onClick(tvShow) }
    ) {
        val imageUrl = tvShow.posterPath?.let { "https://image.tmdb.org/t/p/original$it" }
        AsyncImage(
            model = imageUrl,
            contentDescription = "Featured: ${tvShow.name}",
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop
        )
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(Color.Transparent, MaterialTheme.colorScheme.background),
                        startY = 300f
                    )
                )
        )
        Column(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(16.dp)
        ) {
            Text(
                text = tvShow.name,
                color = Color.White,
                fontSize = 28.sp,
                fontWeight = FontWeight.ExtraBold
            )
            Spacer(modifier = Modifier.height(8.dp))
            Button(
                onClick = { onClick(tvShow) },
                colors = ButtonDefaults.buttonColors(containerColor = PrimaryPurple),
                shape = RoundedCornerShape(8.dp)
            ) {
                Text("Ver detalles")
            }
        }
    }
}

@Composable
fun SeriesRow(shows: List<TvShow>, onTvShowClick: (TvShow) -> Unit) {
    LazyRow(
        contentPadding = PaddingValues(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        items(shows) { tvShow ->
            TvShowCard(tvShow = tvShow, onClick = onTvShowClick)
        }
    }
}

@Composable
fun TvShowCard(tvShow: TvShow, onClick: (TvShow) -> Unit) {
    Column(
        modifier = Modifier
            .width(120.dp)
            .clickable { onClick(tvShow) }
    ) {
        val imageUrl = tvShow.posterPath?.let { "https://image.tmdb.org/t/p/w500$it" }
        AsyncImage(
            model = imageUrl,
            contentDescription = tvShow.name,
            modifier = Modifier
                .height(180.dp)
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp)),
            contentScale = ContentScale.Crop
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = tvShow.name,
            color = Color.White,
            fontSize = 14.sp,
            fontWeight = FontWeight.Medium,
            maxLines = 2
        )
    }
}
