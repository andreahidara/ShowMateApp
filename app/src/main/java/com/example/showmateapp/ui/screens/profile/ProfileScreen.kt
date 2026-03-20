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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Tv
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Stars
import androidx.compose.material.icons.filled.Update
import androidx.compose.material3.*
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import android.content.Context
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
import com.example.showmateapp.data.network.MediaContent
import com.example.showmateapp.ui.navigation.Screen
import com.example.showmateapp.ui.theme.PrimaryPurple
import com.example.showmateapp.ui.theme.StarYellow
import com.example.showmateapp.ui.theme.TextGray
import androidx.compose.material.icons.filled.Star

private val AVATAR_PALETTE = listOf(
    0xFF7C4DFF.toInt(), // Purple
    0xFFE91E63.toInt(), // Pink
    0xFF00BCD4.toInt(), // Cyan
    0xFF4CAF50.toInt(), // Green
    0xFFFF5722.toInt(), // Deep Orange
    0xFF2196F3.toInt(), // Blue
    0xFFFFB300.toInt(), // Amber
    0xFF795548.toInt()  // Brown
)

@Composable
fun ProfileScreen(
    globalNavController: NavController,
    scrollToTopTrigger: Int = 0,
    viewModel: ProfileViewModel = hiltViewModel()
) {
    val userEmail by viewModel.userEmail.collectAsState()
    val watchedShows by viewModel.watchedShows.collectAsState()
    val likedShows by viewModel.likedShows.collectAsState()
    val stats by viewModel.stats.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val isRefreshing by viewModel.isRefreshing.collectAsState()
    val customLists by viewModel.customLists.collectAsState()
    val watchedRatings by viewModel.watchedRatings.collectAsState()

    LaunchedEffect(Unit) {
        viewModel.loadProfileData()
    }

    val context = LocalContext.current
    val prefs = remember { context.getSharedPreferences("profile_prefs", Context.MODE_PRIVATE) }
    var avatarColorInt by remember { mutableIntStateOf(prefs.getInt("avatar_color", AVATAR_PALETTE[0])) }
    var showColorPicker by remember { mutableStateOf(false) }

    var showLogoutDialog by remember { mutableStateOf(false) }
    var showResetDialog by remember { mutableStateOf(false) }

    val scrollState = rememberScrollState()
    LaunchedEffect(scrollToTopTrigger) {
        if (scrollToTopTrigger > 0) scrollState.animateScrollTo(0)
    }

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

    if (showColorPicker) {
        AlertDialog(
            onDismissRequest = { showColorPicker = false },
            containerColor = Color(0xFF1A1A1A),
            title = { Text("Color del avatar", color = Color.White, fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("Elige un color para tu avatar:", color = TextGray, fontSize = 14.sp)
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        AVATAR_PALETTE.take(4).forEach { colorInt ->
                            val color = Color(colorInt)
                            Box(
                                modifier = Modifier
                                    .size(44.dp)
                                    .clip(CircleShape)
                                    .background(color)
                                    .clickable {
                                        avatarColorInt = colorInt
                                        prefs.edit().putInt("avatar_color", colorInt).apply()
                                        showColorPicker = false
                                    },
                                contentAlignment = Alignment.Center
                            ) {
                                if (avatarColorInt == colorInt) {
                                    Icon(
                                        Icons.Default.Settings,
                                        contentDescription = null,
                                        tint = Color.White,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            }
                        }
                    }
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        AVATAR_PALETTE.drop(4).forEach { colorInt ->
                            val color = Color(colorInt)
                            Box(
                                modifier = Modifier
                                    .size(44.dp)
                                    .clip(CircleShape)
                                    .background(color)
                                    .clickable {
                                        avatarColorInt = colorInt
                                        prefs.edit().putInt("avatar_color", colorInt).apply()
                                        showColorPicker = false
                                    },
                                contentAlignment = Alignment.Center
                            ) {
                                if (avatarColorInt == colorInt) {
                                    Icon(
                                        Icons.Default.Settings,
                                        contentDescription = null,
                                        tint = Color.White,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {},
            dismissButton = {
                TextButton(onClick = { showColorPicker = false }) {
                    Text("Cancelar", color = TextGray)
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

    @OptIn(ExperimentalMaterial3Api::class)
    Scaffold(
        containerColor = MaterialTheme.colorScheme.background
    ) { padding ->
        if (isLoading) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = PrimaryPurple)
            }
        } else {
            PullToRefreshBox(
                isRefreshing = isRefreshing,
                onRefresh = { viewModel.refresh() },
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
            ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(scrollState)
            ) {
                ProfileHeaderPremium(
                    userName = userEmail,
                    totalHours = stats.totalWatchedHours,
                    avatarColor = Color(avatarColorInt),
                    onAvatarClick = { showColorPicker = true }
                )

                WatchedShowsSection(
                    items = watchedShows,
                    ratings = watchedRatings,
                    onShowClick = { id -> globalNavController.navigate(Screen.Detail(id)) },
                    onViewAll = { globalNavController.navigate(Screen.AllShows("watched")) }
                )

                FavoritesInlineSection(
                    items = likedShows,
                    onShowClick = { id -> globalNavController.navigate(Screen.Detail(id)) },
                    onViewAll = { globalNavController.navigate(Screen.AllShows("favorites")) }
                )

                val posterPaths = remember(watchedShows, likedShows) {
                    buildMap {
                        watchedShows.forEach { put(it.show.id, it.show.posterPath) }
                        likedShows.forEach { put(it.id, it.posterPath) }
                    }
                }
                CustomListsInlineSection(
                    lists = customLists,
                    posterPaths = posterPaths,
                    onListClick = { name -> globalNavController.navigate(Screen.ListDetail(name)) },
                    onViewAllClick = { globalNavController.navigate(Screen.CustomLists) }
                )

                SettingsSectionPremium(
                    onSettingsClick = { globalNavController.navigate(Screen.Settings) },
                    onResetClick = { showResetDialog = true },
                    onLogoutClick = { showLogoutDialog = true },
                    onAboutClick = { globalNavController.navigate(Screen.About) }
                )
                
                Spacer(modifier = Modifier.height(100.dp))
            }
            } // closes PullToRefreshBox
        }
    }
}

@Composable
fun ProfileHeaderPremium(
    userName: String,
    totalHours: Int,
    avatarColor: Color = PrimaryPurple,
    onAvatarClick: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val level = remember(totalHours) {
        when {
            totalHours > 100 -> "Cinéfilo Experto"
            totalHours > 50 -> "Fan Entusiasta"
            else -> "Espectador Casual"
        }
    }

    val avatarGradient = remember(avatarColor) {
        Brush.linearGradient(colors = listOf(avatarColor, avatarColor.copy(alpha = 0.7f)))
    }

    val bannerGradient = remember {
        Brush.verticalGradient(
            colors = listOf(
                PrimaryPurple.copy(alpha = 0.25f),
                Color.Transparent
            )
        )
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .background(bannerGradient)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(top = 24.dp, bottom = 28.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .size(100.dp)
                    .clip(CircleShape)
                    .background(avatarGradient)
                    .clickable { onAvatarClick() },
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = userName.take(1).uppercase(),
                    color = Color.White,
                    fontSize = 40.sp,
                    fontWeight = FontWeight.Black
                )
                // Edit icon overlay at bottom-right
                Box(
                    modifier = Modifier
                        .size(26.dp)
                        .align(Alignment.BottomEnd)
                        .clip(CircleShape)
                        .background(Color(0xFF1A1A1A)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Default.Settings,
                        contentDescription = "Cambiar color",
                        tint = Color.White.copy(alpha = 0.8f),
                        modifier = Modifier.size(14.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            Text(
                text = userName,
                color = Color.White,
                fontSize = 26.sp,
                fontWeight = FontWeight.Black,
                letterSpacing = (-0.5).sp
            )

            Spacer(modifier = Modifier.height(6.dp))

            Surface(
                color = PrimaryPurple.copy(alpha = 0.18f),
                shape = RoundedCornerShape(20.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 5.dp)
                ) {
                    Icon(Icons.Default.Stars, contentDescription = null, tint = PrimaryPurple, modifier = Modifier.size(13.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = level,
                        color = PrimaryPurple,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            val levelProgress = remember(totalHours) {
                when {
                    totalHours >= 100 -> 1f
                    totalHours >= 50  -> (totalHours - 50) / 50f
                    else              -> totalHours / 50f
                }
            }
            val nextLevelLabel = remember(totalHours) {
                when {
                    totalHours >= 100 -> null
                    totalHours >= 50  -> "Cinéfilo Experto"
                    else              -> "Fan Entusiasta"
                }
            }
            Column(
                modifier = Modifier
                    .padding(horizontal = 40.dp)
                    .fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                LinearProgressIndicator(
                    progress = { levelProgress },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(4.dp)
                        .clip(CircleShape),
                    color = PrimaryPurple,
                    trackColor = Color.White.copy(alpha = 0.15f)
                )
                if (nextLevelLabel != null) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "${(levelProgress * 100).toInt()}% hacia $nextLevelLabel",
                        color = TextGray,
                        fontSize = 11.sp
                    )
                }
            }
        }
    }
}



@Composable
private fun ProfileSectionHeader(
    title: String,
    accentColor: Color,
    count: Int,
    modifier: Modifier = Modifier,
    onViewAll: (() -> Unit)? = null
) {
    Row(
        modifier = modifier
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
                    .background(accentColor)
            )
            Spacer(modifier = Modifier.width(10.dp))
            Text(text = title, color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Bold)
        }
        if (onViewAll != null && count > 0) {
            TextButton(
                onClick = onViewAll,
                contentPadding = PaddingValues(horizontal = 4.dp, vertical = 0.dp)
            ) {
                Text(
                    text = "Ver todas ($count)",
                    color = PrimaryPurple,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }
        } else if (count > 0) {
            Text(text = "$count series", color = TextGray, fontSize = 13.sp)
        }
    }
}

@Composable
fun WatchedShowsSection(
    items: List<WatchedShowItem>,
    ratings: Map<Int, Int> = emptyMap(),
    onShowClick: (Int) -> Unit,
    onViewAll: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.padding(top = 32.dp)) {
        ProfileSectionHeader(title = "Lo que he visto", accentColor = PrimaryPurple, count = items.size, onViewAll = onViewAll)
        Spacer(modifier = Modifier.height(16.dp))

        if (items.isEmpty()) {
            EmptySectionPlaceholder(
                message = "Aún no has marcado ninguna serie como vista",
                icon = Icons.Default.Tv
            )
        } else {
            LazyRow(
                contentPadding = PaddingValues(horizontal = 24.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                items(items, key = { it.show.id }) { item ->
                    PosterCard(
                        posterPath = item.show.posterPath,
                        name = item.show.name,
                        onClick = { onShowClick(item.show.id) }
                    ) {
                        val badgeText = if (item.episodesWatched > 0) "${item.episodesWatched} ep" else "Vista"
                        val badgeColor = if (item.episodesWatched > 0) Color(0xFF4CAF50).copy(alpha = 0.9f) else Color.Black.copy(alpha = 0.6f)
                        Surface(
                            modifier = Modifier.align(Alignment.BottomStart).padding(6.dp),
                            color = badgeColor,
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text(badgeText, color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp))
                        }
                        // Badge de valoración (estrella) en esquina superior derecha
                        val userRating = ratings[item.show.id]
                        if (userRating != null) {
                            Surface(
                                modifier = Modifier.align(Alignment.TopEnd).padding(6.dp),
                                color = StarYellow,
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp)
                                ) {
                                    Icon(
                                        Icons.Default.Star,
                                        contentDescription = null,
                                        tint = Color.Black,
                                        modifier = Modifier.size(9.dp)
                                    )
                                    Spacer(modifier = Modifier.width(2.dp))
                                    Text(
                                        text = "$userRating",
                                        color = Color.Black,
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Black
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun PosterCard(
    posterPath: String?,
    name: String,
    onClick: () -> Unit,
    badge: @Composable BoxScope.() -> Unit = {}
) {
    Box(
        modifier = Modifier
            .width(110.dp)
            .aspectRatio(2f / 3f)
            .clip(RoundedCornerShape(16.dp))
            .clickable(onClick = onClick)
    ) {
        AsyncImage(
            model = ImageRequest.Builder(LocalContext.current)
                .data("https://image.tmdb.org/t/p/w500$posterPath")
                .crossfade(true)
                .build(),
            contentDescription = name,
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop,
            placeholder = painterResource(R.drawable.ic_logo_placeholder)
        )
        badge()
    }
}

@Composable
private fun EmptySectionPlaceholder(
    message: String,
    icon: ImageVector = Icons.Default.Tv,
    actionLabel: String? = null,
    onAction: (() -> Unit)? = null
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp),
        color = Color.White.copy(alpha = 0.03f),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 28.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(52.dp)
                    .clip(CircleShape)
                    .background(Color.White.copy(alpha = 0.06f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, contentDescription = null, tint = TextGray, modifier = Modifier.size(26.dp))
            }
            Text(message, color = TextGray, fontSize = 14.sp, textAlign = TextAlign.Center)
            if (actionLabel != null && onAction != null) {
                Surface(
                    onClick = onAction,
                    color = PrimaryPurple.copy(alpha = 0.15f),
                    shape = RoundedCornerShape(20.dp)
                ) {
                    Text(
                        actionLabel,
                        color = PrimaryPurple,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun CustomListsInlineSection(
    lists: Map<String, List<Int>>,
    posterPaths: Map<Int, String?> = emptyMap(),
    onListClick: (String) -> Unit = {},
    onViewAllClick: () -> Unit,
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
                    text = "Mis listas",
                    color = Color.White,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold
                )
            }
            TextButton(onClick = onViewAllClick) {
                Text(
                    text = if (lists.isEmpty()) "Crear lista" else "Ver todas",
                    color = PrimaryPurple,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        if (lists.isEmpty()) {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp)
                    .height(80.dp)
                    .clickable { onViewAllClick() },
                color = Color.White.copy(alpha = 0.03f),
                shape = RoundedCornerShape(16.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxSize(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Icon(
                        Icons.AutoMirrored.Filled.List,
                        contentDescription = null,
                        tint = TextGray,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text("Crea tu primera lista personalizada", color = TextGray, fontSize = 14.sp)
                }
            }
        } else {
            LazyRow(
                contentPadding = PaddingValues(horizontal = 24.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(lists.entries.toList(), key = { it.key }) { (name, ids) ->
                    val previewPosters = ids.take(3).mapNotNull { posterPaths[it] }
                    Surface(
                        modifier = Modifier
                            .width(150.dp)
                            .height(110.dp)
                            .clickable { onListClick(name) },
                        color = Color.White.copy(alpha = 0.05f),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            // Posters apilados o icono si no hay
                            if (previewPosters.isEmpty()) {
                                Box(
                                    modifier = Modifier
                                        .size(32.dp)
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(PrimaryPurple.copy(alpha = 0.2f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        Icons.AutoMirrored.Filled.List,
                                        contentDescription = null,
                                        tint = PrimaryPurple,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            } else {
                                Box(modifier = Modifier.height(44.dp)) {
                                    previewPosters.forEachIndexed { i, path ->
                                        AsyncImage(
                                            model = ImageRequest.Builder(LocalContext.current)
                                                .data("https://image.tmdb.org/t/p/w92$path")
                                                .crossfade(true)
                                                .build(),
                                            contentDescription = null,
                                            modifier = Modifier
                                                .size(width = 30.dp, height = 44.dp)
                                                .offset(x = (i * 20).dp)
                                                .clip(RoundedCornerShape(6.dp)),
                                            contentScale = ContentScale.Crop
                                        )
                                    }
                                }
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = name,
                                color = Color.White,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = "${ids.size} ${if (ids.size == 1) "serie" else "series"}",
                                color = TextGray,
                                fontSize = 11.sp
                            )
                        }
                    }
                }
                item {
                    Surface(
                        modifier = Modifier
                            .width(90.dp)
                            .height(110.dp)
                            .clickable { onViewAllClick() },
                        color = PrimaryPurple.copy(alpha = 0.08f),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Column(
                            modifier = Modifier.fillMaxSize(),
                            verticalArrangement = Arrangement.Center,
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(Icons.Default.Add, contentDescription = null, tint = PrimaryPurple, modifier = Modifier.size(24.dp))
                            Spacer(modifier = Modifier.height(4.dp))
                            Text("Nueva", color = PrimaryPurple, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun FavoritesInlineSection(
    items: List<MediaContent>,
    onShowClick: (Int) -> Unit,
    onViewAll: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.padding(top = 32.dp)) {
        ProfileSectionHeader(title = "Favoritos", accentColor = Color(0xFFE91E63), count = items.size, onViewAll = onViewAll)
        Spacer(modifier = Modifier.height(16.dp))

        if (items.isEmpty()) {
            EmptySectionPlaceholder(
                message = "Aún no tienes series favoritas",
                icon = Icons.Default.Favorite
            )
        } else {
            LazyRow(
                contentPadding = PaddingValues(horizontal = 24.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                items(items, key = { it.id }) { media ->
                    PosterCard(
                        posterPath = media.posterPath,
                        name = media.name,
                        onClick = { onShowClick(media.id) }
                    )
                }
            }
        }
    }
}

@Composable
fun SettingsSectionPremium(onSettingsClick: () -> Unit, onResetClick: () -> Unit, onLogoutClick: () -> Unit, onAboutClick: () -> Unit, modifier: Modifier = Modifier) {
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
