package com.example.showmateapp.ui.screens.search

import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.foundation.background
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
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
import com.example.showmateapp.ui.theme.PrimaryPurple
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
    val yearFrom by viewModel.yearFrom.collectAsState()
    val yearTo by viewModel.yearTo.collectAsState()
    val selectedRating by viewModel.selectedRating.collectAsState()

    val errorMessage by viewModel.errorMessage.collectAsState()

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            Column(
                modifier = Modifier
                    .statusBarsPadding()
                    .padding(top = 0.dp)
            ) {
                // Spacer to give padding instead of the header
                Spacer(modifier = Modifier.height(16.dp))

                // Search Bar rediseñada tipo "Premium Pill"
                OutlinedTextField(
                    value = query,
                    onValueChange = {
                        query = it
                        viewModel.updateSuggestions(it)
                        viewModel.searchMedia(it)
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    placeholder = { Text("¿Qué quieres ver hoy?", color = TextGray) },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = PrimaryPurple) },
                    trailingIcon = {
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(end = 4.dp)) {
                            if (query.isNotEmpty()) {
                                IconButton(onClick = { 
                                    query = ""
                                    viewModel.searchMedia("") 
                                }) {
                                    Icon(Icons.Default.Clear, contentDescription = "Clear", tint = Color.White.copy(alpha = 0.6f))
                                }
                            }
                            // Badge indicador de filtro activo
                            Box {
                                IconButton(onClick = { showFilters = true }) {
                                    Icon(
                                        imageVector = Icons.Default.FilterList,
                                        contentDescription = "Filters", 
                                        tint = if (isFilterActive) PrimaryPurple else Color.White
                                    )
                                }
                                if (isFilterActive) {
                                    Box(
                                        modifier = Modifier
                                            .size(8.dp)
                                            .clip(CircleShape)
                                            .background(PrimaryPurple)
                                            .align(Alignment.TopEnd)
                                            .offset(x = (-8).dp, y = 8.dp)
                                    )
                                }
                            }
                        }
                    },
                    shape = RoundedCornerShape(16.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = Color.White.copy(alpha = 0.05f),
                        unfocusedContainerColor = Color.White.copy(alpha = 0.05f),
                        focusedBorderColor = PrimaryPurple.copy(alpha = 0.5f),
                        unfocusedBorderColor = Color.White.copy(alpha = 0.1f),
                        cursorColor = PrimaryPurple,
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White
                    ),
                    singleLine = true
                )

                // Quick Genre Filters
                val genres = SearchViewModel.AVAILABLE_GENRES
                LazyRow(
                    contentPadding = PaddingValues(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.padding(bottom = 8.dp)
                ) {
                    items(genres) { (id, name) ->
                        FilterChip(
                            selected = selectedGenre == id,
                            onClick = { viewModel.updateGenre(if (selectedGenre == id) null else id) },
                            label = { Text(name, fontSize = 13.sp) },
                            shape = RoundedCornerShape(12.dp),
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = PrimaryPurple,
                                selectedLabelColor = Color.White,
                                labelColor = Color.White.copy(alpha = 0.7f),
                                containerColor = Color.White.copy(alpha = 0.05f)
                            ),
                            border = null
                        )
                    }
                }

                // Inline suggestions
                if (suggestions.isNotEmpty()) {
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp),
                        color = Color(0xFF1A1A2E),
                        shape = RoundedCornerShape(12.dp),
                        tonalElevation = 4.dp
                    ) {
                        Column {
                            suggestions.forEachIndexed { idx, media ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable {
                                            query = media.name
                                            viewModel.updateSuggestions("")
                                            viewModel.searchMedia(media.name)
                                        }
                                        .padding(horizontal = 16.dp, vertical = 10.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Search,
                                        contentDescription = null,
                                        tint = TextGray,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Text(media.name, color = Color.White, fontSize = 14.sp)
                                }
                                if (idx < suggestions.lastIndex) {
                                    HorizontalDivider(
                                        color = Color.White.copy(alpha = 0.06f),
                                        modifier = Modifier.padding(horizontal = 16.dp)
                                    )
                                }
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                }
            }
        }
    ) { padding ->
        val showRecent = query.isBlank() && !isFilterActive && recentSearches.isNotEmpty()
        val listToShow = if (query.isBlank() && !isFilterActive) trendingShows else searchResults
        val titleToShow = if (query.isBlank() && !isFilterActive) "Tendencias" else "Resultados de búsqueda"
        val listTag = if (query.isBlank() && !isFilterActive) "search_trending" else "search_results"

        Column(modifier = Modifier.padding(padding)) {
            if (showRecent) {
                Column(modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "BÚSQUEDAS RECIENTES",
                            color = TextGray,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.sp
                        )
                        TextButton(
                            onClick = { viewModel.clearRecentSearches() },
                            contentPadding = PaddingValues(0.dp)
                        ) {
                            Text("Borrar", color = PrimaryPurple, fontSize = 12.sp)
                        }
                    }
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.padding(top = 8.dp)
                    ) {
                        items(recentSearches) { recent ->
                            Surface(
                                shape = RoundedCornerShape(20.dp),
                                color = SurfaceDark,
                                modifier = Modifier.clickable {
                                    query = recent
                                    viewModel.searchMedia(recent)
                                }
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                                ) {
                                    Text(recent, color = Color.White, fontSize = 13.sp)
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Icon(
                                        imageVector = Icons.Default.Clear,
                                        contentDescription = "Eliminar",
                                        tint = TextGray,
                                        modifier = Modifier
                                            .size(14.dp)
                                            .clickable { viewModel.removeRecentSearch(recent) }
                                    )
                                }
                            }
                        }
                    }
                }
            }
            if (isLoading) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    PulseLoader()
                }
            } else if (errorMessage != null) {
                ErrorView(
                    message = errorMessage!!,
                    onRetry = { viewModel.searchMedia(query) }
                )
            } else if (listToShow.isNotEmpty()) {
                val gridState = rememberLazyGridState()
                LaunchedEffect(scrollToTopTrigger) {
                    if (scrollToTopTrigger > 0) gridState.animateScrollToItem(0)
                }
                Text(
                    text = titleToShow,
                    color = TextGray,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 12.dp),
                    letterSpacing = 1.sp
                )
                LazyVerticalGrid(
                    state = gridState,
                    columns = GridCells.Fixed(2),
                    contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 120.dp),
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    verticalArrangement = Arrangement.spacedBy(20.dp),
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
                            width = 0.dp // Para que use todo el ancho del grid
                        )
                    }
                }
            } else if (query.isNotBlank() || isFilterActive) {
                 NoResultsState(query)
            }
        }
    }

    if (showFilters) {
        ModalBottomSheet(
            onDismissRequest = { showFilters = false },
            sheetState = sheetState,
            containerColor = Color(0xFF1A1A1A), // Un gris muy oscuro casi negro
            contentColor = Color.White,
            dragHandle = { BottomSheetDefaults.DragHandle(color = Color.White.copy(alpha = 0.2f)) }
        ) {
            FilterSheetContent(
                selectedGenre = selectedGenre,
                yearFrom = yearFrom,
                yearTo = yearTo,
                selectedRating = selectedRating,
                onGenreSelected = { viewModel.updateGenre(it) },
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
    yearFrom: Int = SearchViewModel.MIN_YEAR,
    yearTo: Int = SearchViewModel.CURRENT_YEAR,
    selectedRating: Float?,
    onGenreSelected: (String?) -> Unit,
    onYearRangeSelected: (Int, Int) -> Unit,
    onRatingSelected: (Float?) -> Unit,
    onClear: () -> Unit
) {
    val currentYear = SearchViewModel.CURRENT_YEAR
    val genres = SearchViewModel.AVAILABLE_GENRES

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp)
            .padding(bottom = 40.dp)
            .navigationBarsPadding()
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Refinar búsqueda", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Black)
            if (selectedGenre != null || yearFrom > 1990 || yearTo < currentYear || selectedRating != null) {
                TextButton(onClick = onClear) {
                    Text("Restablecer", color = PrimaryPurple, fontWeight = FontWeight.Bold)
                }
            }
        }

        Spacer(modifier = Modifier.height(32.dp))

        SectionTitle("Géneros")
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
            modifier = Modifier.padding(vertical = 12.dp)
        ) {
            genres.forEach { (id, name) ->
                FilterChip(
                    selected = selectedGenre == id,
                    onClick = { onGenreSelected(if (selectedGenre == id) null else id) },
                    label = { Text(name, modifier = Modifier.padding(vertical = 4.dp)) },
                    shape = RoundedCornerShape(12.dp),
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = PrimaryPurple,
                        selectedLabelColor = Color.White,
                        labelColor = Color.White.copy(alpha = 0.7f),
                        containerColor = Color.White.copy(alpha = 0.05f)
                    ),
                    border = null
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        Row(verticalAlignment = Alignment.CenterVertically) {
            SectionTitle("Año de estreno")
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = if (yearFrom == 1990 && yearTo == currentYear) "Todos"
                       else "$yearFrom – $yearTo",
                color = PrimaryPurple,
                fontWeight = FontWeight.Black,
                fontSize = 15.sp
            )
        }
        RangeSlider(
            value = yearFrom.toFloat()..yearTo.toFloat(),
            onValueChange = { range ->
                onYearRangeSelected(range.start.toInt(), range.endInclusive.toInt())
            },
            valueRange = 1990f..currentYear.toFloat(),
            steps = currentYear - 1990 - 1,
            colors = SliderDefaults.colors(
                thumbColor = Color.White,
                activeTrackColor = PrimaryPurple,
                inactiveTrackColor = Color.White.copy(alpha = 0.1f)
            ),
            modifier = Modifier.padding(vertical = 8.dp)
        )

        Spacer(modifier = Modifier.height(24.dp))

        Row(verticalAlignment = Alignment.CenterVertically) {
            SectionTitle("Calificación mínima")
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "${selectedRating?.toInt() ?: 0}★", 
                color = PrimaryPurple, 
                fontWeight = FontWeight.Black,
                fontSize = 18.sp
            )
        }
        
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
        
        Spacer(modifier = Modifier.height(24.dp))
    }
}

@Composable
fun SectionTitle(title: String) {
    Text(
        text = title.uppercase(),
        style = MaterialTheme.typography.labelMedium,
        color = TextGray,
        fontWeight = FontWeight.Bold,
        letterSpacing = 1.sp
    )
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
        Icon(
            Icons.Default.Search,
            contentDescription = null,
            modifier = Modifier.size(80.dp),
            tint = Color.White.copy(alpha = 0.05f)
        )
        Spacer(modifier = Modifier.height(24.dp))
        Text(
            text = if (query.isNotBlank()) "Sin resultados para \"$query\"" else "No hay coincidencias",
            color = Color.White,
            fontSize = 20.sp,
            fontWeight = FontWeight.Black,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(12.dp))
        Text(
            text = "Intenta ajustar tus filtros o buscar con otras palabras clave.",
            color = TextGray,
            fontSize = 15.sp,
            textAlign = TextAlign.Center,
            lineHeight = 22.sp
        )
    }
}
