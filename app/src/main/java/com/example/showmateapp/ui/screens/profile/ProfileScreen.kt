package com.example.showmateapp.ui.screens.profile

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.example.showmateapp.R
import com.example.showmateapp.ui.theme.AccentBlue
import com.example.showmateapp.ui.theme.PrimaryPurple
import com.example.showmateapp.ui.theme.ShowMateAppTheme
import com.example.showmateapp.ui.theme.SurfaceDark
import com.example.showmateapp.ui.theme.TextGray

@Composable
fun ProfileScreen(
    globalNavController: NavController,
    viewModel: ProfileViewModel = hiltViewModel()
) {
    val userName by viewModel.userEmail.collectAsState()
    val favoritesCount by viewModel.favoritesCount.collectAsState()

    LaunchedEffect(Unit) {
        viewModel.loadProfileData()
    }

    ProfileScreenContent(
        userName = userName,
        favoritesCount = favoritesCount,
        onSettingsClick = { globalNavController.navigate("settings") },
        onLogoutClick = { /* Handle logout */ }
    )
}

data class FavoriteShow(val title: String, val genres: String, val rating: String, val imageUrl: String)
data class RecentlyWatchedShow(val title: String, val episode: String, val progress: Float, val imageUrl: String)

@Composable
fun ProfileScreenContent(
    userName: String,
    favoritesCount: Int,
    onSettingsClick: () -> Unit,
    onLogoutClick: () -> Unit = {}
) {
    val favoriteShows = listOf(
        FavoriteShow("Stranger Things", "Sci-Fi, Horror", "9.8", "https://image.tmdb.org/t/p/w500/49WpT9UwiW9G97P3J2Gqdn9Yp9u.jpg"),
        FavoriteShow("Breaking Bad", "Crime, Drama", "9.5", "https://image.tmdb.org/t/p/w500/ggws398pU490Yf68vS7YmS083uO.jpg"),
        FavoriteShow("Black Mirror", "Sci-Fi, Anthology", "8.8", "https://image.tmdb.org/t/p/w500/7pEncl96Y9O99CidpCsh0HnS0O7.jpg")
    )

    val recentlyWatched = listOf(
        RecentlyWatchedShow("The Last of Us", "S1 : E5 \"Endure and Survive\"", 0.7f, "https://image.tmdb.org/t/p/w500/uKVH59BqV9792oQrqQ6vQ76977m.jpg"),
        RecentlyWatchedShow("Severance", "S1 : E8 \"What's for Dinner?\"", 0.4f, "https://image.tmdb.org/t/p/w500/8X99v09vH7X8oH6f7o9y6O7z7fG.jpg")
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // Sophisticated Background Gradient
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(420.dp)
                .background(
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            PrimaryPurple.copy(alpha = 0.12f),
                            AccentBlue.copy(alpha = 0.05f),
                            Color.Transparent
                        )
                    )
                )
        )

        // Floating Settings Icon (Premium feel)
        Surface(
            modifier = Modifier
                .statusBarsPadding()
                .padding(16.dp)
                .align(Alignment.TopEnd)
                .size(44.dp),
            shape = CircleShape,
            color = Color.White.copy(alpha = 0.08f),
            tonalElevation = 0.dp
        ) {
            IconButton(onClick = onSettingsClick) {
                Icon(
                    imageVector = Icons.Default.Settings,
                    contentDescription = stringResource(R.string.profile_account_settings),
                    tint = Color.White,
                    modifier = Modifier.size(22.dp)
                )
            }
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
        ) {
            Spacer(modifier = Modifier.height(80.dp))

            // User Header
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(contentAlignment = Alignment.BottomEnd) {
                    // Outer Ring with Sweep Gradient
                    Box(
                        modifier = Modifier
                            .size(118.dp)
                            .border(
                                width = 2.dp,
                                brush = Brush.sweepGradient(
                                    listOf(PrimaryPurple, AccentBlue, PrimaryPurple)
                                ),
                                shape = CircleShape
                            )
                            .padding(6.dp)
                            .clip(CircleShape)
                            .background(Color.White.copy(alpha = 0.05f))
                            .shadow(20.dp, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = userName.take(1).uppercase(),
                            color = Color.White,
                            fontSize = 44.sp,
                            fontWeight = FontWeight.Black
                        )
                    }
                    
                    // Edit Badge
                    Surface(
                        modifier = Modifier
                            .size(36.dp)
                            .offset(x = (-2).dp, y = (-2).dp),
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.primary,
                        shadowElevation = 10.dp
                    ) {
                        IconButton(onClick = { /* TODO: Edit profile */ }) {
                            Icon(
                                imageVector = Icons.Default.Edit,
                                contentDescription = "Editar perfil",
                                tint = Color.White,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }
                
                Spacer(modifier = Modifier.height(24.dp))
                
                Text(
                    text = userName,
                    color = Color.White,
                    fontSize = 32.sp,
                    fontWeight = FontWeight.ExtraBold,
                    letterSpacing = (-1).sp
                )
                
                Spacer(modifier = Modifier.height(8.dp))
                
                // Professional Status Badge
                Surface(
                    shape = RoundedCornerShape(100.dp),
                    color = AccentBlue.copy(alpha = 0.15f),
                    border = BorderStroke(1.dp, AccentBlue.copy(alpha = 0.25f))
                ) {
                    Text(
                        text = if (favoritesCount > 5) stringResource(R.string.profile_expert).uppercase() else stringResource(R.string.profile_beginner).uppercase(),
                        color = AccentBlue,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Black,
                        letterSpacing = 1.5.sp,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(36.dp))

            // Stats Section (Glassmorphism card)
            Surface(
                modifier = Modifier
                    .padding(horizontal = 24.dp)
                    .fillMaxWidth(),
                shape = RoundedCornerShape(32.dp),
                color = SurfaceDark.copy(alpha = 0.8f),
                border = BorderStroke(1.dp, Color.White.copy(alpha = 0.1f))
            ) {
                Row(
                    modifier = Modifier.padding(vertical = 28.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    ProfileStat(
                        value = favoritesCount.toString(),
                        label = stringResource(R.string.profile_favorites)
                    )
                    Box(modifier = Modifier.width(1.dp).height(40.dp).background(Color.White.copy(alpha = 0.05f)))
                    ProfileStat(
                        value = "${favoritesCount * 2}h",
                        label = stringResource(R.string.profile_time)
                    )
                    Box(modifier = Modifier.width(1.dp).height(40.dp).background(Color.White.copy(alpha = 0.05f)))
                    ProfileStat(
                        value = "0",
                        label = stringResource(R.string.profile_reviews)
                    )
                }
            }

            Spacer(modifier = Modifier.height(48.dp))

            // Favorites Section
            Column(modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = stringResource(R.string.profile_favorites_title),
                        color = Color.White,
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = (-0.5).sp
                    )
                    TextButton(onClick = { /* TODO */ }) {
                        Text(
                            text = stringResource(R.string.profile_see_all),
                            color = PrimaryPurple,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.ExtraBold
                        )
                    }
                }
                
                Spacer(modifier = Modifier.height(12.dp))
                
                LazyRow(
                    contentPadding = PaddingValues(horizontal = 24.dp),
                    horizontalArrangement = Arrangement.spacedBy(20.dp)
                ) {
                    items(favoriteShows) { show ->
                        FavoriteItem(show)
                    }
                }
            }

            Spacer(modifier = Modifier.height(44.dp))

            // Recently Watched
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp)
            ) {
                Text(
                    text = stringResource(R.string.profile_recently_watched),
                    color = Color.White,
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = (-0.5).sp,
                    modifier = Modifier.padding(bottom = 20.dp)
                )
                
                recentlyWatched.forEach { show ->
                    RecentlyWatchedItem(show)
                    Spacer(modifier = Modifier.height(16.dp))
                }
            }

            Spacer(modifier = Modifier.height(40.dp))

            // Settings & More
            Text(
                text = stringResource(R.string.profile_general).uppercase(),
                color = Color.White.copy(alpha = 0.4f),
                fontSize = 12.sp,
                fontWeight = FontWeight.Black,
                letterSpacing = 2.sp,
                modifier = Modifier.padding(bottom = 16.dp, start = 32.dp)
            )

            Surface(
                modifier = Modifier
                    .padding(horizontal = 24.dp)
                    .fillMaxWidth(),
                shape = RoundedCornerShape(28.dp),
                color = SurfaceDark,
                border = BorderStroke(1.dp, Color.White.copy(alpha = 0.05f))
            ) {
                Column {
                    ProfileActionButton(
                        icon = Icons.AutoMirrored.Filled.List,
                        iconColor = Color(0xFFFFB74D),
                        title = stringResource(R.string.profile_watchlist),
                        onClick = { /* TODO */ }
                    )
                    HorizontalDivider(color = Color.White.copy(alpha = 0.05f), modifier = Modifier.padding(horizontal = 24.dp))
                    ProfileActionButton(
                        icon = Icons.Default.Info,
                        iconColor = Color(0xFF81C784),
                        title = stringResource(R.string.profile_help_support),
                        onClick = { /* TODO */ }
                    )
                }
            }

            Spacer(modifier = Modifier.height(56.dp))

            // Professional Logout Button
            TextButton(
                onClick = onLogoutClick,
                modifier = Modifier
                    .padding(horizontal = 24.dp)
                    .fillMaxWidth()
                    .height(64.dp)
                    .background(Color(0xFFFF4B4B).copy(alpha = 0.08f), RoundedCornerShape(20.dp)),
                colors = ButtonDefaults.textButtonColors(contentColor = Color(0xFFFF4B4B))
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ExitToApp,
                    contentDescription = null,
                    modifier = Modifier.size(22.dp)
                )
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = stringResource(R.string.profile_sign_out),
                    fontWeight = FontWeight.Black,
                    fontSize = 16.sp,
                    letterSpacing = 0.5.sp
                )
            }
            
            Spacer(modifier = Modifier.height(80.dp))
        }
    }
}

