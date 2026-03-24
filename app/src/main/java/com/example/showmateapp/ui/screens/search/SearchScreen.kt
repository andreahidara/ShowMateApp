package com.example.showmateapp.ui.screens.search

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.TravelExplore
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.example.showmateapp.ui.components.premium.ErrorView
import com.example.showmateapp.ui.components.premium.PulseLoader
import com.example.showmateapp.ui.components.premium.ShowCard
import com.example.showmateapp.ui.navigation.Screen
import com.example.showmateapp.ui.theme.AccentBlue
import com.example.showmateapp.ui.theme.PrimaryPurple
import com.example.showmateapp.ui.theme.PrimaryPurpleLight
import com.example.showmateapp.ui.theme.SurfaceDark
import com.example.showmateapp.ui.theme.TextGray

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
    val searchResults by viewModel.searchResults.collectAsState()
    val trendingShows by viewModel.trendingShows.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val isFilterActive by viewModel.isFilterActive.collectAsState()
    val recentSearches by viewModel.recentSearches.collectAsState()
    val suggestions by viewModel.suggestions.collectAsState()

    var showFilters by remember { mutableStateOf(false) }
    val sheetState = rememberModalBottomSheetState()

    val selectedGenre by viewModel.selectedGenre.collectAsState()
    val selectedPlatform by viewModel.selectedPlatform.collectAsState()
    val yearFrom by viewModel.yearFrom.collectAsState()
    val yearTo by viewModel.yearTo.collectAsState()
    val selectedRating by viewModel.selectedRating.collectAsState()
    val errorMessage by viewModel.errorMessage.collectAsState()
    val searchMode by viewModel.searchMode.collectAsState()

    val showRecent = query.isBlank() && !isFilterActive && recentSearches.isNotEmpty()
    val listToShow = if (query.isBlank() && !isFilterActive) trendingShows else searchResults
    val listTag = if (query.isBlank() && !isFilterActive) "search_trending" else "search_results"

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            Column(
                modifier = Modifier
                    .statusBarsPadding()
                    .background(MaterialTheme.colorScheme.background)
            ) {
                Spacer(Modifier.height(8.dp))

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(50.dp)
                            .clip(RoundedCornerShape(16.dp))
                            .background(Color.White.copy(alpha = 0.07f))
                            .border(
                                width = 1.dp,
                                color = if (query.isNotEmpty()) PrimaryPurple.copy(alpha = 0.4f)
                                        else Color.White.copy(alpha = 0.08f),
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
                                        "¿Qué quieres ver hoy?",
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
                            tint = if (query.isNotEmpty()) PrimaryPurple else TextGray,
                            modifier = Modifier
                                .align(Alignment.CenterStart)
                                .size(20.dp)
                        )
                        if (query.isNotEmpty()) {
                            Box(
                                modifier = Modifier
                                    .align(Alignment.CenterEnd)
                                    .size(22.dp)
                                    .clip(CircleShape)
                                    .background(Color.White.copy(alpha = 0.1f))
                                    .clickable {
                                        query = ""
                                        viewModel.searchMedia("")
                                        viewModel.updateSuggestions("")
                                    },
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    Icons.Default.Clear,
                                    contentDescription = "Limpiar",
                                    tint = Color.White.copy(alpha = 0.7f),
                                    modifier = Modifier.size(14.dp)
                                )
                            }
                        }
                    }

                    Box(
                        modifier = Modifier
                            .size(50.dp)
                            .clip(RoundedCornerShape(16.dp))
                            .background(
                                if (isFilterActive) PrimaryPurple
                                else Color.White.copy(alpha = 0.07f)
                            )
                            .border(
                                width = 1.dp,
                                color = if (isFilterActive) PrimaryPurple
                                        else Color.White.copy(alpha = 0.08f),
                                shape = RoundedCornerShape(16.dp)
                            )
                            .clickable { showFilters = true },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Default.FilterList,
                            contentDescription = "Filtros",
                            tint = Color.White,
                            modifier = Modifier.size(22.dp)
                        )
                        if (isFilterActive) {
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .background(Color(0xFF4CAF50), CircleShape)
                                    .align(Alignment.TopEnd)
                                    .offset(x = (-4).dp, y = 4.dp)
                            )
                        }
                    }
                }

                Spacer(Modifier.height(10.dp))

                LazyRow(
                    contentPadding = PaddingValues(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.padding(bottom = 4.dp)
                ) {
                    items(SearchMode.entries) { mode ->
                        val isSelected = searchMode == mode
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(20.dp))
                                .background(
                                    if (isSelected) PrimaryPurple
                                    else Color.White.copy(alpha = 0.06f)
                                )
                                .border(
                                    1.dp,
                                    if (isSelected) Color.Transparent
                                    else Color.White.copy(alpha = 0.1f),
                                    RoundedCornerShape(20.dp)
                                )
                                .clickable { viewModel.setSearchMode(mode) }
                                .padding(horizontal = 14.dp, vertical = 7.dp)
                        ) {
                            Text(
                                text = mode.label,
                                color = if (isSelected) Color.White else Color.White.copy(alpha = 0.6f),
                                fontSize = 13.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                            )
                        }
                    }
                }

                AnimatedVisibility(visible = suggestions.isNotEmpty()) {
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp),
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
                    Spacer(Modifier.height(4.dp))
                }
            }
        }
    ) { padding ->
        Column(modifier = Modifier.padding(padding)) {
            if (showRecent) {
                Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "RECIENTES",
                            color = TextGray,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.5.sp
                        )
                        TextButton(
                            onClick = { viewModel.clearRecentSearches() },
                            contentPadding = PaddingValues(0.dp)
                        ) {
                            Text("Borrar todo", color = PrimaryPurple, fontSize = 12.sp, fontWeight = FontWeight.Medium)
                        }
                    }
                    Spacer(Modifier.height(6.dp))
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(recentSearches) { recent ->
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
                    "loading" -> Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) { PulseLoader() }

                    "error" -> ErrorView(
                        message = errorMessage!!,
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
                                    text = if (query.isBlank() && !isFilterActive)
                                        "TENDENCIAS"
                                    else
                                        "${listToShow.size} RESULTADOS",
                                    color = TextGray,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    letterSpacing = 1.5.sp
                                )
                            }
                            LazyVerticalGrid(
                                state = gridState,
                                columns = GridCells.Fixed(2),
                                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 120.dp),
                                horizontalArrangement = Arrangement.spacedBy(14.dp),
                                verticalArrangement = Arrangement.spacedBy(18.dp),
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
                                        width = 0.dp
                                    )
                                }
                            }
                        }
                    }

                    "empty" -> {
                        if (searchMode != SearchMode.TITLE && query.isNotBlank()) {
                            SearchModeComingSoon(searchMode)
                        } else {
                            NoResultsState(query)
                        }
                    }

                    else -> {}
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
                "Filtros",
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
                        "Restablecer",
                        color = PrimaryPurpleLight,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }

        Spacer(Modifier.height(24.dp))

        FilterSectionLabel("Géneros")
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

        FilterSectionLabel("Plataformas")
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
            FilterSectionLabel("Año de estreno")
            Text(
                text = if (yearFrom == SearchViewModel.MIN_YEAR && yearTo == currentYear) "Todos"
                       else "$yearFrom – $yearTo",
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
            FilterSectionLabel("Calificación mínima")
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(3.dp)
            ) {
                Icon(Icons.Default.Star, null, tint = Color(0xFFFFC107), modifier = Modifier.size(15.dp))
                Text(
                    text = if (selectedRating == null || selectedRating == 0f) "Todas"
                           else "${"%.0f".format(selectedRating)}/10",
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
            text = "Búsqueda por $label",
            color = Color.White,
            fontSize = 20.sp,
            fontWeight = FontWeight.Black,
            textAlign = TextAlign.Center
        )
        Spacer(Modifier.height(10.dp))
        Text(
            text = "Próximamente podrás buscar directamente por $label.",
            color = TextGray,
            fontSize = 14.sp,
            textAlign = TextAlign.Center,
            lineHeight = 21.sp
        )
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
                .size(88.dp)
                .background(
                    Brush.radialGradient(
                        listOf(Color.White.copy(alpha = 0.04f), Color.Transparent)
                    ),
                    CircleShape
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                Icons.Default.Search,
                contentDescription = null,
                modifier = Modifier.size(44.dp),
                tint = Color.White.copy(alpha = 0.12f)
            )
        }
        Spacer(Modifier.height(24.dp))
        Text(
            text = if (query.isNotBlank()) "Sin resultados para\n\"$query\""
                   else "No hay coincidencias",
            color = Color.White,
            fontSize = 20.sp,
            fontWeight = FontWeight.Black,
            textAlign = TextAlign.Center,
            lineHeight = 28.sp
        )
        Spacer(Modifier.height(10.dp))
        Text(
            text = "Prueba con otros términos o ajusta los filtros.",
            color = TextGray,
            fontSize = 14.sp,
            textAlign = TextAlign.Center,
            lineHeight = 21.sp
        )
    }
}
