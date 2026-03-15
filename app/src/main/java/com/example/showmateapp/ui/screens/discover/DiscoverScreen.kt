package com.example.showmateapp.ui.screens.discover

import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.example.showmateapp.data.network.MediaContent
import com.example.showmateapp.ui.navigation.Screen
import com.example.showmateapp.ui.components.premium.*
import com.example.showmateapp.ui.theme.PrimaryPurple
import com.example.showmateapp.ui.theme.StarYellow

@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
fun DiscoverScreen(
    globalNavController: NavController,
    sharedTransitionScope: SharedTransitionScope,
    animatedVisibilityScope: AnimatedVisibilityScope,
    viewModel: DiscoverViewModel = hiltViewModel()
) {
    val heroShow by viewModel.heroShow.collectAsState()
    val topGenreShows by viewModel.topGenreShows.collectAsState()
    val secondGenreShows by viewModel.secondGenreShows.collectAsState()
    val topGenreName by viewModel.topGenreName.collectAsState()
    val secondGenreName by viewModel.secondGenreName.collectAsState()
    val similarShows by viewModel.similarShows.collectAsState()
    val similarToName by viewModel.similarToName.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()

    DiscoverScreenContent(
        heroShow = heroShow,
        topGenreShows = topGenreShows,
        secondGenreShows = secondGenreShows,
        similarShows = similarShows,
        topGenreName = topGenreName,
        secondGenreName = secondGenreName,
        similarToName = similarToName,
        isLoading = isLoading,
        sharedTransitionScope = sharedTransitionScope,
        animatedVisibilityScope = animatedVisibilityScope,
        onMediaClick = { media -> navigateToDetail(globalNavController, media) }
    )
}

@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
fun DiscoverScreenContent(
    heroShow: MediaContent?,
    topGenreShows: List<MediaContent>,
    secondGenreShows: List<MediaContent>,
    similarShows: List<MediaContent>,
    topGenreName: String,
    secondGenreName: String,
    similarToName: String,
    isLoading: Boolean,
    sharedTransitionScope: SharedTransitionScope,
    animatedVisibilityScope: AnimatedVisibilityScope,
    onMediaClick: (MediaContent) -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        if (isLoading) {
            PulseLoader()
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize()
            ) {
                item {
                    heroShow?.let {
                        DiscoverHeroSection(
                            media = it,
                            sharedTransitionScope = sharedTransitionScope,
                            animatedVisibilityScope = animatedVisibilityScope,
                            onClick = { media -> onMediaClick(media) }
                        )
                    }
                }

                item {
                    ShowSection(
                        title = "Porque te gusta: $topGenreName",
                        items = topGenreShows,
                        sharedTransitionScope = sharedTransitionScope,
                        animatedVisibilityScope = animatedVisibilityScope,
                        onItemClick = onMediaClick
                    )
                }

                item {
                    ShowSection(
                        title = "Más de: $secondGenreName",
                        items = secondGenreShows,
                        sharedTransitionScope = sharedTransitionScope,
                        animatedVisibilityScope = animatedVisibilityScope,
                        onItemClick = onMediaClick
                    )
                }

                if (similarShows.isNotEmpty() && similarToName.isNotEmpty()) {
                    item {
                        ShowSection(
                            title = "Porque viste $similarToName",
                            items = similarShows,
                            sharedTransitionScope = sharedTransitionScope,
                            animatedVisibilityScope = animatedVisibilityScope,
                            onItemClick = onMediaClick
                        )
                    }
                }

                item { Spacer(modifier = Modifier.height(100.dp)) }
            }
        }
    }
}

@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
fun DiscoverHeroSection(
    media: MediaContent,
    sharedTransitionScope: SharedTransitionScope,
    animatedVisibilityScope: AnimatedVisibilityScope,
    onClick: (MediaContent) -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(550.dp)
            .clickable { onClick(media) }
    ) {
        val imageUrl = "https://image.tmdb.org/t/p/original${media.posterPath}"
        with(sharedTransitionScope) {
            AsyncImage(
                model = imageUrl,
                contentDescription = "Hero: ${media.name}",
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
                        colors = listOf(
                            Color.Transparent,
                            Color.Transparent,
                            MaterialTheme.colorScheme.background.copy(alpha = 0.4f),
                            MaterialTheme.colorScheme.background
                        ),
                        startY = 0f
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
                .align(Alignment.BottomCenter)
                .padding(bottom = 32.dp, start = 24.dp, end = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color.Black.copy(alpha = 0.5f))
                    .padding(horizontal = 12.dp, vertical = 6.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Star,
                    contentDescription = "Star",
                    tint = StarYellow,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = "Top Match Recomendado",
                    color = StarYellow,
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.sp
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = media.name,
                color = Color.White,
                fontSize = 32.sp,
                fontWeight = FontWeight.ExtraBold,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(20.dp))

            Button(
                onClick = { onClick(media) },
                colors = ButtonDefaults.buttonColors(containerColor = PrimaryPurple),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .fillMaxWidth(0.8f)
                    .height(50.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.PlayArrow,
                    contentDescription = "Play",
                    tint = Color.White
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text("Ver Detalles", fontSize = 16.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}

private fun navigateToDetail(navController: NavController, media: MediaContent) {
    navController.navigate(Screen.Detail(media.id))
}
