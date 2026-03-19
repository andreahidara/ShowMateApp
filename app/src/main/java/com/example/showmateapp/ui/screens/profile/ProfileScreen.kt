package com.example.showmateapp.ui.screens.profile

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.Analytics
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Stars
import androidx.compose.material.icons.filled.Update
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.showmateapp.R
import com.example.showmateapp.ui.navigation.Screen
import com.example.showmateapp.ui.theme.PrimaryPurple
import com.example.showmateapp.ui.theme.TextGray

@Composable
fun ProfileScreen(
    globalNavController: NavController,
    viewModel: ProfileViewModel = hiltViewModel()
) {
    val userEmail by viewModel.userEmail.collectAsState()
    val watchedShows by viewModel.watchedShows.collectAsState()
    val stats by viewModel.stats.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()

    val totalEpisodesWatched = remember(watchedShows) {
        watchedShows.sumOf { it.episodesWatched }
    }

    LaunchedEffect(Unit) {
        viewModel.loadProfileData()
    }

    var showLogoutDialog by remember { mutableStateOf(false) }
    var showResetDialog by remember { mutableStateOf(false) }

    if (showLogoutDialog) {
        AlertDialog(
            onDismissRequest = { showLogoutDialog = false },
            containerColor = Color(0xFF1A1A1A),
            title = { Text("Cerrar Sesión", color = Color.White, fontWeight = FontWeight.Bold) },
            text = { Text("¿Estás seguro de que quieres salir de ShowMate?", color = TextGray) },
            confirmButton = {
                Button(
                    onClick = {
                        showLogoutDialog = false
                        viewModel.logout(onSuccess = {
                            globalNavController.navigate(Screen.Login) {
                                popUpTo(Screen.Main) { inclusive = true }
                            }
                        })
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color.Red.copy(alpha = 0.8f))
                ) { Text("Salir", fontWeight = FontWeight.Bold) }
            },
            dismissButton = {
                TextButton(onClick = { showLogoutDialog = false }) { 
                    Text("Cancelar", color = Color.White) 
                }
            }
        )
    }

    if (showResetDialog) {
        AlertDialog(
            onDismissRequest = { showResetDialog = false },
            containerColor = Color(0xFF1A1A1A),
            title = { Text("Reiniciar algoritmo", color = Color.White, fontWeight = FontWeight.Bold) },
            text = { Text("Tus recomendaciones volverán a cero. ¿Deseas continuar?", color = TextGray) },
            confirmButton = {
                Button(
                    onClick = {
                        showResetDialog = false
                        viewModel.resetAlgorithmData(onComplete = {
                            globalNavController.navigate(Screen.Onboarding) {
                                popUpTo(Screen.Main) { inclusive = true }
                            }
                        })
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = PrimaryPurple)
                ) { Text("Reiniciar", fontWeight = FontWeight.Bold) }
            },
            dismissButton = {
                TextButton(onClick = { showResetDialog = false }) { 
                    Text("Cancelar", color = Color.White) 
                }
            }
        )
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background
    ) { padding ->
        if (isLoading) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = PrimaryPurple)
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .verticalScroll(rememberScrollState())
            ) {
                ProfileHeaderPremium(userEmail, stats.totalWatchedHours)

                ProfileStatsPremium(stats.watchedCount, stats.totalWatchedHours, totalEpisodesWatched, stats.topGenres)

                WatchedShowsSection(
                    items = watchedShows,
                    onShowClick = { id -> globalNavController.navigate(Screen.Detail(id)) }
                )

                SettingsSectionPremium(
                    onSettingsClick = { globalNavController.navigate(Screen.Settings) },
                    onResetClick = { showResetDialog = true },
                    onLogoutClick = { showLogoutDialog = true },
                    onAboutClick = { globalNavController.navigate(Screen.About) },
                    onStatsClick = { globalNavController.navigate(Screen.Stats) }
                )
                
                Spacer(modifier = Modifier.height(100.dp))
            }
        }
    }
}

