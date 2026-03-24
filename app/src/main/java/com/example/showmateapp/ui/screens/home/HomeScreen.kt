package com.example.showmateapp.ui.screens.home

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.animation.core.*
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.LiveTv
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Tv
import androidx.compose.material3.*
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.showmateapp.R
import com.example.showmateapp.data.network.MediaContent
import com.example.showmateapp.ui.components.premium.*
import com.example.showmateapp.ui.navigation.Screen
import com.example.showmateapp.ui.theme.AccentBlue
import com.example.showmateapp.ui.theme.PrimaryPurple
import com.example.showmateapp.ui.theme.PrimaryPurpleLight
import com.example.showmateapp.ui.theme.StarYellow
import com.example.showmateapp.ui.theme.SurfaceDark
import com.example.showmateapp.util.UiText
import java.util.Calendar
import kotlinx.coroutines.delay

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
    val trendingListState    = rememberLazyListState()
    val newReleasesListState = rememberLazyListState()
    val actionListState      = rememberLazyListState()
    val comedyListState      = rememberLazyListState()
    val mysteryListState     = rememberLazyListState()

    if (isLoading && trendingShows.isEmpty()) {
        Scaffold(
            topBar = { HomeTopAppBar() },
            containerColor = MaterialTheme.colorScheme.background
        ) { paddingValues ->
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
            ) {
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
                            items = trendingShows.drop(5).ifEmpty { trendingShows },
                            sharedTransitionScope = sharedTransitionScope,
                            animatedVisibilityScope = animatedVisibilityScope,
                            onItemClick = onMediaClick,
                            tag = "trending",
                            subtitle = "Las más populares en este momento",
                            listState = trendingListState
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
                                subtitle = "Estrenos de los últimos 3 meses",
                                listState = newReleasesListState
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
                                tag = "action",
                                listState = actionListState
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
                                tag = "comedy",
                                listState = comedyListState
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
                                tag = "mystery",
                                listState = mysteryListState
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
            .padding(horizontal = 16.dp, vertical = 10.dp)
    ) {
        Row(
            modifier = Modifier.align(Alignment.CenterStart),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            if (userName.isNotBlank()) {
                Box(
                    modifier = Modifier
                        .size(42.dp)
                        .background(Brush.linearGradient(gradientColors), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = userName.first().uppercaseChar().toString(),
                        color = Color.White,
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Black
                    )
                }
            }
            Column {
                Text(
                    text = "ShowMate",
                    style = TextStyle(brush = Brush.linearGradient(colors = gradientColors)),
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Black,
                    letterSpacing = (-1.5).sp
                )
                if (userName.isNotBlank()) {
                    Text(
                        text = "${greeting()}, $userName",
                        color = Color.White.copy(alpha = 0.55f),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }

        Box(
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .clip(RoundedCornerShape(14.dp))
                .background(PrimaryPurple.copy(alpha = 0.14f))
                .clickable(onClick = onPickWhatToWatch)
                .padding(horizontal = 12.dp, vertical = 8.dp),
            contentAlignment = Alignment.Center
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.LiveTv,
                    contentDescription = "¿Qué veo hoy?",
                    tint = PrimaryPurpleLight,
                    modifier = Modifier.size(18.dp)
                )
                Text(
                    text = "¿Qué veo?",
                    color = PrimaryPurpleLight,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

private fun navigateToDetail(navController: NavController, media: MediaContent, tag: String) {
    navController.navigate(Screen.Detail(media.id, tag))
}

private val tmdbGenreNames = mapOf(
    10759 to "Acción", 16 to "Animación", 35 to "Comedia", 80 to "Crimen",
    99 to "Documental", 18 to "Drama", 10751 to "Familiar", 9648 to "Misterio",
    10765 to "Sci-Fi", 10768 to "Política", 37 to "Western", 10762 to "Infantil",
    10764 to "Reality", 10766 to "Telenovela", 53 to "Thriller"
)

@Composable
fun WhatToWatchDialog(
    media: MediaContent,
    onDismiss: () -> Unit,
    onViewDetails: () -> Unit
) {
    val matchPct = if (media.affinityScore > 0f)
        (media.affinityScore * 10).toInt().coerceIn(0, 100) else -1
    val matchColor = when {
        matchPct >= 80 -> Color(0xFF4CAF50)
        matchPct >= 50 -> Color(0xFFFFC107)
        matchPct >= 0  -> Color.White.copy(alpha = 0.7f)
        else           -> null
    }

    val matchPulse = rememberInfiniteTransition(label = "matchPulse")
    val matchGlow by matchPulse.animateFloat(
        initialValue = 0.15f, targetValue = 0.35f,
        animationSpec = infiniteRepeatable(
            tween(1200, easing = FastOutSlowInEasing), RepeatMode.Reverse
        ), label = "glow"
    )

    val genreNames = remember(media.id) {
        (media.genres?.map { it.name }?.takeIf { it.isNotEmpty() }
            ?: media.safeGenreIds.mapNotNull { tmdbGenreNames[it] }).take(3)
    }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(28.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 12.dp,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(260.dp)
                ) {
                    val imageUrl = (media.backdropPath ?: media.posterPath)
                        ?.let { "https://image.tmdb.org/t/p/w780$it" }
                    AsyncImage(
                        model = imageUrl,
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .fillMaxSize()
                            .clip(RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp))
                    )

                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .clip(RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp))
                            .background(
                                Brush.verticalGradient(
                                    colors = listOf(
                                        Color.Black.copy(alpha = 0.1f),
                                        Color.Transparent,
                                        Color.Black.copy(alpha = 0.7f),
                                        Color.Black.copy(alpha = 0.95f)
                                    )
                                )
                            )
                    )

                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(10.dp)
                            .size(34.dp)
                            .background(Color.Black.copy(alpha = 0.45f), CircleShape)
                    ) {
                        Icon(
                            Icons.Default.Close,
                            contentDescription = "Cerrar",
                            tint = Color.White,
                            modifier = Modifier.size(18.dp)
                        )
                    }

                    if (matchColor != null && matchPct >= 0) {
                        Box(
                            modifier = Modifier
                                .align(Alignment.TopStart)
                                .padding(12.dp)
                                .clip(RoundedCornerShape(20.dp))
                                .background(matchColor.copy(alpha = matchGlow))
                                .border(1.dp, matchColor.copy(alpha = 0.6f), RoundedCornerShape(20.dp))
                                .padding(horizontal = 10.dp, vertical = 5.dp)
                        ) {
                            Text(
                                text = "$matchPct% match",
                                color = Color.White,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.ExtraBold
                            )
                        }
                    }

                    Column(
                        modifier = Modifier
                            .align(Alignment.BottomStart)
                            .padding(horizontal = 18.dp, vertical = 14.dp)
                    ) {
                        Text(
                            text = "PARA TI HOY",
                            color = PrimaryPurpleLight,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.ExtraBold,
                            letterSpacing = 2.sp
                        )
                        Spacer(Modifier.height(4.dp))
                        Text(
                            text = media.name,
                            color = Color.White,
                            fontSize = 24.sp,
                            fontWeight = FontWeight.Black,
                            lineHeight = 28.sp,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                        Spacer(Modifier.height(6.dp))
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            if (media.voteAverage > 0f) {
                                Icon(
                                    Icons.Default.Star,
                                    contentDescription = null,
                                    tint = StarYellow,
                                    modifier = Modifier.size(13.dp)
                                )
                                Text(
                                    text = "%.1f".format(media.voteAverage),
                                    color = StarYellow,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            val year = media.firstAirDate?.take(4)
                            if (year != null) {
                                Text("·", color = Color.White.copy(alpha = 0.4f), fontSize = 13.sp)
                                Text(year, color = Color.White.copy(alpha = 0.7f), fontSize = 13.sp)
                            }
                            media.numberOfSeasons?.let {
                                Text("·", color = Color.White.copy(alpha = 0.4f), fontSize = 13.sp)
                                Text("$it temp.", color = Color.White.copy(alpha = 0.7f), fontSize = 13.sp)
                            }
                        }
                    }
                }

                Column(modifier = Modifier.padding(horizontal = 18.dp, vertical = 16.dp)) {
                    if (media.overview.isNotBlank()) {
                        Text(
                            text = media.overview,
                            color = Color.White.copy(alpha = 0.65f),
                            fontSize = 13.sp,
                            lineHeight = 19.sp,
                            maxLines = 3,
                            overflow = TextOverflow.Ellipsis
                        )
                        Spacer(Modifier.height(14.dp))
                    }

                    if (genreNames.isNotEmpty()) {
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            genreNames.forEach { genre ->
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(20.dp))
                                        .background(Color.White.copy(alpha = 0.08f))
                                        .border(1.dp, Color.White.copy(alpha = 0.12f), RoundedCornerShape(20.dp))
                                        .padding(horizontal = 10.dp, vertical = 4.dp)
                                ) {
                                    Text(
                                        text = genre,
                                        color = Color.White.copy(alpha = 0.75f),
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Medium
                                    )
                                }
                            }
                        }
                        Spacer(Modifier.height(16.dp))
                    }

                    Button(
                        onClick = onViewDetails,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = PrimaryPurple),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Icon(
                            Icons.Default.PlayArrow,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(
                            "Ver detalles",
                            fontWeight = FontWeight.Black,
                            fontSize = 15.sp
                        )
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
    val bannerDuration = 4500

    LaunchedEffect(shows.size) {
        while (true) {
            delay(bannerDuration.toLong())
            currentIndex = (currentIndex + 1) % shows.size
        }
    }

    AnimatedContent(
        targetState = currentIndex,
        transitionSpec = { fadeIn(tween(700)) togetherWith fadeOut(tween(700)) },
        label = "banner_crossfade"
    ) { idx ->
        val media = shows[idx]
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(500.dp)
                .padding(horizontal = 16.dp, vertical = 8.dp)
                .clip(RoundedCornerShape(28.dp))
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
                                Color.Black.copy(alpha = 0.05f),
                                Color.Transparent,
                                Color.Black.copy(alpha = 0.6f),
                                Color.Black.copy(alpha = 0.95f)
                            ),
                            startY = 0f
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

                Spacer(modifier = Modifier.height(14.dp))

                if (shows.size > 1) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(5.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        repeat(shows.size) { dotIdx ->
                            val isActive = dotIdx == idx
                            val progressAnim = remember { Animatable(0f) }
                            LaunchedEffect(idx) {
                                if (dotIdx == idx) {
                                    progressAnim.snapTo(0f)
                                    progressAnim.animateTo(
                                        1f,
                                        tween(bannerDuration, easing = LinearEasing)
                                    )
                                } else {
                                    progressAnim.snapTo(0f)
                                }
                            }
                            Box(
                                modifier = Modifier
                                    .height(3.dp)
                                    .width(if (isActive) 32.dp else 6.dp)
                                    .clip(CircleShape)
                                    .background(Color.White.copy(alpha = 0.25f))
                            ) {
                                if (isActive) {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxHeight()
                                            .fillMaxWidth(progressAnim.value)
                                            .background(Color.White)
                                    )
                                }
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(14.dp))
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
                    Box(
                        modifier = Modifier
                            .align(Alignment.BottomStart)
                            .offset(x = (-4).dp, y = 10.dp)
                    ) {
                        Text(
                            text = "$rank",
                            style = TextStyle(
                                brush = Brush.verticalGradient(
                                    colors = listOf(
                                        Color.White,
                                        Color.White.copy(alpha = 0.05f)
                                    )
                                )
                            ),
                            fontSize = if (rank < 10) 74.sp else 60.sp,
                            fontWeight = FontWeight.Black,
                            lineHeight = 74.sp,
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
                Column(modifier = Modifier.width(130.dp)) {
                    Box(
                        modifier = Modifier
                            .width(130.dp)
                            .aspectRatio(2f / 3f)
                            .clip(RoundedCornerShape(16.dp))
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
                                        colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.55f)),
                                        startY = 120f
                                    )
                                )
                        )

                        Box(
                            modifier = Modifier
                                .align(Alignment.Center)
                                .size(40.dp)
                                .background(Color.Black.copy(alpha = 0.5f), CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.PlayArrow,
                                contentDescription = "Reproducir",
                                tint = Color.White,
                                modifier = Modifier.size(24.dp)
                            )
                        }

                        if (progress > 0f) {
                            LinearProgressIndicator(
                                progress = { progress },
                                modifier = Modifier
                                    .align(Alignment.BottomCenter)
                                    .fillMaxWidth()
                                    .height(3.dp),
                                color = PrimaryPurple,
                                trackColor = Color.White.copy(alpha = 0.18f)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(6.dp))
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

        LazyRow(
            modifier = Modifier.padding(bottom = 12.dp),
            contentPadding = PaddingValues(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(platforms, key = { it }) { platform ->
                val isSelected = selectedPlatform == platform
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(20.dp))
                        .background(
                            if (isSelected) AccentBlue
                            else Color.White.copy(alpha = 0.07f)
                        )
                        .then(
                            if (isSelected) Modifier
                            else Modifier.border(
                                width = 1.dp,
                                color = Color.White.copy(alpha = 0.12f),
                                shape = RoundedCornerShape(20.dp)
                            )
                        )
                        .clickable { onPlatformSelected(platform) }
                        .padding(horizontal = 14.dp, vertical = 6.dp)
                ) {
                    Text(
                        text = platform,
                        color = if (isSelected) Color.White else Color.White.copy(alpha = 0.65f),
                        fontSize = 12.sp,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                    )
                }
            }
        }

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
                Text(
                    "Sin novedades en $selectedPlatform esta semana",
                    color = Color.White.copy(alpha = 0.4f),
                    fontSize = 13.sp
                )
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
