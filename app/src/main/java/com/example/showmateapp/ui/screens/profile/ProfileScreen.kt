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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.example.showmateapp.data.network.MediaContent
import com.example.showmateapp.ui.navigation.Screen
import com.example.showmateapp.ui.theme.SurfaceDark
import com.example.showmateapp.ui.theme.PrimaryPurple

@Composable
fun ProfileScreen(
    globalNavController: NavController,
    viewModel: ProfileViewModel = hiltViewModel()
) {
    val userEmail by viewModel.userEmail.collectAsState()
    val favoriteShows by viewModel.favoriteShows.collectAsState()
    val favoritesCount by viewModel.favoritesCount.collectAsState()
    val totalWatchedHours by viewModel.totalWatchedHours.collectAsState()

    LaunchedEffect(Unit) {
        viewModel.loadProfileData()
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .verticalScroll(rememberScrollState())
    ) {
        ProfileHeader(userEmail)
        
        Spacer(modifier = Modifier.height(24.dp))
        
        ProfileStats(favoritesCount, totalWatchedHours)
        
        Spacer(modifier = Modifier.height(24.dp))

        FavoritesSection(
            favorites = favoriteShows,
            onShowClick = { showId -> globalNavController.navigate(Screen.Detail(showId)) },
            onViewAllClick = { /* Navigate to Favorites */ }
        )

        Spacer(modifier = Modifier.height(24.dp))

        SettingsSection(
            onSettingsClick = { globalNavController.navigate(Screen.Settings) },
            onResetClick = {
                viewModel.resetAlgorithmData(onComplete = {
                    globalNavController.navigate(Screen.Onboarding) {
                        popUpTo<Screen.Main> { inclusive = true }
                    }
                })
            },
            onLogoutClick = { 
                viewModel.logout(onSuccess = {
                    globalNavController.navigate(Screen.Login) {
                        popUpTo<Screen.Main> { inclusive = true }
                    }
                })
            }
        )
        
        Spacer(modifier = Modifier.height(80.dp))
    }
}

@Composable
fun ProfileHeader(userEmail: String) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(200.dp)
    ) {
        // Background Gradient
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(PrimaryPurple.copy(alpha = 0.5f), Color.Transparent)
                    )
                )
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(top = 40.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .size(100.dp)
                    .clip(CircleShape)
                    .border(2.dp, PrimaryPurple, CircleShape)
            ) {
                AsyncImage(
                    model = "https://via.placeholder.com/150",
                    contentDescription = "Profile Picture",
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
            }
            
            Spacer(modifier = Modifier.height(12.dp))
            
            Text(
                text = userEmail,
                color = Color.White,
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
fun ProfileStats(favoritesCount: Int, totalHours: Int) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp),
        horizontalArrangement = Arrangement.SpaceEvenly
    ) {
        StatItem(totalHours.toString(), "Horas")
        StatItem(favoritesCount.toString(), "Favoritos")
        StatItem("15", "Listas")
    }
}

@Composable
fun StatItem(count: String, label: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = count,
            color = Color.White,
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = label,
            color = Color.Gray,
            fontSize = 12.sp
        )
    }
}

@Composable
fun FavoritesSection(
    favorites: List<MediaContent>,
    onShowClick: (Int) -> Unit,
    onViewAllClick: () -> Unit
) {
    Column {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Tus Favoritos",
                color = Color.White,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "Ver todo",
                color = PrimaryPurple,
                fontSize = 14.sp,
                modifier = Modifier.clickable { onViewAllClick() }
            )
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        LazyRow(
            contentPadding = PaddingValues(horizontal = 24.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(favorites) { media ->
                FavoriteMiniCard(media, onShowClick)
            }
        }
    }
}

@Composable
fun FavoriteMiniCard(media: MediaContent, onShowClick: (Int) -> Unit) {
    Card(
        modifier = Modifier
            .width(100.dp)
            .height(150.dp)
            .clickable { onShowClick(media.id) },
        shape = RoundedCornerShape(8.dp)
    ) {
        AsyncImage(
            model = "https://image.tmdb.org/t/p/w342${media.posterPath}",
            contentDescription = media.name,
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop
        )
    }
}

@Composable
fun SettingsSection(onSettingsClick: () -> Unit, onResetClick: () -> Unit, onLogoutClick: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp)
    ) {
        Text(
            text = "Ajustes",
            color = Color.White,
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = 16.dp)
        )
        SettingsItem(
            icon = Icons.Default.Star,
            title = "Reiniciar mis gustos",
            onClick = onResetClick,
            textColor = PrimaryPurple
        )
        SettingsItem(Icons.Default.Settings, "Configuración", onSettingsClick)
        SettingsItem(Icons.AutoMirrored.Filled.List, "Mis Listas", {})
        SettingsItem(Icons.Default.Star, "Calificaciones", {})
        SettingsItem(Icons.Default.Info, "Acerca de", {})
        SettingsItem(
            icon = Icons.AutoMirrored.Filled.ExitToApp,
            title = "Cerrar Sesión",
            onClick = onLogoutClick,
            textColor = Color.Red
        )
    }
}

@Composable
fun SettingsItem(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    onClick: () -> Unit,
    textColor: Color = Color.White
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 12.dp)
            .clickable { onClick() },
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = title,
            tint = if (textColor == Color.Red) Color.Red else Color.Gray,
            modifier = Modifier.size(24.dp)
        )
        Spacer(modifier = Modifier.width(16.dp))
        Text(
            text = title,
            color = textColor,
            fontSize = 16.sp,
            modifier = Modifier.weight(1f)
        )
        Icon(
            imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
            contentDescription = null,
            tint = Color.Gray
        )
    }
}
