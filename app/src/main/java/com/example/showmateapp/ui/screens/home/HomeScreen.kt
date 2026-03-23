package com.example.showmateapp.ui.screens.home

import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.filled.Tv
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.LiveTv
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Bolt
import java.util.Calendar
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.*
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.*
import kotlinx.coroutines.delay
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.TextStyle
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
import com.example.showmateapp.ui.theme.AccentBlue
import com.example.showmateapp.ui.theme.PrimaryPurple
import com.example.showmateapp.ui.theme.StarYellow
import com.example.showmateapp.ui.theme.SurfaceDark
import com.example.showmateapp.util.UiText
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.window.Dialog

@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
fun HomeScreen(
    navController: NavController,
    sharedTransitionScope: SharedTransitionScope,
    animatedVisibilityScope: AnimatedVisibilityScope,
    scrollToTopTrigger: Int = 0,
    viewModel: HomeViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    HomeScreenContent(
        userName = uiState.userName,
        scrollToTopTrigger = scrollToTopTrigger,
        upNextShows = uiState.upNextShows,
        upNextProgress = uiState.upNextProgress,
        trendingShows = uiState.trendingShows,
        top10Shows = uiState.top10Shows,
        newReleasesShows = uiState.newReleasesShows,
        actionShows = uiState.actionShows,
        comedyShows = uiState.comedyShows,
        mysteryShows = uiState.mysteryShows,
        thisWeekShows = uiState.thisWeekShows,
        selectedPlatform = uiState.selectedPlatform,
        platformShows = uiState.platformShows,
        isPlatformLoading = uiState.isPlatformLoading,
        isLoading = uiState.isLoading,
        isRefreshing = uiState.isRefreshing,
        errorMessage = uiState.errorMessage,
        sharedTransitionScope = sharedTransitionScope,
        animatedVisibilityScope = animatedVisibilityScope,
        onMediaClick = { media, tag -> navigateToDetail(navController, media, tag) },
        onRetry = { viewModel.loadData() },
        onRefresh = { viewModel.refresh() },
        onPickWhatToWatch = { viewModel.requestWhatToWatch() },
        onPlatformSelected = { viewModel.selectPlatform(it) }
    )

    if (uiState.showContextSelector) {
        ContextSelectorDialog(
            onDismiss = { viewModel.dismissContextSelector() },
            onConfirm = { mood, time -> viewModel.pickWhatToWatchToday(mood, time) }
        )
    }

    uiState.whatToWatchToday?.let { media ->
        WhatToWatchDialog(
            media = media,
            onDismiss = { viewModel.dismissWhatToWatch() },
            onViewDetails = {
                viewModel.dismissWhatToWatch()
                navigateToDetail(navController, media, "wtw")
            }
        )
    }
}

