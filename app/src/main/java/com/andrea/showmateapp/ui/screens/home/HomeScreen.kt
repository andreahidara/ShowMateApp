package com.andrea.showmateapp.ui.screens.home

import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
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
            actionShows = uiState.actionShows,
            comedyShows = uiState.comedyShows,
            mysteryShows = uiState.mysteryShows,
            dramaShows = uiState.dramaShows,
            scifiShows = uiState.scifiShows,
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
            isLoadingMoreThisWeek = uiState.isLoadingMoreThisWeek
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
    mysteryShows: List<MediaContent>,
    dramaShows: List<MediaContent> = emptyList(),
    scifiShows: List<MediaContent> = emptyList(),
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
    isLoadingMoreThisWeek: Boolean = false
) {
    val trendingListState    = rememberLazyListState()
    val newReleasesListState = rememberLazyListState()
    val actionListState      = rememberLazyListState()
    val comedyListState      = rememberLazyListState()
    val mysteryListState     = rememberLazyListState()
    val dramaListState       = rememberLazyListState()
    val scifiListState       = rememberLazyListState()

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
                item { ShowSectionSkeleton(stringResource(R.string.home_trending)) }
                item { ShowSectionSkeleton(stringResource(R.string.home_popular_spain)) }
                item { ShowSectionSkeleton(stringResource(R.string.home_action_adventure)) }
                item { Spacer(modifier = Modifier.height(100.dp)) }
            }
        }
    } else if (criticalError != null) {
        ErrorContent(type = criticalError, onRetry = onRetry)
    } else {
        Scaffold(
            topBar = { HomeTopAppBar(userName = userName, onPickWhatToWatch = onPickWhatToWatch, onAvatarClick = onNavigateToProfile) },
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

                    if (mysteryShows.isNotEmpty()) {
                        item {
                            ShowSection(
                                title = stringResource(R.string.home_mystery_suspense),
                                items = mysteryShows,
                                sharedTransitionScope = sharedTransitionScope,
                                animatedVisibilityScope = animatedVisibilityScope,
                                onItemClick = onMediaClick,
                                tag = "mystery",
                                listState = mysteryListState
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

                    if (scifiShows.isNotEmpty()) {
                        item {
                            ShowSection(
                                title = stringResource(R.string.home_scifi),
                                items = scifiShows,
                                sharedTransitionScope = sharedTransitionScope,
                                animatedVisibilityScope = animatedVisibilityScope,
                                onItemClick = onMediaClick,
                                tag = "scifi",
                                subtitle = stringResource(R.string.home_scifi_desc),
                                listState = scifiListState
                            )
                        }
                    }

                    item { Spacer(modifier = Modifier.height(100.dp)) }
                }
            }
        }
    }
}

private fun navigateToDetail(navController: NavController, media: MediaContent, tag: String) {
    navController.navigate(Screen.Detail(media.id, tag))
}
