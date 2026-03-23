package com.example.showmateapp.ui.screens.detail

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.StarBorder
import androidx.compose.material3.*
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
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
import com.example.showmateapp.data.network.Provider
import com.example.showmateapp.ui.components.premium.*
import com.example.showmateapp.ui.navigation.Screen
import com.example.showmateapp.ui.theme.HeartRed
import com.example.showmateapp.ui.theme.PrimaryPurple
import com.example.showmateapp.ui.theme.StarYellow
import com.example.showmateapp.ui.theme.TextGray
import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalUriHandler
import com.example.showmateapp.data.network.CountryProviders
import kotlinx.coroutines.launch

@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
fun DetailScreen(
    navController: NavController,
    showId: Int,
    sharedElementTag: String?,
    sharedTransitionScope: SharedTransitionScope,
    animatedVisibilityScope: AnimatedVisibilityScope,
    viewModel: DetailViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val showWhyDialog by viewModel.showWhyDialog.collectAsState()
    val whyFactors by viewModel.whyFactors.collectAsState()

    LaunchedEffect(showId) {
        viewModel.loadShowDetails(showId)
    }

    if (showWhyDialog) {
        WhyRecommendedDialog(factors = whyFactors, onDismiss = { viewModel.dismissWhyDialog() })
    }

    DetailScreenContent(
        uiState = uiState,
        onBackClick = { navController.popBackStack() },
        onLikeClick = { viewModel.requestToggleLiked() },
        onEssentialClick = { viewModel.toggleEssential() },
        onWatchedClick = { viewModel.requestToggleWatched() },
        onRateClick = { viewModel.rateShow(it) },
        onClearRateClick = { viewModel.clearRating() },
        onRetry = { viewModel.loadShowDetails(showId) },
        onSimilarShowClick = { show, tag ->
            navController.navigate(Screen.Detail(showId = show.id, sharedElementTag = tag))
        },
        onEpisodeToggle = { viewModel.toggleEpisodeWatched(it) },
        onSeasonChange = { showId, seasonNum -> viewModel.loadSeasonDetails(showId, seasonNum) },
        onClearActionError = { viewModel.clearActionError() },
        onReviewTextChange = { viewModel.onReviewTextChange(it) },
        onSaveReview = { viewModel.saveReview() },
        onDeleteReview = { viewModel.deleteReview() },
        onMarkNextEpisode = { viewModel.markNextEpisodeWatched() },
        onShowAddToListDialog = { viewModel.showAddToListDialog() },
        onHideAddToListDialog = { viewModel.hideAddToListDialog() },
        onAddToList = { viewModel.addToList(it) },
        onWhyDialogClick = { viewModel.showWhyDialog() },
        whyFactors = whyFactors,
        sharedTransitionScope = sharedTransitionScope,
        animatedVisibilityScope = animatedVisibilityScope,
        sharedElementTag = sharedElementTag
    )

    if (uiState.showUnlikeConfirm) {
        AlertDialog(
            onDismissRequest = { viewModel.cancelConfirm() },
            containerColor = Color(0xFF1A1A2E),
            title = { Text("Quitar de favoritos", color = Color.White, fontWeight = FontWeight.Bold) },
            text = { Text("¿Seguro que quieres quitar esta serie de tus favoritos?", color = TextGray) },
            confirmButton = {
                Button(
                    onClick = { viewModel.toggleLiked() },
                    colors = ButtonDefaults.buttonColors(containerColor = HeartRed)
                ) { Text("Quitar", fontWeight = FontWeight.Bold) }
            },
            dismissButton = {
                TextButton(onClick = { viewModel.cancelConfirm() }) {
                    Text("Cancelar", color = TextGray)
                }
            }
        )
    }

    if (uiState.showUnwatchConfirm) {
        AlertDialog(
            onDismissRequest = { viewModel.cancelConfirm() },
            containerColor = Color(0xFF1A1A2E),
            title = { Text("Marcar como no vista", color = Color.White, fontWeight = FontWeight.Bold) },
            text = { Text("¿Seguro que quieres marcar esta serie como no vista?", color = TextGray) },
            confirmButton = {
                Button(
                    onClick = { viewModel.toggleWatched() },
                    colors = ButtonDefaults.buttonColors(containerColor = PrimaryPurple)
                ) { Text("Confirmar", fontWeight = FontWeight.Bold) }
            },
            dismissButton = {
                TextButton(onClick = { viewModel.cancelConfirm() }) {
                    Text("Cancelar", color = TextGray)
                }
            }
        )
    }
}