@OptIn(ExperimentalSharedTransitionApi::class, ExperimentalMaterial3Api::class)
@Composable
fun HomeScreenContent(
    userName: String = "",
    scrollToTopTrigger: Int = 0,
    upNextShows: List<MediaContent>,
    upNextProgress: Map<Int, Float> = emptyMap(),
    trendingShows: List<MediaContent>,
    top10Shows: List<MediaContent> = emptyList(),
    newReleasesShows: List<MediaContent> = emptyList(),
    actionShows: List<MediaContent>,
    comedyShows: List<MediaContent>,
    mysteryShows: List<MediaContent>,
    thisWeekShows: List<MediaContent>,
    selectedPlatform: String? = null,
    platformShows: Map<String, List<MediaContent>> = emptyMap(),
    isPlatformLoading: Boolean = false,
    isLoading: Boolean,
    isRefreshing: Boolean,
    errorMessage: UiText?,
    sharedTransitionScope: SharedTransitionScope,
    animatedVisibilityScope: AnimatedVisibilityScope,
    onMediaClick: (MediaContent, String) -> Unit,
    onRetry: () -> Unit,
    onRefresh: () -> Unit,
    onPickWhatToWatch: () -> Unit = {},
    onPlatformSelected: (String?) -> Unit = {}
) {
    if (isLoading && trendingShows.isEmpty()) {
        // Skeleton: muestra estructura mientras cargan los datos
        Scaffold(
            topBar = { HomeTopAppBar() },
            containerColor = MaterialTheme.colorScheme.background
        ) { paddingValues ->
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
            ) {
                // Hero skeleton
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(480.dp)
                            .padding(16.dp)
                            .clip(RoundedCornerShape(24.dp))
                            .background(shimmerBrush())
                    )
                }
                item { ShowSectionSkeleton("Tendencias") }
                item { ShowSectionSkeleton("Populares en España") }
                item { ShowSectionSkeleton("Acción y Aventura") }
                item { Spacer(modifier = Modifier.height(100.dp)) }
            }
        }
    } else if (errorMessage != null) {
        ErrorView(message = errorMessage.asString(), onRetry = onRetry)
    } else {
        Scaffold(
            topBar = { HomeTopAppBar(userName = userName, onPickWhatToWatch = onPickWhatToWatch) },
            containerColor = MaterialTheme.colorScheme.background
        ) { paddingValues ->
            val listState = rememberLazyListState()
            LaunchedEffect(scrollToTopTrigger) {
                if (scrollToTopTrigger > 0) listState.animateScrollToItem(0)
            }
            PullToRefreshBox(
                isRefreshing = isRefreshing,
                onRefresh = onRefresh,
                modifier = Modifier.padding(paddingValues)
            ) {
                LazyColumn(
                    state = listState,
                    modifier = Modifier.fillMaxSize()
                ) {
                    if (trendingShows.isNotEmpty()) {
                        item {
                            FeaturedBanner(
                                shows = trendingShows.take(5),
                                sharedTransitionScope = sharedTransitionScope,
                                animatedVisibilityScope = animatedVisibilityScope,
                                onClick = { media -> onMediaClick(media, "hero") }
                            )
                        }
                    }

                    if (upNextShows.isNotEmpty()) {
                        item {
                            UpNextSection(
                                shows = upNextShows,
                                progressMap = upNextProgress,
                                sharedTransitionScope = sharedTransitionScope,
                                animatedVisibilityScope = animatedVisibilityScope,
                                onItemClick = onMediaClick
                            )
                        }
                    }

                    item {
                        ShowSection(
                            title = stringResource(id = R.string.trending_now),
                            // Skip first 5 already shown in the banner
                            items = trendingShows.drop(5).ifEmpty { trendingShows },
                            sharedTransitionScope = sharedTransitionScope,
                            animatedVisibilityScope = animatedVisibilityScope,
                            onItemClick = onMediaClick,
                            tag = "trending",
                            subtitle = "Las más populares en este momento"
                        )
                    }

                    if (top10Shows.isNotEmpty()) {
                        item {
                            Top10Section(
                                shows = top10Shows,
                                sharedTransitionScope = sharedTransitionScope,
                                animatedVisibilityScope = animatedVisibilityScope,
                                onItemClick = onMediaClick
                            )
                        }
                    }

                    if (newReleasesShows.isNotEmpty()) {
                        item {
                            ShowSection(
                                title = "Recién estrenadas",
                                items = newReleasesShows,
                                sharedTransitionScope = sharedTransitionScope,
                                animatedVisibilityScope = animatedVisibilityScope,
                                onItemClick = onMediaClick,
                                tag = "new_releases",
                                subtitle = "Estrenos de los últimos 3 meses"
                            )
                        }
                    }

                    if (thisWeekShows.isNotEmpty()) {
                        item {
                            ThisWeekSection(
                                allShows = thisWeekShows,
                                selectedPlatform = selectedPlatform,
                                platformShows = platformShows,
                                isPlatformLoading = isPlatformLoading,
                                sharedTransitionScope = sharedTransitionScope,
                                animatedVisibilityScope = animatedVisibilityScope,
                                onMediaClick = onMediaClick,
                                onPlatformSelected = onPlatformSelected
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

private fun greeting(): String {
    val hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
    return when {
        hour < 12 -> "Buenos días"
        hour < 20 -> "Buenas tardes"
        else      -> "Buenas noches"
    }
}

@Composable
fun HomeTopAppBar(userName: String = "", onPickWhatToWatch: () -> Unit = {}) {
    val gradientColors = listOf(PrimaryPurple, Color(0xFFE040FB))
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .statusBarsPadding()
            .padding(horizontal = 16.dp, vertical = 12.dp)
    ) {
        Column(modifier = Modifier.align(Alignment.CenterStart)) {
            Text(
                text = stringResource(id = R.string.showmate),
                style = TextStyle(
                    brush = Brush.linearGradient(colors = gradientColors)
                ),
                fontSize = 32.sp,
                fontWeight = FontWeight.Black,
                letterSpacing = (-1.5).sp
            )
            if (userName.isNotBlank()) {
                Text(
                    text = "${greeting()}, $userName",
                    color = Color.White,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium
                )
            }
        }
        IconButton(
            onClick = onPickWhatToWatch,
            modifier = Modifier.align(Alignment.CenterEnd)
        ) {
            Icon(
                imageVector = Icons.Default.LiveTv,
                contentDescription = "¿Qué veo hoy?",
                tint = PrimaryPurple,
                modifier = Modifier.size(26.dp)
            )
        }
    }
}

private fun navigateToDetail(navController: NavController, media: MediaContent, tag: String) {
    navController.navigate(Screen.Detail(media.id, tag))
}

@Composable
fun WhatToWatchDialog(
    media: MediaContent,
    onDismiss: () -> Unit,
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
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "Para ti hoy",
                            color = PrimaryPurple,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.sp
                        )
                        if (media.affinityScore > 0f) {
                            val matchPct = (media.affinityScore * 10).toInt().coerceIn(0, 100)
                            val matchColor = when {
                                matchPct >= 80 -> Color(0xFF4CAF50)
                                matchPct >= 50 -> Color(0xFFFFC107)
                                else           -> Color.White.copy(alpha = 0.7f)
                            }
                            Surface(
                                color = matchColor.copy(alpha = 0.15f),
                                shape = androidx.compose.foundation.shape.RoundedCornerShape(20.dp)
                            ) {
                                Text(
                                    text = "$matchPct% Match",
                                    color = matchColor,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                                )
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = media.name,
                        color = MaterialTheme.colorScheme.onSurface,
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Black,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.height(20.dp))
                    Button(
                        onClick = onViewDetails,
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = PrimaryPurple),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Ver detalles", color = Color.White, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@Composable
fun ContextSelectorDialog(
    onDismiss: () -> Unit,
    onConfirm: (MoodOption?, TimeOption?) -> Unit
) {
    var selectedMood by remember { mutableStateOf<MoodOption?>(null) }
    var selectedTime by remember { mutableStateOf<TimeOption?>(null) }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 8.dp,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(24.dp), verticalArrangement = Arrangement.spacedBy(20.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Default.LiveTv,
                        contentDescription = null,
                        tint = PrimaryPurple,
                        modifier = Modifier.size(22.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = "¿Qué veo hoy?",
                        color = Color.White,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Black
                    )
                }

                // Mood selector
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("¿Cómo estás?", color = Color.White.copy(alpha = 0.7f), fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                        MoodOption.entries.forEach { mood ->
                            val selected = selectedMood == mood
                            Surface(
                                onClick = { selectedMood = if (selected) null else mood },
                                shape = RoundedCornerShape(14.dp),
                                color = if (selected) PrimaryPurple else Color.White.copy(alpha = 0.08f),
                                modifier = Modifier.weight(1f)
                            ) {
                                Column(
                                    modifier = Modifier.padding(vertical = 10.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Text(mood.emoji, fontSize = 18.sp)
                                    Text(
                                        mood.label,
                                        color = if (selected) Color.White else Color.White.copy(alpha = 0.6f),
                                        fontSize = 10.sp,
                                        fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
                                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                                    )
                                }
                            }
                        }
                    }
                }

                // Time selector
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("¿Cuánto tiempo tienes?", color = Color.White.copy(alpha = 0.7f), fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                        TimeOption.entries.forEach { time ->
                            val selected = selectedTime == time
                            Surface(
                                onClick = { selectedTime = if (selected) null else time },
                                shape = RoundedCornerShape(14.dp),
                                color = if (selected) AccentBlue else Color.White.copy(alpha = 0.08f),
                                modifier = Modifier.weight(1f)
                            ) {
                                Text(
                                    text = time.label,
                                    color = if (selected) Color.White else Color.White.copy(alpha = 0.6f),
                                    fontSize = 12.sp,
                                    fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
                                    modifier = Modifier.padding(vertical = 12.dp),
                                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                                )
                            }
                        }
                    }
                }

                Button(
                    onClick = { onConfirm(selectedMood, selectedTime) },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = PrimaryPurple),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Icon(Icons.Default.Bolt, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Sorpréndeme", fontWeight = FontWeight.Black, fontSize = 15.sp)
                }
            }
        }
    }
}

@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
fun FeaturedBanner(
    shows: List<MediaContent>,
    sharedTransitionScope: SharedTransitionScope,
    animatedVisibilityScope: AnimatedVisibilityScope,
    onClick: (MediaContent) -> Unit
) {
    if (shows.isEmpty()) return
    val tag = "hero"
    var currentIndex by remember { mutableIntStateOf(0) }

    LaunchedEffect(shows.size) {
        while (true) {
            delay(4500)
            currentIndex = (currentIndex + 1) % shows.size
        }
    }

    AnimatedContent(
        targetState = currentIndex,
        transitionSpec = { fadeIn(tween(600)) togetherWith fadeOut(tween(600)) },
        label = "banner_crossfade"
    ) { idx ->
        val media = shows[idx]
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
                    fontSize = 32.sp,
                    fontWeight = FontWeight.Black,
                    lineHeight = 36.sp,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Dot indicators
                if (shows.size > 1) {
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        repeat(shows.size) { dotIdx ->
                            Box(
                                modifier = Modifier
                                    .size(if (dotIdx == idx) 8.dp else 5.dp)
                                    .clip(CircleShape)
                                    .background(
                                        if (dotIdx == idx) Color.White
                                        else Color.White.copy(alpha = 0.35f)
                                    )
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                }

                Button(
                    onClick = { onClick(media) },
                    colors = ButtonDefaults.buttonColors(containerColor = Color.White),
                    shape = RoundedCornerShape(14.dp),
                    contentPadding = PaddingValues(horizontal = 24.dp, vertical = 14.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.PlayArrow,
                        contentDescription = null,
                        tint = Color.Black,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Ver ahora", color = Color.Black, fontWeight = FontWeight.Black, fontSize = 15.sp)
                }
            }
        }
    }
}

@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
fun Top10Section(
    shows: List<MediaContent>,
    sharedTransitionScope: SharedTransitionScope,
    animatedVisibilityScope: AnimatedVisibilityScope,
    onItemClick: (MediaContent, String) -> Unit
) {
    Column {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .width(3.dp)
                    .height(20.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(Color(0xFFE040FB))
            )
            Spacer(modifier = Modifier.width(10.dp))
            Text(
                text = "Top 10 esta semana",
                color = Color.White,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold
            )
        }
        LazyRow(
            contentPadding = PaddingValues(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(shows.take(10), key = { it.id }) { show ->
                val rank = shows.indexOf(show) + 1
                Box(
                    modifier = Modifier
                        .width(130.dp)
                        .height(190.dp)
                        .clickable { onItemClick(show, "top10") }
                ) {
                    with(sharedTransitionScope) {
                        AsyncImage(
                            model = ImageRequest.Builder(LocalContext.current)
                                .data(show.posterPath?.let { "https://image.tmdb.org/t/p/w500$it" })
                                .crossfade(true)
                                .build(),
                            contentDescription = show.name,
                            modifier = Modifier
                                .fillMaxSize()
                                .clip(RoundedCornerShape(14.dp))
                                .sharedElement(
                                    state = rememberSharedContentState(key = "image-${show.id}-top10"),
                                    animatedVisibilityScope = animatedVisibilityScope
                                ),
                            placeholder = painterResource(R.drawable.ic_logo_placeholder),
                            error = painterResource(R.drawable.ic_logo_placeholder),
                            contentScale = ContentScale.Crop
                        )
                    }
                    // Rank number badge - bottom-left, large
                    Box(
                        modifier = Modifier
                            .align(Alignment.BottomStart)
                            .offset(x = (-5).dp, y = 14.dp)
                    ) {
                        // Shadow layer
                        Text(
                            text = "$rank",
                            color = Color.Black.copy(alpha = 0.6f),
                            fontSize = if (rank < 10) 72.sp else 58.sp,
                            fontWeight = FontWeight.Black,
                            lineHeight = 72.sp,
                            modifier = Modifier.offset(x = 2.dp, y = 2.dp)
                        )
                        Text(
                            text = "$rank",
                            color = Color.White,
                            fontSize = if (rank < 10) 72.sp else 58.sp,
                            fontWeight = FontWeight.Black,
                            lineHeight = 72.sp
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
fun UpNextSection(
    shows: List<MediaContent>,
    progressMap: Map<Int, Float>,
    sharedTransitionScope: SharedTransitionScope,
    animatedVisibilityScope: AnimatedVisibilityScope,
    onItemClick: (MediaContent, String) -> Unit
) {
    Column {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .width(3.dp)
                    .height(20.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(PrimaryPurple)
            )
            Spacer(modifier = Modifier.width(10.dp))
            Text(
                text = "Continuar viendo",
                color = Color.White,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold
            )
        }
        LazyRow(
            contentPadding = PaddingValues(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(shows, key = { it.id }) { show ->
                val progress = progressMap[show.id] ?: 0f
                Column(modifier = Modifier.width(110.dp)) {
                    Box(
                        modifier = Modifier
                            .width(110.dp)
                            .aspectRatio(2f / 3f)
                            .clip(RoundedCornerShape(14.dp))
                            .clickable { onItemClick(show, "up_next") }
                    ) {
                        with(sharedTransitionScope) {
                            AsyncImage(
                                model = ImageRequest.Builder(LocalContext.current)
                                    .data(show.posterPath?.let { "https://image.tmdb.org/t/p/w500$it" })
                                    .crossfade(true)
                                    .build(),
                                contentDescription = show.name,
                                modifier = Modifier
                                    .fillMaxSize()
                                    .sharedElement(
                                        state = rememberSharedContentState(key = "image-${show.id}-up_next"),
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
                                        colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.5f)),
                                        startY = 100f
                                    )
                                )
                        )
                    }
                    if (progress > 0f) {
                        Spacer(modifier = Modifier.height(4.dp))
                        LinearProgressIndicator(
                            progress = { progress },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(3.dp)
                                .clip(CircleShape),
                            color = PrimaryPurple,
                            trackColor = Color.White.copy(alpha = 0.15f)
                        )
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = show.name,
                        color = Color.White,
                        fontSize = 12.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
fun ThisWeekSection(
    allShows: List<MediaContent>,
    selectedPlatform: String?,
    platformShows: Map<String, List<MediaContent>>,
    isPlatformLoading: Boolean,
    sharedTransitionScope: SharedTransitionScope,
    animatedVisibilityScope: AnimatedVisibilityScope,
    onMediaClick: (MediaContent, String) -> Unit,
    onPlatformSelected: (String?) -> Unit
) {
    val platforms = remember { listOf("Netflix", "Prime", "Disney+", "Max", "Paramount+") }
    val displayedShows = if (selectedPlatform != null) {
        platformShows[selectedPlatform] ?: emptyList()
    } else {
        allShows
    }

    Column {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .width(3.dp)
                    .height(20.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(AccentBlue)
            )
            Spacer(modifier = Modifier.width(10.dp))
            Icon(
                imageVector = Icons.Default.Tv,
                contentDescription = null,
                tint = AccentBlue,
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = "Esta semana en tus plataformas",
                color = Color.White,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold
            )
        }

        // Chips de plataformas filtrables
        LazyRow(
            modifier = Modifier.padding(bottom = 12.dp),
            contentPadding = PaddingValues(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(platforms, key = { it }) { platform ->
                val isSelected = selectedPlatform == platform
                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = if (isSelected) AccentBlue
                            else Color.White.copy(alpha = 0.07f),
                    modifier = Modifier.clickable { onPlatformSelected(platform) }
                ) {
                    Text(
                        text = platform,
                        color = if (isSelected) Color.White else Color.White.copy(alpha = 0.7f),
                        fontSize = 11.sp,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 5.dp)
                    )
                }
            }
        }

        // Lista de series (o spinner mientras carga)
        if (isPlatformLoading && selectedPlatform != null && !platformShows.containsKey(selectedPlatform)) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(130.dp),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(color = AccentBlue, modifier = Modifier.size(28.dp))
            }
        } else if (displayedShows.isEmpty() && selectedPlatform != null) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(100.dp),
                contentAlignment = Alignment.Center
            ) {
                Text("Sin novedades en $selectedPlatform esta semana", color = Color.White.copy(alpha = 0.4f), fontSize = 13.sp)
            }
        } else {
            LazyRow(
                contentPadding = PaddingValues(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(displayedShows, key = { it.id }) { show ->
                    ThisWeekCard(
                        media = show,
                        sharedTransitionScope = sharedTransitionScope,
                        animatedVisibilityScope = animatedVisibilityScope,
                        onClick = { onMediaClick(show, "thisweek") }
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
fun ThisWeekCard(
    media: MediaContent,
    sharedTransitionScope: SharedTransitionScope,
    animatedVisibilityScope: AnimatedVisibilityScope,
    onClick: () -> Unit
) {
    val imageUrl = (media.backdropPath ?: media.posterPath)
        ?.let { "https://image.tmdb.org/t/p/w500$it" }

    Box(
        modifier = Modifier
            .width(220.dp)
            .height(130.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(SurfaceDark)
            .clickable { onClick() }
    ) {
        with(sharedTransitionScope) {
            AsyncImage(
                model = ImageRequest.Builder(LocalContext.current)
                    .data(imageUrl)
                    .crossfade(true)
                    .build(),
                contentDescription = media.name,
                modifier = Modifier
                    .fillMaxSize()
                    .sharedElement(
                        state = rememberSharedContentState(key = "image-${media.id}-thisweek"),
                        animatedVisibilityScope = animatedVisibilityScope
                    ),
                placeholder = painterResource(R.drawable.ic_logo_placeholder),
                error = painterResource(R.drawable.ic_logo_placeholder),
                contentScale = ContentScale.Crop
            )
        }

        // Gradiente inferior
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color.Transparent,
                            Color.Black.copy(alpha = 0.85f)
                        ),
                        startY = 40f
                    )
                )
        )

        // Badge "EN ANTENA" arriba a la izquierda
        Row(
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(8.dp)
                .clip(RoundedCornerShape(6.dp))
                .background(AccentBlue)
                .padding(horizontal = 6.dp, vertical = 3.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(5.dp)
                    .clip(CircleShape)
                    .background(Color.White)
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = "EN ANTENA",
                color = Color.White,
                fontSize = 8.sp,
                fontWeight = FontWeight.ExtraBold,
                letterSpacing = 0.5.sp
            )
        }

        // Título y nota en la parte inferior
        Column(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(10.dp)
        ) {
            Text(
                text = media.name,
                color = Color.White,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            if (media.voteAverage > 0f) {
                Text(
                    text = "${"%.1f".format(media.voteAverage)} ★",
                    color = StarYellow,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium
                )
            }
        }
    }
}
