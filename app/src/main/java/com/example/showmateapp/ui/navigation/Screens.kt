package com.example.showmateapp.ui.navigation

import kotlinx.serialization.Serializable

sealed interface Screen {
    @Serializable object Splash : Screen
    @Serializable object Login : Screen
    @Serializable object SignUp : Screen
    @Serializable object Onboarding : Screen
    @Serializable object Swipe : Screen
    @Serializable object Main : Screen
    @Serializable object Home : Screen
    @Serializable object Search : Screen
    @Serializable object Discover : Screen
    @Serializable object Favorites : Screen
    @Serializable object Profile : Screen
    @Serializable object Settings : Screen
    @Serializable object About : Screen
    @Serializable object Stats : Screen
    @Serializable object CustomLists : Screen
    @Serializable object FriendCompare : Screen
    @Serializable data class Detail(val showId: Int, val sharedElementTag: String? = null) : Screen
}
