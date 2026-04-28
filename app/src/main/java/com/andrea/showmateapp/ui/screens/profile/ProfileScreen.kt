package com.andrea.showmateapp.ui.screens.profile

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Stars
import androidx.compose.material.icons.filled.Tv
import androidx.compose.material.icons.filled.Update
import androidx.compose.material.icons.filled.WatchLater
import androidx.compose.material3.*
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.andrea.showmateapp.R
import com.andrea.showmateapp.data.model.*
import com.andrea.showmateapp.domain.usecase.GetProfileStatsUseCase
import com.andrea.showmateapp.ui.components.ErrorView
import com.andrea.showmateapp.ui.components.TmdbImage
import com.andrea.showmateapp.ui.components.shimmerBrush
import com.andrea.showmateapp.ui.navigation.Screen
import com.andrea.showmateapp.ui.theme.PrimaryPurple
import com.andrea.showmateapp.ui.theme.PrimaryPurpleDark
import com.andrea.showmateapp.ui.theme.PrimaryPurpleLight
import com.andrea.showmateapp.ui.theme.StarYellow
import com.andrea.showmateapp.ui.theme.SurfaceVariantDark
import com.andrea.showmateapp.ui.theme.TextGray
import com.andrea.showmateapp.util.TmdbUtils

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(
    globalNavController: NavController,
    scrollToTopTrigger: Int = 0,
    viewModel: ProfileViewModel = hiltViewModel()
) {
    val userName by viewModel.displayName.collectAsStateWithLifecycle()
    val watchedShows by viewModel.watchedShows.collectAsStateWithLifecycle()
    val likedShows by viewModel.likedShows.collectAsStateWithLifecycle()
    val watchlistShows by viewModel.watchlistShows.collectAsStateWithLifecycle()
    val stats by viewModel.stats.collectAsStateWithLifecycle()
    val userLevel by viewModel.userLevel.collectAsStateWithLifecycle()
    val isLoading by viewModel.isLoading.collectAsStateWithLifecycle()
    val isRefreshing by viewModel.isRefreshing.collectAsStateWithLifecycle()
    val customLists by viewModel.customLists.collectAsStateWithLifecycle()
    val watchedRatings by viewModel.watchedRatings.collectAsStateWithLifecycle()
    val personality by viewModel.viewerPersonality.collectAsStateWithLifecycle()
    val avatarColorInt by viewModel.avatarColorInt.collectAsStateWithLifecycle()
    val photoUrl by viewModel.photoUrl.collectAsStateWithLifecycle()
    val isUploadingPhoto by viewModel.isUploadingPhoto.collectAsStateWithLifecycle()
    val error by viewModel.error.collectAsStateWithLifecycle()

    var showColorPicker by remember { mutableStateOf(false) }
    var showLogoutDialog by remember { mutableStateOf(false) }
    var showAvatarOptions by remember { mutableStateOf(false) }

    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        uri?.let { viewModel.uploadProfilePhoto(it) }
    }

    val listState = rememberLazyListState()
    LaunchedEffect(scrollToTopTrigger) {
        if (scrollToTopTrigger > 0) listState.animateScrollToItem(0)
    }

    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        var isFirstResume = true
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                if (isFirstResume) {
                    isFirstResume = false
                    return@LifecycleEventObserver
                }
                viewModel.refresh()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    val achievementProgress by viewModel.achievementProgress.collectAsStateWithLifecycle()
    val friendCount by viewModel.friendCount.collectAsStateWithLifecycle()
    val posterPaths = remember(watchedShows, likedShows, watchlistShows) {
        buildMap {
            watchedShows.forEach { put(it.show.id, it.show.posterPath) }
            likedShows.forEach { put(it.id, it.posterPath) }
            watchlistShows.forEach { put(it.id, it.posterPath) }
        }
    }

    if (showLogoutDialog) {
        LogoutConfirmDialog(
            onConfirm = {
                showLogoutDialog = false
                viewModel.logout(onSuccess = {
                    globalNavController.navigate(Screen.Login) {
                        popUpTo(Screen.Main) { inclusive = true }
                    }
                })
            },
            onDismiss = { showLogoutDialog = false }
        )
    }

    if (showColorPicker) {
        AvatarColorPickerDialog(
            palette = ProfileViewModel.AVATAR_PALETTE,
            selectedColorInt = avatarColorInt,
            onColorSelected = { colorInt ->
                viewModel.updateAvatarColor(colorInt)
                showColorPicker = false
            },
            onDismiss = { showColorPicker = false }
        )
    }

    if (showAvatarOptions) {
        AlertDialog(
            onDismissRequest = { showAvatarOptions = false },
            containerColor = Color(0xFF1A1A2E),
            title = { Text("Cambiar avatar", color = Color.White, fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Surface(
                        onClick = {
                            showAvatarOptions = false
                            photoPickerLauncher.launch(
                                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                            )
                        },
                        color = Color.White.copy(alpha = 0.06f),
                        shape = androidx.compose.foundation.shape.RoundedCornerShape(12.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                Icons.Default.Stars,
                                contentDescription = null,
                                tint = PrimaryPurple,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(Modifier.width(12.dp))
                            Text("Subir foto de perfil", color = Color.White, fontSize = 15.sp)
                        }
                    }
                    Surface(
                        onClick = {
                            showAvatarOptions = false
                            showColorPicker = true
                        },
                        color = Color.White.copy(alpha = 0.06f),
                        shape = androidx.compose.foundation.shape.RoundedCornerShape(12.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                Icons.Default.Edit,
                                contentDescription = null,
                                tint = PrimaryPurple,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(Modifier.width(12.dp))
                            Text("Elegir color de avatar", color = Color.White, fontSize = 15.sp)
                        }
                    }
                }
            },
            confirmButton = {},
            dismissButton = {
                TextButton(onClick = { showAvatarOptions = false }) {
                    Text(stringResource(R.string.cancel), color = TextGray)
                }
            }
        )
    }

    Scaffold(containerColor = MaterialTheme.colorScheme.background, contentWindowInsets = WindowInsets(0.dp)) { padding ->
        if (isLoading) {
            ProfileSkeleton()
        } else if (error != null) {
            ErrorView(
                message = error!!,
                onRetry = viewModel::retryLoad,
                modifier = Modifier.padding(padding)
            )
        } else {
            PullToRefreshBox(
                isRefreshing = isRefreshing,
                onRefresh = { viewModel.refresh() },
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
            ) {
                LazyColumn(
                    state = listState,
                    modifier = Modifier.fillMaxSize()
                ) {
                    item {
                        ProfileHeaderPremium(
                            userName = userName,
                            userLevel = userLevel,
                            personalityLabel = personality?.label,
                            avatarColor = Color(avatarColorInt),
                            photoUrl = photoUrl,
                            isUploadingPhoto = isUploadingPhoto,
                            watchedCount = stats.watchedCount,
                            totalHours = stats.totalHours,
                            friendCount = friendCount,
                            onAvatarClick = { showAvatarOptions = true }
                        )
                    }

                    item { StatsSection(stats = stats, personalityLabel = personality?.label) }

                    item {
                        AchievementsEntryButton(
                            onClick = { globalNavController.navigate(Screen.Achievements) },
                            unlockedCount = achievementProgress.first,
                            totalCount = achievementProgress.second
                        )
                    }

                    item {
                        WatchedShowsSection(
                            items = watchedShows,
                            ratings = watchedRatings,
                            onShowClick = { id -> globalNavController.navigate(Screen.Detail(id)) },
                            onViewAll = { globalNavController.navigate(Screen.AllShows("watched")) }
                        )
                    }

                    item {
                        FavoritesInlineSection(
                            items = likedShows,
                            onShowClick = { id -> globalNavController.navigate(Screen.Detail(id)) },
                            onViewAll = { globalNavController.navigate(Screen.AllShows("favorites")) }
                        )
                    }

                    item {
                        FavoritesInlineSection(
                            title = "Pendientes",
                            items = watchlistShows,
                            onShowClick = { id -> globalNavController.navigate(Screen.Detail(id)) },
                            onViewAll = { globalNavController.navigate(Screen.AllShows("watchlist")) },
                            accentColor = PrimaryPurple,
                            emptyMessage = "Aún no tienes series en tu lista de pendientes",
                            icon = Icons.Default.WatchLater
                        )
                    }

                    item {
                        CustomListsInlineSection(
                            lists = customLists,
                            posterPaths = posterPaths,
                            onListClick = { name -> globalNavController.navigate(Screen.ListDetail(name)) },
                            onViewAllClick = { globalNavController.navigate(Screen.CustomLists) }
                        )
                    }

                    item {
                        SettingsSectionPremium(
                            onSettingsClick = { globalNavController.navigate(Screen.Settings) },
                            onLogoutClick = { showLogoutDialog = true },
                            onAboutClick = { globalNavController.navigate(Screen.About) }
                        )
                    }

                    item { Spacer(modifier = Modifier.height(100.dp)) }
                }
            }
        }
    }
}

