package com.example.showmateapp.ui.screens.home

import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.LiveTv
import androidx.compose.material3.*
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.ExperimentalMaterial3Api
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
import coil.request.ImageRequest
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import com.example.showmateapp.R
import com.example.showmateapp.data.network.MediaContent
import com.example.showmateapp.ui.navigation.Screen
import com.example.showmateapp.ui.components.premium.*
import com.example.showmateapp.ui.theme.PrimaryPurple
import com.example.showmateapp.util.UiText
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.window.Dialog

@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
fun HomeScreen(
    navController: NavController,
    sharedTransitionScope: SharedTransitionScope,
    animatedVisibilityScope: AnimatedVisibilityScope,
    viewModel: HomeViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    val whatToWatch = uiState.whatToWatchToday

    HomeScreenContent(
        upNextShows = uiState.upNextShows,
        trendingShows = uiState.trendingShows,
        actionShows = uiState.actionShows,
        comedyShows = uiState.comedyShows,
        popularInSpain = uiState.popularInSpain,
        mysteryShows = uiState.mysteryShows,
        isLoading = uiState.isLoading,
        isRefreshing = uiState.isRefreshing,
        errorMessage = uiState.errorMessage,
        sharedTransitionScope = sharedTransitionScope,
        animatedVisibilityScope = animatedVisibilityScope,
        onMediaClick = { media, tag -> navigateToDetail(navController, media, tag) },
        onRetry = { viewModel.loadData() },
        onRefresh = { viewModel.refresh() },
        onPickWhatToWatch = { viewModel.pickWhatToWatchToday() }
    )

    if (whatToWatch != null) {
        WhatToWatchDialog(
            media = whatToWatch,
            onDismiss = { viewModel.dismissWhatToWatch() },
            onPickAgain = { viewModel.pickWhatToWatchToday() },
            onViewDetails = {
                viewModel.dismissWhatToWatch()
                navigateToDetail(navController, whatToWatch, "wtw")
            }
        )
    }
}

@OptIn(ExperimentalSharedTransitionApi::class, ExperimentalMaterial3Api::class)
@Composable
fun HomeScreenContent(
    upNextShows: List<MediaContent>,
    trendingShows: List<MediaContent>,
    actionShows: List<MediaContent>,
    comedyShows: List<MediaContent>,
    popularInSpain: List<MediaContent>,
    mysteryShows: List<MediaContent>,
    isLoading: Boolean,
    isRefreshing: Boolean,
    errorMessage: UiText?,
    sharedTransitionScope: SharedTransitionScope,
    animatedVisibilityScope: AnimatedVisibilityScope,
    onMediaClick: (MediaContent, String) -> Unit,
    onRetry: () -> Unit,
    onRefresh: () -> Unit,
    onPickWhatToWatch: () -> Unit = {}
) {
    if (isLoading) {
        PulseLoader()
    } else if (errorMessage != null) {
        ErrorView(message = errorMessage.asString(), onRetry = onRetry)
    } else {
        Scaffold(
            topBar = { HomeTopAppBar() },
            floatingActionButton = {
                ExtendedFloatingActionButton(
                    onClick = onPickWhatToWatch,
                    containerColor = PrimaryPurple,
                    contentColor = Color.White,
                    icon = { Icon(Icons.Default.LiveTv, contentDescription = null) },
                    text = { Text("¿Qué veo hoy?", fontWeight = FontWeight.Bold) }
                )
            },
            containerColor = MaterialTheme.colorScheme.background
        ) { paddingValues ->
            PullToRefreshBox(
                isRefreshing = isRefreshing,
                onRefresh = onRefresh,
                modifier = Modifier.padding(paddingValues)
            ) {
                LazyColumn(
                    modifier = Modifier.fillMaxSize()
                ) {
                    if (trendingShows.isNotEmpty()) {
                        item {
                            FeaturedBanner(
                                media = trendingShows.first(),
                                sharedTransitionScope = sharedTransitionScope,
                                animatedVisibilityScope = animatedVisibilityScope,
                                onClick = { media -> onMediaClick(media, "hero") }
                            )
                        }
                    }

                    if (upNextShows.isNotEmpty()) {
                        item {
                            ShowSection(
                                title = "Continuar viendo",
                                items = upNextShows,
                                sharedTransitionScope = sharedTransitionScope,
                                animatedVisibilityScope = animatedVisibilityScope,
                                onItemClick = onMediaClick,
                                tag = "up_next"
                            )
                        }
                    }

                    item {
                        ShowSection(
                            title = stringResource(id = R.string.trending_now),
                            items = trendingShows,
                            sharedTransitionScope = sharedTransitionScope,
                            animatedVisibilityScope = animatedVisibilityScope,
                            onItemClick = onMediaClick,
                            tag = "trending"
                        )
                    }

                    if (popularInSpain.isNotEmpty()) {
                        item {
                            ShowSection(
                                title = "Populares en España",
                                items = popularInSpain,
                                sharedTransitionScope = sharedTransitionScope,
                                animatedVisibilityScope = animatedVisibilityScope,
                                onItemClick = onMediaClick,
                                tag = "spain"
                            )
                        }
                    }

                    if (actionShows.isNotEmpty()) {
                        item {
                            ShowSection(
                                title = "Acción y Aventura",
                                items = actionShows,
                                sharedTransitionScope = sharedTransitionScope,
                                animatedVisibilityScope = animatedVisibilityScope,
                                onItemClick = onMediaClick,
                                tag = "action"
                            )
                        }
                    }

                    if (comedyShows.isNotEmpty()) {
                        item {
                            ShowSection(
                                title = "Comedias",
                                items = comedyShows,
                                sharedTransitionScope = sharedTransitionScope,
                                animatedVisibilityScope = animatedVisibilityScope,
                                onItemClick = onMediaClick,
                                tag = "comedy"
                            )
                        }
                    }

                    if (mysteryShows.isNotEmpty()) {
                        item {
                            ShowSection(
                                title = "Misterio y Suspense",
                                items = mysteryShows,
                                sharedTransitionScope = sharedTransitionScope,
                                animatedVisibilityScope = animatedVisibilityScope,
                                onItemClick = onMediaClick,
                                tag = "mystery"
                            )
                        }
                    }

                    item { Spacer(modifier = Modifier.height(100.dp)) }
                }
            }
        }
    }
}

