package com.andrea.showmateapp.ui.screenshot

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionLayout
import app.cash.paparazzi.DeviceConfig
import app.cash.paparazzi.Paparazzi
import com.andrea.showmateapp.data.network.MediaContent
import com.andrea.showmateapp.ui.screens.home.HomeScreenContent
import com.andrea.showmateapp.util.ErrorType
import org.junit.Rule
import org.junit.Test

/**
 * Screenshot tests for [HomeScreenContent] using Paparazzi.
 * Each test captures a pixel-perfect snapshot — failures indicate visual regressions.
 *
 * Run: ./gradlew recordPaparazziDebug   (record new golden images)
 *       ./gradlew verifyPaparazziDebug  (compare against golden images)
 */
@OptIn(ExperimentalSharedTransitionApi::class)
class HomeScreenshotTest {

    @get:Rule
    val paparazzi = Paparazzi(
        deviceConfig = DeviceConfig.PIXEL_5,
        theme = "android:Theme.Material.NoTitleBar"
    )

    private val sampleShows = listOf(
        MediaContent(id = 1, name = "Breaking Bad", posterPath = null, voteAverage = 9.5f),
        MediaContent(id = 2, name = "The Wire", posterPath = null, voteAverage = 9.3f),
        MediaContent(id = 3, name = "Ozark", posterPath = null, voteAverage = 8.4f)
    )

    @Test
    fun home_loadingState() {
        paparazzi.snapshot {
            SharedTransitionLayout {
                AnimatedContent(targetState = Unit, label = "test") { _ ->
                    HomeScreenContent(
                        isLoading = true,
                        isRefreshing = false,
                        trendingShows = emptyList(),
                        actionShows = emptyList(),
                        comedyShows = emptyList(),
                        mysteryShows = emptyList(),
                        thisWeekShows = emptyList(),
                        upNextShows = emptyList(),
                        sharedTransitionScope = this@SharedTransitionLayout,
                        animatedVisibilityScope = this@AnimatedContent,
                        onMediaClick = { _, _ -> },
                        onRetry = {},
                        onRefresh = {}
                    )
                }
            }
        }
    }

    @Test
    fun home_errorState_network() {
        paparazzi.snapshot {
            SharedTransitionLayout {
                AnimatedContent(targetState = Unit, label = "test") { _ ->
                    HomeScreenContent(
                        isLoading = false,
                        isRefreshing = false,
                        criticalError = ErrorType.Network,
                        trendingShows = emptyList(),
                        actionShows = emptyList(),
                        comedyShows = emptyList(),
                        mysteryShows = emptyList(),
                        thisWeekShows = emptyList(),
                        upNextShows = emptyList(),
                        sharedTransitionScope = this@SharedTransitionLayout,
                        animatedVisibilityScope = this@AnimatedContent,
                        onMediaClick = { _, _ -> },
                        onRetry = {},
                        onRefresh = {}
                    )
                }
            }
        }
    }

    @Test
    fun home_errorState_unknown() {
        paparazzi.snapshot {
            SharedTransitionLayout {
                AnimatedContent(targetState = Unit, label = "test") { _ ->
                    HomeScreenContent(
                        isLoading = false,
                        isRefreshing = false,
                        criticalError = ErrorType.Unknown,
                        trendingShows = emptyList(),
                        actionShows = emptyList(),
                        comedyShows = emptyList(),
                        mysteryShows = emptyList(),
                        thisWeekShows = emptyList(),
                        upNextShows = emptyList(),
                        sharedTransitionScope = this@SharedTransitionLayout,
                        animatedVisibilityScope = this@AnimatedContent,
                        onMediaClick = { _, _ -> },
                        onRetry = {},
                        onRefresh = {}
                    )
                }
            }
        }
    }

    @Test
    fun home_dataLoadedState() {
        paparazzi.snapshot {
            SharedTransitionLayout {
                AnimatedContent(targetState = Unit, label = "test") { _ ->
                    HomeScreenContent(
                        isLoading = false,
                        isRefreshing = false,
                        userName = "Andrea",
                        trendingShows = sampleShows,
                        actionShows = sampleShows,
                        comedyShows = sampleShows,
                        mysteryShows = sampleShows,
                        thisWeekShows = sampleShows,
                        upNextShows = emptyList(),
                        sharedTransitionScope = this@SharedTransitionLayout,
                        animatedVisibilityScope = this@AnimatedContent,
                        onMediaClick = { _, _ -> },
                        onRetry = {},
                        onRefresh = {}
                    )
                }
            }
        }
    }

    @Test
    fun home_refreshingState() {
        paparazzi.snapshot {
            SharedTransitionLayout {
                AnimatedContent(targetState = Unit, label = "test") { _ ->
                    HomeScreenContent(
                        isLoading = false,
                        isRefreshing = true,
                        trendingShows = sampleShows,
                        actionShows = emptyList(),
                        comedyShows = emptyList(),
                        mysteryShows = emptyList(),
                        thisWeekShows = emptyList(),
                        upNextShows = emptyList(),
                        sharedTransitionScope = this@SharedTransitionLayout,
                        animatedVisibilityScope = this@AnimatedContent,
                        onMediaClick = { _, _ -> },
                        onRetry = {},
                        onRefresh = {}
                    )
                }
            }
        }
    }
}
