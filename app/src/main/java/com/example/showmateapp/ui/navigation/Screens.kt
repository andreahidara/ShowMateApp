package com.example.showmateapp.ui.navigation

import kotlinx.serialization.Serializable

sealed interface Screen {
    @Serializable object Splash : Screen
    @Serializable object Login : Screen
    @Serializable object SignUp : Screen
    @Serializable object Onboarding : Screen
    @Serializable object Swipe : Screen
    @Serializable object Main : Screen
    @Serializable object Settings : Screen
    @Serializable data class Detail(val showId: Int) : Screen
}
