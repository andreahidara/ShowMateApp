package com.example.showmateapp.ui.screens.main

import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.showmateapp.ui.components.BottomNavBar
import com.example.showmateapp.ui.screens.discover.DiscoverScreen
import com.example.showmateapp.ui.screens.favorites.FavoritesScreen
import com.example.showmateapp.ui.screens.home.HomeScreen
import com.example.showmateapp.ui.screens.profile.ProfileScreen
import com.example.showmateapp.ui.screens.search.SearchScreen

@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
fun MainScreen(
    globalNavController: NavController,
    sharedTransitionScope: SharedTransitionScope,
    animatedVisibilityScope: AnimatedVisibilityScope
) {
    val bottomNavController = rememberNavController()

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        contentColor = MaterialTheme.colorScheme.onBackground,
        bottomBar = {
            BottomNavBar(navController = bottomNavController)
        }
    ) { paddingValues ->
        NavHost(
            navController = bottomNavController,
            startDestination = "home",
            modifier = Modifier.padding(paddingValues)
        ) {
            composable("home") { 
                HomeScreen(
                    navController = globalNavController,
                    sharedTransitionScope = sharedTransitionScope,
                    animatedVisibilityScope = animatedVisibilityScope
                ) 
            }
            composable("search") { 
                SearchScreen(
                    globalNavController = globalNavController,
                    sharedTransitionScope = sharedTransitionScope,
                    animatedVisibilityScope = animatedVisibilityScope
                ) 
            }
            composable("discover") { 
                DiscoverScreen(
                    globalNavController = globalNavController,
                    sharedTransitionScope = sharedTransitionScope,
                    animatedVisibilityScope = animatedVisibilityScope
                ) 
            }
            composable("favorites") { 
                FavoritesScreen(
                    globalNavController = globalNavController,
                    sharedTransitionScope = sharedTransitionScope,
                    animatedVisibilityScope = animatedVisibilityScope
                ) 
            }
            composable("profile") { 
                ProfileScreen(globalNavController = globalNavController) 
            }
        }
    }
}
