package com.example.showmateapp.ui.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.showmateapp.ui.screens.login.LoginScreen
import com.example.showmateapp.ui.screens.onboarding.OnboardingScreen
import com.example.showmateapp.ui.screens.splash.SplashScreen
import com.example.showmateapp.ui.screens.swipe.SwipeScreen
import com.example.showmateapp.ui.screens.home.HomeScreen

@Composable
fun AppNavigation() {
    val navController = rememberNavController()

    NavHost(navController = navController, startDestination = "splash") {
        composable("splash") { SplashScreen(navController = navController) }
        composable("login") { LoginScreen(navController = navController) }
        composable("onboarding") { OnboardingScreen(navController = navController) }

        composable(
            route = "swipe/{genres}",
            arguments = listOf(navArgument("genres") { type = NavType.StringType })
        ) { backStackEntry ->
            val names = backStackEntry.arguments?.getString("genres") ?: ""

            // Mapa de traducción interno
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

        composable("home") { HomeScreen(navController = navController) }
    }
}