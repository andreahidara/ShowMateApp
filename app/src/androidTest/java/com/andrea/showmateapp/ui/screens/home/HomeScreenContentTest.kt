package com.andrea.showmateapp.ui.screens.home

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionLayout
import androidx.compose.ui.semantics.ProgressBarRangeInfo
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import com.andrea.showmateapp.data.network.MediaContent
import com.andrea.showmateapp.util.ErrorType
import org.junit.Rule
import org.junit.Test

/**
 * UI tests for [HomeScreenContent].
 * Wrapped with SharedTransitionLayout + AnimatedContent because HomeScreenContent
 * requires both SharedTransitionScope and AnimatedVisibilityScope.
 */
@OptIn(ExperimentalSharedTransitionApi::class)
class HomeScreenContentTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private val sampleShows = listOf(
        MediaContent(id = 1, name = "Breaking Bad"),
        MediaContent(id = 2, name = "The Wire")
    )

    private fun setHomeContent(
        isLoading: Boolean = false,
        isRefreshing: Boolean = false,
        criticalError: ErrorType? = null,
        trendingShows: List<MediaContent> = emptyList(),
        actionShows: List<MediaContent> = emptyList(),
        comedyShows: List<MediaContent> = emptyList(),
        mysteryShows: List<MediaContent> = emptyList(),
        thisWeekShows: List<MediaContent> = emptyList(),
        upNextShows: List<MediaContent> = emptyList(),
        onRetry: () -> Unit = {},
        onRefresh: () -> Unit = {}
    ) {
        composeTestRule.setContent {
            SharedTransitionLayout {
                AnimatedContent(targetState = true, label = "test") { isActive ->
                    if (isActive) HomeScreenContent(
                        userName = "TestUser",
                        upNextShows = upNextShows,
                        trendingShows = trendingShows,
                        actionShows = actionShows,
                        comedyShows = comedyShows,
                        mysteryShows = mysteryShows,
                        thisWeekShows = thisWeekShows,
                        isLoading = isLoading,
                        isRefreshing = isRefreshing,
                        criticalError = criticalError,
                        sharedTransitionScope = this@SharedTransitionLayout,
                        animatedVisibilityScope = this@AnimatedContent,
                        onMediaClick = { _, _ -> },
                        onRetry = onRetry,
                        onRefresh = onRefresh
                    )
                }
            }
        }
    }

    // ── Loading state ─────────────────────────────────────────────────────────

    @Test
    fun givenIsLoadingTrueAndNoShows_whenRendered_thenProgressIndicatorIsVisible() {
        setHomeContent(isLoading = true, trendingShows = emptyList())

        composeTestRule
            .onNode(hasProgressBarRangeInfo(ProgressBarRangeInfo.Indeterminate))
            .assertIsDisplayed()
    }

    // ── Error state ───────────────────────────────────────────────────────────

    @Test
    fun givenNetworkError_whenRendered_thenErrorTitleIsDisplayed() {
        setHomeContent(criticalError = ErrorType.Network, trendingShows = emptyList())

        composeTestRule
            .onNodeWithText("Sin conexión")
            .assertIsDisplayed()
    }

    @Test
    fun givenNetworkError_whenRendered_thenRetryButtonIsDisplayed() {
        setHomeContent(criticalError = ErrorType.Network, trendingShows = emptyList())

        composeTestRule
            .onNodeWithText("Reintentar")
            .assertIsDisplayed()
    }

    @Test
    fun givenNetworkError_whenRetryClicked_thenOnRetryIsCalled() {
        var retryCalled = false
        setHomeContent(
            criticalError = ErrorType.Network,
            trendingShows = emptyList(),
            onRetry = { retryCalled = true }
        )

        composeTestRule.onNodeWithText("Reintentar").performClick()

        assert(retryCalled) { "onRetry should have been called" }
    }

    @Test
    fun givenUnknownError_whenRendered_thenFallbackErrorTitleIsDisplayed() {
        setHomeContent(criticalError = ErrorType.Unknown, trendingShows = emptyList())

        composeTestRule
            .onNodeWithText("¡Ups! Algo salió mal")
            .assertIsDisplayed()
    }

    // ── Data loaded state ─────────────────────────────────────────────────────

    @Test
    fun givenShowsLoaded_whenRendered_thenTrendingShowNameIsDisplayed() {
        setHomeContent(
            isLoading = false,
            trendingShows = sampleShows,
            actionShows = emptyList(),
            comedyShows = emptyList(),
            mysteryShows = emptyList(),
            thisWeekShows = emptyList()
        )

        composeTestRule
            .onNodeWithText("Breaking Bad")
            .assertIsDisplayed()
    }

    @Test
    fun givenShowsLoaded_whenRendered_thenNoProgressIndicatorIsShown() {
        setHomeContent(
            isLoading = false,
            trendingShows = sampleShows
        )

        composeTestRule
            .onNode(hasProgressBarRangeInfo(ProgressBarRangeInfo.Indeterminate))
            .assertDoesNotExist()
    }

    // ── Pull-to-refresh ───────────────────────────────────────────────────────

    @Test
    fun givenIsRefreshingTrue_whenRendered_thenRefreshIndicatorIsVisible() {
        setHomeContent(
            isLoading = false,
            isRefreshing = true,
            trendingShows = sampleShows
        )

        // The pull-to-refresh indicator uses a circular progress bar
        composeTestRule
            .onNode(hasProgressBarRangeInfo(ProgressBarRangeInfo.Indeterminate))
            .assertIsDisplayed()
    }
}
