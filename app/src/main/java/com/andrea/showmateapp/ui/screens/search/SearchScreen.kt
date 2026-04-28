package com.andrea.showmateapp.ui.screens.search

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.TheaterComedy
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.TravelExplore
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import androidx.paging.LoadState
import androidx.paging.compose.collectAsLazyPagingItems
import androidx.paging.compose.itemKey
import com.andrea.showmateapp.R
import com.andrea.showmateapp.ui.components.*
import com.andrea.showmateapp.ui.components.shimmerBrush
import com.andrea.showmateapp.ui.navigation.Screen
import com.andrea.showmateapp.ui.theme.AccentBlue
import com.andrea.showmateapp.ui.theme.PrimaryPurple
import com.andrea.showmateapp.ui.theme.PrimaryPurpleLight
import com.andrea.showmateapp.ui.theme.SurfaceDark
import com.andrea.showmateapp.ui.theme.TextGray

@OptIn(ExperimentalMaterial3Api::class, ExperimentalSharedTransitionApi::class)
@Composable
fun SearchScreen(
    globalNavController: NavController,
    sharedTransitionScope: SharedTransitionScope,
    animatedVisibilityScope: AnimatedVisibilityScope,
    scrollToTopTrigger: Int = 0,
    viewModel: SearchViewModel = hiltViewModel()
) {
    var query by remember { mutableStateOf("") }
    val searchResults by viewModel.searchResults.collectAsStateWithLifecycle()
    val trendingShows by viewModel.trendingShows.collectAsStateWithLifecycle()
    val trendingPeople by viewModel.trendingPeople.collectAsStateWithLifecycle()
    val personSearchResults by viewModel.personSearchResults.collectAsStateWithLifecycle()
    val isLoading by viewModel.isLoading.collectAsStateWithLifecycle()
    val isFilterActive by viewModel.isFilterActive.collectAsStateWithLifecycle()
    val recentSearches by viewModel.recentSearches.collectAsStateWithLifecycle()
    val suggestions by viewModel.suggestions.collectAsStateWithLifecycle()
    val searchMode by viewModel.searchMode.collectAsStateWithLifecycle()

    val pagingItems = viewModel.searchPagingData.collectAsLazyPagingItems()
    val usePaging = query.isNotBlank() && searchMode == SearchMode.TITLE && !isFilterActive

    val showTrendingPeople = query.isBlank() && !isFilterActive &&
        (searchMode == SearchMode.ACTOR || searchMode == SearchMode.CREATOR)

    val showPersonResults = query.isNotBlank() && !isFilterActive &&
        (searchMode == SearchMode.ACTOR || searchMode == SearchMode.CREATOR)

    var showFilters by remember { mutableStateOf(false) }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    val selectedGenre by viewModel.selectedGenre.collectAsStateWithLifecycle()
    val selectedPlatform by viewModel.selectedPlatform.collectAsStateWithLifecycle()
    val yearFrom by viewModel.yearFrom.collectAsStateWithLifecycle()
    val yearTo by viewModel.yearTo.collectAsStateWithLifecycle()
    val selectedRating by viewModel.selectedRating.collectAsStateWithLifecycle()
    val errorMessage by viewModel.errorMessage.collectAsStateWithLifecycle()

    val showRecent = query.isBlank() && !isFilterActive && recentSearches.isNotEmpty()
    val listToShow = if (query.isBlank() && !isFilterActive) trendingShows else searchResults
    val listTag = if (query.isBlank() && !isFilterActive) "search_trending" else "search_results"

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        contentWindowInsets = WindowInsets(0.dp),
        topBar = {
            Column(
                modifier = Modifier
                    .statusBarsPadding()
                    .background(MaterialTheme.colorScheme.background)
                    .padding(top = 12.dp)
            ) {
                val gradientColors = listOf(PrimaryPurple, com.andrea.showmateapp.ui.theme.PrimaryMagenta)
                Text(
                    text = stringResource(R.string.nav_search),
                    style = TextStyle(brush = Brush.linearGradient(colors = gradientColors)),
                    fontSize = 32.sp,
                    fontWeight = FontWeight.Black,
                    letterSpacing = (-1.5).sp,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                )

                Spacer(Modifier.height(4.dp))

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val interactionSource = remember { MutableInteractionSource() }
                    val isFocused by interactionSource.collectIsFocusedAsState()
                    val glowAlpha by animateFloatAsState(
                        targetValue = if (isFocused) 0.5f else 0f,
                        animationSpec = tween(400),
                        label = "glowAlpha"
                    )

                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(52.dp)
                            .shadow(
                                elevation = if (isFocused) 12.dp else 0.dp,
                                shape = RoundedCornerShape(16.dp),
                                spotColor = PrimaryPurple.copy(alpha = glowAlpha),
                                ambientColor = PrimaryPurple.copy(alpha = glowAlpha)
                            )
                            .clip(RoundedCornerShape(16.dp))
                            .background(Color.White.copy(alpha = if (isFocused) 0.1f else 0.07f))
                            .border(
                                width = 1.dp,
                                color = when {
                                    isFocused -> PrimaryPurple.copy(alpha = 0.6f)
                                    query.isNotEmpty() -> PrimaryPurple.copy(alpha = 0.3f)
                                    else -> Color.White.copy(alpha = 0.1f)
                                },
                                shape = RoundedCornerShape(16.dp)
                            )
                            .padding(horizontal = 14.dp),
                        contentAlignment = Alignment.CenterStart
                    ) {
                        BasicTextField(
                            value = query,
                            onValueChange = {
                                query = it
                                viewModel.updateSuggestions(it)
                                viewModel.searchMedia(it)
                            },
                            interactionSource = interactionSource,
                            singleLine = true,
                            textStyle = TextStyle(
                                color = Color.White,
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Normal
                            ),
                            cursorBrush = SolidColor(PrimaryPurple),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(start = 32.dp, end = if (query.isNotEmpty()) 32.dp else 0.dp),
                            decorationBox = { inner ->
                                if (query.isEmpty()) {
                                    Text(
                                        stringResource(R.string.search_placeholder),
                                        color = TextGray,
                                        fontSize = 15.sp
                                    )
                                }
                                inner()
                            }
                        )
                        Icon(
                            Icons.Default.Search,
                            contentDescription = null,
                            tint = if (isFocused || query.isNotEmpty()) PrimaryPurple else TextGray,
                            modifier = Modifier
                                .align(Alignment.CenterStart)
                                .size(20.dp)
                        )
                        if (query.isNotEmpty()) {
                            Box(
                                modifier = Modifier
                                    .align(Alignment.CenterEnd)
                                    .size(24.dp)
                                    .clip(CircleShape)
                                    .background(Color.White.copy(alpha = 0.12f))
                                    .clickable {
                                        query = ""
                                        viewModel.searchMedia("")
                                        viewModel.updateSuggestions("")
                                    },
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    Icons.Default.Clear,
                                    contentDescription = stringResource(R.string.search_clear_all),
                                    tint = Color.White.copy(alpha = 0.8f),
                                    modifier = Modifier.size(14.dp)
                                )
                            }
                        }
                    }

                    Box(
                        modifier = Modifier
                            .size(52.dp)
                            .clip(RoundedCornerShape(16.dp))
                            .background(
                                if (isFilterActive) {
                                    PrimaryPurple
                                } else {
                                    Color.White.copy(alpha = 0.07f)
                                }
                            )
                            .border(
                                width = 1.dp,
                                color = if (isFilterActive) {
                                    PrimaryPurple
                                } else {
                                    Color.White.copy(alpha = 0.1f)
                                },
                                shape = RoundedCornerShape(16.dp)
                            )
                            .clickable { showFilters = true },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Default.FilterList,
                            contentDescription = stringResource(R.string.search_filters),
                            tint = Color.White,
                            modifier = Modifier.size(22.dp)
                        )
                        if (isFilterActive) {
                            Box(
                                modifier = Modifier
                                    .size(10.dp)
                                    .background(Color(0xFF4CAF50), CircleShape)
                                    .border(2.dp, MaterialTheme.colorScheme.background, CircleShape)
                                    .align(Alignment.TopEnd)
                                    .offset(x = (-2).dp, y = 2.dp)
                            )
                        }
                    }
                }

                Spacer(Modifier.height(10.dp))

                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    contentPadding = PaddingValues(horizontal = 16.dp),
                    modifier = Modifier.padding(bottom = 10.dp)
                ) {
                    items(SearchMode.entries, key = { it.name }) { mode ->
                        val isSelected = mode == searchMode
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(20.dp))
                                .background(
                                    if (isSelected) {
                                        PrimaryPurple
                                    } else {
                                        Color.White.copy(alpha = 0.07f)
                                    }
                                )
                                .border(
                                    1.dp,
                                    if (isSelected) {
                                        Color.Transparent
                                    } else {
                                        Color.White.copy(alpha = 0.12f)
                                    },
                                    RoundedCornerShape(20.dp)
                                )
                                .clickable {
                                    viewModel.setSearchMode(mode)
                                    if (query.isNotBlank()) {
                                        viewModel.searchMedia(query)
                                    }
                                }
                                .padding(horizontal = 12.dp, vertical = 5.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = mode.label,
                                color = if (isSelected) Color.White else TextGray,
                                fontSize = 12.sp,
                                fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal
                            )
                        }
                    }
                }
            }
        }
    ) { padding ->
        Box(modifier = Modifier.fillMaxSize()) {
            Column(modifier = Modifier.padding(padding)) {
                if (showRecent) {
                    Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = stringResource(R.string.search_recent),
                                color = TextGray,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 1.5.sp
                            )
                            TextButton(
                                onClick = { viewModel.clearRecentSearches() },
                                contentPadding = PaddingValues(0.dp)
                            ) {
                                Text(
                                    stringResource(R.string.search_clear_all),
                                    color = PrimaryPurple,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }
                        Spacer(Modifier.height(6.dp))
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            items(recentSearches, key = { it }) { recent ->
                                Row(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(20.dp))
                                        .background(SurfaceDark)
                                        .border(1.dp, Color.White.copy(alpha = 0.07f), RoundedCornerShape(20.dp))
                                        .clickable {
                                            query = recent
                                            viewModel.searchMedia(recent)
                                        }
                                        .padding(horizontal = 12.dp, vertical = 7.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Icon(
                                        Icons.Default.History,
                                        contentDescription = null,
                                        tint = TextGray,
                                        modifier = Modifier.size(13.dp)
                                    )
                                    Text(recent, color = Color.White.copy(alpha = 0.85f), fontSize = 13.sp)
                                    Icon(
                                        Icons.Default.Clear,
                                        contentDescription = "Eliminar",
                                        tint = TextGray,
                                        modifier = Modifier
                                            .size(12.dp)
                                            .clickable { viewModel.removeRecentSearch(recent) }
                                    )
                                }
                            }
                        }
                        Spacer(Modifier.height(12.dp))
                    }
                }

                if (usePaging) {
                    val isPagingLoading = pagingItems.loadState.refresh is LoadState.Loading
                    val isPagingError = pagingItems.loadState.refresh is LoadState.Error

                    AnimatedContent(
                        targetState = when {
                            isPagingLoading && pagingItems.itemCount == 0 -> "loading"
                            isPagingError -> "error"
                            pagingItems.itemCount > 0 -> "results"
                            !isPagingLoading -> "empty"
                            else -> "loading"
                        },
                        transitionSpec = { fadeIn(tween(300)) togetherWith fadeOut(tween(200)) },
                        label = "searchPagingState"
                    ) { state ->
                        when (state) {
                            "loading" -> SearchGridSkeleton()
                            "error" -> ErrorView(
                                message = stringResource(R.string.error_unknown),
                                onRetry = { pagingItems.retry() }
                            )
                            "results" -> {
                                val gridState = rememberLazyGridState()
                                LaunchedEffect(scrollToTopTrigger) {
                                    if (scrollToTopTrigger > 0) gridState.animateScrollToItem(0)
                                }
                                Column {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        Box(Modifier.size(6.dp).background(PrimaryPurple, CircleShape))
                                        Text(
                                            text = stringResource(R.string.search_results_count, pagingItems.itemCount),
                                            color = TextGray,
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            letterSpacing = 1.5.sp
                                        )
                                    }
                                    LazyVerticalGrid(
                                        state = gridState,
                                        columns = GridCells.Adaptive(minSize = 105.dp),
                                        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 120.dp),
                                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                                        verticalArrangement = Arrangement.spacedBy(16.dp),
                                        modifier = Modifier.fillMaxSize()
                                    ) {
                                        items(
                                            count = pagingItems.itemCount,
                                            key = pagingItems.itemKey { it.id }
                                        ) { index ->
                                            pagingItems[index]?.let { media ->
                                                ShowCard(
                                                    media = media,
                                                    sharedTransitionScope = sharedTransitionScope,
                                                    animatedVisibilityScope = animatedVisibilityScope,
                                                    onClick = { selectedMedia, tag ->
                                                        globalNavController.navigate(
                                                            Screen.Detail(selectedMedia.id, tag)
                                                        )
                                                    },
                                                    tag = "search_results",
                                                    width = Dp.Unspecified
                                                )
                                            }
                                        }
                                        if (pagingItems.loadState.append is LoadState.Loading) {
                                            item {
                                                Box(
                                                    modifier = Modifier.fillMaxWidth().padding(16.dp),
                                                    contentAlignment = Alignment.Center
                                                ) {
                                                    CircularProgressIndicator(
                                                        color = PrimaryPurple,
                                                        modifier = Modifier.size(24.dp)
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                            else -> NoResultsState(query)
                        }
                    }
                } else if (showPersonResults) {
                    val personLabel =
                        if (searchMode == SearchMode.ACTOR) "ACTORES ENCONTRADOS" else "DIRECTORES ENCONTRADOS"
                    Column {
                        Row(
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Box(modifier = Modifier.size(6.dp).background(PrimaryPurple, CircleShape))
                            Text(
                                text = personLabel,
                                color = TextGray,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 1.5.sp
                            )
                        }
                        if (isLoading) {
                            SearchGridSkeleton()
                        } else if (personSearchResults.isEmpty()) {
                            NoResultsState(query)
                        } else {
                            LazyVerticalGrid(
                                columns = GridCells.Adaptive(minSize = 100.dp),
                                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 120.dp),
                                horizontalArrangement = Arrangement.spacedBy(12.dp),
                                verticalArrangement = Arrangement.spacedBy(16.dp),
                                modifier = Modifier.fillMaxSize()
                            ) {
                                items(personSearchResults, key = { it.id }) { person ->
                                    TrendingPersonCard(
                                        person = person,
                                        onClick = { }
                                    )
                                }
                            }
                        }
                    }
                } else if (showTrendingPeople) {
                    val personLabel =
                        if (searchMode == SearchMode.ACTOR) "ACTORES EN TENDENCIA" else "DIRECTORES EN TENDENCIA"
                    Column {
                        Row(
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Box(modifier = Modifier.size(6.dp).background(PrimaryPurple, CircleShape))
                            Text(
                                text = personLabel,
                                color = TextGray,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 1.5.sp
                            )
                        }
                        if (trendingPeople.isEmpty()) {
                            Box(
                                modifier = Modifier.fillMaxWidth().height(200.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                CircularProgressIndicator(color = PrimaryPurple, modifier = Modifier.size(28.dp))
                            }
                        } else {
                            LazyVerticalGrid(
                                columns = GridCells.Adaptive(minSize = 100.dp),
                                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 120.dp),
                                horizontalArrangement = Arrangement.spacedBy(12.dp),
                                verticalArrangement = Arrangement.spacedBy(16.dp),
                                modifier = Modifier.fillMaxSize()
                            ) {
                                items(trendingPeople, key = { it.id }) { person ->
                                    TrendingPersonCard(
                                        person = person,
                                        onClick = { }
                                    )
                                }
                            }
                        }
                    }
                } else {
                    AnimatedContent(
                        targetState = when {
                            isLoading -> "loading"
                            errorMessage != null -> "error"
                            listToShow.isNotEmpty() -> "results"
                            query.isNotBlank() || isFilterActive -> "empty"
                            else -> "idle"
                        },
                        transitionSpec = { fadeIn(tween(300)) togetherWith fadeOut(tween(200)) },
                        label = "searchState"
                    ) { state ->
                        when (state) {
                            "loading" -> SearchGridSkeleton()

                            "error" -> ErrorView(
                                message = errorMessage?.asString() ?: stringResource(R.string.error_unknown),
                                onRetry = { viewModel.searchMedia(query) }
                            )

                            "results" -> {
                                val gridState = rememberLazyGridState()
                                LaunchedEffect(scrollToTopTrigger) {
                                    if (scrollToTopTrigger > 0) gridState.animateScrollToItem(0)
                                }
                                Column {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(6.dp)
                                                .background(PrimaryPurple, CircleShape)
                                        )
                                        Text(
                                            text = if (query.isBlank() && !isFilterActive) {
                                                stringResource(R.string.search_trending_label)
                                            } else {
                                                stringResource(R.string.search_results_count, listToShow.size)
                                            },
                                            color = TextGray,
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            letterSpacing = 1.5.sp
                                        )
                                    }
                                    LazyVerticalGrid(
                                        state = gridState,
                                        columns = GridCells.Adaptive(minSize = 105.dp),
                                        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 120.dp),
                                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                                        verticalArrangement = Arrangement.spacedBy(16.dp),
                                        modifier = Modifier.fillMaxSize()
                                    ) {
                                        items(listToShow, key = { it.id }) { media ->
                                            ShowCard(
                                                media = media,
                                                sharedTransitionScope = sharedTransitionScope,
                                                animatedVisibilityScope = animatedVisibilityScope,
                                                onClick = { selectedMedia, tag ->
                                                    globalNavController.navigate(Screen.Detail(selectedMedia.id, tag))
                                                },
                                                tag = listTag,
                                                width = Dp.Unspecified
                                            )
                                        }
                                    }
                                }
                            }

                            "empty" -> {
                                NoResultsState(query)
                            }

                            else -> {}
                        }
                    }
                }
            }

            AnimatedVisibility(
                visible = suggestions.isNotEmpty(),
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.TopCenter)
                    .padding(top = padding.calculateTopPadding())
                    .padding(horizontal = 16.dp)
            ) {
                Surface(
                    color = Color(0xFF1C1C2E),
                    shape = RoundedCornerShape(16.dp),
                    tonalElevation = 8.dp,
                    shadowElevation = 8.dp
                ) {
                    Column(modifier = Modifier.padding(vertical = 4.dp)) {
                        suggestions.forEachIndexed { idx, media ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        query = media.name
                                        viewModel.updateSuggestions("")
                                        viewModel.searchMedia(media.name)
                                    }
                                    .padding(horizontal = 16.dp, vertical = 12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(32.dp)
                                        .background(PrimaryPurple.copy(alpha = 0.12f), CircleShape),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        Icons.Default.Search,
                                        contentDescription = null,
                                        tint = PrimaryPurple,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                                Spacer(Modifier.width(12.dp))
                                Text(
                                    media.name,
                                    color = Color.White,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                            if (idx < suggestions.lastIndex) {
                                HorizontalDivider(
                                    color = Color.White.copy(alpha = 0.05f),
                                    modifier = Modifier.padding(horizontal = 16.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    if (showFilters) {
        ModalBottomSheet(
            onDismissRequest = { showFilters = false },
            sheetState = sheetState,
            containerColor = Color(0xFF1A1A2A),
            contentColor = Color.White,
            dragHandle = {
                Box(
                    modifier = Modifier
                        .padding(top = 12.dp, bottom = 4.dp)
                        .width(36.dp)
                        .height(4.dp)
                        .background(Color.White.copy(alpha = 0.15f), CircleShape)
                )
            }
        ) {
            FilterSheetContent(
                selectedGenre = selectedGenre,
                selectedPlatform = selectedPlatform,
                yearFrom = yearFrom,
                yearTo = yearTo,
                selectedRating = selectedRating,
                onGenreSelected = { viewModel.updateGenre(it) },
                onPlatformSelected = { viewModel.updatePlatform(it) },
                onYearRangeSelected = { from, to -> viewModel.updateYearRange(from, to) },
                onRatingSelected = { viewModel.updateRating(it) },
                onClear = { viewModel.clearFilters() }
            )
        }
    }
}

@OptIn(ExperimentalLayoutApi::class, ExperimentalMaterial3Api::class)
@Composable
fun FilterSheetContent(
    selectedGenre: String?,
    selectedPlatform: String? = null,
    yearFrom: Int = SearchViewModel.MIN_YEAR,
    yearTo: Int = SearchViewModel.CURRENT_YEAR,
    selectedRating: Float?,
    onGenreSelected: (String?) -> Unit,
    onPlatformSelected: (String?) -> Unit = {},
    onYearRangeSelected: (Int, Int) -> Unit,
    onRatingSelected: (Float?) -> Unit,
    onClear: () -> Unit
) {
    val currentYear = SearchViewModel.CURRENT_YEAR
    val genres = SearchViewModel.AVAILABLE_GENRES
    val platforms = SearchViewModel.AVAILABLE_PLATFORMS
    val hasActiveFilters = selectedGenre != null || selectedPlatform != null ||
        yearFrom > SearchViewModel.MIN_YEAR || yearTo < currentYear || selectedRating != null

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp)
            .padding(bottom = 40.dp)
            .navigationBarsPadding()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                stringResource(R.string.search_filters),
                color = Color.White,
                fontSize = 22.sp,
                fontWeight = FontWeight.Black
            )
            if (hasActiveFilters) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(20.dp))
                        .background(Color.White.copy(alpha = 0.08f))
                        .clickable(onClick = onClear)
                        .padding(horizontal = 14.dp, vertical = 7.dp)
                ) {
                    Text(
                        stringResource(R.string.search_reset),
                        color = PrimaryPurpleLight,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }

        Spacer(Modifier.height(24.dp))

        FilterSectionLabel(stringResource(R.string.search_genres))
        Spacer(Modifier.height(10.dp))
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            genres.forEach { (id, name) ->
                val isSelected = selectedGenre == id
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(20.dp))
                        .background(if (isSelected) PrimaryPurple else Color.White.copy(alpha = 0.06f))
                        .border(
                            1.dp,
                            if (isSelected) Color.Transparent else Color.White.copy(alpha = 0.1f),
                            RoundedCornerShape(20.dp)
                        )
                        .clickable { onGenreSelected(if (isSelected) null else id) }
                        .padding(horizontal = 14.dp, vertical = 8.dp)
                ) {
                    Text(
                        name,
                        color = if (isSelected) Color.White else Color.White.copy(alpha = 0.65f),
                        fontSize = 13.sp,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                    )
                }
            }
        }

        Spacer(Modifier.height(24.dp))

        FilterSectionLabel(stringResource(R.string.search_platforms))
        Spacer(Modifier.height(10.dp))
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            platforms.forEach { (id, name) ->
                val isSelected = selectedPlatform == id
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(20.dp))
                        .background(if (isSelected) AccentBlue else Color.White.copy(alpha = 0.06f))
                        .border(
                            1.dp,
                            if (isSelected) Color.Transparent else Color.White.copy(alpha = 0.1f),
                            RoundedCornerShape(20.dp)
                        )
                        .clickable { onPlatformSelected(if (isSelected) null else id) }
                        .padding(horizontal = 14.dp, vertical = 8.dp)
                ) {
                    Text(
                        name,
                        color = if (isSelected) Color.White else Color.White.copy(alpha = 0.65f),
                        fontSize = 13.sp,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                    )
                }
            }
        }

        Spacer(Modifier.height(24.dp))

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier.fillMaxWidth()
        ) {
            FilterSectionLabel(stringResource(R.string.search_release_year))
            Text(
                text = if (yearFrom == SearchViewModel.MIN_YEAR && yearTo == currentYear) {
                    stringResource(R.string.search_all)
                } else {
                    "$yearFrom – $yearTo"
                },
                color = PrimaryPurpleLight,
                fontWeight = FontWeight.Black,
                fontSize = 14.sp
            )
        }
        Spacer(Modifier.height(4.dp))
        RangeSlider(
            value = yearFrom.toFloat()..yearTo.toFloat(),
            onValueChange = { range ->
                onYearRangeSelected(range.start.toInt(), range.endInclusive.toInt())
            },
            valueRange = SearchViewModel.MIN_YEAR.toFloat()..currentYear.toFloat(),
            steps = currentYear - SearchViewModel.MIN_YEAR - 1,
            colors = SliderDefaults.colors(
                thumbColor = Color.White,
                activeTrackColor = PrimaryPurple,
                inactiveTrackColor = Color.White.copy(alpha = 0.1f)
            )
        )

        Spacer(Modifier.height(20.dp))

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier.fillMaxWidth()
        ) {
            FilterSectionLabel(stringResource(R.string.search_min_rating))
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(3.dp)
            ) {
                Icon(Icons.Default.Star, null, tint = Color(0xFFFFC107), modifier = Modifier.size(15.dp))
                Text(
                    text = if (selectedRating == null || selectedRating == 0f) {
                        stringResource(R.string.search_rating_all)
                    } else {
                        stringResource(R.string.search_rating_format, selectedRating)
                    },
                    color = Color(0xFFFFC107),
                    fontWeight = FontWeight.Black,
                    fontSize = 14.sp
                )
            }
        }
        Spacer(Modifier.height(4.dp))
        Slider(
            value = selectedRating ?: 0f,
            onValueChange = { onRatingSelected(if (it == 0f) null else it) },
            valueRange = 0f..10f,
            steps = 9,
            colors = SliderDefaults.colors(
                thumbColor = Color.White,
                activeTrackColor = PrimaryPurple,
                inactiveTrackColor = Color.White.copy(alpha = 0.1f)
            )
        )

        Spacer(Modifier.height(16.dp))
    }
}