@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
fun DetailScreenContent(
    uiState: DetailUiState,
    onBackClick: () -> Unit,
    onLikeClick: () -> Unit,
    onEssentialClick: () -> Unit,
    onWatchedClick: () -> Unit,
    onRateClick: (Int) -> Unit,
    onClearRateClick: () -> Unit,
    onRetry: () -> Unit,
    onSimilarShowClick: (MediaContent, String) -> Unit,
    onEpisodeToggle: (Int) -> Unit,
    onSeasonChange: (Int, Int) -> Unit,
    onClearActionError: () -> Unit,
    onReviewTextChange: (String) -> Unit = {},
    onSaveReview: () -> Unit = {},
    onDeleteReview: () -> Unit = {},
    onMarkNextEpisode: () -> Unit = {},
    onShowAddToListDialog: () -> Unit = {},
    onHideAddToListDialog: () -> Unit = {},
    onAddToList: (String) -> Unit = {},
    onWhyDialogClick: () -> Unit = {},
    whyFactors: List<WhyFactor> = emptyList(),
    sharedTransitionScope: SharedTransitionScope,
    animatedVisibilityScope: AnimatedVisibilityScope,
    sharedElementTag: String?
) {
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val haptic = LocalHapticFeedback.current

    if (uiState.showAddToListDialog) {
        AlertDialog(
            onDismissRequest = onHideAddToListDialog,
            containerColor = Color(0xFF1A1A2E),
            title = {
                Text("Añadir a lista", color = Color.White, fontWeight = FontWeight.Bold)
            },
            text = {
                if (uiState.customLists.isEmpty()) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                        Icon(Icons.AutoMirrored.Filled.List, contentDescription = null, tint = PrimaryPurple.copy(alpha = 0.5f), modifier = Modifier.size(40.dp))
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            "Aún no tienes listas. Crea una desde tu perfil.",
                            color = TextGray,
                            fontSize = 14.sp,
                            textAlign = TextAlign.Center
                        )
                    }
                } else {
                    Column {
                        uiState.customLists.entries.toList().forEach { (name, ids) ->
                            val alreadyAdded = uiState.media?.id?.let { ids.contains(it) } == true
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable(enabled = !alreadyAdded) { onAddToList(name) }
                                    .padding(vertical = 12.dp, horizontal = 4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(36.dp)
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(PrimaryPurple.copy(alpha = 0.15f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(Icons.AutoMirrored.Filled.List, contentDescription = null, tint = PrimaryPurple, modifier = Modifier.size(20.dp))
                                }
                                Spacer(modifier = Modifier.width(16.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(name, color = if (alreadyAdded) TextGray else Color.White, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
                                    Text("${ids.size} series", color = TextGray, fontSize = 12.sp)
                                }
                                if (alreadyAdded) {
                                    Icon(Icons.Default.Check, contentDescription = null, tint = Color(0xFF4CAF50), modifier = Modifier.size(18.dp))
                                }
                            }
                            HorizontalDivider(color = Color.White.copy(alpha = 0.06f))
                        }
                    }
                }
            },
            confirmButton = {},
            dismissButton = {
                TextButton(onClick = onHideAddToListDialog) {
                    Text("Cerrar", color = TextGray)
                }
            }
        )
    }

    LaunchedEffect(uiState.actionError) {
        uiState.actionError?.let {
            scope.launch {
                snackbarHostState.showSnackbar(it, duration = SnackbarDuration.Short)
                onClearActionError()
            }
        }
    }

    if (uiState.isLoading) {
        DetailScreenSkeleton()
        return
    }

    if (uiState.errorMessage != null) {
        ErrorView(message = uiState.errorMessage, onRetry = onRetry)
        return
    }

    val show = uiState.media ?: return

    Box(modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier.align(Alignment.BottomCenter)
        )
        
        Box(modifier = Modifier.fillMaxWidth().height(450.dp)) {
            val imageUrl = remember(show.backdropPath, show.posterPath) {
                (show.backdropPath ?: show.posterPath)?.let { "https://image.tmdb.org/t/p/w1280$it" }
            }
            
            with(sharedTransitionScope) {
                val sharedElementKey = if (sharedElementTag != null) {
                    "image-${show.id}-$sharedElementTag"
                } else {
                    "image-${show.id}"
                }

                AsyncImage(
                    model = ImageRequest.Builder(LocalContext.current)
                        .data(imageUrl)
                        .crossfade(true)
                        .build(),
                    contentDescription = null,
                    modifier = Modifier
                        .fillMaxSize()
                        .sharedElement(
                            state = rememberSharedContentState(key = sharedElementKey),
                            animatedVisibilityScope = animatedVisibilityScope
                        ),
                    placeholder = painterResource(R.drawable.ic_logo_placeholder),
                    error = painterResource(R.drawable.ic_logo_placeholder),
                    contentScale = ContentScale.Crop
                )
            }
            
            val backgroundGradient = remember {
                Brush.verticalGradient(
                    colors = listOf(Color.Black.copy(alpha = 0.4f), Color.Transparent, Color.Black.copy(alpha = 0.8f)),
                    startY = 0f,
                    endY = Float.POSITIVE_INFINITY
                )
            }
            Box(
                modifier = Modifier.fillMaxSize().background(backgroundGradient)
            )
        }

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(bottom = 32.dp)
        ) {
            item {
                Spacer(modifier = Modifier.height(320.dp))
            }
            
            item {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    color = MaterialTheme.colorScheme.background,
                    shape = RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp)
                ) {
                    Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 24.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.Top
                        ) {
                            Text(
                                text = show.name,
                                style = MaterialTheme.typography.headlineLarge.copy(fontWeight = FontWeight.Black, fontSize = 34.sp),
                                color = Color.White,
                                modifier = Modifier.weight(1f)
                            )
                            if (show.affinityScore > 0f && whyFactors.isNotEmpty()) {
                                Column(horizontalAlignment = Alignment.End) {
                                    MatchBadge(
                                        score = show.affinityScore,
                                        isAffinity = true,
                                        modifier = Modifier.clickable { onWhyDialogClick() }
                                    )
                                    Text(
                                        text = "¿Por qué?",
                                        color = PrimaryPurple.copy(alpha = 0.7f),
                                        fontSize = 10.sp,
                                        modifier = Modifier
                                            .padding(top = 2.dp)
                                            .clickable { onWhyDialogClick() }
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        val year = remember(show.firstAirDate) { show.firstAirDate?.take(4) ?: "N/A" }
                        val seasons = remember(show.numberOfSeasons) { show.numberOfSeasons?.let { "$it ${if (it == 1) "Temporada" else "Temporadas"}" } ?: "N/A" }
                        
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(text = year, style = MaterialTheme.typography.titleSmall, color = TextGray)
                            Text(text = " • ", style = MaterialTheme.typography.titleSmall, color = TextGray)
                            Text(text = seasons, style = MaterialTheme.typography.titleSmall, color = TextGray)
                            Text(text = " • ", style = MaterialTheme.typography.titleSmall, color = PrimaryPurple, fontWeight = FontWeight.Bold)
                        }

                        Spacer(modifier = Modifier.height(24.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            val watchedColor by animateColorAsState(
                                targetValue = if (uiState.isWatched) Color(0xFF4CAF50) else Color.White.copy(alpha = 0.1f),
                                label = "watchedColor"
                            )
                            ActionIconButton(
                                icon = Icons.Default.Check,
                                text = if (uiState.isWatched) "Vista" else "Marcar vista",
                                containerColor = watchedColor,
                                contentColor = if (uiState.isWatched) Color.White else TextGray,
                                modifier = Modifier.weight(1.2f),
                                onClick = {
                                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                    onWatchedClick()
                                }
                            )

                            val likeColor by animateColorAsState(
                                targetValue = if (uiState.isLiked) HeartRed else Color.White.copy(alpha = 0.1f),
                                label = "likeColor"
                            )
                            ActionIconButton(
                                icon = if (uiState.isLiked) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                                text = "Me gusta",
                                containerColor = likeColor,
                                contentColor = Color.White,
                                modifier = Modifier.weight(1f),
                                onClick = {
                                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                    onLikeClick()
                                }
                            )

                            val essentialColor by animateColorAsState(
                                targetValue = if (uiState.isEssential) StarYellow else Color.White.copy(alpha = 0.1f),
                                label = "essentialColor"
                            )
                            ActionIconButton(
                                icon = if (uiState.isEssential) Icons.Default.Star else Icons.Default.StarBorder,
                                text = "Top",
                                containerColor = essentialColor,
                                contentColor = Color.White,
                                modifier = Modifier.weight(0.8f),
                                onClick = {
                                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                    onEssentialClick()
                                }
                            )
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        OutlinedButton(
                            onClick = onShowAddToListDialog,
                            modifier = Modifier.fillMaxWidth().height(46.dp),
                            border = BorderStroke(1.dp, PrimaryPurple.copy(alpha = 0.5f)),
                            shape = RoundedCornerShape(16.dp),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = PrimaryPurple)
                        ) {
                            Icon(Icons.AutoMirrored.Filled.List, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Añadir a lista", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        }

                        val trailerKey = show.videos?.results?.firstOrNull { it.site == "YouTube" && it.type == "Trailer" }?.key
                        if (trailerKey != null) {
                            Spacer(modifier = Modifier.height(24.dp))
                            val uriHandler = LocalUriHandler.current
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(180.dp)
                                    .clip(RoundedCornerShape(16.dp))
                                    .clickable { uriHandler.openUri("https://www.youtube.com/watch?v=$trailerKey") }
                            ) {
                                AsyncImage(
                                    model = ImageRequest.Builder(LocalContext.current)
                                        .data("https://img.youtube.com/vi/$trailerKey/mqdefault.jpg")
                                        .crossfade(true)
                                        .build(),
                                    contentDescription = "Tráiler de ${show.name}",
                                    modifier = Modifier.fillMaxSize(),
                                    contentScale = ContentScale.Crop,
                                    placeholder = painterResource(R.drawable.ic_logo_placeholder)
                                )
                                Box(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .background(Color.Black.copy(alpha = 0.35f))
                                )
                                Box(
                                    modifier = Modifier
                                        .size(60.dp)
                                        .align(Alignment.Center)
                                        .clip(CircleShape)
                                        .background(Color.White.copy(alpha = 0.92f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        Icons.Default.PlayArrow,
                                        contentDescription = null,
                                        tint = Color.Black,
                                        modifier = Modifier.size(34.dp)
                                    )
                                }
                                Surface(
                                    modifier = Modifier
                                        .align(Alignment.BottomStart)
                                        .padding(12.dp),
                                    color = Color.Black.copy(alpha = 0.6f),
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    Text(
                                        "Tráiler Oficial",
                                        color = Color.White,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 12.sp,
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(24.dp))

                        // Tabs: Información / Mi reseña
                        var selectedDetailTab by remember { mutableIntStateOf(0) }
                        val detailTabs = listOf("Información", "Mi reseña")

                        val hasReviewContent = uiState.userRating != null || uiState.isReviewSaved

                        TabRow(
                            selectedTabIndex = selectedDetailTab,
                            containerColor = Color.Transparent,
                            contentColor = Color.White,
                            indicator = { tabPositions ->
                                if (selectedDetailTab < tabPositions.size) {
                                    TabRowDefaults.SecondaryIndicator(
                                        Modifier.tabIndicatorOffset(tabPositions[selectedDetailTab]),
                                        color = PrimaryPurple
                                    )
                                }
                            },
                            divider = { HorizontalDivider(color = Color.White.copy(alpha = 0.1f)) }
                        ) {
                            detailTabs.forEachIndexed { index, title ->
                                Tab(
                                    selected = selectedDetailTab == index,
                                    onClick = { selectedDetailTab = index },
                                    text = {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.Center
                                        ) {
                                            Text(
                                                text = title,
                                                fontWeight = if (selectedDetailTab == index) FontWeight.Bold else FontWeight.Normal,
                                                color = if (selectedDetailTab == index) PrimaryPurple else TextGray
                                            )
                                            if (index == 1 && hasReviewContent) {
                                                Spacer(modifier = Modifier.width(5.dp))
                                                Box(
                                                    modifier = Modifier
                                                        .size(6.dp)
                                                        .clip(CircleShape)
                                                        .background(PrimaryPurple)
                                                )
                                            }
                                        }
                                    }
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(24.dp))

                        AnimatedContent(
                            targetState = selectedDetailTab,
                            transitionSpec = {
                                val direction = if (targetState > initialState) 1 else -1
                                (slideInHorizontally(tween(300)) { it * direction } + fadeIn(tween(200))) togetherWith
                                (slideOutHorizontally(tween(300)) { -it * direction } + fadeOut(tween(200)))
                            },
                            label = "detail_tab_anim"
                        ) { tab ->
                        if (tab == 0) {
                            // Tab Información
                            Column(modifier = Modifier.fillMaxWidth()) {
                            val esProviders = show.watchProviders?.results?.get("ES")
                            if (esProviders != null &&
                                (!esProviders.flatrate.isNullOrEmpty() ||
                                 !esProviders.rent.isNullOrEmpty() ||
                                 !esProviders.buy.isNullOrEmpty())
                            ) {
                                WatchProvidersSection(providers = esProviders, showName = show.name)
                                Spacer(modifier = Modifier.height(32.dp))
                            }

                            var isSynopsisExpanded by remember { mutableStateOf(false) }
                            Text(
                                text = stringResource(R.string.detail_synopsis),
                                style = MaterialTheme.typography.titleLarge,
                                color = Color.White,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(
                                text = show.overview.ifEmpty { stringResource(R.string.detail_no_synopsis) },
                                style = MaterialTheme.typography.bodyLarge,
                                color = TextGray,
                                lineHeight = 24.sp,
                                maxLines = if (isSynopsisExpanded) Int.MAX_VALUE else 3,
                                modifier = Modifier
                                    .animateContentSize()
                                    .clickable { isSynopsisExpanded = !isSynopsisExpanded }
                            )

                            if (!show.seasons.isNullOrEmpty()) {
                                Spacer(modifier = Modifier.height(32.dp))
                                EpisodesSection(
                                    seasons = show.seasons,
                                    selectedSeason = uiState.selectedSeason,
                                    isSeasonLoading = uiState.isSeasonLoading,
                                    watchedEpisodes = uiState.watchedEpisodes,
                                    onEpisodeToggle = onEpisodeToggle,
                                    onSeasonChange = { seasonNum ->
                                        (uiState.media?.id)?.let { id ->
                                            onSeasonChange(id, seasonNum)
                                        }
                                    },
                                    onMarkNextEpisode = onMarkNextEpisode
                                )
                            }

                            val cast = show.credits?.cast ?: emptyList()
                            if (cast.isNotEmpty()) {
                                Spacer(modifier = Modifier.height(32.dp))
                                Text(
                                    text = stringResource(R.string.detail_top_cast),
                                    style = MaterialTheme.typography.titleLarge,
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold
                                )
                                Spacer(modifier = Modifier.height(16.dp))
                                LazyRow(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                                    items(cast.take(10), key = { it.id }) { member ->
                                        CastMemberItem(member)
                                    }
                                }
                            }

                            if (uiState.isSimilarLoading || uiState.similarShows.isNotEmpty()) {
                                Spacer(modifier = Modifier.height(32.dp))
                                Text(
                                    text = "Te puede gustar",
                                    style = MaterialTheme.typography.titleLarge,
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold
                                )
                                Spacer(modifier = Modifier.height(16.dp))
                                if (uiState.isSimilarLoading) {
                                    LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                        items(5) {
                                            Box(
                                                modifier = Modifier
                                                    .width(120.dp)
                                                    .height(170.dp)
                                                    .clip(RoundedCornerShape(12.dp))
                                                    .background(Color.White.copy(alpha = 0.08f))
                                            )
                                        }
                                    }
                                } else {
                                    LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                        items(uiState.similarShows, key = { it.id }) { similarShow ->
                                            ShowCard(
                                                media = similarShow,
                                                sharedTransitionScope = sharedTransitionScope,
                                                animatedVisibilityScope = animatedVisibilityScope,
                                                onClick = { s, t -> onSimilarShowClick(s, t) },
                                                tag = "similar"
                                            )
                                        }
                                    }
                                }
                            }
                            } // cierra Column tab 0
                        } else {
                            // Tab Mi reseña
                            Column(modifier = Modifier.fillMaxWidth()) {
                                StarRatingSection(
                                    userRating = uiState.userRating,
                                    onRateClick = onRateClick,
                                    onClearRateClick = onClearRateClick
                                )

                                Spacer(modifier = Modifier.height(24.dp))

                                ReviewSection(
                                    reviewText = uiState.userReview,
                                    isSaving = uiState.isSavingReview,
                                    isReviewSaved = uiState.isReviewSaved,
                                    onTextChange = onReviewTextChange,
                                    onSave = onSaveReview,
                                    onDelete = onDeleteReview
                                )
                            } // cierra Column tab 1
                        }
                        } // cierra AnimatedContent lambda
                    }
                }
            }
        }

        Surface(
            onClick = onBackClick,
            shape = CircleShape,
            color = Color.Black.copy(alpha = 0.4f),
            modifier = Modifier.statusBarsPadding().padding(16.dp).size(44.dp)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = Color.White)
            }
        }
    }
}

@Composable
fun WhyRecommendedDialog(
    factors: List<WhyFactor>,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Color(0xFF1A1A2E),
        title = {
            Text(
                "¿Por qué te lo recomendamos?",
                color = Color.White,
                fontWeight = FontWeight.Bold,
                fontSize = 17.sp
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                if (factors.isEmpty()) {
                    Text(
                        "Interactúa con más series para obtener recomendaciones personalizadas.",
                        color = TextGray,
                        fontSize = 14.sp,
                        lineHeight = 20.sp
                    )
                } else {
                    factors.forEach { factor ->
                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Text(text = factor.emoji, fontSize = 16.sp)
                                Text(
                                    text = factor.label,
                                    color = Color.White,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    modifier = Modifier.weight(1f)
                                )
                                Text(
                                    text = "${(factor.score * 100).toInt()}%",
                                    color = when {
                                        factor.score > 0.7f -> Color(0xFF4CAF50)
                                        factor.score > 0.4f -> Color(0xFFFFC107)
                                        else -> PrimaryPurple
                                    },
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            LinearProgressIndicator(
                                progress = { factor.score },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(6.dp)
                                    .clip(RoundedCornerShape(50)),
                                color = when {
                                    factor.score > 0.7f -> Color(0xFF4CAF50)
                                    factor.score > 0.4f -> Color(0xFFFFC107)
                                    else -> PrimaryPurple
                                },
                                trackColor = Color.White.copy(alpha = 0.1f)
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = onDismiss,
                colors = ButtonDefaults.buttonColors(containerColor = PrimaryPurple)
            ) {
                Text("Entendido", color = Color.White, fontWeight = FontWeight.Bold)
            }
        }
    )
}

@Composable
fun ActionIconButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    text: String,
    containerColor: Color,
    contentColor: Color,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        modifier = modifier.height(52.dp),
        shape = RoundedCornerShape(16.dp),
        color = containerColor
    ) {
        Row(
            modifier = Modifier.fillMaxSize(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Icon(icon, contentDescription = null, tint = contentColor, modifier = Modifier.size(20.dp))
            Spacer(modifier = Modifier.width(8.dp))
            Text(text = text, color = contentColor, fontSize = 13.sp, fontWeight = FontWeight.Bold, maxLines = 1)
        }
    }
}

private fun providerSearchUrl(providerId: Int, showName: String): String? {
    val encoded = java.net.URLEncoder.encode(showName, "UTF-8")
    return when (providerId) {
        8, 213 -> "https://www.netflix.com/search?q=$encoded"
        9, 119  -> "https://www.primevideo.com/search?k=$encoded"
        337     -> "https://www.disneyplus.com/es-es/search/$encoded"
        384, 29 -> "https://play.max.com/search?q=$encoded"
        2, 350  -> "https://tv.apple.com/search?term=$encoded"
        531     -> "https://www.paramountplus.com/es/search/$encoded/"
        1773    -> "https://www.skyshowtime.com/es/search?q=$encoded"
        63      -> "https://www.filmin.es/buscar?q=$encoded"
        149     -> "https://ver.movistarplus.es/buscar/?q=$encoded"
        35, 105 -> "https://www.rakuten.tv/es/search?q=$encoded"
        541     -> "https://www.atresplayer.com/buscar/?q=$encoded"
        566     -> "https://www.rtve.es/play/buscar/?q=$encoded"
        188     -> "https://www.youtube.com/results?search_query=$encoded"
        else    -> null
    }
}

@Composable
fun WatchProvidersSection(providers: CountryProviders, showName: String) {
    val jwLink = providers.link

    Column {
        Text(
            text = "¿Dónde verlo?",
            style = MaterialTheme.typography.titleLarge,
            color = Color.White,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(16.dp))

        if (!providers.flatrate.isNullOrEmpty()) {
            ProviderTypeRow(
                label = "En suscripción",
                providers = providers.flatrate,
                showName = showName,
                fallbackUrl = jwLink
            )
        }
        if (!providers.rent.isNullOrEmpty()) {
            Spacer(modifier = Modifier.height(16.dp))
            ProviderTypeRow(
                label = "De alquiler",
                providers = providers.rent,
                showName = showName,
                fallbackUrl = jwLink
            )
        }
        if (!providers.buy.isNullOrEmpty()) {
            Spacer(modifier = Modifier.height(16.dp))
            ProviderTypeRow(
                label = "De compra",
                providers = providers.buy,
                showName = showName,
                fallbackUrl = jwLink
            )
        }

        Spacer(modifier = Modifier.height(12.dp))
        Text(
            text = "Datos de disponibilidad: JustWatch",
            style = MaterialTheme.typography.labelSmall,
            color = TextGray,
        )
    }
}

@Composable
fun ProviderTypeRow(
    label: String,
    providers: List<Provider>,
    showName: String,
    fallbackUrl: String?
) {
    val uriHandler = LocalUriHandler.current
    Text(
        text = label,
        style = MaterialTheme.typography.labelLarge,
        color = TextGray,
        fontWeight = FontWeight.SemiBold
    )
    Spacer(modifier = Modifier.height(8.dp))
    LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        items(providers, key = { it.providerId }) { provider ->
            val url = providerSearchUrl(provider.providerId, showName) ?: fallbackUrl
            ProviderLogo(
                provider = provider,
                onClick = { url?.let { uriHandler.openUri(it) } }
            )
        }
    }
}

@Composable
fun ProviderLogo(provider: Provider, onClick: () -> Unit = {}, modifier: Modifier = Modifier) {
    AsyncImage(
        model = "https://image.tmdb.org/t/p/original${provider.logoPath}",
        contentDescription = provider.providerName,
        modifier = modifier
            .size(52.dp)
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onClick),
        contentScale = ContentScale.Crop
    )
}

@Composable
fun CastMemberItem(member: com.example.showmateapp.data.network.CastMember, modifier: Modifier = Modifier) {
    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = modifier.width(80.dp)) {
        AsyncImage(
            model = "https://image.tmdb.org/t/p/w185${member.profilePath}",
            contentDescription = member.name,
            modifier = Modifier.size(80.dp).clip(CircleShape),
            contentScale = ContentScale.Crop,
            placeholder = painterResource(R.drawable.ic_logo_placeholder)
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(text = member.name, style = MaterialTheme.typography.bodySmall, color = Color.White, textAlign = TextAlign.Center, maxLines = 2)
    }
}

@Composable
fun StarRatingSection(
    userRating: Int?,
    onRateClick: (Int) -> Unit,
    onClearRateClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val starCount = 5
    val haptic = LocalHapticFeedback.current
    val visualRating = userRating?.let { ((it + 1) / 2).coerceIn(1, 5) } ?: 0

    Surface(
        color = Color.White.copy(alpha = 0.05f),
        shape = RoundedCornerShape(20.dp),
        modifier = modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(horizontal = 20.dp, vertical = 16.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = "Tu puntuación",
                    color = Color.White,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold
                )
                if (userRating != null) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "$userRating/10",
                            color = StarYellow,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.ExtraBold
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        IconButton(
                            onClick = onClearRateClick,
                            modifier = Modifier.size(24.dp)
                        ) {
                            Icon(
                                Icons.Default.Close,
                                contentDescription = "Quitar valoración",
                                tint = Color.White.copy(alpha = 0.4f),
                                modifier = Modifier.size(14.dp)
                            )
                        }
                    }
                }
            }
            Spacer(modifier = Modifier.height(10.dp))
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                repeat(starCount) { index ->
                    val filled = index < visualRating
                    IconButton(
                        onClick = {
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            onRateClick((index + 1) * 2)
                        },
                        modifier = Modifier.size(44.dp)
                    ) {
                        Icon(
                            imageVector = if (filled) Icons.Default.Star else Icons.Default.StarBorder,
                            contentDescription = "Estrella ${index + 1}",
                            tint = if (filled) StarYellow else Color.White.copy(alpha = 0.25f),
                            modifier = Modifier.size(32.dp)
                        )
                    }
                }
                if (userRating == null) {
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Toca para valorar",
                        color = Color.White.copy(alpha = 0.3f),
                        fontSize = 12.sp
                    )
                }
            }
        }
    }
}

