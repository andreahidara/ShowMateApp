package com.example.showmateapp.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.currentBackStackEntryAsState
import com.example.showmateapp.ui.navigation.Screen
import com.example.showmateapp.ui.theme.PrimaryPurple
import com.example.showmateapp.ui.theme.SurfaceDark
import com.example.showmateapp.ui.theme.TextGray

@Composable
fun BottomNavBar(navController: NavController) {
    val items = listOf(
        BottomNavItem(Screen.Home, "Inicio", Icons.Default.Home),
        BottomNavItem(Screen.Search, "Buscar", Icons.Default.Search),
        BottomNavItem(Screen.Discover, "Descubrir", Icons.Default.Star),
        BottomNavItem(Screen.Favorites, "Favoritos", Icons.Default.Favorite),
        BottomNavItem(Screen.Profile, "Perfil", Icons.Default.Person)
    )

    Box {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(1.dp)
                .background(
                    brush = Brush.horizontalGradient(
                        listOf(Color.Transparent, PrimaryPurple.copy(alpha = 0.5f), Color.Transparent)
                    )
                )
        )

        NavigationBar(
            containerColor = SurfaceDark.copy(alpha = 0.95f),
            tonalElevation = 8.dp,
            modifier = Modifier.height(80.dp)
        ) {
            val navBackStackEntry by navController.currentBackStackEntryAsState()
            val currentDestination = navBackStackEntry?.destination

            items.forEach { item ->
                val isSelected = currentDestination?.hierarchy?.any { it.hasRoute(item.route::class) } == true
                
                val iconScale by animateFloatAsState(targetValue = if (isSelected) 1.2f else 1f)

                NavigationBarItem(
                    icon = {
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier
                                .width(56.dp)
                                .height(32.dp)
                                .clip(RoundedCornerShape(16.dp))
                                .background(
                                    if (isSelected) PrimaryPurple.copy(alpha = 0.15f)
                                    else Color.Transparent
                                )
                        ) {
                            Icon(
                                imageVector = item.icon,
                                contentDescription = item.title,
                                modifier = Modifier
                                    .size(22.dp)
                                    .scale(iconScale),
                                tint = if (isSelected) PrimaryPurple else TextGray
                            )
                        }
                    },
                    label = {
                        Text(
                            text = item.title,
                            style = androidx.compose.ui.text.TextStyle(
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                color = if (isSelected) PrimaryPurple else TextGray
                            )
                        )
                    },
                    selected = isSelected,
                    onClick = {
                        navController.navigate(item.route) {
                            popUpTo(navController.graph.findStartDestination().id) {
                                saveState = true
                            }
                            launchSingleTop = true
                            restoreState = true
                        }
                    },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = PrimaryPurple,
                        unselectedIconColor = TextGray,
                        selectedTextColor = PrimaryPurple,
                        unselectedTextColor = TextGray,
                        indicatorColor = Color.Transparent
                    )
                )
            }
        }
    }
}

data class BottomNavItem(
    val route: Any,
    val title: String,
    val icon: androidx.compose.ui.graphics.vector.ImageVector
)
