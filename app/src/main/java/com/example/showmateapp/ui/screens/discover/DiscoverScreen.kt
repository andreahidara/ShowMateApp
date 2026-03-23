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
import com.example.showmateapp.ui.theme.AccentBlue
import com.example.showmateapp.ui.theme.PrimaryPurple
import com.example.showmateapp.ui.theme.PrimaryPurpleLight
import com.example.showmateapp.ui.theme.StarYellow

@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
fun DiscoverScreen(
    globalNavController: NavController,
    sharedTransitionScope: SharedTransitionScope,
    animatedVisibilityScope: AnimatedVisibilityScope,
    viewModel: DiscoverViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsState()
    DiscoverScreenContent(
        state = state,
        sharedTransitionScope = sharedTransitionScope,
        animatedVisibilityScope = animatedVisibilityScope,
        onMediaClick = { media, tag -> navigateToDetail(globalNavController, media, tag) },
        onRetry = { viewModel.retry() }
    )
}

@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
fun DiscoverScreenContent(
    state: DiscoverUiState,
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
        if (state.isLoading) {
            PulseLoader()
        } else if (state.errorMessage != null) {
            ErrorView(message = state.errorMessage, onRetry = onRetry)
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .statusBarsPadding()
            ) {
                // 1. Hero
                item {
                    state.heroShow?.let {
                        DiscoverHeroSection(
                            media = it,
                            sharedTransitionScope = sharedTransitionScope,
                            animatedVisibilityScope = animatedVisibilityScope,
                            onClick = { media -> onMediaClick(media, "discover_hero") }
                        )
                    }
                }

                // 2. Context picks (new)
                if (state.contextPicksShows.isNotEmpty() && state.contextPicksTitle.isNotEmpty()) {
                    item {
                        ShowSection(
                            title = state.contextPicksTitle,
                            items = state.contextPicksShows,
                            sharedTransitionScope = sharedTransitionScope,
                            animatedVisibilityScope = animatedVisibilityScope,
                            onItemClick = onMediaClick,
                            accentColor = AccentBlue,
                            tag = "discover_context",
                            subtitle = "Adaptado a cómo consumes series"
                        )
                    }
                }

                // 3. Day-of-week (new)
                if (state.dayOfWeekShows.isNotEmpty() && state.dayOfWeekTitle.isNotEmpty()) {
                    item {
                        ShowSection(
                            title = state.dayOfWeekTitle,
                            items = state.dayOfWeekShows,
                            sharedTransitionScope = sharedTransitionScope,
                            animatedVisibilityScope = animatedVisibilityScope,
                            onItemClick = onMediaClick,
                            accentColor = PrimaryPurpleLight,
                            tag = "discover_dayofweek",
                            subtitle = "Basado en tu patrón de visualización"
                        )
                    }
                }

                // 4. Top genre
                item {
                    ShowSection(
                        title = "Porque te gusta: ${state.topGenreName}",
                        items = state.topGenreShows,
                        sharedTransitionScope = sharedTransitionScope,
                        animatedVisibilityScope = animatedVisibilityScope,
                        onItemClick = onMediaClick,
                        tag = "discover_genre1"
                    )
                }

                // 5. Narrative style (new)
                if (state.narrativeStyleShows.isNotEmpty() && state.narrativeStyleLabel.isNotEmpty()) {
                    item {
                        ShowSection(
                            title = state.narrativeStyleLabel,
                            items = state.narrativeStyleShows,
                            sharedTransitionScope = sharedTransitionScope,
                            animatedVisibilityScope = animatedVisibilityScope,
                            onItemClick = onMediaClick,
                            tag = "discover_narrative",
                            subtitle = "Tu estilo narrativo preferido según el algoritmo"
                        )
                    }
                }

                // 6. Second genre
                item {
                    ShowSection(
                        title = "Más de: ${state.secondGenreName}",
                        items = state.secondGenreShows,
                        sharedTransitionScope = sharedTransitionScope,
                        animatedVisibilityScope = animatedVisibilityScope,
                        onItemClick = onMediaClick,
                        tag = "discover_genre2"
                    )
                }

                // 7. Hidden gems (new)
                if (state.hiddenGemShows.isNotEmpty()) {
                    item {
                        ShowSection(
                            title = "Joyas ocultas para ti",
                            items = state.hiddenGemShows,
                            sharedTransitionScope = sharedTransitionScope,
                            animatedVisibilityScope = animatedVisibilityScope,
                            onItemClick = onMediaClick,
                            accentColor = AccentBlue,
                            tag = "discover_hidden",
                            subtitle = "Alta afinidad, pocas valoraciones globales"
                        )
                    }
                }

                // 8. Third genre
                if (state.thirdGenreShows.isNotEmpty() && state.thirdGenreName.isNotEmpty()) {
                    item {
                        ShowSection(
                            title = "También te puede gustar: ${state.thirdGenreName}",
                            items = state.thirdGenreShows,
                            sharedTransitionScope = sharedTransitionScope,
                            animatedVisibilityScope = animatedVisibilityScope,
                            onItemClick = onMediaClick,
                            tag = "discover_genre3"
                        )
                    }
                }

                // 9. Mood section (new)
                if (state.moodSectionShows.isNotEmpty() && state.moodSectionTitle.isNotEmpty()) {
                    item {
                        ShowSection(
                            title = state.moodSectionTitle,
                            items = state.moodSectionShows,
                            sharedTransitionScope = sharedTransitionScope,
                            animatedVisibilityScope = animatedVisibilityScope,
                            onItemClick = onMediaClick,
                            tag = "discover_mood",
                            subtitle = "Seleccionado por tu tono favorito"
                        )
                    }
                }

                // 10. Exploration (new)
                if (state.explorationShows.isNotEmpty() && state.explorationGenreName.isNotEmpty()) {
                    item {
                        ShowSection(
                            title = "Sal de tu zona de confort: ${state.explorationGenreName}",
                            items = state.explorationShows,
                            sharedTransitionScope = sharedTransitionScope,
                            animatedVisibilityScope = animatedVisibilityScope,
                            onItemClick = onMediaClick,
                            accentColor = Color(0xFFFF9800),
                            tag = "discover_exploration",
                            subtitle = "Tu género menos explorado con buenos scores"
                        )
                    }
                }

                // 11. Time travel
                if (state.timeTravelShows.isNotEmpty()) {
                    item {
                        ShowSection(
                            title = "Porque te gustan los viajes en el tiempo",
                            items = state.timeTravelShows,
                            sharedTransitionScope = sharedTransitionScope,
                            animatedVisibilityScope = animatedVisibilityScope,
                            onItemClick = onMediaClick,
                            tag = "discover_timetravel"
                        )
                    }
                }

                // 12. Top keyword
                if (state.topKeywordShows.isNotEmpty() && state.topKeywordLabel.isNotEmpty()) {
                    item {
                        ShowSection(
                            title = state.topKeywordLabel,
                            items = state.topKeywordShows,
                            sharedTransitionScope = sharedTransitionScope,
                            animatedVisibilityScope = animatedVisibilityScope,
                            onItemClick = onMediaClick,
                            tag = "discover_keyword"
                        )
                    }
                }

                // 13. Top rated
                if (state.topRatedShows.isNotEmpty() && state.topGenreName.isNotEmpty()) {
                    item {
                        ShowSection(
                            title = "Los mejor valorados en ${state.topGenreName}",
                            items = state.topRatedShows,
                            sharedTransitionScope = sharedTransitionScope,
                            animatedVisibilityScope = animatedVisibilityScope,
                            onItemClick = onMediaClick,
                            tag = "discover_toprated"
                        )
                    }
                }

                // 14. Similar shows
                if (state.similarShows.isNotEmpty() && state.similarToName.isNotEmpty()) {
                    item {
                        ShowSection(
                            title = "Porque viste ${state.similarToName}",
                            items = state.similarShows,
                            sharedTransitionScope = sharedTransitionScope,
                            animatedVisibilityScope = animatedVisibilityScope,
                            onItemClick = onMediaClick,
                            tag = "discover_similar"
                        )
                    }
                }

                // 15. Creator (new)
                if (state.creatorShows.isNotEmpty() && state.creatorName.isNotEmpty()) {
                    item {
                        ShowSection(
                            title = "Del creador: ${state.creatorName}",
                            items = state.creatorShows,
                            sharedTransitionScope = sharedTransitionScope,
                            animatedVisibilityScope = animatedVisibilityScope,
                            onItemClick = onMediaClick,
                            tag = "discover_creator",
                            subtitle = "Series del showrunner que más te gusta"
                        )
                    }
                }

                // 16. Actor 1
                if (state.actorShows.isNotEmpty() && state.actorName.isNotEmpty()) {
                    item {
                        ShowSection(
                            title = "Porque te gusta ${state.actorName}",
                            items = state.actorShows,
                            sharedTransitionScope = sharedTransitionScope,
                            animatedVisibilityScope = animatedVisibilityScope,
                            onItemClick = onMediaClick,
                            tag = "discover_actor"
                        )
                    }
                }

                // 17. Actor 2
                if (state.secondActorShows.isNotEmpty() && state.secondActorName.isNotEmpty()) {
                    item {
                        ShowSection(
                            title = "Porque te gusta ${state.secondActorName}",
                            items = state.secondActorShows,
                            sharedTransitionScope = sharedTransitionScope,
                            animatedVisibilityScope = animatedVisibilityScope,
                            onItemClick = onMediaClick,
                            tag = "discover_actor2"
                        )
                    }
                }

                // 18. Collaborative (new)
                if (state.collaborativeShows.isNotEmpty()) {
                    item {
                        ShowSection(
                            title = "Lo que ven usuarios con tu gusto",
                            items = state.collaborativeShows,
                            sharedTransitionScope = sharedTransitionScope,
                            animatedVisibilityScope = animatedVisibilityScope,
                            onItemClick = onMediaClick,
                            accentColor = Color(0xFF4CAF50),
                            tag = "discover_collab",
                            subtitle = "Popular entre usuarios con gustos similares"
                        )
                    }
                }

                // 19. Spacer
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
