package com.andrea.showmateapp.ui.screens.home

import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.WifiOff
import androidx.compose.material3.*
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.andrea.showmateapp.R
import com.andrea.showmateapp.data.network.MediaContent
import com.andrea.showmateapp.ui.components.premium.*
import com.andrea.showmateapp.ui.navigation.Screen
import com.andrea.showmateapp.ui.screens.home.components.*
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

    if (isLoading && trendingShows.isEmpty()) {
        LazyColumn(modifier = Modifier.fillMaxSize()) {
            item {
                HomeTopAppBar(
                    userName = "Cargando...",
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
                        isLoadingMore = isLoadingMoreTrending
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
                            listState = actionListState
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
                            listState = comedyListState
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
                            listState = dramaListState
                        )
                    }
                }

                item { Spacer(modifier = Modifier.height(100.dp)) }
            }
        }
    }
} // HomeScreenContent

private fun navigateToDetail(navController: NavController, media: MediaContent, tag: String) {
    navController.navigate(Screen.Detail(media.id, tag))
}
