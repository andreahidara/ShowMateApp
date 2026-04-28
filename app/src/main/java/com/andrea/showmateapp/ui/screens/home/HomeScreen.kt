package com.andrea.showmateapp.ui.screens.home

import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ThumbDown
import androidx.compose.material.icons.filled.WifiOff
import androidx.compose.material3.*
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.andrea.showmateapp.R
import com.andrea.showmateapp.data.model.MediaContent
import com.andrea.showmateapp.ui.components.*
import com.andrea.showmateapp.ui.navigation.Screen
import com.andrea.showmateapp.ui.screens.home.components.*
import com.andrea.showmateapp.ui.theme.PrimaryPurple
import com.andrea.showmateapp.ui.theme.SurfaceDark
import com.andrea.showmateapp.ui.theme.TextGray
import com.andrea.showmateapp.util.ErrorType
import com.andrea.showmateapp.util.SnackbarManager

@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
fun HomeScreen(
    navController: NavController,
    sharedTransitionScope: SharedTransitionScope,
    animatedVisibilityScope: AnimatedVisibilityScope,
    scrollToTopTrigger: Int = 0,
    onNavigateToProfile: () -> Unit = {},
    viewModel: HomeViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(Unit) {
        SnackbarManager.events.collect { event ->
            snackbarHostState.showSnackbar(
                message = event.message,
                actionLabel = event.actionLabel,
                duration = event.duration
            )
        }
    }

    val onMediaClick: (MediaContent, String) -> Unit = remember(navController) {
        { media, tag -> navigateToDetail(navController, media, tag) }
    }

    val onRetry = remember(viewModel) { { viewModel.onAction(HomeAction.LoadData) } }
    val onRefresh = remember(viewModel) { { viewModel.onAction(HomeAction.Refresh) } }
    val onPickWhatToWatch = remember(viewModel) { { viewModel.onAction(HomeAction.RequestWhatToWatch) } }
    val onLoadMoreTrending = remember(viewModel) { { viewModel.onAction(HomeAction.LoadMoreTrending) } }
    val onLoadMoreThisWeek = remember(viewModel) { { viewModel.onAction(HomeAction.LoadMoreThisWeek) } }
    val onPlatformSelected: (String?) -> Unit = remember(viewModel) {
        { platform -> viewModel.onAction(HomeAction.SelectPlatform(platform)) }
    }

    var feedbackMedia by remember { mutableStateOf<MediaContent?>(null) }
    val dismissedMsg = stringResource(R.string.feedback_dismissed)
    val watchedMsg = stringResource(R.string.feedback_marked_watched)

    Box(modifier = Modifier.fillMaxSize()) {
        HomeScreenContent(
            userName = uiState.userName,
            scrollToTopTrigger = scrollToTopTrigger,
            onNavigateToProfile = onNavigateToProfile,
            upNextShows = uiState.upNextShows,
            upNextProgress = uiState.upNextProgress,
            trendingShows = uiState.trendingShows,
            top10Shows = uiState.top10Shows,
            newReleasesShows = uiState.newReleasesShows,
            actionShows = uiState.genres.action,
            comedyShows = uiState.genres.comedy,
            dramaShows = uiState.genres.drama,
            sciFiShows = uiState.genres.sciFi,
            mysteryShows = uiState.genres.mystery,
            thisWeekShows = uiState.thisWeekShows,
            selectedPlatform = uiState.selectedPlatform,
            platformShows = uiState.platformShows,
            isPlatformLoading = uiState.isPlatformLoading,
            isLoading = uiState.isLoading,
            isRefreshing = uiState.isRefreshing,
            criticalError = uiState.criticalError,
            sharedTransitionScope = sharedTransitionScope,
            animatedVisibilityScope = animatedVisibilityScope,
            onMediaClick = onMediaClick,
            onMediaLongPress = { media -> feedbackMedia = media },
            onRetry = onRetry,
            onRefresh = onRefresh,
            onPickWhatToWatch = onPickWhatToWatch,
            onPlatformSelected = onPlatformSelected,
            onLoadMoreTrending = onLoadMoreTrending,
            onLoadMoreThisWeek = onLoadMoreThisWeek,
            isLoadingMoreTrending = uiState.isLoadingMoreTrending,
            isLoadingMoreThisWeek = uiState.isLoadingMoreThisWeek,
            isFromCache = uiState.isFromCache
        )

        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .navigationBarsPadding()
        )
    }

    feedbackMedia?.let { media ->
        RecommendationFeedbackSheet(
            media = media,
            onDismiss = { feedbackMedia = null },
            onNotInterested = {
                feedbackMedia = null
                viewModel.onAction(HomeAction.SwipeLeft(media.id))
                SnackbarManager.showMessage(dismissedMsg)
            },
            onAlreadyWatched = {
                feedbackMedia = null
                viewModel.onAction(HomeAction.MarkAsWatched(media.id))
                SnackbarManager.showMessage(watchedMsg)
            }
        )
    }

    if (uiState.showContextSelector) {
        ContextSelectorDialog(
            onDismiss = { viewModel.onAction(HomeAction.DismissContextSelector) },
            onConfirm = { mood, time -> viewModel.onAction(HomeAction.PickWhatToWatchToday(mood, time)) }
        )
    }

    uiState.whatToWatchToday?.let { media ->
        WhatToWatchDialog(
            media = media,
            onDismiss = { viewModel.onAction(HomeAction.DismissWhatToWatch) },
            onViewDetails = {
                viewModel.onAction(HomeAction.DismissWhatToWatch)
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
    onNavigateToProfile: () -> Unit = {},
    upNextShows: List<MediaContent>,
    upNextProgress: Map<Int, Float> = emptyMap(),
    trendingShows: List<MediaContent>,
    top10Shows: List<MediaContent> = emptyList(),
    newReleasesShows: List<MediaContent> = emptyList(),
    actionShows: List<MediaContent>,
    comedyShows: List<MediaContent>,
    dramaShows: List<MediaContent> = emptyList(),
    sciFiShows: List<MediaContent> = emptyList(),
    mysteryShows: List<MediaContent> = emptyList(),
    thisWeekShows: List<MediaContent>,
    selectedPlatform: String? = null,
    platformShows: Map<String, List<MediaContent>> = emptyMap(),
    isPlatformLoading: Boolean = false,
    isLoading: Boolean,
    isRefreshing: Boolean,
    criticalError: ErrorType? = null,
    sharedTransitionScope: SharedTransitionScope,
    animatedVisibilityScope: AnimatedVisibilityScope,
    onMediaClick: (MediaContent, String) -> Unit,
    onMediaLongPress: ((MediaContent) -> Unit)? = null,
    onRetry: () -> Unit,
    onRefresh: () -> Unit,
    onPickWhatToWatch: () -> Unit = {},
    onPlatformSelected: (String?) -> Unit = {},
    onLoadMoreTrending: () -> Unit = {},
    onLoadMoreThisWeek: () -> Unit = {},
    isLoadingMoreTrending: Boolean = false,
    isLoadingMoreThisWeek: Boolean = false,
    isFromCache: Boolean = false
) {
    val trendingListState = rememberLazyListState()
    val newReleasesListState = rememberLazyListState()
    val actionListState = rememberLazyListState()
    val comedyListState = rememberLazyListState()
    val dramaListState = rememberLazyListState()
    val sciFiListState = rememberLazyListState()
    val mysteryListState = rememberLazyListState()

    if (isLoading && trendingShows.isEmpty()) {
        LazyColumn(modifier = Modifier.fillMaxSize()) {
            item {
                HomeTopAppBar(
                    userName = stringResource(R.string.home_loading),
                    onPickWhatToWatch = {},
                    onAvatarClick = {}
                )
            }
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
            item { ShowSectionSkeleton(stringResource(R.string.home_trending)) }
            item { ShowSectionSkeleton(stringResource(R.string.home_popular_spain)) }
            item { ShowSectionSkeleton(stringResource(R.string.home_action_adventure)) }
            item { ShowSectionSkeleton(stringResource(R.string.home_drama)) }
            item { ShowSectionSkeleton(stringResource(R.string.home_scifi)) }
            item { ShowSectionSkeleton(stringResource(R.string.home_mystery_suspense)) }
            item { Spacer(modifier = Modifier.height(100.dp)) }
        }
    } else if (criticalError != null) {
        ErrorContent(type = criticalError, onRetry = onRetry)
    } else {
        val listState = rememberLazyListState()
        LaunchedEffect(scrollToTopTrigger) {
            if (scrollToTopTrigger > 0) listState.animateScrollToItem(0)
        }

        PullToRefreshBox(
            isRefreshing = isRefreshing,
            onRefresh = onRefresh,
            modifier = Modifier.fillMaxSize()
        ) {
            LazyColumn(
                state = listState,
                modifier = Modifier.fillMaxSize()
            ) {
                item {
                    HomeTopAppBar(
                        userName = userName,
                        onPickWhatToWatch = onPickWhatToWatch,
                        onAvatarClick = onNavigateToProfile
                    )
                }

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
                        title = stringResource(id = R.string.home_trending),
                        items = trendingShows.drop(5).ifEmpty { trendingShows },
                        sharedTransitionScope = sharedTransitionScope,
                        animatedVisibilityScope = animatedVisibilityScope,
                        onItemClick = onMediaClick,
                        tag = "trending",
                        subtitle = stringResource(R.string.home_trending_desc),
                        listState = trendingListState,
                        onLoadMore = onLoadMoreTrending,
                        isLoadingMore = isLoadingMoreTrending,
                        onItemLongPress = onMediaLongPress
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
                            title = stringResource(R.string.home_new_releases),
                            items = newReleasesShows,
                            sharedTransitionScope = sharedTransitionScope,
                            animatedVisibilityScope = animatedVisibilityScope,
                            onItemClick = onMediaClick,
                            tag = "new_releases",
                            subtitle = stringResource(R.string.home_new_releases_desc),
                            listState = newReleasesListState,
                            onItemLongPress = onMediaLongPress
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
                            onPlatformSelected = onPlatformSelected,
                            onLoadMore = onLoadMoreThisWeek,
                            isLoadingMore = isLoadingMoreThisWeek
                        )
                    }
                }

                if (actionShows.isNotEmpty()) {
                    item {
                        ShowSection(
                            title = stringResource(R.string.home_action_adventure),
                            items = actionShows,
                            sharedTransitionScope = sharedTransitionScope,
                            animatedVisibilityScope = animatedVisibilityScope,
                            onItemClick = onMediaClick,
                            tag = "action",
                            listState = actionListState,
                            onItemLongPress = onMediaLongPress
                        )
                    }
                }

                if (comedyShows.isNotEmpty()) {
                    item {
                        ShowSection(
                            title = stringResource(R.string.home_comedies),
                            items = comedyShows,
                            sharedTransitionScope = sharedTransitionScope,
                            animatedVisibilityScope = animatedVisibilityScope,
                            onItemClick = onMediaClick,
                            tag = "comedy",
                            listState = comedyListState,
                            onItemLongPress = onMediaLongPress
                        )
                    }
                }

                if (dramaShows.isNotEmpty()) {
                    item {
                        ShowSection(
                            title = stringResource(R.string.home_drama),
                            items = dramaShows,
                            sharedTransitionScope = sharedTransitionScope,
                            animatedVisibilityScope = animatedVisibilityScope,
                            onItemClick = onMediaClick,
                            tag = "drama",
                            subtitle = stringResource(R.string.home_drama_desc),
                            listState = dramaListState,
                            onItemLongPress = onMediaLongPress
                        )
                    }
                }

                if (sciFiShows.isNotEmpty()) {
                    item {
                        ShowSection(
                            title = stringResource(R.string.home_scifi),
                            items = sciFiShows,
                            sharedTransitionScope = sharedTransitionScope,
                            animatedVisibilityScope = animatedVisibilityScope,
                            onItemClick = onMediaClick,
                            tag = "scifi",
                            subtitle = stringResource(R.string.home_scifi_desc),
                            listState = sciFiListState,
                            onItemLongPress = onMediaLongPress
                        )
                    }
                }

                if (mysteryShows.isNotEmpty()) {
                    item {
                        ShowSection(
                            title = stringResource(R.string.home_mystery_suspense),
                            items = mysteryShows,
                            sharedTransitionScope = sharedTransitionScope,
                            animatedVisibilityScope = animatedVisibilityScope,
                            onItemClick = onMediaClick,
                            tag = "mystery",
                            listState = mysteryListState,
                            onItemLongPress = onMediaLongPress
                        )
                    }
                }

                item { Spacer(modifier = Modifier.height(100.dp)) }
            }
        }
    }
}

