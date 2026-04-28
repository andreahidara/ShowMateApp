package com.andrea.showmateapp.ui.screens.discover

import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.TravelExplore
import androidx.compose.material.icons.filled.WifiOff
import androidx.compose.material3.*
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.andrea.showmateapp.R
import com.andrea.showmateapp.data.model.MediaContent
import com.andrea.showmateapp.ui.components.*
import com.andrea.showmateapp.ui.navigation.Screen
import com.andrea.showmateapp.ui.theme.*
import com.andrea.showmateapp.util.TmdbUtils

private val discoverGenreResIds = mapOf(
    10759 to R.string.discover_genre_action_name,
    16 to R.string.discover_genre_animation_name,
    35 to R.string.discover_genre_comedy_name,
    80 to R.string.discover_genre_crime_name,
    99 to R.string.discover_genre_documentary_name,
    18 to R.string.discover_genre_drama_name,
    10751 to R.string.discover_genre_family_name,
    9648 to R.string.discover_genre_mystery_name,
    10765 to R.string.discover_genre_scifi_name,
    10768 to R.string.discover_genre_politics_name,
    37 to R.string.discover_genre_western_name,
    53 to R.string.discover_genre_thriller_name,
    10762 to R.string.discover_genre_kids_name,
    10764 to R.string.discover_genre_reality_name
)

@OptIn(ExperimentalSharedTransitionApi::class, ExperimentalMaterial3Api::class)
@Composable
fun DiscoverScreen(
    globalNavController: NavController,
    sharedTransitionScope: SharedTransitionScope,
    animatedVisibilityScope: AnimatedVisibilityScope,
    viewModel: DiscoverViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val onLoadMoreTopGenre = remember(viewModel) { { viewModel.loadMoreTopGenre() } }
    val onLoadMoreSecondGenre = remember(viewModel) { { viewModel.loadMoreSecondGenre() } }

    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        var isFirstResume = true
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                if (isFirstResume) {
                    isFirstResume = false
                    return@LifecycleEventObserver
                }
                if (!viewModel.uiState.value.isLoading) viewModel.refresh()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    DiscoverScreenContent(
        state = state,
        sharedTransitionScope = sharedTransitionScope,
        animatedVisibilityScope = animatedVisibilityScope,
        onMediaClick = { media, tag -> navigateToDetail(globalNavController, media, tag) },
        onRetry = { viewModel.retry() },
        onRefresh = { viewModel.refresh() },
        isFromCache = state.isFromCache,
        onLoadMoreTopGenre = onLoadMoreTopGenre,
        onLoadMoreSecondGenre = onLoadMoreSecondGenre
    )
}

