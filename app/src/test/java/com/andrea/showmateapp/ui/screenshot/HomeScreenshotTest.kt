package com.andrea.showmateapp.ui.screenshot

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionLayout
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalInspectionMode
import app.cash.paparazzi.DeviceConfig
import app.cash.paparazzi.Paparazzi
import com.andrea.showmateapp.data.network.MediaContent
import com.andrea.showmateapp.ui.screens.home.HomeScreenContent
import com.andrea.showmateapp.util.MainDispatcherRule
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalSharedTransitionApi::class)
class HomeScreenshotTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    @get:Rule
    val paparazzi = Paparazzi(
        deviceConfig = DeviceConfig.PIXEL_5,
        theme = "android:Theme.Material.NoTitleBar"
    )

    private fun createSampleShow(id: Int, name: String, vote: Float) = MediaContent(
        id = id,
        name = name,
        overview = "",
        posterPath = null,
        genreIds = emptyList(),
        genres = null,
        popularity = 0f,
        keywords = null,
        credits = null,
        affinityScore = 0f,
        reasons = emptyList(),
        firstAirDate = null,
        numberOfSeasons = null,
        status = null,
        voteAverage = vote,
        voteCount = 0,
        episodeRunTime = null,
        backdropPath = null,
        watchProviders = null,
        seasons = null,
        videos = null,
        originCountry = emptyList()
    )

    private val sampleShows = listOf(
        createSampleShow(1, "The Bear", 8.5f),
        createSampleShow(2, "Severance", 8.7f),
        createSampleShow(3, "Succession", 8.9f)
    )

    @Test
    fun home_dataLoadedState() {
        paparazzi.snapshot {
            CompositionLocalProvider(LocalInspectionMode provides true) {
                SharedTransitionLayout {
                    AnimatedContent(targetState = Unit, label = "test") { _ ->
                        HomeScreenContent(
                            userName = "Andrea",
                            scrollToTopTrigger = 0,
                            onNavigateToProfile = {},
                            upNextShows = emptyList(),
                            upNextProgress = emptyMap(),
                            trendingShows = sampleShows,
                            top10Shows = emptyList(),
                            newReleasesShows = emptyList(),
                            actionShows = sampleShows,
                            comedyShows = sampleShows,
                            dramaShows = emptyList(),
                            thisWeekShows = sampleShows,
                            selectedPlatform = null,
                            platformShows = emptyMap(),
                            isPlatformLoading = false,
                            isLoading = false,
                            isRefreshing = false,
                            criticalError = null,
                            sharedTransitionScope = this@SharedTransitionLayout,
                            animatedVisibilityScope = this@AnimatedContent,
                            onMediaClick = { _, _ -> },
                            onRetry = {},
                            onRefresh = {},
                            onPickWhatToWatch = {},
                            onPlatformSelected = {},
                            onLoadMoreTrending = {},
                            onLoadMoreThisWeek = {},
                            isLoadingMoreTrending = false,
                            isLoadingMoreThisWeek = false
                        )
                    }
                }
            }
        }
    }

    @Test
    fun home_refreshingState() {
        paparazzi.snapshot {
            CompositionLocalProvider(LocalInspectionMode provides true) {
                SharedTransitionLayout {
                    AnimatedContent(targetState = Unit, label = "test") { _ ->
                        HomeScreenContent(
                            userName = "",
                            scrollToTopTrigger = 0,
                            onNavigateToProfile = {},
                            upNextShows = emptyList(),
                            upNextProgress = emptyMap(),
                            trendingShows = sampleShows,
                            top10Shows = emptyList(),
                            newReleasesShows = emptyList(),
                            actionShows = emptyList(),
                            comedyShows = emptyList(),
                            dramaShows = emptyList(),
                            thisWeekShows = emptyList(),
                            selectedPlatform = null,
                            platformShows = emptyMap(),
                            isPlatformLoading = false,
                            isLoading = false,
                            isRefreshing = true,
                            criticalError = null,
                            sharedTransitionScope = this@SharedTransitionLayout,
                            animatedVisibilityScope = this@AnimatedContent,
                            onMediaClick = { _, _ -> },
                            onRetry = {},
                            onRefresh = {},
                            onPickWhatToWatch = {},
                            onPlatformSelected = {},
                            onLoadMoreTrending = {},
                            onLoadMoreThisWeek = {},
                            isLoadingMoreTrending = false,
                            isLoadingMoreThisWeek = false
                        )
                    }
                }
            }
        }
    }
}