@Composable
private fun FilterSectionLabel(title: String) {
    Text(
        text = title.uppercase(),
        color = TextGray,
        fontSize = 11.sp,
        fontWeight = FontWeight.Bold,
        letterSpacing = 1.2.sp
    )
}

@Composable
fun SectionTitle(title: String) {
    FilterSectionLabel(title)
}

@Composable
fun SearchModeComingSoon(mode: SearchMode) {
    val label = when (mode) {
        SearchMode.ACTOR -> "actor"
        SearchMode.CREATOR -> "creador"
        else -> mode.label.lowercase()
    }
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            modifier = Modifier
                .size(88.dp)
                .background(
                    Brush.radialGradient(
                        listOf(PrimaryPurple.copy(alpha = 0.2f), Color.Transparent)
                    ),
                    CircleShape
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                Icons.Default.TravelExplore,
                contentDescription = null,
                modifier = Modifier.size(44.dp),
                tint = PrimaryPurple.copy(alpha = 0.6f)
            )
        }
        Spacer(Modifier.height(24.dp))
        Text(
            text = stringResource(R.string.search_by_label, label),
            color = Color.White,
            fontSize = 20.sp,
            fontWeight = FontWeight.Black,
            textAlign = TextAlign.Center
        )
        Spacer(Modifier.height(10.dp))
        Text(
            text = stringResource(R.string.search_coming_soon, label),
            color = TextGray,
            fontSize = 14.sp,
            textAlign = TextAlign.Center,
            lineHeight = 21.sp
        )
    }
}

