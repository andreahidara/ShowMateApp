@file:Suppress("ktlint:standard:filename")

package com.andrea.showmateapp.ui.navigation

import kotlinx.serialization.Serializable

sealed interface Screen {
    @Serializable object Splash : Screen

    @Serializable object Welcome : Screen

    @Serializable object Login : Screen

    @Serializable object SignUp : Screen

    @Serializable object Consent : Screen

    @Serializable object Onboarding : Screen

    @Serializable object Swipe : Screen

    @Serializable object Main : Screen

    @Serializable object Home : Screen

    @Serializable object Search : Screen

    @Serializable object Discover : Screen

    @Serializable object Friends : Screen

    @Serializable object Profile : Screen

    @Serializable object Settings : Screen

    @Serializable object About : Screen

    @Serializable object Stats : Screen

    @Serializable object CustomLists : Screen

    @Serializable data class ListDetail(val listName: String) : Screen

    @Serializable data class AllShows(val type: String) : Screen

    @Serializable data class Detail(val showId: Int, val sharedElementTag: String? = null) : Screen

    @Serializable data class GroupMatch(val memberEmails: String) : Screen

    @Serializable object Achievements : Screen
}
