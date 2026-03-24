package com.example.showmateapp.ui.screens.main

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.showmateapp.ui.navigation.Screen
import com.example.showmateapp.ui.components.BottomNavBar
import com.example.showmateapp.ui.screens.discover.DiscoverScreen
import com.example.showmateapp.ui.screens.friends.FriendsScreen
import com.example.showmateapp.ui.screens.home.HomeScreen
import com.example.showmateapp.ui.screens.profile.ProfileScreen
import com.example.showmateapp.ui.screens.search.SearchScreen

@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
fun MainScreen(
    globalNavController: NavController,
    sharedTransitionScope: SharedTransitionScope,
    animatedVisibilityScope: AnimatedVisibilityScope,
    viewModel: MainScreenViewModel = hiltViewModel()
) {
    val isOnline by viewModel.networkMonitor.isOnline.collectAsState(initial = true)
    val isLoggedIn by viewModel.authRepository.authState.collectAsState(initial = true)

    LaunchedEffect(isLoggedIn) {
        if (!isLoggedIn) {
            globalNavController.navigate(com.example.showmateapp.ui.navigation.Screen.Welcome) {
                popUpTo(0) { inclusive = true }
            }
        }
    }

    val bottomNavController = rememberNavController()

    var homeScrollTrigger by remember { mutableIntStateOf(0) }
    var searchScrollTrigger by remember { mutableIntStateOf(0) }
    var profileScrollTrigger by remember { mutableIntStateOf(0) }

    Box(modifier = Modifier.fillMaxSize()) {
    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        contentColor = MaterialTheme.colorScheme.onBackground,
        bottomBar = {
            BottomNavBar(
                navController = bottomNavController,
                onScrollToTop = { route ->
                    when (route) {
                        is Screen.Home    -> homeScrollTrigger++
                        is Screen.Search  -> searchScrollTrigger++
                        is Screen.Profile -> profileScrollTrigger++
                        else -> {}
                    }
                }
            )
        }
    ) { paddingValues ->
        NavHost(
            navController = bottomNavController,
            startDestination = Screen.Home,
            modifier = Modifier.padding(paddingValues)
        ) {
            composable<Screen.Home> {
                HomeScreen(
                    navController = globalNavController,
                    sharedTransitionScope = sharedTransitionScope,
                    animatedVisibilityScope = animatedVisibilityScope,
                    scrollToTopTrigger = homeScrollTrigger
                )
            }
            composable<Screen.Search> {
                SearchScreen(
                    globalNavController = globalNavController,
                    sharedTransitionScope = sharedTransitionScope,
                    animatedVisibilityScope = animatedVisibilityScope,
                    scrollToTopTrigger = searchScrollTrigger
                )
            }
            composable<Screen.Discover> {
                DiscoverScreen(
                    globalNavController = globalNavController,
                    sharedTransitionScope = sharedTransitionScope,
                    animatedVisibilityScope = animatedVisibilityScope
                )
            }
            composable<Screen.Friends> {
                FriendsScreen(globalNavController = globalNavController)
            }
            composable<Screen.Profile> {
                ProfileScreen(
                    globalNavController = globalNavController,
                    scrollToTopTrigger = profileScrollTrigger
                )
            }
        }
    }

    AnimatedVisibility(
        visible = !isOnline,
        enter = slideInVertically { -it } + fadeIn(),
        exit = slideOutVertically { -it } + fadeOut(),
        modifier = Modifier
            .align(Alignment.TopCenter)
            .zIndex(10f)
            .fillMaxWidth()
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color(0xFFFFC107))
                .statusBarsPadding()
                .padding(vertical = 6.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "Sin conexión a internet",
                color = Color.Black,
                fontSize = 13.sp
            )
        }
    }
    }
}