@OptIn(ExperimentalSharedTransitionApi::class, ExperimentalMaterial3Api::class)
@Composable
fun DiscoverScreenContent(
    state: DiscoverUiState,
    sharedTransitionScope: SharedTransitionScope,
    animatedVisibilityScope: AnimatedVisibilityScope,
    onMediaClick: (MediaContent, String) -> Unit,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier,
    onRefresh: () -> Unit = {},
    isFromCache: Boolean = false,
    onLoadMoreTopGenre: (() -> Unit)? = null,
    onLoadMoreSecondGenre: (() -> Unit)? = null
) {
    val sectionStates = remember { List(17) { LazyListState() } }

    PullToRefreshBox(
        isRefreshing = state.isRefreshing,
        onRefresh = onRefresh,
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        if (state.isLoading) {
            DiscoverSkeleton()
        } else if (state.errorMessage != null) {
            ErrorView(message = state.errorMessage, onRetry = onRetry)
        } else {
            LazyColumn(modifier = Modifier.fillMaxSize()) {
                item { DiscoverHeader() }

                if (isFromCache) {
                    item {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 4.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(Color(0xFF2A2A2A))
                                .padding(horizontal = 12.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                Icons.Default.WifiOff,
                                contentDescription = null,
                                tint = Color(0xFFFFC107),
                                modifier = Modifier.size(14.dp)
                            )
                            Text(
                                text = stringResource(R.string.offline_cached_content),
                                color = Color(0xFFFFC107),
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }

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

                if (state.contextPicksShows.isNotEmpty() && state.contextPicksTitle.isNotEmpty() ||
                    state.dayOfWeekShows.isNotEmpty() && state.dayOfWeekTitle.isNotEmpty()
                ) {
                    item { DiscoverCategoryDivider(R.string.discover_for_you_now) }
                }

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
                            subtitle = stringResource(R.string.discover_context_subtitle),
                            listState = sectionStates[0]
                        )
                    }
                }

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
                            subtitle = stringResource(R.string.discover_dayofweek_subtitle),
                            listState = sectionStates[1]
                        )
                    }
                }

                item { DiscoverCategoryDivider(R.string.discover_your_fav_genres) }

                item {
                    ShowSection(
                        title = stringResource(R.string.discover_top_genre_title, state.topGenreName),
                        items = state.topGenreShows,
                        sharedTransitionScope = sharedTransitionScope,
                        animatedVisibilityScope = animatedVisibilityScope,
                        onItemClick = onMediaClick,
                        tag = "discover_genre1",
                        subtitle = state.topGenreSubtitle.takeIf { it.isNotEmpty() },
                        listState = sectionStates[2],
                        onLoadMore = onLoadMoreTopGenre,
                        isLoadingMore = state.isLoadingMoreTopGenre
                    )
                }

                if (state.narrativeStyleShows.isNotEmpty() && state.narrativeStyleLabel.isNotEmpty()) {
                    item {
                        ShowSection(
                            title = state.narrativeStyleLabel,
                            items = state.narrativeStyleShows,
                            sharedTransitionScope = sharedTransitionScope,
                            animatedVisibilityScope = animatedVisibilityScope,
                            onItemClick = onMediaClick,
                            tag = "discover_narrative",
                            subtitle = stringResource(R.string.discover_narrative_subtitle),
                            listState = sectionStates[3]
                        )
                    }
                }

                item {
                    ShowSection(
                        title = stringResource(R.string.discover_second_genre_title, state.secondGenreName),
                        items = state.secondGenreShows,
                        sharedTransitionScope = sharedTransitionScope,
                        animatedVisibilityScope = animatedVisibilityScope,
                        onItemClick = onMediaClick,
                        tag = "discover_genre2",
                        subtitle = state.secondGenreSubtitle.takeIf { it.isNotEmpty() },
                        listState = sectionStates[4],
                        onLoadMore = onLoadMoreSecondGenre,
                        isLoadingMore = state.isLoadingMoreSecondGenre
                    )
                }

                if (state.thirdGenreShows.isNotEmpty() && state.thirdGenreName.isNotEmpty()) {
                    item {
                        ShowSection(
                            title = stringResource(R.string.discover_third_genre_title, state.thirdGenreName),
                            items = state.thirdGenreShows,
                            sharedTransitionScope = sharedTransitionScope,
                            animatedVisibilityScope = animatedVisibilityScope,
                            onItemClick = onMediaClick,
                            tag = "discover_genre3",
                            subtitle = state.thirdGenreSubtitle.takeIf { it.isNotEmpty() },
                            listState = sectionStates[6]
                        )
                    }
                }

                if (state.hiddenGemShows.isNotEmpty() ||
                    state.explorationShows.isNotEmpty() ||
                    state.moodSectionShows.isNotEmpty()
                ) {
                    item { DiscoverCategoryDivider(R.string.discover_explore_new) }
                }

                if (state.hiddenGemShows.isNotEmpty()) {
                    item {
                        ShowSection(
                            title = stringResource(R.string.discover_hidden_gems),
                            items = state.hiddenGemShows,
                            sharedTransitionScope = sharedTransitionScope,
                            animatedVisibilityScope = animatedVisibilityScope,
                            onItemClick = onMediaClick,
                            accentColor = AccentBlue,
                            tag = "discover_hidden",
                            subtitle = stringResource(R.string.discover_hidden_gems_subtitle),
                            listState = sectionStates[5]
                        )
                    }
                }

                if (state.explorationShows.isNotEmpty() && state.explorationGenreName.isNotEmpty()) {
                    item {
                        ShowSection(
                            title = stringResource(R.string.discover_exploration_title, state.explorationGenreName),
                            items = state.explorationShows,
                            sharedTransitionScope = sharedTransitionScope,
                            animatedVisibilityScope = animatedVisibilityScope,
                            onItemClick = onMediaClick,
                            accentColor = PillCreator,
                            tag = "discover_exploration",
                            subtitle = stringResource(R.string.discover_exploration_subtitle),
                            listState = sectionStates[8]
                        )
                    }
                }

                if (state.moodSectionShows.isNotEmpty() && state.moodSectionTitle.isNotEmpty()) {
                    item {
                        ShowSection(
                            title = state.moodSectionTitle,
                            items = state.moodSectionShows,
                            sharedTransitionScope = sharedTransitionScope,
                            animatedVisibilityScope = animatedVisibilityScope,
                            onItemClick = onMediaClick,
                            tag = "discover_mood",
                            subtitle = stringResource(R.string.discover_mood_subtitle),
                            listState = sectionStates[7]
                        )
                    }
                }

                if (state.similarShows.isNotEmpty() ||
                    state.actorShows.isNotEmpty() ||
                    state.creatorShows.isNotEmpty() ||
                    state.timeTravelShows.isNotEmpty()
                ) {
                    item { DiscoverCategoryDivider(R.string.discover_because_you_watched) }
                }

                if (state.similarShows.isNotEmpty() && state.similarToName.isNotEmpty()) {
                    item {
                        ShowSection(
                            title = stringResource(R.string.discover_similar_title, state.similarToName),
                            items = state.similarShows,
                            sharedTransitionScope = sharedTransitionScope,
                            animatedVisibilityScope = animatedVisibilityScope,
                            onItemClick = onMediaClick,
                            tag = "discover_similar",
                            listState = sectionStates[12]
                        )
                    }
                }

                if (state.actorShows.isNotEmpty() && state.actorName.isNotEmpty()) {
                    item {
                        ShowSection(
                            title = stringResource(R.string.discover_actor_title, state.actorName),
                            items = state.actorShows,
                            sharedTransitionScope = sharedTransitionScope,
                            animatedVisibilityScope = animatedVisibilityScope,
                            onItemClick = onMediaClick,
                            tag = "discover_actor",
                            listState = sectionStates[14]
                        )
                    }
                }

                if (state.secondKeywordShows.isNotEmpty() && state.secondKeywordLabel.isNotEmpty()) {
                    item {
                        ShowSection(
                            title = state.secondKeywordLabel,
                            items = state.secondKeywordShows,
                            sharedTransitionScope = sharedTransitionScope,
                            animatedVisibilityScope = animatedVisibilityScope,
                            onItemClick = onMediaClick,
                            tag = "discover_keyword2",
                            listState = sectionStates[15]
                        )
                    }
                }

                if (state.creatorShows.isNotEmpty() && state.creatorName.isNotEmpty()) {
                    item {
                        ShowSection(
                            title = stringResource(R.string.discover_creator_title, state.creatorName),
                            items = state.creatorShows,
                            sharedTransitionScope = sharedTransitionScope,
                            animatedVisibilityScope = animatedVisibilityScope,
                            onItemClick = onMediaClick,
                            tag = "discover_creator",
                            subtitle = stringResource(R.string.discover_creator_subtitle),
                            listState = sectionStates[13]
                        )
                    }
                }

                if (state.timeTravelShows.isNotEmpty()) {
                    item {
                        ShowSection(
                            title = stringResource(R.string.discover_timetravel),
                            items = state.timeTravelShows,
                            sharedTransitionScope = sharedTransitionScope,
                            animatedVisibilityScope = animatedVisibilityScope,
                            onItemClick = onMediaClick,
                            tag = "discover_timetravel",
                            listState = sectionStates[9]
                        )
                    }
                }

                if (state.topKeywordShows.isNotEmpty() && state.topKeywordLabel.isNotEmpty() ||
                    state.topRatedShows.isNotEmpty() ||
                    state.collaborativeShows.isNotEmpty()
                ) {
                    item { DiscoverCategoryDivider(R.string.discover_rated_popular) }
                }

                if (state.topKeywordShows.isNotEmpty() && state.topKeywordLabel.isNotEmpty()) {
                    item {
                        ShowSection(
                            title = state.topKeywordLabel,
                            items = state.topKeywordShows,
                            sharedTransitionScope = sharedTransitionScope,
                            animatedVisibilityScope = animatedVisibilityScope,
                            onItemClick = onMediaClick,
                            tag = "discover_keyword",
                            listState = sectionStates[10]
                        )
                    }
                }

                if (state.topRatedShows.isNotEmpty() && state.topGenreName.isNotEmpty()) {
                    item {
                        ShowSection(
                            title = stringResource(R.string.discover_top_rated_title, state.topGenreName),
                            items = state.topRatedShows,
                            sharedTransitionScope = sharedTransitionScope,
                            animatedVisibilityScope = animatedVisibilityScope,
                            onItemClick = onMediaClick,
                            tag = "discover_toprated",
                            listState = sectionStates[11]
                        )
                    }
                }

                if (state.collaborativeShows.isNotEmpty()) {
                    item {
                        ShowSection(
                            title = stringResource(R.string.discover_collab),
                            items = state.collaborativeShows,
                            sharedTransitionScope = sharedTransitionScope,
                            animatedVisibilityScope = animatedVisibilityScope,
                            onItemClick = onMediaClick,
                            accentColor = SuccessGreen,
                            tag = "discover_collab",
                            subtitle = stringResource(R.string.discover_collab_subtitle),
                            listState = sectionStates[16]
                        )
                    }
                }

                item { Spacer(modifier = Modifier.height(100.dp)) }
            }
        }
    }
}

