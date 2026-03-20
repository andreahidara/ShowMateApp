package com.example.showmateapp.ui.screens.main

import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
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
    animatedVisibilityScope: AnimatedVisibilityScope
) {
    val bottomNavController = rememberNavController()

    var homeScrollTrigger by remember { mutableIntStateOf(0) }
    var searchScrollTrigger by remember { mutableIntStateOf(0) }
    var profileScrollTrigger by remember { mutableIntStateOf(0) }

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
}