private const val REVIEW_MAX_CHARS = 500

@Composable
fun ReviewSection(
    reviewText: String,
    isSaving: Boolean,
    isReviewSaved: Boolean,
    onTextChange: (String) -> Unit,
    onSave: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier
) {
    var isEditing by remember(isReviewSaved) {
        mutableStateOf(reviewText.isBlank() || !isReviewSaved)
    }

    Column(modifier = modifier) {
        Text(
            text = "Mi reseña",
            style = MaterialTheme.typography.titleLarge,
            color = Color.White,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(16.dp))

        Surface(
            color = Color.White.copy(alpha = 0.05f),
            shape = RoundedCornerShape(20.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(20.dp)) {

                if (!isEditing && reviewText.isNotBlank()) {
                    Column {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    Icons.Default.Check,
                                    contentDescription = null,
                                    tint = Color(0xFF4CAF50),
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "Reseña guardada",
                                    color = Color(0xFF4CAF50),
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            Row {
                                TextButton(
                                    onClick = { isEditing = true },
                                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)
                                ) {
                                    Text("Editar", color = PrimaryPurple, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                                }
                                TextButton(
                                    onClick = onDelete,
                                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)
                                ) {
                                    Text("Borrar", color = Color.Red.copy(alpha = 0.7f), fontSize = 13.sp)
                                }
                            }
                        }
                        Spacer(modifier = Modifier.height(12.dp))
                        Surface(
                            color = PrimaryPurple.copy(alpha = 0.08f),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier
                                    .padding(16.dp)
                                    .height(IntrinsicSize.Min)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .width(3.dp)
                                        .fillMaxHeight()
                                        .background(PrimaryPurple, RoundedCornerShape(2.dp))
                                )
                                Spacer(modifier = Modifier.width(12.dp))
                                Text(
                                    text = reviewText,
                                    color = Color.White.copy(alpha = 0.85f),
                                    fontSize = 14.sp,
                                    lineHeight = 22.sp
                                )
                            }
                        }
                    }
                } else {
                    OutlinedTextField(
                        value = reviewText,
                        onValueChange = { if (it.length <= REVIEW_MAX_CHARS) onTextChange(it) },
                        modifier = Modifier.fillMaxWidth(),
                        placeholder = {
                            Text(
                                text = "Escribe tu opinión sobre esta serie...",
                                color = Color.White.copy(alpha = 0.3f),
                                fontSize = 14.sp
                            )
                        },
                        minLines = 3,
                        maxLines = 7,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = PrimaryPurple,
                            unfocusedBorderColor = Color.White.copy(alpha = 0.12f),
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            cursorColor = PrimaryPurple,
                            focusedContainerColor = Color.Transparent,
                            unfocusedContainerColor = Color.Transparent
                        ),
                        shape = RoundedCornerShape(12.dp),
                        textStyle = androidx.compose.ui.text.TextStyle(fontSize = 14.sp, lineHeight = 21.sp)
                    )

                    Spacer(modifier = Modifier.height(10.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "${reviewText.length}/$REVIEW_MAX_CHARS",
                            color = if (reviewText.length > REVIEW_MAX_CHARS * 0.9) StarYellow
                                    else Color.White.copy(alpha = 0.3f),
                            fontSize = 12.sp,
                            modifier = Modifier.weight(1f)
                        )
                        Button(
                            onClick = {
                                onSave()
                                isEditing = false
                            },
                            enabled = reviewText.isNotBlank() && !isSaving,
                            colors = ButtonDefaults.buttonColors(containerColor = PrimaryPurple),
                            shape = RoundedCornerShape(12.dp),
                            contentPadding = PaddingValues(horizontal = 20.dp, vertical = 10.dp)
                        ) {
                            if (isSaving) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(16.dp),
                                    color = Color.White,
                                    strokeWidth = 2.dp
                                )
                            } else {
                                Text("Guardar", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun EpisodesSection(
    seasons: List<com.example.showmateapp.data.network.Season>,
    selectedSeason: com.example.showmateapp.data.network.SeasonResponse?,
    isSeasonLoading: Boolean,
    watchedEpisodes: List<Int>,
    modifier: Modifier = Modifier,
    onEpisodeToggle: (Int) -> Unit,
    onSeasonChange: (Int) -> Unit,
    onMarkNextEpisode: () -> Unit = {}
) {
    Column(modifier = modifier) {
        Text(
            text = "Episodios",
            style = MaterialTheme.typography.titleLarge,
            color = Color.White,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(16.dp))

        var selectedTabIndex by remember(selectedSeason) { 
            mutableIntStateOf(seasons.indexOfFirst { it.seasonNumber == selectedSeason?.season_number }.coerceAtLeast(0)) 
        }

        val filteredSeasons = remember(seasons) { seasons.filter { it.seasonNumber > 0 } }
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            contentPadding = PaddingValues(vertical = 4.dp)
        ) {
            items(filteredSeasons, key = { it.seasonNumber }) { season ->
                val index = filteredSeasons.indexOf(season)
                val isSelected = selectedTabIndex == index
                Surface(
                    onClick = {
                        selectedTabIndex = index
                        onSeasonChange(season.seasonNumber)
                    },
                    shape = RoundedCornerShape(50),
                    color = if (isSelected) PrimaryPurple else Color.Transparent,
                    border = if (isSelected) null else BorderStroke(1.dp, PrimaryPurple.copy(alpha = 0.5f)),
                    modifier = Modifier.height(36.dp)
                ) {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier.padding(horizontal = 16.dp)
                    ) {
                        Text(
                            text = "T${season.seasonNumber}",
                            color = if (isSelected) Color.White else TextGray,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                            fontSize = 14.sp
                        )
                    }
                }
            }
        }
        
        Spacer(modifier = Modifier.height(16.dp))

        if (selectedSeason != null && selectedSeason.episodes.isNotEmpty()) {
            val totalEps = selectedSeason.episodes.size
            val watchedInSeason = selectedSeason.episodes.count { watchedEpisodes.contains(it.id) }
            val progress = watchedInSeason.toFloat() / totalEps.toFloat()
            Column(modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "Progreso de temporada",
                        color = TextGray,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium
                    )
                    Text(
                        text = "$watchedInSeason / $totalEps ep",
                        color = if (watchedInSeason == totalEps) Color(0xFF4CAF50) else PrimaryPurple,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
                Spacer(modifier = Modifier.height(6.dp))
                LinearProgressIndicator(
                    progress = { progress },
                    modifier = Modifier.fillMaxWidth().height(6.dp).clip(CircleShape),
                    color = if (watchedInSeason == totalEps) Color(0xFF4CAF50) else PrimaryPurple,
                    trackColor = Color.White.copy(alpha = 0.1f)
                )
            }
            Spacer(modifier = Modifier.height(16.dp))
        }

        if (selectedSeason != null) {
            val nextEpisode = selectedSeason.episodes.firstOrNull { it.id !in watchedEpisodes }
            if (nextEpisode != null) {
                OutlinedButton(
                    onClick = onMarkNextEpisode,
                    modifier = Modifier.fillMaxWidth(),
                    border = BorderStroke(1.dp, PrimaryPurple),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = PrimaryPurple)
                ) {
                    Icon(
                        Icons.Default.PlayArrow,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Siguiente: ${nextEpisode.episode_number}. ${nextEpisode.name}",
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp,
                        maxLines = 1,
                        overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                    )
                }
                Spacer(modifier = Modifier.height(12.dp))
            }
        }

        if (isSeasonLoading) {
            repeat(4) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(width = 100.dp, height = 60.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color.White.copy(alpha = 0.08f))
                    )
                    Spacer(modifier = Modifier.width(16.dp))
                    Column(modifier = Modifier.weight(1f).padding(end = 8.dp)) {
                        Box(modifier = Modifier.fillMaxWidth(0.7f).height(14.dp).clip(RoundedCornerShape(4.dp)).background(Color.White.copy(alpha = 0.08f)))
                        Spacer(modifier = Modifier.height(8.dp))
                        Box(modifier = Modifier.fillMaxWidth(0.3f).height(12.dp).clip(RoundedCornerShape(4.dp)).background(Color.White.copy(alpha = 0.05f)))
                    }
                }
            }
        } else if (selectedSeason != null) {
            selectedSeason.episodes.forEach { episode ->
                val isWatched = watchedEpisodes.contains(episode.id)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp)
                        .clickable { onEpisodeToggle(episode.id) },
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val imageModel = if (episode.still_path != null)
                        "https://image.tmdb.org/t/p/w300${episode.still_path}"
                    else null
                    AsyncImage(
                        model = imageModel,
                        contentDescription = null,
                        modifier = Modifier.size(width = 100.dp, height = 60.dp).clip(RoundedCornerShape(8.dp)),
                        contentScale = ContentScale.Crop,
                        error = painterResource(R.drawable.ic_logo_placeholder),
                        placeholder = painterResource(R.drawable.ic_logo_placeholder)
                    )
                    Spacer(modifier = Modifier.width(16.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(text = "${episode.episode_number}. ${episode.name}", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        val runtimeText = episode.runtime?.takeIf { it > 0 }?.let { "$it min" } ?: "N/A"
                        Text(text = runtimeText, color = TextGray, fontSize = 12.sp)
                    }
                    Checkbox(
                        checked = isWatched,
                        onCheckedChange = { onEpisodeToggle(episode.id) },
                        colors = CheckboxDefaults.colors(checkedColor = PrimaryPurple)
                    )
                }
            }
        } else {
            LinearProgressIndicator(modifier = Modifier.fillMaxWidth(), color = PrimaryPurple)
        }
    }
}