@Composable
private fun DiscoverHeader() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .statusBarsPadding()
            .padding(horizontal = 16.dp, vertical = 12.dp)
    ) {
        Column {
            Text(
                text = stringResource(R.string.discover_header_title),
                style = TextStyle(
                    brush = Brush.linearGradient(listOf(PrimaryPurple, PrimaryMagenta))
                ),
                fontSize = 32.sp,
                fontWeight = FontWeight.Black,
                letterSpacing = (-2).sp
            )
            androidx.compose.foundation.layout.Box(
                modifier = Modifier
                    .padding(top = 3.dp, bottom = 4.dp)
                    .width(38.dp)
                    .height(3.dp)
                    .clip(androidx.compose.foundation.shape.RoundedCornerShape(2.dp))
                    .background(Brush.linearGradient(listOf(PrimaryPurple, PrimaryMagenta)))
            )
            Text(
                text = stringResource(R.string.discover_header_subtitle),
                color = Color.White.copy(alpha = 0.5f),
                fontSize = 13.sp,
                fontWeight = FontWeight.Normal
            )
        }
    }
}

private val dividerIcons = mapOf(
    R.string.discover_for_you_now to Icons.Default.AutoAwesome,
    R.string.discover_your_fav_genres to Icons.Default.Favorite,
    R.string.discover_explore_new to Icons.Default.TravelExplore,
    R.string.discover_because_you_watched to Icons.Default.PlayArrow,
    R.string.discover_rated_popular to Icons.Default.Star
)

