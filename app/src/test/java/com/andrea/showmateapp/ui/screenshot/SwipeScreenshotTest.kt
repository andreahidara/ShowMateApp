package com.andrea.showmateapp.ui.screenshot

import app.cash.paparazzi.DeviceConfig
import app.cash.paparazzi.Paparazzi
import com.andrea.showmateapp.data.network.MediaContent
import com.andrea.showmateapp.ui.screens.swipe.SwipeScreenContent
import org.junit.Rule
import org.junit.Test

/**
 * Screenshot tests for [SwipeScreenContent] using Paparazzi.
 *
 * Run: ./gradlew recordPaparazziDebug   (record new golden images)
 *       ./gradlew verifyPaparazziDebug  (compare against golden images)
 */
class SwipeScreenshotTest {

    @get:Rule
    val paparazzi = Paparazzi(
        deviceConfig = DeviceConfig.PIXEL_5,
        theme = "android:Theme.Material.NoTitleBar"
    )

    private val sampleShows = listOf(
        MediaContent(id = 1, name = "Breaking Bad", overview = "A chemistry teacher turns to crime.", posterPath = null, voteAverage = 9.5f),
        MediaContent(id = 2, name = "The Wire", overview = "Crime drama set in Baltimore.", posterPath = null, voteAverage = 9.3f)
    )

    @Test
    fun swipe_loadingState() {
        paparazzi.snapshot {
            SwipeScreenContent(
                showsToRate = emptyList(),
                errorMessage = null,
                isLoading = true,
                ratedCount = 0,
                lastRemovedShow = null,
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
                lastRemovedShow = null,
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
                lastRemovedShow = null,
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
                lastRemovedShow = sampleShows.first(),
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
                lastRemovedShow = null,
                onLikeShow = {},
                onSkipShow = {},
                onUndoAction = {},
                onNavigateToHome = {}
            )
        }
    }
}