@Composable
private fun ScoreFactorRow(label: String, weight: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(text = label, color = Color.White, fontSize = 13.sp)
        Surface(
            color = PrimaryPurple.copy(alpha = 0.15f),
            shape = RoundedCornerShape(8.dp)
        ) {
            Text(
                text = weight,
                color = PrimaryPurple,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
            )
        }
    }
}

@Composable
private fun DetailScreenSkeleton() {
    val shimmer = shimmerBrush()
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // Hero image area
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(450.dp)
                .background(shimmer)
        )
        // Content overlay
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.BottomStart)
                .padding(start = 16.dp, end = 16.dp, bottom = 16.dp, top = 270.dp)
        ) {
            // Title shimmer
            Box(
                modifier = Modifier
                    .fillMaxWidth(0.75f)
                    .height(28.dp)
                    .clip(RoundedCornerShape(6.dp))
                    .background(shimmer)
            )
            Spacer(modifier = Modifier.height(8.dp))
            Box(
                modifier = Modifier
                    .fillMaxWidth(0.45f)
                    .height(16.dp)
                    .clip(RoundedCornerShape(6.dp))
                    .background(shimmer)
            )
            Spacer(modifier = Modifier.height(20.dp))
            // Action buttons row
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                repeat(3) {
                    Box(
                        modifier = Modifier
                            .size(56.dp)
                            .clip(CircleShape)
                            .background(shimmer)
                    )
                }
            }
            Spacer(modifier = Modifier.height(24.dp))
            // Synopsis lines
            repeat(3) { i ->
                Box(
                    modifier = Modifier
                        .fillMaxWidth(if (i == 2) 0.6f else 1f)
                        .height(14.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .background(shimmer)
                )
                Spacer(modifier = Modifier.height(6.dp))
            }
        }
    }
}