@Composable
private fun DiscoverCategoryDivider(@androidx.annotation.StringRes labelRes: Int) {
    val label = stringResource(labelRes)
    val icon = dividerIcons[labelRes]
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp)
            .padding(top = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Box(
            modifier = Modifier
                .height(1.dp)
                .weight(1f)
                .background(
                    Brush.horizontalGradient(
                        listOf(Color.White.copy(alpha = 0.08f), Color.Transparent)
                    )
                )
        )
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(5.dp)
        ) {
            if (icon != null) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = Color.White.copy(alpha = 0.28f),
                    modifier = Modifier.size(10.dp)
                )
            }
            Text(
                text = label.uppercase(),
                color = Color.White.copy(alpha = 0.30f),
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.5.sp
            )
        }
        Box(
            modifier = Modifier
                .height(1.dp)
                .weight(1f)
                .background(
                    Brush.horizontalGradient(
                        listOf(Color.Transparent, Color.White.copy(alpha = 0.08f))
                    )
                )
        )
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
    val context = androidx.compose.ui.platform.LocalContext.current
    val genreNames = remember(media.id) {
        (
            media.genres?.map { it.name }?.takeIf { it.isNotEmpty() }
                ?: media.safeGenreIds.mapNotNull { discoverGenreResIds[it]?.let { resId -> context.getString(resId) } }
            ).take(3)
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(500.dp)
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .clip(RoundedCornerShape(28.dp))
            .clickable { onClick(media) }
    ) {
        with(sharedTransitionScope) {
            TmdbImage(
                path = media.backdropPath ?: media.posterPath,
                contentDescription = "Hero: ${media.name}",
                size = TmdbUtils.ImageSize.W1280,
                modifier = Modifier
                    .fillMaxSize()
                    .let {
                        if (LocalInspectionMode.current) {
                            it
                        } else {
                            it.sharedElement(
                                sharedContentState = rememberSharedContentState(key = "image-${media.id}-$tag"),
                                animatedVisibilityScope = animatedVisibilityScope
                            )
                        }
                    }
            )
        }

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colorStops = arrayOf(
                            0f to Color.Black.copy(alpha = 0.15f),
                            0.35f to Color.Transparent,
                            0.65f to Color.Black.copy(alpha = 0.5f),
                            1f to Color.Black.copy(alpha = 0.97f)
                        )
                    )
                )
        )

        if (media.affinityScore > 0f) {
            MatchBadge(
                score = media.affinityScore,
                isAffinity = true,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(14.dp)
            )
        }
        if (media.voteAverage > 0f) {
            MatchBadge(
                score = media.voteAverage,
                isAffinity = false,
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(14.dp)
            )
        }

        Column(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(18.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
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
                media.firstAirDate?.take(4)?.let { year ->
                    Text("·", color = Color.White.copy(alpha = 0.4f), fontSize = 13.sp)
                    Text(year, color = Color.White.copy(alpha = 0.65f), fontSize = 13.sp)
                }
                media.numberOfSeasons?.let { seasons ->
                    Text("·", color = Color.White.copy(alpha = 0.4f), fontSize = 13.sp)
                    Text("$seasons temp.", color = Color.White.copy(alpha = 0.65f), fontSize = 13.sp)
                }
            }

            Spacer(Modifier.height(8.dp))

            Text(
                text = media.name,
                color = Color.White,
                fontSize = 30.sp,
                fontWeight = FontWeight.Black,
                lineHeight = 34.sp,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )

            if (media.overview.isNotBlank()) {
                Spacer(Modifier.height(8.dp))
                Text(
                    text = media.overview,
                    color = Color.White.copy(alpha = 0.6f),
                    fontSize = 13.sp,
                    lineHeight = 19.sp,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }

            if (genreNames.isNotEmpty()) {
                Spacer(Modifier.height(12.dp))
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    userScrollEnabled = false
                ) {
                    items(genreNames, key = { it }) { genreName ->
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(20.dp))
                                .background(Color.White.copy(alpha = 0.12f))
                                .border(1.dp, Color.White.copy(alpha = 0.18f), RoundedCornerShape(20.dp))
                                .padding(horizontal = 10.dp, vertical = 4.dp)
                        ) {
                            Text(
                                text = genreName,
                                color = Color.White.copy(alpha = 0.8f),
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }
            }

            Spacer(Modifier.height(16.dp))

            val btnInteraction = remember { MutableInteractionSource() }
            val isBtnPressed by btnInteraction.collectIsPressedAsState()
            val btnScale by animateFloatAsState(
                targetValue = if (isBtnPressed) 0.94f else 1f,
                animationSpec = spring(dampingRatio = 0.45f, stiffness = 600f),
                label = "btnScale"
            )

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .scale(btnScale)
                    .shadow(16.dp, RoundedCornerShape(50.dp), spotColor = PrimaryPurple)
                    .clip(RoundedCornerShape(50.dp))
                    .background(Brush.horizontalGradient(listOf(PrimaryPurple, PrimaryMagenta)))
                    .clickable(
                        interactionSource = btnInteraction,
                        indication = null
                    ) { onClick(media) }
                    .padding(horizontal = 24.dp, vertical = 14.dp),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    Icons.Default.PlayArrow,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(Modifier.width(6.dp))
                Text(stringResource(R.string.discover_see_now), color = Color.White, fontWeight = FontWeight.ExtraBold, fontSize = 14.sp)
            }
        }
    }
}

@Composable
private fun DiscoverSkeleton() {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
    ) {
        item {
            Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)) {
                Box(
                    modifier = Modifier
                        .width(160.dp)
                        .height(32.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(shimmerBrush())
                )
                Spacer(Modifier.height(6.dp))
                Box(
                    modifier = Modifier
                        .width(240.dp)
                        .height(14.dp)
                        .clip(RoundedCornerShape(6.dp))
                        .background(shimmerBrush())
                )
            }
        }
        item {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(540.dp)
                    .padding(horizontal = 12.dp, vertical = 4.dp)
                    .clip(RoundedCornerShape(28.dp))
                    .background(shimmerBrush())
            )
        }
        item { ShowSectionSkeleton(stringResource(R.string.discover_skeleton_for_you_now)) }
        item { ShowSectionSkeleton(stringResource(R.string.discover_skeleton_fav_genres)) }
        item { ShowSectionSkeleton(stringResource(R.string.discover_skeleton_explore)) }
        item { Spacer(Modifier.height(100.dp)) }
    }
}

private fun navigateToDetail(navController: NavController, media: MediaContent, tag: String) {
    navController.navigate(Screen.Detail(media.id, tag))
}
