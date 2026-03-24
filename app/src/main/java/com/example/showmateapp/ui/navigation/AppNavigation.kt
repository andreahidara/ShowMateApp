package com.example.showmateapp.ui.navigation

import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionLayout
import androidx.compose.runtime.Composable
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.toRoute
import com.example.showmateapp.ui.screens.detail.DetailScreen
import com.example.showmateapp.ui.screens.login.LoginScreen
import com.example.showmateapp.ui.screens.main.MainScreen
import com.example.showmateapp.ui.screens.onboarding.OnboardingScreen
import com.example.showmateapp.ui.screens.welcome.WelcomeScreen
import com.example.showmateapp.ui.screens.profile.about.AboutScreen
import com.example.showmateapp.ui.screens.friends.GroupMatchScreen
import com.example.showmateapp.ui.screens.profile.friends.FriendCompareScreen
import com.example.showmateapp.ui.screens.profile.allshows.AllShowsScreen
import com.example.showmateapp.ui.screens.profile.lists.CustomListsScreen
import com.example.showmateapp.ui.screens.profile.lists.ListDetailScreen
import com.example.showmateapp.ui.screens.profile.settings.SettingsScreen
import com.example.showmateapp.ui.screens.stats.StatsScreen
import com.example.showmateapp.ui.screens.splash.SplashScreen
import com.example.showmateapp.ui.screens.signup.SignUpScreen

@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
fun AppNavigation() {
    val navController = rememberNavController()

    SharedTransitionLayout {
        NavHost(
            navController = navController,
            startDestination = Screen.Splash
        ) {
            composable<Screen.Splash> {
                SplashScreen(
                    onNavigateToMain = {
                        navController.navigate(Screen.Main) {
                            popUpTo(Screen.Splash) { inclusive = true }
                        }
                    },
                    onNavigateToLogin = {
                        navController.navigate(Screen.Welcome) {
                            popUpTo(Screen.Splash) { inclusive = true }
                        }
                    }
                )
            }
            composable<Screen.Welcome> {
                WelcomeScreen(onGetStarted = {
                    navController.navigate(Screen.Login) {
                        popUpTo(Screen.Welcome) { inclusive = true }
                    }
                })
            }
            composable<Screen.Login> { LoginScreen(navController = navController) }
            composable<Screen.SignUp> {
                SignUpScreen(
                    navController = navController,
                    onSignUpSuccess = {
                        navController.navigate(Screen.Onboarding) {
                            popUpTo(Screen.Login) { inclusive = true }
                        }
                    }
                )
            }
            composable<Screen.Onboarding> {
                OnboardingScreen(onFinish = {
                    navController.navigate(Screen.Swipe) {
                        popUpTo(Screen.Onboarding) { inclusive = true }
                    }
                })
            }

            composable<Screen.Swipe> {
                com.example.showmateapp.ui.screens.swipe.SwipeScreen(
                    navController = navController
                )
            }

            composable<Screen.Main> { 
                MainScreen(
                    globalNavController = navController,
                    sharedTransitionScope = this@SharedTransitionLayout,
                    animatedVisibilityScope = this@composable
                ) 
            }
            composable<Screen.Settings> { SettingsScreen(navController = navController) }
            composable<Screen.About> { AboutScreen(navController = navController) }
            composable<Screen.Stats> { StatsScreen(navController = navController) }
            composable<Screen.CustomLists> { CustomListsScreen(navController = navController) }
            composable<Screen.ListDetail> { ListDetailScreen(navController = navController) }
            composable<Screen.AllShows> { AllShowsScreen(navController = navController) }
            composable<Screen.FriendCompare> { FriendCompareScreen(navController = navController) }
            composable<Screen.GroupMatch> { backStackEntry ->
                val route = backStackEntry.toRoute<Screen.GroupMatch>()
                val emails = route.memberEmails.split(",").filter { it.isNotBlank() }
                GroupMatchScreen(navController = navController, memberEmails = emails)
            }

            composable<Screen.Detail> { backStackEntry ->
                val detailRoute = backStackEntry.toRoute<Screen.Detail>()
                DetailScreen(
                    navController = navController, 
                    showId = detailRoute.showId,
                    sharedElementTag = detailRoute.sharedElementTag,
                    sharedTransitionScope = this@SharedTransitionLayout,
                    animatedVisibilityScope = this@composable
                )
            }
        }
    }
}
