package com.example.showmateapp.ui.screens.home

import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.material3.MaterialTheme
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
import com.example.showmateapp.data.network.MediaContent
import com.example.showmateapp.ui.navigation.Screen
import com.example.showmateapp.ui.components.premium.*
import com.example.showmateapp.ui.theme.PrimaryPurple

@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
fun HomeScreen(
    navController: NavController,
    sharedTransitionScope: SharedTransitionScope,
    animatedVisibilityScope: AnimatedVisibilityScope,
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
        sharedTransitionScope = sharedTransitionScope,
        animatedVisibilityScope = animatedVisibilityScope,
        onMediaClick = { media -> navigateToDetail(navController, media) },
        onRetry = { viewModel.loadData() }
    )
}

@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
fun HomeScreenContent(
    trendingShows: List<MediaContent>,
    popularShows: List<MediaContent>,
    isLoading: Boolean,
    errorMessage: String?,
    sharedTransitionScope: SharedTransitionScope,
    animatedVisibilityScope: AnimatedVisibilityScope,
    onMediaClick: (MediaContent) -> Unit,
    onRetry: () -> Unit
) {
    if (isLoading) {
        PulseLoader()
    } else if (errorMessage != null) {
        ErrorView(message = errorMessage, onRetry = onRetry)
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
                    FeaturedBanner(
                        media = trendingShows.first(),
                        sharedTransitionScope = sharedTransitionScope,
                        animatedVisibilityScope = animatedVisibilityScope,
                        onClick = { media -> onMediaClick(media) }
                    )
                }
            }

            // Trending Row
            item {
                ShowSection(
                    title = "Tendencias Ahora",
                    items = trendingShows,
                    sharedTransitionScope = sharedTransitionScope,
                    animatedVisibilityScope = animatedVisibilityScope,
                    onItemClick = onMediaClick
                )
            }

            // Popular Row
            item {
                ShowSection(
                    title = "Populares",
                    items = popularShows,
                    sharedTransitionScope = sharedTransitionScope,
                    animatedVisibilityScope = animatedVisibilityScope,
                    onItemClick = onMediaClick
                )
            }

            item { Spacer(modifier = Modifier.height(80.dp)) }
        }
    }
}

private fun navigateToDetail(navController: NavController, media: MediaContent) {
    navController.navigate(Screen.Detail(media.id))
}

@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
fun FeaturedBanner(
    media: MediaContent,
    sharedTransitionScope: SharedTransitionScope,
    animatedVisibilityScope: AnimatedVisibilityScope,
    onClick: (MediaContent) -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(400.dp)
            .clickable { onClick(media) }
    ) {
        val imageUrl = media.posterPath?.let { "https://image.tmdb.org/t/p/original$it" }
        
        with(sharedTransitionScope) {
            AsyncImage(
                model = imageUrl,
                contentDescription = "Featured: ${media.name}",
                modifier = Modifier
                    .fillMaxSize()
                    .sharedElement(
                        state = rememberSharedContentState(key = "image-${media.id}"),
                        animatedVisibilityScope = animatedVisibilityScope
                    ),
                contentScale = ContentScale.Crop
            )
        }
        
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

        if (media.affinityScore > 0f) {
            MatchBadge(
                affinityScore = media.affinityScore,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(16.dp)
            )
        }
        Column(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(16.dp)
        ) {
            Text(
                text = media.name,
                color = Color.White,
                fontSize = 28.sp,
                fontWeight = FontWeight.ExtraBold
            )
            Spacer(modifier = Modifier.height(8.dp))
            Button(
                onClick = { onClick(media) },
                colors = ButtonDefaults.buttonColors(containerColor = PrimaryPurple),
                shape = RoundedCornerShape(8.dp)
            ) {
                Text("Ver detalles")
            }
        }
    }
}

