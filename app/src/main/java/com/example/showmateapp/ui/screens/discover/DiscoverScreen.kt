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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.showmateapp.R
import com.example.showmateapp.data.network.MediaContent
import com.example.showmateapp.ui.navigation.Screen
import com.example.showmateapp.ui.components.premium.ErrorView
import com.example.showmateapp.ui.components.premium.MatchBadge
import com.example.showmateapp.ui.components.premium.PulseLoader
import com.example.showmateapp.ui.components.premium.ShowSection
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
    val timeTravelShows by viewModel.timeTravelShows.collectAsState()
    val actorShows by viewModel.actorShows.collectAsState()
    val actorName by viewModel.actorName.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val errorMessage by viewModel.errorMessage.collectAsState()

    DiscoverScreenContent(
        heroShow = heroShow,
        topGenreShows = topGenreShows,
        secondGenreShows = secondGenreShows,
        similarShows = similarShows,
        topGenreName = topGenreName,
        secondGenreName = secondGenreName,
        similarToName = similarToName,
        timeTravelShows = timeTravelShows,
        actorShows = actorShows,
        actorName = actorName,
        isLoading = isLoading,
        errorMessage = errorMessage,
        sharedTransitionScope = sharedTransitionScope,
        animatedVisibilityScope = animatedVisibilityScope,
        onMediaClick = { media, tag -> navigateToDetail(globalNavController, media, tag) },
        onRetry = { viewModel.retry() }
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
    timeTravelShows: List<MediaContent>,
    actorShows: List<MediaContent>,
    actorName: String,
    isLoading: Boolean,
    errorMessage: String?,
    sharedTransitionScope: SharedTransitionScope,
    animatedVisibilityScope: AnimatedVisibilityScope,
    onMediaClick: (MediaContent, String) -> Unit,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        if (isLoading) {
            PulseLoader()
        } else if (errorMessage != null) {
            ErrorView(message = errorMessage, onRetry = onRetry)
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .statusBarsPadding()
            ) {
                item {
                    heroShow?.let {
                        DiscoverHeroSection(
                            media = it,
                            sharedTransitionScope = sharedTransitionScope,
                            animatedVisibilityScope = animatedVisibilityScope,
                            onClick = { media -> onMediaClick(media, "discover_hero") }
                        )
                    }
                }

                item {
                    ShowSection(
                        title = "Porque te gusta: $topGenreName",
                        items = topGenreShows,
                        sharedTransitionScope = sharedTransitionScope,
                        animatedVisibilityScope = animatedVisibilityScope,
                        onItemClick = onMediaClick,
                        tag = "discover_genre1"
                    )
                }

                item {
                    ShowSection(
                        title = "Más de: $secondGenreName",
                        items = secondGenreShows,
                        sharedTransitionScope = sharedTransitionScope,
                        animatedVisibilityScope = animatedVisibilityScope,
                        onItemClick = onMediaClick,
                        tag = "discover_genre2"
                    )
                }

                if (timeTravelShows.isNotEmpty()) {
                    item {
                        ShowSection(
                            title = "Porque te gustan los viajes en el tiempo",
                            items = timeTravelShows,
                            sharedTransitionScope = sharedTransitionScope,
                            animatedVisibilityScope = animatedVisibilityScope,
                            onItemClick = onMediaClick,
                            tag = "discover_timetravel"
                        )
                    }
                }

                if (similarShows.isNotEmpty() && similarToName.isNotEmpty()) {
                    item {
                        ShowSection(
                            title = "Porque viste $similarToName",
                            items = similarShows,
                            sharedTransitionScope = sharedTransitionScope,
                            animatedVisibilityScope = animatedVisibilityScope,
                            onItemClick = onMediaClick,
                            tag = "discover_similar"
                        )
                    }
                }

                if (actorShows.isNotEmpty() && actorName.isNotEmpty()) {
                    item {
                        ShowSection(
                            title = "Porque te gusta $actorName",
                            items = actorShows,
                            sharedTransitionScope = sharedTransitionScope,
                            animatedVisibilityScope = animatedVisibilityScope,
                            onItemClick = onMediaClick,
                            tag = "discover_actor"
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
    val tag = "discover_hero"
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(520.dp)
            .padding(16.dp)
            .clip(RoundedCornerShape(24.dp))
            .clickable { onClick(media) }
    ) {
        val imageUrl = (media.backdropPath ?: media.posterPath)?.let { "https://image.tmdb.org/t/p/w1280$it" }
        with(sharedTransitionScope) {
            AsyncImage(
                model = ImageRequest.Builder(LocalContext.current)
                    .data(imageUrl)
                    .crossfade(true)
                    .build(),
                contentDescription = "Hero: ${media.name}",
                modifier = Modifier
                    .fillMaxSize()
                    .sharedElement(
                        state = rememberSharedContentState(key = "image-${media.id}-$tag"),
                        animatedVisibilityScope = animatedVisibilityScope
                    ),
                placeholder = painterResource(R.drawable.ic_logo_placeholder),
                error = painterResource(R.drawable.ic_logo_placeholder),
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
                            Color.Black.copy(alpha = 0.1f),
                            Color.Black.copy(alpha = 0.85f),
                            Color.Black.copy(alpha = 0.97f)
                        ),
                        startY = 200f
                    )
                )
        )

        if (media.affinityScore > 0f) {
            MatchBadge(
                score = media.affinityScore,
                isAffinity = true,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(16.dp)
            )
        } else if (media.voteAverage > 0f) {
            MatchBadge(
                score = media.voteAverage,
                isAffinity = false,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(16.dp)
            )
        }

        // "Top Match" pill badge at top-start
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(16.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(Color.Black.copy(alpha = 0.6f))
                .padding(horizontal = 10.dp, vertical = 5.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Star,
                contentDescription = null,
                tint = StarYellow,
                modifier = Modifier.size(13.dp)
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = "Top Match",
                color = StarYellow,
                fontWeight = FontWeight.ExtraBold,
                fontSize = 11.sp,
                letterSpacing = 0.5.sp
            )
        }

        Column(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(20.dp)
        ) {
            // Metadata row
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                if (media.voteAverage > 0f) {
                    Text(
                        text = "${"%.1f".format(media.voteAverage)} ★",
                        color = Color(0xFFFFC107),
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
                val year = media.firstAirDate?.take(4)
                if (year != null) {
                    Text("·", color = Color.White.copy(alpha = 0.5f), fontSize = 13.sp)
                    Text(year, color = Color.White.copy(alpha = 0.7f), fontSize = 13.sp)
                }
                media.numberOfSeasons?.let { seasons ->
                    Text("·", color = Color.White.copy(alpha = 0.5f), fontSize = 13.sp)
                    Text("$seasons temp.", color = Color.White.copy(alpha = 0.7f), fontSize = 13.sp)
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = media.name,
                color = Color.White,
                fontSize = 30.sp,
                fontWeight = FontWeight.Black,
                lineHeight = 34.sp,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(modifier = Modifier.height(16.dp))

            Button(
                onClick = { onClick(media) },
                colors = ButtonDefaults.buttonColors(containerColor = Color.White),
                shape = RoundedCornerShape(14.dp),
                contentPadding = PaddingValues(horizontal = 28.dp, vertical = 14.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.PlayArrow,
                    contentDescription = null,
                    tint = Color.Black,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text("Ver detalles", color = Color.Black, fontWeight = FontWeight.Black, fontSize = 15.sp)
            }
        }
    }
}

private fun navigateToDetail(navController: NavController, media: MediaContent, tag: String) {
    navController.navigate(Screen.Detail(media.id, tag))
}