@Composable
private fun SearchGridSkeleton() {
    val shimmer = shimmerBrush()
    LazyVerticalGrid(
        columns = GridCells.Adaptive(minSize = 105.dp),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 120.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        modifier = Modifier.fillMaxSize(),
        userScrollEnabled = false
    ) {
        itemsIndexed(List(6) { it }) { _, _ ->
            Column {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(2f / 3f)
                        .clip(RoundedCornerShape(16.dp))
                        .background(shimmer)
                )
                Spacer(Modifier.height(8.dp))
                Box(
                    modifier = Modifier
                        .fillMaxWidth(0.75f)
                        .height(14.dp)
                        .clip(RoundedCornerShape(6.dp))
                        .background(shimmer)
                )
                Spacer(Modifier.height(4.dp))
                Box(
                    modifier = Modifier
                        .fillMaxWidth(0.45f)
                        .height(11.dp)
                        .clip(RoundedCornerShape(6.dp))
                        .background(shimmer)
                )
            }
        }
    }
}

@Composable
fun NoResultsState(query: String) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            modifier = Modifier
                .size(110.dp)
                .background(
                    Brush.radialGradient(
                        listOf(PrimaryPurple.copy(alpha = 0.18f), Color.Transparent)
                    ),
                    CircleShape
                )
                .border(1.dp, PrimaryPurple.copy(alpha = 0.20f), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                Icons.Default.TravelExplore,
                contentDescription = null,
                modifier = Modifier.size(52.dp),
                tint = PrimaryPurpleLight.copy(alpha = 0.55f)
            )
        }
        Spacer(Modifier.height(28.dp))
        Text(
            text = if (query.isNotBlank()) {
                stringResource(R.string.search_no_results, query)
            } else {
                stringResource(R.string.search_no_matches)
            },
            color = Color.White,
            fontSize = 20.sp,
            fontWeight = FontWeight.Black,
            textAlign = TextAlign.Center,
            lineHeight = 28.sp
        )
        Spacer(Modifier.height(10.dp))
        Text(
            text = stringResource(R.string.search_try_other_terms),
            color = TextGray,
            fontSize = 14.sp,
            textAlign = TextAlign.Center,
            lineHeight = 21.sp
        )
        if (query.isNotBlank()) {
            Spacer(Modifier.height(6.dp))
            Text(
                text = "Prueba con el título exacto o en inglés",
                color = TextGray.copy(alpha = 0.6f),
                fontSize = 12.sp,
                textAlign = TextAlign.Center,
                lineHeight = 18.sp
            )
        }
    }
}

