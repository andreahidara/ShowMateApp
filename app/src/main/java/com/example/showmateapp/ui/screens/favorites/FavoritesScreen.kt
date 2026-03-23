package com.example.showmateapp.ui.screens.favorites

import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Explore
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Sort
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
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
                OutlinedButton(
                    onClick = { showSortDropdown = true },
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = PrimaryPurple),
                    border = androidx.compose.foundation.BorderStroke(1.dp, PrimaryPurple.copy(alpha = 0.5f))
                ) {
                    Icon(
                        imageVector = Icons.Default.Sort,
                        contentDescription = "Ordenar",
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = state.sortOption.label,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium
                    )
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
        TabRow(
            selectedTabIndex = tabIndex,
            containerColor = MaterialTheme.colorScheme.background,
            contentColor = Color.White,
            indicator = { tabPositions ->
                if (tabIndex < tabPositions.size) {
                    val pos = tabPositions[tabIndex]
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .wrapContentSize(Alignment.BottomStart)
                            .offset(x = pos.left)
                            .width(pos.width)
                            .height(3.dp)
                            .background(PrimaryPurple)
                    )
                }
            }
        ) {
            FavoriteTab.entries.forEachIndexed { index, tab ->
                val count = when (tab) {
                    FavoriteTab.LIKED -> state.favorites.size
                    FavoriteTab.ESSENTIAL -> state.essentials.size
                    FavoriteTab.WATCHED -> state.watched.size
                }
                Tab(
                    selected = index == tabIndex,
                    onClick = { viewModel.selectTab(tab) },
                    text = {
                        Text(
                            text = "${tab.label} ($count)",
                            fontSize = 12.sp,
                            fontWeight = if (index == tabIndex) FontWeight.Bold else FontWeight.Normal,
                            maxLines = 1
                        )
                    },
                    selectedContentColor = PrimaryPurple,
                    unselectedContentColor = Color.Gray
                )
            }
        }

        val currentList = when (state.selectedTab) {
            FavoriteTab.LIKED -> state.favorites
            FavoriteTab.ESSENTIAL -> state.essentials
            FavoriteTab.WATCHED -> state.watched
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
        Icon(
            imageVector = Icons.Default.Favorite,
            contentDescription = null,
            tint = HeartRed.copy(alpha = 0.3f),
            modifier = Modifier.size(80.dp)
        )
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
            color = Color.Gray,
            fontSize = 14.sp,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = 32.dp)
        )
        if (showExploreButton) {
            Spacer(modifier = Modifier.height(28.dp))
            Button(
                onClick = onExploreClick,
                colors = ButtonDefaults.buttonColors(containerColor = HeartRed),
                shape = androidx.compose.foundation.shape.RoundedCornerShape(14.dp),
                contentPadding = PaddingValues(horizontal = 24.dp, vertical = 12.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Explore,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text("Explorar series", color = Color.White, fontWeight = FontWeight.Bold)
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
