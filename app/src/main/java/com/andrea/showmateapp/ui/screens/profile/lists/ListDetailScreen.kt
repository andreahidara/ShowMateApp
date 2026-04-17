package com.andrea.showmateapp.ui.screens.profile.lists

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.andrea.showmateapp.data.model.MediaContent
import com.andrea.showmateapp.ui.components.premium.TmdbImage
import com.andrea.showmateapp.ui.navigation.Screen
import com.andrea.showmateapp.ui.theme.HeartRed
import com.andrea.showmateapp.ui.theme.PrimaryPurple
import com.andrea.showmateapp.ui.theme.PrimaryPurpleLight
import com.andrea.showmateapp.ui.theme.TextGray
import com.andrea.showmateapp.util.TmdbUtils

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ListDetailScreen(navController: NavController, viewModel: ListDetailViewModel = hiltViewModel()) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    var editMode by remember { mutableStateOf(false) }
    var showToRemove by remember { mutableStateOf<MediaContent?>(null) }

    if (showToRemove != null) {
        AlertDialog(
            onDismissRequest = { showToRemove = null },
            containerColor = Color(0xFF1A1A2E),
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Default.Delete,
                        contentDescription = null,
                        tint = HeartRed,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(Modifier.width(8.dp))
                    Text("Quitar de la lista", color = Color.White, fontWeight = FontWeight.Bold)
                }
            },
            text = {
                Text(
                    "¿Quitar «${showToRemove?.name}» de «${uiState.listName}»?",
                    color = TextGray,
                    lineHeight = 20.sp
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        showToRemove?.let { viewModel.removeFromList(it.id) }
                        showToRemove = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = HeartRed)
                ) { Text("Quitar", fontWeight = FontWeight.Bold) }
            },
            dismissButton = {
                TextButton(onClick = { showToRemove = null }) { Text("Cancelar", color = TextGray) }
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = uiState.listName,
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        if (!uiState.isLoading) {
                            Text(
                                text = if (uiState.shows.isEmpty()) {
                                    "Vacía"
                                } else {
                                    "${uiState.shows.size} ${if (uiState.shows.size == 1) "serie" else "series"}"
                                },
                                color = TextGray,
                                fontSize = 12.sp
                            )
                        }
                    }
                },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Volver", tint = Color.White)
                    }
                },
                actions = {
                    if (uiState.shows.isNotEmpty()) {
                        val editBgColor by animateColorAsState(
                            targetValue = if (editMode) PrimaryPurple.copy(alpha = 0.22f) else Color.Transparent,
                            animationSpec = tween(200),
                            label = "editBg"
                        )
                        IconButton(
                            onClick = { editMode = !editMode },
                            modifier = Modifier
                                .clip(RoundedCornerShape(10.dp))
                                .background(editBgColor)
                        ) {
                            Icon(
                                imageVector = if (editMode) Icons.Default.Close else Icons.Default.Edit,
                                contentDescription = if (editMode) "Salir del modo edición" else "Editar lista",
                                tint = if (editMode) PrimaryPurpleLight else TextGray
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background)
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { paddingValues ->
        when {
            uiState.isLoading -> {
                Box(Modifier.fillMaxSize().padding(paddingValues), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = PrimaryPurple)
                }
            }

            uiState.error != null -> {
                Box(Modifier.fillMaxSize().padding(paddingValues), contentAlignment = Alignment.Center) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text(uiState.error ?: "", color = TextGray, fontSize = 14.sp)
                        TextButton(onClick = { navController.popBackStack() }) {
                            Text("Volver", color = PrimaryPurple)
                        }
                    }
                }
            }

            uiState.shows.isEmpty() -> {
                ListDetailEmptyState(
                    modifier = Modifier.fillMaxSize().padding(paddingValues),
                    listName = uiState.listName
                )
            }

            else -> {
                LazyVerticalGrid(
                    columns = GridCells.Fixed(2),
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues),
                    contentPadding = PaddingValues(horizontal = 14.dp, vertical = 14.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp),
                    horizontalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    items(uiState.shows, key = { it.id }) { show ->
                        ListDetailShowCard(
                            show = show,
                            editMode = editMode,
                            onClick = {
                                if (editMode) {
                                    showToRemove = show
                                } else {
                                    navController.navigate(Screen.Detail(show.id))
                                }
                            },
                            onRemove = { showToRemove = show }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ListDetailShowCard(show: MediaContent, editMode: Boolean, onClick: () -> Unit, onRemove: () -> Unit) {
    Box(
        modifier = Modifier
            .aspectRatio(2f / 3f)
            .clip(RoundedCornerShape(16.dp))
            .clickable(onClick = onClick)
            .then(
                if (editMode) {
                    Modifier.border(2.dp, PrimaryPurple.copy(alpha = 0.5f), RoundedCornerShape(16.dp))
                } else {
                    Modifier
                }
            )
    ) {
        TmdbImage(
            path = show.posterPath,
            contentDescription = show.name,
            size = TmdbUtils.ImageSize.W342,
            modifier = Modifier.fillMaxSize()
        )

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.45f)
                .align(Alignment.BottomCenter)
                .background(
                    Brush.verticalGradient(
                        listOf(Color.Transparent, Color.Black.copy(alpha = 0.85f))
                    )
                )
        )
        Text(
            text = show.name,
            color = Color.White,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Start,
            lineHeight = 16.sp,
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(horizontal = 10.dp, vertical = 8.dp)
        )

        if (show.voteAverage > 0) {
            Box(
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(8.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color.Black.copy(alpha = 0.65f))
                    .padding(horizontal = 6.dp, vertical = 3.dp)
            ) {
                Text(
                    text = "★ ${"%.1f".format(show.voteAverage)}",
                    color = Color(0xFFFFD700),
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        if (editMode) {
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(6.dp)
                    .size(26.dp)
                    .clip(CircleShape)
                    .background(HeartRed)
                    .clickable(onClick = onRemove),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Default.Close,
                    contentDescription = "Quitar de la lista",
                    tint = Color.White,
                    modifier = Modifier.size(14.dp)
                )
            }
        }
    }
}

@Composable
private fun ListDetailEmptyState(modifier: Modifier = Modifier, listName: String) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            modifier = Modifier
                .size(90.dp)
                .clip(CircleShape)
                .background(
                    Brush.radialGradient(listOf(PrimaryPurple.copy(alpha = 0.20f), Color.Transparent))
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                Icons.AutoMirrored.Filled.List,
                contentDescription = null,
                tint = PrimaryPurpleLight,
                modifier = Modifier.size(40.dp)
            )
        }
        Spacer(Modifier.height(20.dp))
        Text(
            "«$listName» está vacía",
            color = Color.White,
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center
        )
        Spacer(Modifier.height(8.dp))
        Text(
            "Añade series desde la pantalla de detalle\npulsando el botón de listas",
            color = TextGray,
            fontSize = 14.sp,
            lineHeight = 20.sp,
            textAlign = TextAlign.Center
        )
        Spacer(Modifier.height(24.dp))
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(14.dp))
                .background(PrimaryPurple.copy(alpha = 0.14f))
                .padding(horizontal = 20.dp, vertical = 10.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    Icons.Default.Movie,
                    contentDescription = null,
                    tint = PrimaryPurpleLight,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    "Explora series para añadir",
                    color = PrimaryPurpleLight,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
    }
}

