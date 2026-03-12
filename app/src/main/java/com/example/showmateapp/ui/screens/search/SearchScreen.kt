package com.example.showmateapp.ui.screens.search

import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
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

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            Column(modifier = Modifier.statusBarsPadding()) {
                // Search Bar
                TextField(
                    value = query,
                    onValueChange = {
                        query = it
                        viewModel.searchMedia(it)
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                        .clip(RoundedCornerShape(12.dp)),
                    placeholder = { Text("Buscar series...", color = TextGray) },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = TextGray) },
                    trailingIcon = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            if (query.isNotEmpty()) {
                                IconButton(onClick = { 
                                    query = ""
                                    viewModel.searchMedia("") 
                                }) {
                                    Icon(Icons.Default.Clear, contentDescription = "Clear search", tint = Color.White)
                                }
                            }
                            IconButton(onClick = { showFilters = true }) {
                                Icon(
                                    imageVector = Icons.Default.FilterList,
                                    contentDescription = "Filters", 
                                    tint = if (isFilterActive) PrimaryPurple else Color.White
                                )
                            }
                        }
                    },
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = SurfaceDark,
                        unfocusedContainerColor = SurfaceDark,
                        disabledContainerColor = SurfaceDark,
                        focusedIndicatorColor = Color.Transparent,
                        unfocusedIndicatorColor = Color.Transparent,
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White
                    ),
                    singleLine = true
                )
            }
        }
    ) { padding ->
        val listToShow = if (query.isBlank() && !isFilterActive) trendingShows else searchResults
        val titleToShow = if (query.isBlank() && !isFilterActive) "Tendencias Ahora" else "Resultados"

        Column(modifier = Modifier.padding(padding)) {
            if (isLoading) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    PulseLoader()
                }
            } else if (listToShow.isNotEmpty()) {
                Text(
                    text = titleToShow,
                    color = Color.White,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                )
                LazyVerticalGrid(
                    columns = GridCells.Fixed(2),
                    contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 100.dp, top = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    items(listToShow, key = { it.id }) { media ->
                        ShowCard(
                            media = media,
                            sharedTransitionScope = sharedTransitionScope,
                            animatedVisibilityScope = animatedVisibilityScope,
                            onClick = { selectedMedia -> 
                                globalNavController.navigate(Screen.Detail(selectedMedia.id))
                            }
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
            containerColor = SurfaceDark,
            contentColor = Color.White
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
            .padding(24.dp)
            .navigationBarsPadding()
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Filtros", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
            TextButton(onClick = onClear) {
                Text("Limpiar", color = PrimaryPurple, fontWeight = FontWeight.Bold)
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        Text("Género", fontWeight = FontWeight.Bold, color = TextGray)
        Spacer(modifier = Modifier.height(12.dp))
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            genres.forEach { (id, name) ->
                FilterChip(
                    selected = selectedGenre == id,
                    onClick = { onGenreSelected(if (selectedGenre == id) null else id) },
                    label = { Text(name) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = PrimaryPurple,
                        selectedLabelColor = Color.White,
                        labelColor = TextGray,
                        containerColor = Color.White.copy(alpha = 0.05f)
                    ),
                    border = null
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        Text("Año de Lanzamiento", fontWeight = FontWeight.Bold, color = TextGray)
        Spacer(modifier = Modifier.height(12.dp))
        val years = (2020..2025).reversed().toList()
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            years.forEach { year ->
                FilterChip(
                    selected = selectedYear == year,
                    onClick = { onYearSelected(if (selectedYear == year) null else year) },
                    label = { Text(year.toString()) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = PrimaryPurple,
                        selectedLabelColor = Color.White
                    )
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        Text("Puntuación Mínima: ${selectedRating?.toInt() ?: 0}★", fontWeight = FontWeight.Bold, color = TextGray)
        Slider(
            value = selectedRating ?: 0f,
            onValueChange = { onRatingSelected(if (it == 0f) null else it) },
            valueRange = 0f..10f,
            steps = 9,
            colors = SliderDefaults.colors(
                thumbColor = PrimaryPurple,
                activeTrackColor = PrimaryPurple,
                inactiveTrackColor = Color.White.copy(alpha = 0.1f)
            )
        )
        
        Spacer(modifier = Modifier.height(32.dp))
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
        Icon(
            Icons.Default.Search,
            contentDescription = null,
            modifier = Modifier.size(80.dp),
            tint = SurfaceDark
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = if (query.isNotBlank()) "Sin resultados para \"$query\"" else "No hay coincidencias",
            color = Color.White,
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "Intenta ajustar tus filtros o buscar con otras palabras clave.",
            color = TextGray,
            fontSize = 14.sp,
            fontWeight = FontWeight.Medium,
            textAlign = TextAlign.Center
        )
    }
}
