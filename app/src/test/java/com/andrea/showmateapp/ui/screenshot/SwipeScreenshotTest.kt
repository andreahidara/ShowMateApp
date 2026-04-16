package com.andrea.showmateapp.ui.screenshot

import app.cash.paparazzi.DeviceConfig
import app.cash.paparazzi.Paparazzi
import com.andrea.showmateapp.data.model.MediaContent
import com.andrea.showmateapp.ui.screens.swipe.SwipeScreenContent
import com.andrea.showmateapp.ui.screens.swipe.SwipeUiState
import com.andrea.showmateapp.util.MainDispatcherRule
import org.junit.Rule
import org.junit.Test

class SwipeScreenshotTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    @get:Rule
    val paparazzi = Paparazzi(
        deviceConfig = DeviceConfig.PIXEL_5,
        theme = "android:Theme.Material.NoTitleBar"
    )

    private val sampleShows = listOf(
        MediaContent(
            id = 1,
            name = "Breaking Bad",
            overview = "A chemistry teacher turns to crime.",
            posterPath = null,
            voteAverage = 9.5f
        ),
        MediaContent(
            id = 2,
            name = "The Wire",
            overview = "Crime drama set in Baltimore.",
            posterPath = null,
            voteAverage = 9.3f
        )
    )

    @Test
    fun swipe_loadingState() {
        paparazzi.snapshot {
            SwipeScreenContent(
                showsToRate = emptyList(),
                errorMessage = null,
                isLoading = true,
                ratedCount = 0,
                lastAction = null,
                onLikeShow = {},
                onSkipShow = {},
                onUndoAction = {},
                onNavigateToHome = {}
            )
        }
    }

    @Test
    fun swipe_errorState() {
        paparazzi.snapshot {
            SwipeScreenContent(
                showsToRate = emptyList(),
                errorMessage = "No se pudo cargar el contenido. Inténtalo de nuevo.",
                isLoading = false,
                ratedCount = 0,
                lastAction = null,
                onLikeShow = {},
                onSkipShow = {},
                onUndoAction = {},
                onNavigateToHome = {}
            )
        }
    }

    @Test
    fun swipe_showsLoaded() {
        paparazzi.snapshot {
            SwipeScreenContent(
                showsToRate = sampleShows,
                errorMessage = null,
                isLoading = false,
                ratedCount = 0,
                lastAction = null,
                onLikeShow = {},
                onSkipShow = {},
                onUndoAction = {},
                onNavigateToHome = {}
            )
        }
    }

    @Test
    fun swipe_withUndoAvailable() {
        paparazzi.snapshot {
            SwipeScreenContent(
                showsToRate = sampleShows.drop(1),
                errorMessage = null,
                isLoading = false,
                ratedCount = 1,
                lastAction = SwipeUiState.SwipeAction(sampleShows.first(), isLike = true),
                onLikeShow = {},
                onSkipShow = {},
                onUndoAction = {},
                onNavigateToHome = {}
            )
        }
    }

    @Test
    fun swipe_emptyStateAfterRating() {
        paparazzi.snapshot {
            SwipeScreenContent(
                showsToRate = emptyList(),
                errorMessage = null,
                isLoading = false,
                ratedCount = 10,
                lastAction = null,
                onLikeShow = {},
                onSkipShow = {},
                onUndoAction = {},
                onNavigateToHome = {}
            )
        }
    }
}
