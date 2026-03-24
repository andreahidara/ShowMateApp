package com.example.showmateapp.ui.screens.favorites

import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Sort
import androidx.compose.material.icons.filled.Explore
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.example.showmateapp.ui.components.premium.*
import com.example.showmateapp.ui.navigation.Screen
import com.example.showmateapp.ui.theme.HeartRed
import com.example.showmateapp.ui.theme.PrimaryPurple
import com.example.showmateapp.ui.theme.PrimaryPurpleDark
import com.example.showmateapp.ui.theme.TextGray

@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
fun FavoritesScreen(
    globalNavController: NavController,
    sharedTransitionScope: SharedTransitionScope,
    animatedVisibilityScope: AnimatedVisibilityScope,
    onExploreClick: () -> Unit = {},
    viewModel: FavoritesViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsState()
    var showSortDropdown by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .statusBarsPadding()
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .padding(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 8.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Favorite,
                contentDescription = null,
                tint = HeartRed,
                modifier = Modifier.size(22.dp)
            )
            Spacer(modifier = Modifier.width(10.dp))
            Text(
                text = "Mis Favoritos",
                color = Color.White,
                fontSize = 28.sp,
                fontWeight = FontWeight.Black,
                modifier = Modifier.weight(1f)
            )
            Box {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(20.dp))
                        .background(Color.White.copy(alpha = 0.08f))
                        .clickable { showSortDropdown = true }
                        .padding(horizontal = 12.dp, vertical = 8.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.Sort,
                            contentDescription = "Ordenar",
                            tint = PrimaryPurple,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = state.sortOption.label,
                            color = PrimaryPurple,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
                DropdownMenu(
                    expanded = showSortDropdown,
                    onDismissRequest = { showSortDropdown = false }
                ) {
                    SortOption.entries.forEach { option ->
                        DropdownMenuItem(
                            text = {
                                Text(
                                    text = option.label,
                                    fontWeight = if (option == state.sortOption) FontWeight.Bold else FontWeight.Normal,
                                    color = if (option == state.sortOption) PrimaryPurple else Color.Unspecified
                                )
                            },
                            onClick = {
                                viewModel.setSortOption(option)
                                showSortDropdown = false
                            }
                        )
                    }
                }
            }
        }

        val tabIndex = FavoriteTab.entries.indexOf(state.selectedTab)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 6.dp)
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            FavoriteTab.entries.forEachIndexed { index, tab ->
                val isSelected = index == tabIndex
                val count = when (tab) {
                    FavoriteTab.LIKED -> state.favorites.size
                    FavoriteTab.ESSENTIAL -> state.essentials.size
                    FavoriteTab.WATCHED -> state.watched.size
                    FavoriteTab.WATCHLIST -> state.watchlist.size
                }
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(20.dp))
                        .background(
                            if (isSelected)
                                Brush.horizontalGradient(listOf(PrimaryPurple, PrimaryPurpleDark))
                            else
                                Brush.horizontalGradient(
                                    listOf(Color.White.copy(alpha = 0.08f), Color.White.copy(alpha = 0.08f))
                                )
                        )
                        .clickable { viewModel.selectTab(tab) }
                        .padding(horizontal = 16.dp, vertical = 9.dp)
                ) {
                    Text(
                        text = "${tab.label} ($count)",
                        color = if (isSelected) Color.White else TextGray,
                        fontSize = 13.sp,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                    )
                }
            }
        }

        val currentList = when (state.selectedTab) {
            FavoriteTab.LIKED -> state.favorites
            FavoriteTab.ESSENTIAL -> state.essentials
            FavoriteTab.WATCHED -> state.watched
            FavoriteTab.WATCHLIST -> state.watchlist
        }

        if (currentList.isEmpty()) {
            val (emptyTitle, emptySubtitle) = when (state.selectedTab) {
                FavoriteTab.LIKED -> Pair(
                    "Sin favoritos aún",
                    "Aún no tienes favoritos. Desliza en Swipe para guardar series."
                )
                FavoriteTab.ESSENTIAL -> Pair(
                    "Sin imprescindibles",
                    "Ninguna serie marcada como imprescindible todavía."
                )
                FavoriteTab.WATCHED -> Pair(
                    "Sin series vistas",
                    "No has marcado ninguna serie como vista."
                )
                FavoriteTab.WATCHLIST -> Pair(
                    "Lista vacía",
                    "Añade series desde el detalle con el botón «Pendiente»."
                )
            }
            EmptyTabState(
                title = emptyTitle,
                subtitle = emptySubtitle,
                showExploreButton = state.selectedTab == FavoriteTab.LIKED,
                onExploreClick = onExploreClick
            )
        } else {
            LazyVerticalGrid(
                columns = GridCells.Fixed(3),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                contentPadding = PaddingValues(16.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                items(currentList, key = { it.id }) { media ->
                    ShowCard(
                        media = media,
                        sharedTransitionScope = sharedTransitionScope,
                        animatedVisibilityScope = animatedVisibilityScope,
                        onClick = { selectedMedia, tag ->
                            globalNavController.navigate(Screen.Detail(selectedMedia.id, tag))
                        },
                        tag = when (state.selectedTab) {
                            FavoriteTab.LIKED -> "favorite"
                            FavoriteTab.ESSENTIAL -> "essential"
                            FavoriteTab.WATCHED -> "watched"
                            FavoriteTab.WATCHLIST -> "watchlist"
                        }
                    )
                }
            }
        }
    }
}

@Composable
fun EmptyTabState(
    title: String,
    subtitle: String,
    showExploreButton: Boolean,
    onExploreClick: () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .size(120.dp)
                .clip(CircleShape)
                .background(
                    Brush.radialGradient(
                        listOf(
                            HeartRed.copy(alpha = 0.15f),
                            Color.Transparent
                        )
                    )
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.Favorite,
                contentDescription = null,
                tint = HeartRed.copy(alpha = 0.5f),
                modifier = Modifier.size(52.dp)
            )
        }
        Spacer(modifier = Modifier.height(20.dp))
        Text(
            text = title,
            color = Color.White,
            fontSize = 22.sp,
            fontWeight = FontWeight.ExtraBold
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = subtitle,
            color = TextGray,
            fontSize = 14.sp,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = 32.dp)
        )
        if (showExploreButton) {
            Spacer(modifier = Modifier.height(28.dp))
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(16.dp))
                    .background(
                        Brush.horizontalGradient(listOf(HeartRed, HeartRed.copy(alpha = 0.7f)))
                    )
                    .clickable { onExploreClick() }
                    .padding(horizontal = 24.dp, vertical = 14.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Explore,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Explorar series",
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp
                    )
                }
            }
        }
    }
}

@Composable
fun EmptyFavoritesState(onExploreClick: () -> Unit = {}) {
    EmptyTabState(
        title = "Sin favoritos aún",
        subtitle = "Dale ♥ a cualquier serie para guardarla aquí",
        showExploreButton = true,
        onExploreClick = onExploreClick
    )
}