@Composable
fun ProfileHeaderPremium(userName: String, totalHours: Int, modifier: Modifier = Modifier) {
    val level = remember(totalHours) {
        when {
            totalHours > 100 -> "Cinéfilo Experto"
            totalHours > 50 -> "Fan Entusiasta"
            else -> "Espectador Casual"
        }
    }

    val gradientBrush = remember {
        Brush.linearGradient(colors = listOf(PrimaryPurple, Color(0xFF9C27B0)))
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .statusBarsPadding()
            .padding(top = 16.dp, bottom = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .size(110.dp)
                .clip(CircleShape)
                .background(gradientBrush)
                .padding(3.dp)
                .clip(CircleShape)
                .background(Color.Black),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = userName.take(1).uppercase(),
                color = Color.White,
                fontSize = 42.sp,
                fontWeight = FontWeight.Black
            )
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        Text(
            text = userName,
            color = Color.White,
            fontSize = 28.sp,
            fontWeight = FontWeight.Black,
            letterSpacing = (-0.5).sp
        )
        
        Surface(
            color = PrimaryPurple.copy(alpha = 0.1f),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.padding(top = 4.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)
            ) {
                Icon(Icons.Default.Stars, contentDescription = null, tint = PrimaryPurple, modifier = Modifier.size(14.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = level,
                    color = PrimaryPurple,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
fun ProfileStatsPremium(watchedCount: Int, hours: Int, totalEpisodes: Int, topGenres: List<Pair<String, Float>>, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp, vertical = 12.dp)
            .clip(RoundedCornerShape(24.dp))
            .background(Color.White.copy(alpha = 0.05f))
            .padding(vertical = 20.dp, horizontal = 20.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            StatItemPremium(watchedCount.toString(), "SERIES", Icons.Default.Analytics)
            VerticalDivider(modifier = Modifier.height(30.dp), color = Color.White.copy(alpha = 0.1f))
            StatItemPremium(hours.toString(), "HORAS", Icons.Default.History)
            VerticalDivider(modifier = Modifier.height(30.dp), color = Color.White.copy(alpha = 0.1f))
            StatItemPremium(totalEpisodes.toString(), "EPISODIOS", Icons.Default.Movie)
        }

        if (topGenres.isNotEmpty()) {
            Spacer(modifier = Modifier.height(24.dp))
            Text(
                text = "TUS GÉNEROS FAVORITOS",
                style = MaterialTheme.typography.labelSmall,
                color = TextGray,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.sp,
                modifier = Modifier.padding(bottom = 12.dp)
            )
            topGenres.forEach { (genre, percentage) ->
                Row(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = genre, 
                        color = Color.White, 
                        fontSize = 13.sp, 
                        fontWeight = FontWeight.Medium, 
                        modifier = Modifier.width(85.dp)
                    )
                    LinearProgressIndicator(
                        progress = { percentage },
                        modifier = Modifier.weight(1f).height(8.dp).clip(CircleShape),
                        color = PrimaryPurple,
                        trackColor = Color.White.copy(alpha = 0.1f)
                    )
                    Text(
                        text = "${(percentage * 100).toInt()}%",
                        color = TextGray,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.width(42.dp),
                        textAlign = TextAlign.End
                    )
                }
            }
        }
    }
}

@Composable
fun StatItemPremium(count: String, label: String, icon: ImageVector, modifier: Modifier = Modifier) {
    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = modifier.width(80.dp)) {
        Icon(icon, contentDescription = null, tint = PrimaryPurple, modifier = Modifier.size(18.dp))
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = count,
            color = Color.White,
            fontSize = 16.sp,
            fontWeight = FontWeight.Black,
            maxLines = 1
        )
        Text(
            text = label,
            color = TextGray,
            fontSize = 9.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 1.sp
        )
    }
}


