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
import com.example.showmateapp.ui.screens.profile.settings.SettingsScreen
import com.example.showmateapp.ui.screens.splash.SplashScreen
import com.example.showmateapp.ui.screens.swipe.SwipeScreen
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
                SplashScreen(onTimeout = {
                    navController.navigate(Screen.Login) { 
                        popUpTo<Screen.Splash> { inclusive = true } 
                    }
                })
            }
            composable<Screen.Login> { LoginScreen(navController = navController) }
            composable<Screen.SignUp> { SignUpScreen(navController = navController) }
            composable<Screen.Onboarding> {
                OnboardingScreen(onFinish = {
                    navController.navigate(Screen.Swipe) { 
                        popUpTo<Screen.Onboarding> { inclusive = true } 
                    }
                })
            }

            composable<Screen.Swipe> {
                SwipeScreen(navController = navController)
            }

            composable<Screen.Main> { 
                MainScreen(
                    globalNavController = navController,
                    sharedTransitionScope = this@SharedTransitionLayout,
                    animatedVisibilityScope = this@composable
                ) 
            }
            composable<Screen.Settings> { SettingsScreen(navController = navController) }

            composable<Screen.Detail> { backStackEntry ->
                val detailRoute = backStackEntry.toRoute<Screen.Detail>()
                DetailScreen(
                    navController = navController, 
                    showId = detailRoute.showId,
                    sharedTransitionScope = this@SharedTransitionLayout,
                    animatedVisibilityScope = this@composable
                )
            }
        }
    }
}
