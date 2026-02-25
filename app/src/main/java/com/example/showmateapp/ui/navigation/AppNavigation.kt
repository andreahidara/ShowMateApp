package com.example.showmateapp.ui.navigation

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.showmateapp.ui.components.BottomNavBar
import com.example.showmateapp.ui.screens.login.LoginScreen
import com.example.showmateapp.ui.screens.onboarding.OnboardingScreen
import com.example.showmateapp.ui.screens.splash.SplashScreen
import com.example.showmateapp.ui.screens.swipe.SwipeScreen
import com.example.showmateapp.ui.screens.home.HomeScreen
import com.example.showmateapp.ui.screens.detail.DetailScreen
import java.net.URLDecoder
import java.net.URLEncoder
import java.nio.charset.StandardCharsets

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppNavigation() {
    val navController = rememberNavController()
    val navBackStackEntry = navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry.value?.destination?.route

    val showBottomBar = currentRoute in listOf("home", "swipe/{genres}")

    Scaffold(
        bottomBar = {
            if (showBottomBar) {
                BottomNavBar(navController = navController)
            }
        }
    ) { paddingValues ->
        NavHost(
            navController = navController,
            startDestination = "splash",
            modifier = Modifier.padding(paddingValues)
        ) {
            composable("splash") { SplashScreen(navController = navController) }
            composable("login") { LoginScreen(navController = navController) }
            composable("onboarding") { OnboardingScreen(navController = navController) }

            composable(
                route = "swipe/{genres}",
                arguments = listOf(navArgument("genres") { type = NavType.StringType })
            ) { backStackEntry ->
                val names = backStackEntry.arguments?.getString("genres") ?: ""

                // Mapa de traducción interno para convertir nombres a IDs de TMDB
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

            composable(
                route = "detail/{showId}?name={name}&overview={overview}&posterPath={posterPath}",
                arguments = listOf(
                    navArgument("showId") { type = NavType.IntType },
                    navArgument("name") { type = NavType.StringType; nullable = true },
                    navArgument("overview") { type = NavType.StringType; nullable = true },
                    navArgument("posterPath") { type = NavType.StringType; nullable = true }
                )
            ) { backStackEntry ->
                val showId = backStackEntry.arguments?.getInt("showId") ?: 0
                
                // Decodificamos los parámetros que vienen codificados desde HomeScreen
                val rawName = backStackEntry.arguments?.getString("name")
                val name = rawName?.let { URLDecoder.decode(it, StandardCharsets.UTF_8.toString()) }
                
                val rawOverview = backStackEntry.arguments?.getString("overview")
                val overview = rawOverview?.let { URLDecoder.decode(it, StandardCharsets.UTF_8.toString()) }
                
                val rawPosterPath = backStackEntry.arguments?.getString("posterPath")
                val posterPath = rawPosterPath?.let { URLDecoder.decode(it, StandardCharsets.UTF_8.toString()) }

                DetailScreen(
                    navController = navController,
                    showId = showId,
                    name = name,
                    overview = overview,
                    posterPath = posterPath
                )
            }
        }
    }
}
