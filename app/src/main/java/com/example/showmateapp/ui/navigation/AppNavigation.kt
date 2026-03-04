package com.example.showmateapp.ui.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.showmateapp.ui.screens.detail.DetailScreen
import com.example.showmateapp.ui.screens.login.LoginScreen
import com.example.showmateapp.ui.screens.main.MainScreen
import com.example.showmateapp.ui.screens.onboarding.OnboardingScreen
import com.example.showmateapp.ui.screens.profile.settings.SettingsScreen
import com.example.showmateapp.ui.screens.splash.SplashScreen
import com.example.showmateapp.ui.screens.swipe.SwipeScreen
import java.net.URLDecoder
import java.nio.charset.StandardCharsets

@Composable
fun AppNavigation() {
    val navController = rememberNavController()

    NavHost(
        navController = navController,
        startDestination = "splash"
    ) {
        composable("splash") { SplashScreen(navController = navController) }
        composable("login") { LoginScreen(navController = navController) }
        composable("onboarding") { OnboardingScreen(navController = navController) }

        composable(
            route = "swipe/{genres}",
            arguments = listOf(navArgument("genres") { type = NavType.StringType })
        ) { backStackEntry ->
            val names = backStackEntry.arguments?.getString("genres") ?: ""

            val genreMap = mapOf(
                "Sci-Fi" to "10765",
                "Detective" to "80",
                "Comedy" to "35",
                "Thriller" to "53",
                "Documentary" to "99",
                "Animation" to "16",
                "Fantasy" to "10765",
                "Drama" to "18"
            )

            val ids = names.split(",")
                .mapNotNull { genreMap[it.trim()] }
                .joinToString(",")

            SwipeScreen(navController = navController, selectedGenres = ids)
        }

        // MainScreen hosts the bottom nav with Home, Search, Discover, Favorites, Profile
        composable("main") { MainScreen(globalNavController = navController) }

        // Settings (navigated from ProfileScreen)
        composable("settings") { SettingsScreen(navController = navController) }

        composable(
            route = "detail/{showId}",
            arguments = listOf(navArgument("showId") { type = NavType.IntType })
        ) { backStackEntry ->
            val showId = backStackEntry.arguments?.getInt("showId") ?: 0
            DetailScreen(navController = navController, showId = showId)
        }
    }
}
