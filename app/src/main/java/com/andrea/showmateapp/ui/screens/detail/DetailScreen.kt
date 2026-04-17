package com.andrea.showmateapp.ui.screens.detail

import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.andrea.showmateapp.R
import com.andrea.showmateapp.data.model.RecommendationReason
import com.andrea.showmateapp.data.model.MediaContent
import com.andrea.showmateapp.ui.components.premium.*
import com.andrea.showmateapp.ui.navigation.Screen
import com.andrea.showmateapp.ui.theme.*
import com.andrea.showmateapp.util.TmdbUtils
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
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val showWhyDialog by viewModel.showWhyDialog.collectAsStateWithLifecycle()
    val whyFactors by viewModel.whyFactors.collectAsStateWithLifecycle()

    LaunchedEffect(showId) {
        viewModel.loadShowDetails(showId)
    }

    LaunchedEffect(uiState.media?.id) {
        uiState.media?.id?.let { mediaId ->
            viewModel.loadSimilarShowsIfNeeded(mediaId)
        }
    }

    if (showWhyDialog) {
        WhyRecommendedDialog(factors = whyFactors, onDismiss = { viewModel.dismissWhyDialog() })
    }

    DetailScreenContent(
        uiState = uiState,
        localWatched = uiState.isWatched,
        localLiked = uiState.isLiked,
        localEssential = uiState.isEssential,
        localWatchlist = uiState.isInWatchlist,
        onBackClick = { navController.popBackStack() },
        onLikeClick = { viewModel.toggleLiked() },
        onEssentialClick = { viewModel.toggleEssential() },
        onWatchedClick = { viewModel.toggleWatched() },
        onWatchlistClick = { viewModel.toggleWatchlist() },
        onRateClick = { viewModel.rateShow(it) },
        onClearRateClick = { viewModel.clearRating() },
        onRetry = { viewModel.loadShowDetails(showId) },
        onSimilarShowClick = { show, tag ->
            navController.navigate(Screen.Detail(showId = show.id, sharedElementTag = tag))
        },
        onEpisodeToggle = { epId, markPrev -> viewModel.toggleEpisodeWatched(epId, markPrev) },
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
        onActorClick = { personId, personName ->
            navController.navigate(Screen.Actor(personId = personId, personName = personName))
        },
        whyFactors = whyFactors,
        sharedTransitionScope = sharedTransitionScope,
        animatedVisibilityScope = animatedVisibilityScope,
        sharedElementTag = sharedElementTag,
        viewModel = viewModel
    )
}