@Composable
fun TrendingPersonCard(person: com.andrea.showmateapp.data.model.PersonSearchResult, onClick: () -> Unit) {
    val isActor = person.knownForDepartment?.lowercase() == "acting"
    val accentColor = if (isActor) PrimaryPurple else AccentBlue
    val icon = if (isActor) Icons.Default.TheaterComedy else Icons.Default.Movie

    Column(
        modifier = Modifier
            .clickable { onClick() }
            .padding(4.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .size(90.dp)
                .clip(CircleShape)
                .background(SurfaceDark)
                .border(2.dp, accentColor.copy(alpha = 0.5f), CircleShape)
        ) {
            if (person.profilePath != null) {
                com.andrea.showmateapp.ui.components.TmdbImage(
                    path = person.profilePath,
                    contentDescription = person.name,
                    size = com.andrea.showmateapp.util.TmdbUtils.ImageSize.W185,
                    modifier = Modifier.fillMaxSize()
                )
            } else {
                Icon(
                    Icons.Default.Person,
                    contentDescription = null,
                    tint = TextGray,
                    modifier = Modifier
                        .size(36.dp)
                        .align(Alignment.Center)
                )
            }

            Box(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .size(28.dp)
                    .clip(CircleShape)
                    .background(accentColor)
                    .border(2.dp, Color(0xFF1A1A2A), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    icon,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(14.dp)
                )
            }
        }
        Spacer(Modifier.height(8.dp))
        Text(
            text = person.name,
            color = Color.White,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center,
            maxLines = 1,
            overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
        )
        Text(
            text = if (isActor) "Actor / Actriz" else "Director / Staff",
            color = accentColor.copy(alpha = 0.8f),
            fontSize = 10.sp,
            fontWeight = FontWeight.SemiBold,
            textAlign = TextAlign.Center
        )
    }
}