@Composable
fun WatchedShowsSection(
    items: List<WatchedShowItem>,
    onShowClick: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.padding(top = 32.dp)) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .width(3.dp)
                        .height(20.dp)
                        .clip(RoundedCornerShape(2.dp))
                        .background(PrimaryPurple)
                )
                Spacer(modifier = Modifier.width(10.dp))
                Text(
                    text = "Lo que he visto",
                    color = Color.White,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold
                )
            }
            if (items.isNotEmpty()) {
                Text(
                    text = "${items.size} series",
                    color = TextGray,
                    fontSize = 13.sp
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        if (items.isEmpty()) {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp)
                    .height(100.dp),
                color = Color.White.copy(alpha = 0.03f),
                shape = RoundedCornerShape(16.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text("Aún no has marcado series como vistas", color = TextGray, fontSize = 14.sp)
                }
            }
        } else {
            LazyRow(
                contentPadding = PaddingValues(horizontal = 24.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                items(items, key = { it.show.id }) { item ->
                    Box(
                        modifier = Modifier
                            .width(110.dp)
                            .aspectRatio(2f / 3f)
                            .clip(RoundedCornerShape(16.dp))
                            .clickable { onShowClick(item.show.id) }
                    ) {
                        AsyncImage(
                            model = ImageRequest.Builder(LocalContext.current)
                                .data("https://image.tmdb.org/t/p/w500${item.show.posterPath}")
                                .crossfade(true)
                                .build(),
                            contentDescription = item.show.name,
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop,
                            placeholder = painterResource(R.drawable.ic_logo_placeholder)
                        )
                        // Badge de episodios vistos
                        if (item.episodesWatched > 0) {
                            Surface(
                                modifier = Modifier
                                    .align(Alignment.BottomStart)
                                    .padding(6.dp),
                                color = Color(0xFF4CAF50).copy(alpha = 0.9f),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Text(
                                    text = "${item.episodesWatched} ep",
                                    color = Color.White,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        } else {
                            Surface(
                                modifier = Modifier
                                    .align(Alignment.BottomStart)
                                    .padding(6.dp),
                                color = Color.Black.copy(alpha = 0.6f),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Text(
                                    text = "Vista",
                                    color = Color.White,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun SettingsSectionPremium(onSettingsClick: () -> Unit, onResetClick: () -> Unit, onLogoutClick: () -> Unit, onAboutClick: () -> Unit, onStatsClick: () -> Unit = {}, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp, vertical = 32.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(bottom = 20.dp)
        ) {
            Box(
                modifier = Modifier
                    .width(3.dp)
                    .height(20.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(PrimaryPurple)
            )
            Spacer(modifier = Modifier.width(10.dp))
            Text(
                text = "Cuenta y Preferencias",
                color = Color.White,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold
            )
        }
        
        Surface(
            color = Color.White.copy(alpha = 0.05f),
            shape = RoundedCornerShape(24.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(vertical = 8.dp)) {
                SettingsItemPremium(Icons.Default.Analytics, "Estadísticas avanzadas", onStatsClick, isAction = true)
                SettingsItemPremium(Icons.Default.Settings, "Configuración", onSettingsClick)
                SettingsItemPremium(Icons.Default.Update, "Reiniciar mis gustos", onResetClick, isAction = true)
                SettingsItemPremium(Icons.Default.Info, "Sobre ShowMate", onAboutClick)
                SettingsItemPremium(Icons.AutoMirrored.Filled.Logout, "Cerrar sesión", onLogoutClick, isDestructive = true)
            }
        }
    }
}

@Composable
fun SettingsItemPremium(
    icon: ImageVector,
    title: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    isDestructive: Boolean = false,
    isAction: Boolean = false
) {
    val tint = remember(isDestructive, isAction) {
        if (isDestructive) Color(0xFFFF5252) else if (isAction) PrimaryPurple else Color.White.copy(alpha = 0.7f)
    }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(horizontal = 20.dp, vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(CircleShape)
                .background(tint.copy(alpha = 0.1f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.size(20.dp))
        }
        Spacer(modifier = Modifier.width(16.dp))
        Text(
            text = title,
            color = if (isDestructive) tint else Color.White,
            fontSize = 16.sp,
            fontWeight = if (isDestructive || isAction) FontWeight.Bold else FontWeight.Medium,
            modifier = Modifier.weight(1f)
        )
        Icon(
            Icons.AutoMirrored.Filled.KeyboardArrowRight,
            contentDescription = null,
            tint = Color.White.copy(alpha = 0.2f)
        )
    }
}
