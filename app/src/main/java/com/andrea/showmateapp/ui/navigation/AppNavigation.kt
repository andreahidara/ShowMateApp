package com.andrea.showmateapp.ui.navigation

import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionLayout
import androidx.compose.runtime.Composable
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navDeepLink
import androidx.navigation.toRoute
import com.andrea.showmateapp.ui.screens.actor.ActorScreen
import com.andrea.showmateapp.ui.screens.consent.ConsentScreen
import com.andrea.showmateapp.ui.screens.detail.DetailScreen
import com.andrea.showmateapp.ui.screens.friends.GroupMatchScreen
import com.andrea.showmateapp.ui.screens.friends.GroupNightsScreen
import com.andrea.showmateapp.ui.screens.friends.sharedlist.CollabListScreen
import com.andrea.showmateapp.ui.screens.friends.sharedlist.SharedListsScreen
import com.andrea.showmateapp.ui.screens.login.LoginScreen
import com.andrea.showmateapp.ui.screens.main.MainScreen
import com.andrea.showmateapp.ui.screens.onboarding.OnboardingScreen
import com.andrea.showmateapp.ui.screens.profile.about.AboutScreen
import com.andrea.showmateapp.ui.screens.profile.allshows.AllShowsScreen
import com.andrea.showmateapp.ui.screens.profile.friends.FriendCompareScreen
import com.andrea.showmateapp.ui.screens.profile.lists.CustomListsScreen
import com.andrea.showmateapp.ui.screens.profile.lists.ListDetailScreen
import com.andrea.showmateapp.ui.screens.profile.settings.SettingsScreen
import com.andrea.showmateapp.ui.screens.signup.SignUpScreen
import com.andrea.showmateapp.ui.screens.splash.SplashScreen
import com.andrea.showmateapp.ui.screens.stats.AchievementsScreen
import com.andrea.showmateapp.ui.screens.stats.StatsScreen
import com.andrea.showmateapp.ui.screens.welcome.WelcomeScreen

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
                    },
                    onNavigateToOnboarding = {
                        navController.navigate(Screen.Onboarding) {
                            popUpTo(Screen.Splash) { inclusive = true }
                        }
                    }
                )
            }
            composable<Screen.Welcome> {
                WelcomeScreen(onGetStarted = {
                    navController.navigate(Screen.Consent)
                })
            }
            composable<Screen.Consent> {
                ConsentScreen(onAccepted = {
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
                com.andrea.showmateapp.ui.screens.swipe.SwipeScreen(
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
            composable<Screen.FriendCompare> { backStackEntry ->
                val route = backStackEntry.toRoute<Screen.FriendCompare>()
                FriendCompareScreen(navController = navController, initialFriendEmail = route.friendEmail)
            }
            composable<Screen.GroupMatch> { backStackEntry ->
                val route = backStackEntry.toRoute<Screen.GroupMatch>()
                val emails = route.memberEmails.split(",").filter { it.isNotBlank() }
                GroupMatchScreen(navController = navController, memberEmails = emails)
            }

            composable<Screen.GroupNights> {
                GroupNightsScreen(navController = navController)
            }

            composable<Screen.Achievements> {
                AchievementsScreen(navController = navController)
            }

            composable<Screen.Actor> { backStackEntry ->
                val route = backStackEntry.toRoute<Screen.Actor>()
                ActorScreen(
                    navController = navController,
                    personId = route.personId,
                    personName = route.personName
                )
            }

            composable<Screen.SharedLists> {
                SharedListsScreen(navController = navController)
            }

            composable<Screen.CollabList> { backStackEntry ->
                val route = backStackEntry.toRoute<Screen.CollabList>()
                CollabListScreen(navController = navController, listId = route.listId)
            }

            composable<Screen.Detail>(
                deepLinks = listOf(navDeepLink { uriPattern = "showmate://detail/{showId}" })
            ) { backStackEntry ->
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