@Composable
fun FavoriteItem(show: FavoriteShow) {
    Column(modifier = Modifier.width(155.dp)) {
        Box(
            modifier = Modifier
                .shadow(15.dp, RoundedCornerShape(24.dp))
                .clip(RoundedCornerShape(24.dp))
        ) {
            AsyncImage(
                model = show.imageUrl,
                contentDescription = show.title,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(230.dp),
                contentScale = ContentScale.Crop
            )
            // Rating Badge (High fidelity)
            Surface(
                modifier = Modifier
                    .padding(12.dp)
                    .align(Alignment.TopEnd),
                shape = RoundedCornerShape(12.dp),
                color = Color.Black.copy(alpha = 0.8f),
                border = BorderStroke(1.dp, Color.White.copy(alpha = 0.2f))
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Star,
                        contentDescription = null,
                        tint = Color(0xFFFFC107),
                        modifier = Modifier.size(12.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = show.rating,
                        color = Color.White,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Black
                    )
                }
            }
        }
        Spacer(modifier = Modifier.height(14.dp))
        Text(
            text = show.title,
            color = Color.White,
            fontSize = 16.sp,
            fontWeight = FontWeight.ExtraBold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        Text(
            text = show.genres,
            color = TextGray,
            fontSize = 13.sp,
            fontWeight = FontWeight.Medium,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
fun RecentlyWatchedItem(show: RecentlyWatchedShow) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(28.dp),
        color = SurfaceDark.copy(alpha = 0.6f),
        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.05f))
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            AsyncImage(
                model = show.imageUrl,
                contentDescription = show.title,
                modifier = Modifier
                    .size(76.dp)
                    .clip(RoundedCornerShape(18.dp)),
                contentScale = ContentScale.Crop
            )
            
            Spacer(modifier = Modifier.width(20.dp))
            
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = show.title,
                    color = Color.White,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = show.episode,
                    color = TextGray,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(modifier = Modifier.height(10.dp))
                // High fidelity progress bar
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(6.dp)
                        .clip(CircleShape)
                        .background(Color.White.copy(alpha = 0.08f))
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(show.progress)
                            .fillMaxHeight()
                            .background(
                                brush = Brush.linearGradient(
                                    colors = listOf(PrimaryPurple, AccentBlue)
                                )
                            )
                    )
                }
            }
            
            Spacer(modifier = Modifier.width(16.dp))
            
            Surface(
                modifier = Modifier.size(44.dp),
                shape = CircleShape,
                color = PrimaryPurple.copy(alpha = 0.15f),
                border = BorderStroke(1.dp, PrimaryPurple.copy(alpha = 0.25f))
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.Default.PlayArrow,
                        contentDescription = "Play",
                        tint = PrimaryPurple,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun ProfileStat(value: String, label: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = value,
            color = Color.White,
            fontSize = 28.sp,
            fontWeight = FontWeight.Black,
            letterSpacing = (-0.5).sp
        )
        Text(
            text = label.uppercase(),
            color = TextGray,
            fontSize = 10.sp,
            fontWeight = FontWeight.Black,
            letterSpacing = 1.6.sp
        )
    }
}

@Composable
fun ProfileActionButton(
    icon: ImageVector,
    iconColor: Color,
    title: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(22.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(48.dp)
                .clip(RoundedCornerShape(16.dp))
                .background(iconColor.copy(alpha = 0.12f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = iconColor,
                modifier = Modifier.size(24.dp)
            )
        }
        Spacer(modifier = Modifier.width(20.dp))
        Text(
            text = title,
            color = Color.White,
            fontSize = 17.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.weight(1f)
        )
        Icon(
            imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
            contentDescription = null,
            tint = TextGray.copy(alpha = 0.3f),
            modifier = Modifier.size(26.dp)
        )
    }
}

@Preview(showBackground = true)
@Composable
fun ProfileScreenPreview() {
    ShowMateAppTheme {
        ProfileScreenContent(
            userName = "Andrea",
            favoritesCount = 12,
            onSettingsClick = {}
        )
    }
}