@Composable
fun ProfileHeaderPremium(
    userName: String,
    userLevel: UserLevel,
    modifier: Modifier = Modifier,
    personalityLabel: String? = null,
    avatarColor: Color = PrimaryPurple,
    photoUrl: String? = null,
    isUploadingPhoto: Boolean = false,
    watchedCount: Int = 0,
    totalHours: Int = 0,
    friendCount: Int = 0,
    onAvatarClick: () -> Unit = {}
) {
    val infiniteTransition = rememberInfiniteTransition(label = "headerGlow")
    val ringAlpha by infiniteTransition.animateFloat(
        initialValue = 0.18f,
        targetValue = 0.42f,
        animationSpec = infiniteRepeatable(
            animation = tween(2200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "ringAlpha"
    )

    val bannerGradient = remember {
        Brush.verticalGradient(
            colorStops = arrayOf(
                0f to PrimaryPurpleDark.copy(alpha = 0.60f),
                0.55f to PrimaryPurple.copy(alpha = 0.20f),
                1f to Color.Transparent
            )
        )
    }
    val avatarGradient = remember(avatarColor) {
        Brush.linearGradient(
            colors = listOf(
                avatarColor.copy(alpha = 0.9f),
                avatarColor.copy(alpha = 0.55f)
            )
        )
    }
    val levelBarGradient = remember {
        Brush.linearGradient(listOf(PrimaryPurpleLight, PrimaryPurple))
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
                .padding(top = 28.dp, bottom = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(contentAlignment = Alignment.Center) {
                Box(
                    modifier = Modifier
                        .size(118.dp)
                        .clip(CircleShape)
                        .background(avatarColor.copy(alpha = ringAlpha))
                )
                Box(
                    modifier = Modifier
                        .size(104.dp)
                        .clip(CircleShape)
                        .background(avatarGradient)
                        .clickable { onAvatarClick() },
                    contentAlignment = Alignment.Center
                ) {
                    if (photoUrl != null) {
                        coil.compose.AsyncImage(
                            model = coil.request.ImageRequest.Builder(androidx.compose.ui.platform.LocalContext.current)
                                .data(photoUrl)
                                .crossfade(true)
                                .memoryCachePolicy(coil.request.CachePolicy.ENABLED)
                                .build(),
                            contentDescription = null,
                            modifier = Modifier.fillMaxSize(),
                            contentScale = androidx.compose.ui.layout.ContentScale.Crop
                        )
                    } else {
                        Text(
                            text = userName.take(1).uppercase(),
                            color = Color.White,
                            fontSize = 42.sp,
                            fontWeight = FontWeight.Black
                        )
                    }
                    if (isUploadingPhoto) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(Color.Black.copy(alpha = 0.5f)),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator(
                                color = Color.White,
                                modifier = Modifier.size(28.dp),
                                strokeWidth = 2.dp
                            )
                        }
                    }
                    Box(
                        modifier = Modifier
                            .size(28.dp)
                            .align(Alignment.BottomEnd)
                            .clip(CircleShape)
                            .background(Color(0xFF151522)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Default.Edit,
                            contentDescription = "Cambiar avatar",
                            tint = Color.White.copy(alpha = 0.85f),
                            modifier = Modifier.size(14.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = userName,
                style = TextStyle(
                    brush = Brush.linearGradient(listOf(PrimaryPurpleLight, PrimaryPurple))
                ),
                fontSize = 30.sp,
                fontWeight = FontWeight.Black,
                letterSpacing = (-1.5).sp
            )
            Box(
                modifier = Modifier
                    .padding(top = 4.dp)
                    .width(38.dp)
                    .height(3.dp)
                    .clip(androidx.compose.foundation.shape.RoundedCornerShape(2.dp))
                    .background(Brush.linearGradient(listOf(PrimaryPurpleLight, PrimaryPurple)))
            )

            Spacer(modifier = Modifier.height(8.dp))

            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(24.dp))
                    .background(
                        Brush.linearGradient(
                            listOf(PrimaryPurple.copy(alpha = 0.45f), PrimaryPurpleDark.copy(alpha = 0.30f))
                        )
                    )
                    .border(
                        1.dp,
                        Brush.linearGradient(
                            listOf(PrimaryPurpleLight.copy(alpha = 0.50f), PrimaryPurple.copy(alpha = 0.20f))
                        ),
                        RoundedCornerShape(24.dp)
                    )
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(horizontal = 18.dp, vertical = 8.dp)
                ) {
                    Icon(
                        if (personalityLabel != null) Icons.Default.Psychology else Icons.Default.Stars,
                        contentDescription = null,
                        tint = PrimaryPurpleLight,
                        modifier = Modifier.size(15.dp)
                    )
                    Spacer(modifier = Modifier.width(7.dp))
                    Text(
                        text = personalityLabel ?: userLevel.label,
                        color = Color.White,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.ExtraBold,
                        letterSpacing = (-0.3).sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            Column(
                modifier = Modifier
                    .padding(horizontal = 40.dp)
                    .fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(5.dp)
                        .clip(CircleShape)
                        .background(Color.White.copy(alpha = 0.12f))
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(userLevel.progress)
                            .height(5.dp)
                            .clip(CircleShape)
                            .background(levelBarGradient)
                    )
                }
                userLevel.nextLabel?.let { nextLabel ->
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = stringResource(
                            R.string.profile_progress_towards,
                            (userLevel.progress * 100).toInt(),
                            nextLabel
                        ),
                        color = TextGray,
                        fontSize = 11.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                ProfileQuickStat(
                    value = watchedCount.toString(),
                    label = "Series",
                    icon = Icons.Default.Tv,
                    color = PrimaryPurpleLight,
                    modifier = Modifier.weight(1f)
                )
                ProfileQuickStat(
                    value = totalHours.toString(),
                    label = "Horas",
                    icon = Icons.Default.Update,
                    color = Color(0xFF00BCD4),
                    modifier = Modifier.weight(1f)
                )
                ProfileQuickStat(
                    value = friendCount.toString(),
                    label = "Amigos",
                    icon = Icons.Default.Star,
                    color = Color(0xFF4CAF50),
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@Composable
private fun ProfileQuickStat(
    value: String,
    label: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    color: Color,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .background(color.copy(alpha = 0.12f))
            .border(1.dp, color.copy(alpha = 0.22f), RoundedCornerShape(16.dp))
            .padding(vertical = 12.dp, horizontal = 6.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(16.dp))
        Text(value, color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Black)
        Text(label, color = color.copy(alpha = 0.80f), fontSize = 10.sp, fontWeight = FontWeight.Medium)
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
                    text = stringResource(R.string.profile_see_all_count, count),
                    color = PrimaryPurple,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }
        } else if (count > 0) {
            Text(text = stringResource(R.string.profile_series_count, count), color = TextGray, fontSize = 13.sp)
        }
    }
}

@Composable
fun WatchedShowsSection(
    items: List<WatchedShowItem>,
    onShowClick: (Int) -> Unit,
    modifier: Modifier = Modifier,
    ratings: Map<Int, Int> = emptyMap(),
    onViewAll: (() -> Unit)? = null
) {
    Column(modifier = modifier.padding(top = 32.dp)) {
        ProfileSectionHeader(
            title = "Lo que he visto",
            accentColor = PrimaryPurple,
            count = items.size,
            onViewAll = onViewAll
        )
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
                        onClick = { onShowClick(item.show.id) },
                        modifier = Modifier.animateItem(),
                        showTitle = true
                    ) {
                        val badgeText = if (item.episodesWatched > 0) "${item.episodesWatched} ep" else "Vista ✓"
                        val badgeColor = Color(0xFF4CAF50).copy(alpha = if (item.episodesWatched > 0) 0.9f else 0.6f)
                        Surface(
                            modifier = Modifier.align(Alignment.BottomStart).padding(6.dp),
                            color = badgeColor,
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text(
                                badgeText,
                                color = Color.White,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
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
    modifier: Modifier = Modifier,
    showTitle: Boolean = false,
    badge: @Composable BoxScope.() -> Unit = {}
) {
    Column(modifier = modifier.width(110.dp)) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(2f / 3f)
                .clip(RoundedCornerShape(16.dp))
                .clickable(onClick = onClick)
        ) {
            TmdbImage(
                path = posterPath,
                contentDescription = name,
                size = TmdbUtils.ImageSize.W500,
                modifier = Modifier.fillMaxSize()
            )
            badge()
        }
        if (showTitle) {
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = name,
                color = Color.White.copy(alpha = 0.85f),
                fontSize = 11.sp,
                fontWeight = FontWeight.Medium,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                lineHeight = 14.sp
            )
        }
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
                    .size(56.dp)
                    .clip(CircleShape)
                    .background(
                        Brush.radialGradient(
                            listOf(PrimaryPurple.copy(alpha = 0.18f), Color.Transparent)
                        )
                    ),
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
    onViewAllClick: () -> Unit,
    modifier: Modifier = Modifier,
    posterPaths: Map<Int, String?> = emptyMap(),
    onListClick: (String) -> Unit = {}
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
                    text = stringResource(R.string.profile_my_lists),
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
                    Box(
                        modifier = Modifier
                            .width(155.dp)
                            .height(115.dp)
                            .clip(RoundedCornerShape(18.dp))
                            .background(
                                Brush.verticalGradient(
                                    listOf(
                                        PrimaryPurple.copy(alpha = 0.12f),
                                        Color.White.copy(alpha = 0.04f)
                                    )
                                )
                            )
                            .clickable { onListClick(name) }
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            if (previewPosters.isEmpty()) {
                                Box(
                                    modifier = Modifier
                                        .size(36.dp)
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(PrimaryPurple.copy(alpha = 0.22f)),
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
                                Box(modifier = Modifier.height(48.dp)) {
                                    previewPosters.forEachIndexed { i, path ->
                                        TmdbImage(
                                            path = path,
                                            contentDescription = null,
                                            size = TmdbUtils.ImageSize.W92,
                                            modifier = Modifier
                                                .size(width = 32.dp, height = 48.dp)
                                                .offset(x = (i * 22).dp)
                                                .clip(RoundedCornerShape(7.dp))
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
                    Box(
                        modifier = Modifier
                            .width(90.dp)
                            .height(115.dp)
                            .clip(RoundedCornerShape(18.dp))
                            .background(
                                Brush.verticalGradient(
                                    listOf(
                                        PrimaryPurple.copy(alpha = 0.18f),
                                        PrimaryPurpleDark.copy(alpha = 0.08f)
                                    )
                                )
                            )
                            .clickable { onViewAllClick() },
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(32.dp)
                                    .clip(CircleShape)
                                    .background(PrimaryPurple.copy(alpha = 0.25f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    Icons.Default.Add,
                                    contentDescription = null,
                                    tint = PrimaryPurpleLight,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                            Text(
                                "Nueva",
                                color = PrimaryPurpleLight,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold
                            )
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
    modifier: Modifier = Modifier,
    onViewAll: (() -> Unit)? = null,
    title: String = "Favoritos",
    accentColor: Color = Color(0xFFE91E63),
    emptyMessage: String = "Aún no tienes series favoritas",
    icon: ImageVector = Icons.Default.Favorite
) {
    Column(modifier = modifier.padding(top = 32.dp)) {
        ProfileSectionHeader(
            title = title,
            accentColor = accentColor,
            count = items.size,
            onViewAll = onViewAll
        )
        Spacer(modifier = Modifier.height(16.dp))

        if (items.isEmpty()) {
            EmptySectionPlaceholder(
                message = emptyMessage,
                icon = icon
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
                        onClick = { onShowClick(media.id) },
                        modifier = Modifier.animateItem(),
                        showTitle = true
                    ) {
                        if (media.voteAverage > 0) {
                            Box(
                                modifier = Modifier
                                    .align(Alignment.TopStart)
                                    .padding(6.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(Color.Black.copy(alpha = 0.65f))
                                    .padding(horizontal = 6.dp, vertical = 3.dp)
                            ) {
                                Text(
                                    text = "★ ${"%.1f".format(media.voteAverage)}",
                                    color = Color(0xFFFFD700),
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold
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
fun StatsSection(
    stats: GetProfileStatsUseCase.ProfileStats,
    modifier: Modifier = Modifier,
    personalityLabel: String? = null
) {
    val genreBarGradient = remember {
        Brush.linearGradient(listOf(PrimaryPurpleLight, PrimaryPurple))
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp, vertical = 24.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(bottom = 16.dp)
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
                text = "Mis estadísticas",
                color = Color.White,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold
            )
        }

        if (personalityLabel != null) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(
                        Brush.linearGradient(
                            listOf(PrimaryPurple.copy(alpha = 0.20f), PrimaryPurpleDark.copy(alpha = 0.10f))
                        )
                    )
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Default.Psychology,
                        contentDescription = null,
                        tint = PrimaryPurpleLight,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = "Tu perfil de espectador",
                            color = TextGray,
                            fontSize = 11.sp
                        )
                        Text(
                            text = personalityLabel,
                            color = Color.White,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }

        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                StatMiniCard(value = stats.watchedCount.toString(), label = "Series", modifier = Modifier.weight(1f))
                StatMiniCard(
                    value = stats.totalEpisodes.toString(),
                    label = "Episodios",
                    modifier = Modifier.weight(1f)
                )
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                StatMiniCard(
                    value = stats.totalHours.toString(),
                    label = "Horas",
                    modifier = Modifier.weight(1f)
                )
                StatMiniCard(
                    value = stats.ratingsCount.toString(),
                    label = "Valoraciones",
                    modifier = Modifier.weight(1f)
                )
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        if (stats.topGenres.isNotEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(18.dp))
                    .background(SurfaceVariantDark)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Géneros favoritos",
                        color = Color.White,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(bottom = 14.dp)
                    )
                    stats.topGenres.forEach { (genreName, score) ->
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 5.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = genreName,
                                    color = Color.White,
                                    fontSize = 13.sp,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    modifier = Modifier.weight(1f)
                                )
                                Text(
                                    text = "${(score * 100).toInt()}%",
                                    color = PrimaryPurpleLight,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            Spacer(modifier = Modifier.height(5.dp))
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(5.dp)
                                    .clip(RoundedCornerShape(3.dp))
                                    .background(Color.White.copy(alpha = 0.08f))
                            ) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth(score)
                                        .height(5.dp)
                                        .clip(RoundedCornerShape(3.dp))
                                        .background(genreBarGradient)
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(18.dp))
                .background(SurfaceVariantDark)
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                if (stats.likedCount > 0) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Afinidad positiva",
                                color = Color.White,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                text = "${(stats.likeRate * 100).toInt()}% de valoradas son Me Gusta",
                                color = TextGray,
                                fontSize = 11.sp
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Box(contentAlignment = Alignment.Center) {
                            CircularProgressIndicator(
                                progress = { stats.likeRate },
                                modifier = Modifier.size(52.dp),
                                color = PrimaryPurple,
                                trackColor = Color.White.copy(alpha = 0.08f),
                                strokeWidth = 4.dp
                            )
                            Text(
                                text = "${(stats.likeRate * 100).toInt()}%",
                                color = Color.White,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    HorizontalDivider(color = Color.White.copy(alpha = 0.06f))
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .clip(CircleShape)
                            .background(StarYellow.copy(alpha = 0.15f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Default.Star,
                            contentDescription = null,
                            tint = StarYellow,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = "Valoración media",
                        color = Color.White,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.weight(1f)
                    )
                    Text(
                        text = if (stats.ratingsCount > 0) {
                            "${"%.1f".format(
                                stats.avgRating
                            )} / 10"
                        } else {
                            "Sin valoraciones"
                        },
                        color = if (stats.ratingsCount > 0) StarYellow else TextGray,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

@Composable
private fun StatMiniCard(value: String, label: String, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .background(
                Brush.verticalGradient(
                    listOf(
                        PrimaryPurple.copy(alpha = 0.22f),
                        SurfaceVariantDark
                    )
                )
            )
    ) {
        Column(
            modifier = Modifier
                .padding(vertical = 14.dp, horizontal = 4.dp)
                .fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = value,
                color = Color.White,
                fontSize = 24.sp,
                fontWeight = FontWeight.Black,
                textAlign = TextAlign.Center
            )
            Text(
                text = label,
                color = TextGray,
                fontSize = 11.sp,
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
fun SettingsSectionPremium(
    onSettingsClick: () -> Unit,
    onLogoutClick: () -> Unit,
    onAboutClick: () -> Unit,
    modifier: Modifier = Modifier
) {
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

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(24.dp))
                .background(Color.White.copy(alpha = 0.05f))
        ) {
            Column(modifier = Modifier.padding(vertical = 8.dp)) {
                SettingsItemPremium(Icons.Default.Settings, "Configuración", onSettingsClick)
                HorizontalDivider(
                    modifier = Modifier.padding(horizontal = 20.dp),
                    color = Color.White.copy(alpha = 0.05f)
                )
                SettingsItemPremium(Icons.Default.Info, "Sobre ShowMate", onAboutClick)
                HorizontalDivider(
                    modifier = Modifier.padding(horizontal = 20.dp),
                    color = Color.White.copy(alpha = 0.05f)
                )
                SettingsItemPremium(
                    Icons.AutoMirrored.Filled.Logout,
                    "Cerrar sesión",
                    onLogoutClick,
                    isDestructive = true
                )
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
    val tint = when {
        isDestructive -> Color(0xFFFF5252)
        isAction -> PrimaryPurpleLight
        else -> Color.White.copy(alpha = 0.75f)
    }
    val iconBg: Brush = when {
        isDestructive -> Brush.linearGradient(
            listOf(Color(0xFFFF5252).copy(alpha = 0.22f), Color(0xFFFF5252).copy(alpha = 0.08f))
        )
        isAction -> Brush.linearGradient(
            listOf(PrimaryPurple.copy(alpha = 0.30f), PrimaryPurpleDark.copy(alpha = 0.15f))
        )
        else -> Brush.linearGradient(listOf(Color.White.copy(alpha = 0.10f), Color.White.copy(alpha = 0.04f)))
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
                .size(38.dp)
                .clip(CircleShape)
                .background(iconBg),
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
            tint = Color.White.copy(alpha = 0.22f)
        )
    }
}

@Composable
private fun ProfileSkeleton() {
    val shimmer = shimmerBrush()
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .statusBarsPadding()
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(200.dp)
                .background(shimmer)
        )
        Spacer(modifier = Modifier.height(16.dp))

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            repeat(3) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(80.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(shimmer)
                )
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        Box(
            modifier = Modifier
                .padding(horizontal = 16.dp)
                .width(160.dp)
                .height(16.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(shimmer)
        )

        Spacer(modifier = Modifier.height(12.dp))

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            repeat(4) {
                Box(
                    modifier = Modifier
                        .width(90.dp)
                        .height(130.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(shimmer)
                )
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        Box(
            modifier = Modifier
                .padding(horizontal = 16.dp)
                .width(120.dp)
                .height(16.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(shimmer)
        )

        Spacer(modifier = Modifier.height(12.dp))

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            repeat(4) {
                Box(
                    modifier = Modifier
                        .width(90.dp)
                        .height(130.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(shimmer)
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        repeat(4) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 6.dp)
                    .height(52.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(shimmer)
            )
        }
    }
}

@Composable
fun AchievementsEntryButton(onClick: () -> Unit, unlockedCount: Int = 0, totalCount: Int = 0) {
    val progress = if (totalCount > 0) unlockedCount.toFloat() / totalCount else 0f
    val cardGradient = remember {
        Brush.linearGradient(
            colorStops = arrayOf(
                0f to Color(0xFF2C2010),
                0.5f to Color(0xFF1E1A0E),
                1f to Color(0xFF191612)
            )
        )
    }
    val progressGradient = remember {
        Brush.linearGradient(listOf(StarYellow, Color(0xFFFFB300)))
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp, vertical = 4.dp)
            .clip(RoundedCornerShape(20.dp))
            .background(cardGradient)
            .clickable { onClick() }
    ) {
        Box(
            modifier = Modifier
                .size(120.dp)
                .offset(x = (-20).dp, y = (-20).dp)
                .background(
                    brush = Brush.radialGradient(
                        listOf(StarYellow.copy(alpha = 0.10f), Color.Transparent)
                    ),
                    shape = CircleShape
                )
        )

        Column(modifier = Modifier.padding(horizontal = 20.dp, vertical = 18.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(42.dp)
                            .clip(RoundedCornerShape(14.dp))
                            .background(StarYellow.copy(alpha = 0.18f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Default.Stars,
                            contentDescription = null,
                            tint = StarYellow,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                    Column {
                        Text(
                            "Logros & Clasificación",
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp
                        )
                        Text(
                            "Desafíos, XP y ranking de amigos",
                            color = TextGray,
                            fontSize = 12.sp
                        )
                    }
                }
                Icon(
                    Icons.AutoMirrored.Filled.KeyboardArrowRight,
                    contentDescription = null,
                    tint = StarYellow.copy(alpha = 0.6f),
                    modifier = Modifier.size(20.dp)
                )
            }

            if (totalCount > 0) {
                Spacer(modifier = Modifier.height(14.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        "$unlockedCount de $totalCount desbloqueados",
                        color = TextGray,
                        fontSize = 11.sp
                    )
                    Text(
                        "${(progress * 100).toInt()}%",
                        color = StarYellow,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
                Spacer(modifier = Modifier.height(6.dp))
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(4.dp)
                        .clip(CircleShape)
                        .background(Color.White.copy(alpha = 0.08f))
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(progress)
                            .height(4.dp)
                            .clip(CircleShape)
                            .background(progressGradient)
                    )
                }
            }
        }
    }
}
