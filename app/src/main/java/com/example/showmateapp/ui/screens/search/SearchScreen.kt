package com.example.showmateapp.ui.screens.search

import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
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
    viewModel: SearchViewModel = hiltViewModel()
) {
    var query by remember { mutableStateOf("") }
    val searchResults by viewModel.searchResults.collectAsState()
    val trendingShows by viewModel.trendingShows.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val isFilterActive by viewModel.isFilterActive.collectAsState()

    var showFilters by remember { mutableStateOf(false) }
    val sheetState = rememberModalBottomSheetState()

    val selectedGenre by viewModel.selectedGenre.collectAsState()
    val selectedYear by viewModel.selectedYear.collectAsState()
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
                val genres = listOf(
                    "10759" to "Acción", "16" to "Animación", "35" to "Comedia", 
                    "80" to "Crimen", "99" to "Docu", "18" to "Drama", 
                    "10751" to "Familia", "10762" to "Kids", "9648" to "Misterio",
                    "10765" to "Sci-Fi"
                )
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
            }
        }
    ) { padding ->
        val listToShow = if (query.isBlank() && !isFilterActive) trendingShows else searchResults
        val titleToShow = if (query.isBlank() && !isFilterActive) "Tendencias" else "Resultados de búsqueda"
        val listTag = if (query.isBlank() && !isFilterActive) "search_trending" else "search_results"

        Column(modifier = Modifier.padding(padding)) {
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
                Text(
                    text = titleToShow,
                    color = TextGray,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 12.dp),
                    letterSpacing = 1.sp
                )
                LazyVerticalGrid(
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
                selectedYear = selectedYear,
                selectedRating = selectedRating,
                onGenreSelected = { viewModel.updateGenre(it) },
                onYearSelected = { viewModel.updateYear(it) },
                onRatingSelected = { viewModel.updateRating(it) },
                onClear = { viewModel.clearFilters() }
            )
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun FilterSheetContent(
    selectedGenre: String?,
    selectedYear: Int?,
    selectedRating: Float?,
    onGenreSelected: (String?) -> Unit,
    onYearSelected: (Int?) -> Unit,
    onRatingSelected: (Float?) -> Unit,
    onClear: () -> Unit
) {
    val genres = listOf(
        "10759" to "Acción", "16" to "Animación", "35" to "Comedia", 
        "80" to "Crimen", "99" to "Docu", "18" to "Drama", 
        "10751" to "Familia", "10762" to "Kids", "9648" to "Misterio",
        "10765" to "Sci-Fi"
    )

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
            if (selectedGenre != null || selectedYear != null || selectedRating != null) {
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

        SectionTitle("Año de estreno")
        val years = (2020..2025).reversed().toList()
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            modifier = Modifier.padding(vertical = 12.dp)
        ) {
            years.forEach { year ->
                FilterChip(
                    selected = selectedYear == year,
                    onClick = { onYearSelected(if (selectedYear == year) null else year) },
                    label = { Text(year.toString()) },
                    shape = RoundedCornerShape(12.dp),
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = PrimaryPurple,
                        selectedLabelColor = Color.White,
                        containerColor = Color.White.copy(alpha = 0.05f)
                    ),
                    border = null
                )
            }
        }

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