@Composable
fun HomeTopAppBar() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .statusBarsPadding()
            .padding(vertical = 12.dp),
        contentAlignment = Alignment.Center
    ) {
        val gradientColors = listOf(com.example.showmateapp.ui.theme.PrimaryPurple, Color(0xFFE040FB))
        Text(
            text = stringResource(id = R.string.showmate),
            style = androidx.compose.ui.text.TextStyle(
                brush = Brush.linearGradient(colors = gradientColors)
            ),
            fontSize = 32.sp,
            fontWeight = FontWeight.Black,
            letterSpacing = (-1.5).sp,
            textAlign = TextAlign.Center
        )
    }
}

private fun navigateToDetail(navController: NavController, media: MediaContent, tag: String) {
    navController.navigate(Screen.Detail(media.id, tag))
}

@Composable
fun WhatToWatchDialog(
    media: MediaContent,
    onDismiss: () -> Unit,
    onPickAgain: () -> Unit,
    onViewDetails: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 8.dp,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column {
                val imageUrl = (media.backdropPath ?: media.posterPath)
                    ?.let { "https://image.tmdb.org/t/p/w780$it" }
                AsyncImage(
                    model = imageUrl,
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp)
                        .clip(RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp))
                )
                Column(modifier = Modifier.padding(20.dp)) {
                    Text(
                        text = "Para ti hoy",
                        color = PrimaryPurple,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = media.name,
                        color = MaterialTheme.colorScheme.onSurface,
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Black,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = media.overview.take(120).let { if (media.overview.length > 120) "$it…" else it },
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                        fontSize = 14.sp,
                        lineHeight = 20.sp
                    )
                    Spacer(modifier = Modifier.height(20.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        OutlinedButton(
                            onClick = onPickAgain,
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text("Otra vez")
                        }
                        Button(
                            onClick = onViewDetails,
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.buttonColors(containerColor = PrimaryPurple),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text("Ver detalles", color = Color.White, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
fun FeaturedBanner(
    media: MediaContent,
    sharedTransitionScope: SharedTransitionScope,
    animatedVisibilityScope: AnimatedVisibilityScope,
    onClick: (MediaContent) -> Unit
) {
    val tag = "hero"
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(480.dp)
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
                contentDescription = "Featured: ${media.name}",
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
        
        // Stronger bottom gradient for legibility
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
        }

        Column(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(20.dp)
        ) {
            // Genre / year info row
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
                    Text(
                        "$seasons temp.",
                        color = Color.White.copy(alpha = 0.7f),
                        fontSize = 13.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = media.name,
                color = Color.White,
                fontSize = 32.sp,
                fontWeight = FontWeight.Black,
                lineHeight = 36.sp,
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
                Text("Ver ahora", color = Color.Black, fontWeight = FontWeight.Black, fontSize = 15.sp)
            }
        }
    }
}