private fun navigateToDetail(navController: NavController, media: MediaContent, tag: String) {
    navController.navigate(Screen.Detail(media.id, tag))
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun RecommendationFeedbackSheet(
    media: MediaContent,
    onDismiss: () -> Unit,
    onNotInterested: () -> Unit,
    onAlreadyWatched: () -> Unit
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = SurfaceDark,
        tonalElevation = 0.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .padding(bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                text = media.name,
                color = Color.White,
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp
            )
            Text(
                text = stringResource(R.string.feedback_sheet_title),
                color = TextGray,
                fontSize = 13.sp,
                modifier = Modifier.padding(bottom = 12.dp)
            )

            FeedbackOption(
                icon = Icons.Default.ThumbDown,
                title = stringResource(R.string.feedback_not_interested),
                subtitle = stringResource(R.string.feedback_not_interested_desc),
                iconTint = Color(0xFFE57373),
                onClick = onNotInterested
            )

            FeedbackOption(
                icon = Icons.Default.CheckCircle,
                title = stringResource(R.string.feedback_already_watched),
                subtitle = stringResource(R.string.feedback_already_watched_desc),
                iconTint = PrimaryPurple,
                onClick = onAlreadyWatched
            )

            Spacer(modifier = Modifier.height(8.dp))
            TextButton(
                onClick = onDismiss,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = stringResource(R.string.cancel),
                    color = TextGray,
                    fontSize = 15.sp
                )
            }
        }
    }
}

@Composable
private fun FeedbackOption(
    icon: ImageVector,
    title: String,
    subtitle: String,
    iconTint: Color,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = iconTint,
            modifier = Modifier.size(24.dp)
        )
        Column {
            Text(
                text = title,
                color = Color.White,
                fontWeight = FontWeight.SemiBold,
                fontSize = 15.sp
            )
            Text(
                text = subtitle,
                color = TextGray,
                fontSize = 12.sp
            )
        }
    }
}
