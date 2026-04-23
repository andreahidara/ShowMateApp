package com.andrea.showmateapp.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Whatshot
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.currentBackStackEntryAsState
import com.andrea.showmateapp.ui.navigation.Screen
import com.andrea.showmateapp.ui.theme.PrimaryPurple
import com.andrea.showmateapp.ui.theme.PrimaryPurpleLight
import com.andrea.showmateapp.ui.theme.SurfaceDark
import com.andrea.showmateapp.ui.theme.TextGray

@Composable
fun BottomNavBar(navController: NavController, onScrollToTop: (Any) -> Unit = {}, friendsBadgeCount: Int = 0) {
    val items = listOf(
        BottomNavItem(Screen.Home, "Inicio", Icons.Default.Home),
        BottomNavItem(Screen.Search, "Buscar", Icons.Default.Search),
        BottomNavItem(Screen.Discover, "Descubrir", Icons.Default.Whatshot),
        BottomNavItem(Screen.Friends, "Amigos", Icons.Default.Group),
        BottomNavItem(Screen.Profile, "Perfil", Icons.Default.Person)
    )

    var lastClickedRoute by remember { mutableStateOf<Any?>(null) }
    var lastClickTime by remember { mutableLongStateOf(0L) }

    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry?.destination

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .padding(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 12.dp)
    ) {
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(28.dp),
            color = SurfaceDark,
            shadowElevation = 20.dp,
            border = BorderStroke(1.dp, PrimaryPurple.copy(alpha = 0.25f))
        ) {
            Column {
                HorizontalDivider(
                    modifier = Modifier.padding(horizontal = 20.dp),
                    thickness = 1.dp,
                    color = Color.White.copy(alpha = 0.05f)
                )
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(64.dp)
                        .padding(horizontal = 4.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    items.forEach { item ->
                        val isSelected = currentDestination?.hierarchy
                            ?.any { it.hasRoute(item.route::class) } == true

                        val iconScale by animateFloatAsState(
                            targetValue = if (isSelected) 1.18f else 1f,
                            animationSpec = spring(dampingRatio = 0.5f, stiffness = 400f),
                            label = "iconScale"
                        )

                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxHeight()
                                .clickable(
                                    interactionSource = remember { MutableInteractionSource() },
                                    indication = null
                                ) {
                                    val now = System.currentTimeMillis()
                                    if (isSelected && lastClickedRoute == item.route && now - lastClickTime < 400L) {
                                        onScrollToTop(item.route)
                                    }
                                    lastClickedRoute = item.route
                                    lastClickTime = now
                                    navController.navigate(item.route) {
                                        popUpTo(navController.graph.findStartDestination().id) {
                                            saveState = true
                                        }
                                        launchSingleTop = true
                                        restoreState = true
                                    }
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center
                            ) {
                                val showBadge = item.route is Screen.Friends && friendsBadgeCount > 0
                                BadgedBox(
                                    badge = {
                                        if (showBadge) {
                                            Badge(
                                                containerColor = PrimaryPurple,
                                                contentColor = Color.White
                                            ) {
                                                Text(
                                                    text = if (friendsBadgeCount > 9) {
                                                        "9+"
                                                    } else {
                                                        friendsBadgeCount.toString()
                                                    },
                                                    fontSize = 9.sp,
                                                    fontWeight = FontWeight.Bold
                                                )
                                            }
                                        }
                                    }
                                ) {
                                    val isDiscover = item.route == Screen.Discover
                                    Box(
                                        contentAlignment = Alignment.Center,
                                        modifier = Modifier
                                            .size(if (isDiscover) 48.dp else 42.dp)
                                            .clip(RoundedCornerShape(if (isDiscover) 24.dp else 14.dp))
                                            .background(
                                                if (isDiscover) {
                                                    Brush.linearGradient(
                                                        listOf(
                                                            PrimaryPurple,
                                                            PrimaryPurpleLight
                                                        )
                                                    )
                                                } else if (isSelected) {
                                                    Brush.linearGradient(
                                                        listOf(
                                                            PrimaryPurple.copy(alpha = 0.30f),
                                                            PrimaryPurpleLight.copy(alpha = 0.15f)
                                                        )
                                                    )
                                                } else {
                                                    Brush.linearGradient(
                                                        listOf(Color.Transparent, Color.Transparent)
                                                    )
                                                }
                                            )
                                    ) {
                                        Icon(
                                            imageVector = item.icon,
                                            contentDescription = item.title,
                                            modifier = Modifier
                                                .size(if (isDiscover) 28.dp else 22.dp)
                                                .scale(if (isDiscover) 1f else iconScale),
                                            tint = if (isDiscover) Color.White else if (isSelected) PrimaryPurple else TextGray
                                        )
                                    }
                                }

                                AnimatedVisibility(
                                    visible = isSelected,
                                    enter = fadeIn() + expandVertically(expandFrom = Alignment.Top),
                                    exit = fadeOut() + shrinkVertically(shrinkTowards = Alignment.Top)
                                ) {
                                    Text(
                                        text = item.title,
                                        style = MaterialTheme.typography.labelSmall,
                                        color = PrimaryPurple,
                                        fontWeight = FontWeight.SemiBold,
                                        fontSize = 10.sp,
                                        modifier = Modifier.padding(top = 2.dp)
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

data class BottomNavItem(
    val route: Any,
    val title: String,
    val icon: androidx.compose.ui.graphics.vector.ImageVector
)