@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
fun DetailScreenContent(
    uiState: DetailUiState,
    localWatched: Boolean = uiState.isWatched,
    localLiked: Boolean = uiState.isLiked,
    localEssential: Boolean = uiState.isEssential,
    localWatchlist: Boolean = uiState.isInWatchlist,
    onBackClick: () -> Unit,
    onLikeClick: () -> Unit,
    onEssentialClick: () -> Unit,
    onWatchedClick: () -> Unit,
    onWatchlistClick: () -> Unit = {},
    onRateClick: (Int) -> Unit,
    onClearRateClick: () -> Unit,
    onRetry: () -> Unit,
    onSimilarShowClick: (MediaContent, String) -> Unit,
    onEpisodeToggle: (Int, Boolean) -> Unit,
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
    onActorClick: (Int, String) -> Unit = { _, _ -> },
    whyFactors: List<RecommendationReason> = emptyList(),
    sharedTransitionScope: SharedTransitionScope,
    animatedVisibilityScope: AnimatedVisibilityScope,
    sharedElementTag: String?,
    viewModel: DetailViewModel
) {
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    if (uiState.showAddToListDialog) {
        AddToListDialog(
            customLists = uiState.customLists,
            mediaId = uiState.media?.id,
            onAddToList = onAddToList,
            onDismiss = onHideAddToListDialog
        )
    }

    val context = LocalContext.current
    LaunchedEffect(uiState.actionError) {
        uiState.actionError?.let { error ->
            snackbarHostState.showSnackbar(error.asString(context), duration = SnackbarDuration.Short)
            onClearActionError()
        }
    }

    LaunchedEffect(uiState.snackbarMessage) {
        uiState.snackbarMessage?.let { msg ->
            snackbarHostState.showSnackbar(msg.asString(context), duration = SnackbarDuration.Short)
            viewModel.clearSnackbarMessage()
        }
    }

    if (uiState.isLoading) {
        DetailScreenSkeleton()
        return
    }

    if (uiState.errorMessage != null) {
        ErrorView(message = uiState.errorMessage.asString(), onRetry = onRetry)
        return
    }

    val show = uiState.media ?: return

    val heroGradient = remember {
        Brush.verticalGradient(
            colorStops = arrayOf(
                0f to Color.Black.copy(alpha = 0.28f),
                0.38f to Color.Transparent,
                0.68f to Color.Black.copy(alpha = 0.70f),
                1f to Color.Black.copy(alpha = 0.96f)
            )
        )
    }

    Box(modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        val scrollState = rememberScrollState()

        Box(modifier = Modifier.fillMaxWidth().height(460.dp)) {
            with(sharedTransitionScope) {
                val sharedElementKey = if (sharedElementTag != null) {
                    "image-${show.id}-$sharedElementTag"
                } else {
                    "image-${show.id}"
                }
                TmdbImage(
                    path = show.backdropPath ?: show.posterPath,
                    contentDescription = null,
                    size = TmdbUtils.ImageSize.W1280,
                    modifier = Modifier
                        .fillMaxSize()
                        .let {
                            if (LocalInspectionMode.current) {
                                it
                            } else {
                                it.sharedElement(
                                    sharedContentState = rememberSharedContentState(key = sharedElementKey),
                                    animatedVisibilityScope = animatedVisibilityScope
                                )
                            }
                        }
                )
            }

            Box(modifier = Modifier.fillMaxSize().background(heroGradient))

            if (show.voteAverage > 0.0) {
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(end = 20.dp, bottom = 72.dp)
                ) {
                    MatchBadge(score = show.voteAverage, isAffinity = false)
                }
            }
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(scrollState)
        ) {
            Spacer(modifier = Modifier.height(330.dp))

            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = MaterialTheme.colorScheme.background,
                shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp, vertical = 28.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.Top
                    ) {
                        Text(
                            text = show.name,
                            color = Color.White,
                            fontSize = 28.sp,
                            fontWeight = FontWeight.Black,
                            letterSpacing = (-0.5).sp,
                            lineHeight = 34.sp,
                            modifier = Modifier.weight(1f)
                        )
                        if (show.affinityScore > 0f && whyFactors.isNotEmpty()) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp),
                                modifier = Modifier.padding(start = 12.dp, top = 4.dp)
                            ) {
                                MatchBadge(
                                    score = show.affinityScore,
                                    isAffinity = true
                                )
                                Row(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(14.dp))
                                        .background(
                                            Brush.linearGradient(
                                                listOf(
                                                    PrimaryPurple.copy(alpha = 0.45f),
                                                    PrimaryMagenta.copy(alpha = 0.30f)
                                                )
                                            )
                                        )
                                        .border(
                                            1.dp,
                                            PrimaryPurpleLight.copy(alpha = 0.55f),
                                            RoundedCornerShape(14.dp)
                                        )
                                        .clickable { onWhyDialogClick() }
                                        .padding(horizontal = 12.dp, vertical = 7.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(5.dp)
                                ) {
                                    Icon(
                                        Icons.Default.AutoAwesome,
                                        contentDescription = null,
                                        tint = Color.White,
                                        modifier = Modifier.size(13.dp)
                                    )
                                    Text(
                                        text = "¿Por qué?",
                                        color = Color.White,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                    }



                    val year = remember(show.firstAirDate) { show.firstAirDate?.take(4) ?: "" }
                    val seasonsCount = remember(show.numberOfSeasons) {
                        show.numberOfSeasons?.let { "$it ${if (it == 1) "Temporada" else "Temporadas"}" } ?: ""
                    }
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (year.isNotEmpty()) MetaChip(year)
                        if (seasonsCount.isNotEmpty()) MetaChip(seasonsCount)
                    }

                    Spacer(modifier = Modifier.height(18.dp))

                    DetailActionButtonsRow(
                        isWatched = localWatched,
                        isLiked = localLiked,
                        isEssential = localEssential,
                        isInWatchlist = localWatchlist,
                        onWatchedClick = onWatchedClick,
                        onLikeClick = onLikeClick,
                        onEssentialClick = onEssentialClick,
                        onWatchlistClick = onWatchlistClick
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(44.dp)
                            .clip(RoundedCornerShape(14.dp))
                            .background(
                                Brush.linearGradient(
                                    listOf(
                                        PrimaryPurple.copy(alpha = 0.18f),
                                        PrimaryPurpleDark.copy(alpha = 0.10f)
                                    )
                                )
                            )
                            .clickable { onShowAddToListDialog() },
                        contentAlignment = Alignment.Center
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                Icons.AutoMirrored.Filled.List,
                                contentDescription = null,
                                tint = PrimaryPurpleLight,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                stringResource(R.string.detail_add_to_list),
                                color = PrimaryPurpleLight,
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp
                            )
                        }
                    }

                    val trailerKey = show.videos?.results?.firstOrNull {
                        it.site == "YouTube" && it.type == "Trailer"
                    }?.key
                    if (trailerKey != null) {
                        Spacer(modifier = Modifier.height(24.dp))
                        val uriHandler = LocalUriHandler.current
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(180.dp)
                                .clip(RoundedCornerShape(18.dp))
                                .clickable {
                                    uriHandler.openUri("https://www.youtube.com/watch?v=$trailerKey")
                                }
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
                                    .background(
                                        Brush.verticalGradient(
                                            listOf(Color.Transparent, Color.Black.copy(alpha = 0.65f))
                                        )
                                    )
                            )
                            Box(
                                modifier = Modifier
                                    .size(48.dp)
                                    .align(Alignment.Center)
                                    .clip(CircleShape)
                                    .background(
                                        Brush.linearGradient(
                                            listOf(PrimaryPurple, PrimaryPurpleDark)
                                        )
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    Icons.Default.PlayArrow,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(28.dp)
                                )
                            }
                            Box(
                                modifier = Modifier
                                    .align(Alignment.TopStart)
                                    .padding(12.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(Color.Black.copy(alpha = 0.55f))
                            ) {
                                Text(
                                    stringResource(R.string.detail_official_trailer),
                                    color = Color.White,
                                    fontWeight = FontWeight.ExtraBold,
                                    fontSize = 9.sp,
                                    letterSpacing = 1.sp,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(28.dp))

                    var selectedDetailTab by remember { mutableIntStateOf(0) }
                    val detailTabs = listOf(
                        stringResource(R.string.detail_tab_info),
                        stringResource(R.string.detail_tab_review)
                    )

                    PremiumTabRow(
                        tabs = listOf(0, 1),
                        selectedTab = selectedDetailTab,
                        onTabSelected = { selectedDetailTab = it },
                        labelProvider = { detailTabs[it] },
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(20.dp))

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
                            Column(modifier = Modifier.fillMaxWidth()) {
                                val esProviders = show.watchProviders?.results?.get("ES")
                                if (esProviders != null &&
                                    (
                                        !esProviders.flatrate.isNullOrEmpty() ||
                                            !esProviders.rent.isNullOrEmpty() ||
                                            !esProviders.buy.isNullOrEmpty()
                                        )
                                ) {
                                    WatchProvidersSection(providers = esProviders, showName = show.name)
                                    Spacer(modifier = Modifier.height(28.dp))
                                }

                                var isSynopsisExpanded by remember { mutableStateOf(false) }
                                DetailSectionHeader(title = stringResource(R.string.detail_synopsis))
                                Spacer(modifier = Modifier.height(10.dp))
                                Text(
                                    text = show.overview.ifEmpty { stringResource(R.string.detail_no_synopsis) },
                                    color = TextGray,
                                    fontSize = 14.sp,
                                    lineHeight = 22.sp,
                                    maxLines = if (isSynopsisExpanded) Int.MAX_VALUE else 4,
                                    modifier = Modifier
                                        .animateContentSize()
                                        .clickable { isSynopsisExpanded = !isSynopsisExpanded }
                                )
                                if (!isSynopsisExpanded && show.overview.length > 180) {
                                    Text(
                                        text = stringResource(R.string.home_read_more),
                                        color = PrimaryPurpleLight,
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        modifier = Modifier
                                            .padding(top = 4.dp)
                                            .clickable { isSynopsisExpanded = true }
                                    )
                                }

                                val showSeasons = show.seasons
                                if (!showSeasons.isNullOrEmpty()) {
                                    Spacer(modifier = Modifier.height(28.dp))
                                    EpisodesSection(
                                        seasons = showSeasons,
                                        selectedSeason = uiState.selectedSeason,
                                        isSeasonLoading = uiState.isSeasonLoading,
                                        watchedEpisodes = uiState.watchedEpisodes,
                                        onEpisodeToggle = onEpisodeToggle,
                                        onSeasonChange = { seasonNum ->
                                            onSeasonChange(show.id, seasonNum)
                                        },
                                        onToggleSeason = { viewModel.toggleSeasonWatched() },
                                        onMarkNextEpisode = onMarkNextEpisode
                                    )
                                }

                                val cast = show.credits?.cast ?: emptyList()
                                if (cast.isNotEmpty()) {
                                    Spacer(modifier = Modifier.height(28.dp))
                                    DetailSectionHeader(title = stringResource(R.string.detail_top_cast))
                                    Spacer(modifier = Modifier.height(14.dp))
                                    LazyRow(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                                        items(cast.take(10), key = { it.id }) { member ->
                                            CastMemberItem(
                                                member = member,
                                                onClick = onActorClick
                                            )
                                        }
                                    }
                                }

                                if (uiState.isSimilarLoading || uiState.similarShows.isNotEmpty()) {
                                    Spacer(modifier = Modifier.height(28.dp))
                                    DetailSectionHeader(title = stringResource(R.string.detail_similar_shows))
                                    Spacer(modifier = Modifier.height(14.dp))
                                    if (uiState.isSimilarLoading) {
                                        LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                            items(5) {
                                                Box(
                                                    modifier = Modifier
                                                        .width(120.dp)
                                                        .height(170.dp)
                                                        .clip(RoundedCornerShape(14.dp))
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
                            }
                        } else {
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
                                Spacer(modifier = Modifier.height(24.dp))
                                SocialReviewsSection(
                                    showId = show.id,
                                    numberOfSeasons = show.numberOfSeasons ?: 1
                                )
                            }
                        }
                    }
                }
            }
        }

        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier.align(Alignment.BottomCenter)
        )

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(horizontal = 16.dp, vertical = 16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                onClick = onBackClick,
                shape = CircleShape,
                color = Color.Black.copy(alpha = 0.45f),
                modifier = Modifier.size(44.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        tint = Color.White
                    )
                }
            }

            Surface(
                onClick = {
                    val shareIntent = android.content.Intent(android.content.Intent.ACTION_SEND).apply {
                        type = "text/plain"
                        putExtra(android.content.Intent.EXTRA_SUBJECT, show.name)
                        putExtra(
                            android.content.Intent.EXTRA_TEXT,
                            "¡Echa un vistazo a \"${show.name}\" en ShowMate! 🎬\n" +
                                "https://www.themoviedb.org/tv/${show.id}"
                        )
                    }
                    context.startActivity(
                        android.content.Intent.createChooser(shareIntent, "Compartir con...")
                    )
                },
                shape = CircleShape,
                color = Color.Black.copy(alpha = 0.45f),
                modifier = Modifier.size(44.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        Icons.Default.Share,
                        contentDescription = "Compartir",
                        tint = Color.White
                    )
                }
            }
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
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(460.dp)
                .background(shimmer)
        )
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.BottomStart)
                .padding(start = 24.dp, end = 24.dp, bottom = 24.dp, top = 280.dp)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(0.75f)
                    .height(30.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(shimmer)
            )
            Spacer(modifier = Modifier.height(10.dp))
            Box(
                modifier = Modifier
                    .fillMaxWidth(0.45f)
                    .height(16.dp)
                    .clip(RoundedCornerShape(6.dp))
                    .background(shimmer)
            )
            Spacer(modifier = Modifier.height(24.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                repeat(4) {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(58.dp)
                            .clip(RoundedCornerShape(18.dp))
                            .background(shimmer)
                    )
                }
            }
            Spacer(modifier = Modifier.height(20.dp))
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

