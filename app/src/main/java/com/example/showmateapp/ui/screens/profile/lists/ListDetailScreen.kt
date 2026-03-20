package com.example.showmateapp.ui.screens.profile.lists

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.showmateapp.R
import com.example.showmateapp.data.network.MediaContent
import com.example.showmateapp.ui.navigation.Screen
import com.example.showmateapp.ui.theme.PrimaryPurple
import com.example.showmateapp.ui.theme.TextGray

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ListDetailScreen(
    navController: NavController,
    viewModel: ListDetailViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    var showToRemove by remember { mutableStateOf<Int?>(null) }

    if (showToRemove != null) {
        AlertDialog(
            onDismissRequest = { showToRemove = null },
            containerColor = Color(0xFF1A1A2E),
            title = { Text("Quitar de la lista", color = Color.White, fontWeight = FontWeight.Bold) },
            text = { Text("¿Quitar esta serie de «${uiState.listName}»?", color = TextGray) },
            confirmButton = {
                Button(
                    onClick = {
                        showToRemove?.let { viewModel.removeFromList(it) }
                        showToRemove = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color.Red.copy(alpha = 0.8f))
                ) { Text("Quitar", fontWeight = FontWeight.Bold) }
            },
            dismissButton = {
                TextButton(onClick = { showToRemove = null }) {
                    Text("Cancelar", color = TextGray)
                }
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = uiState.listName,
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Volver", tint = Color.White)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background)
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { paddingValues ->
        when {
            uiState.isLoading -> {
                Box(
                    modifier = Modifier.fillMaxSize().padding(paddingValues),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = PrimaryPurple)
                }
            }
            uiState.shows.isEmpty() -> {
                Box(
                    modifier = Modifier.fillMaxSize().padding(paddingValues),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text(
                            text = "Lista vacía",
                            color = Color.White,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Añade series desde la pantalla de detalle",
                            color = TextGray,
                            fontSize = 14.sp,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
            else -> {
                LazyVerticalGrid(
                    columns = GridCells.Fixed(3),
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues),
                    contentPadding = PaddingValues(12.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(uiState.shows, key = { it.id }) { show ->
                        ListDetailShowCard(
                            show = show,
                            onClick = { navController.navigate(Screen.Detail(show.id)) },
                            onLongClick = { showToRemove = show.id }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ListDetailShowCard(
    show: MediaContent,
    onClick: () -> Unit,
    onLongClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .aspectRatio(2f / 3f)
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
    ) {
        AsyncImage(
            model = ImageRequest.Builder(LocalContext.current)
                .data("https://image.tmdb.org/t/p/w342${show.posterPath}")
                .crossfade(true)
                .build(),
            contentDescription = show.name,
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop,
            placeholder = painterResource(R.drawable.ic_logo_placeholder),
            error = painterResource(R.drawable.ic_logo_placeholder)
        )
        // Botón eliminar en esquina
        IconButton(
            onClick = onLongClick,
            modifier = Modifier
                .align(Alignment.TopEnd)
                .size(32.dp)
                .padding(4.dp)
        ) {
            Icon(
                Icons.Default.Delete,
                contentDescription = "Quitar de lista",
                tint = Color.White.copy(alpha = 0.8f),
                modifier = Modifier.size(16.dp)
            )
        }
        // Nombre en la parte inferior
        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .padding(4.dp)
        ) {
            Text(
                text = show.name,
                color = Color.White,
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}
