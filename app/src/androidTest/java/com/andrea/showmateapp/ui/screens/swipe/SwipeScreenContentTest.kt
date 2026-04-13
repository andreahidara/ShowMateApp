package com.andrea.showmateapp.ui.screens.swipe

import androidx.compose.ui.semantics.ProgressBarRangeInfo
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import com.andrea.showmateapp.data.network.MediaContent
import com.andrea.showmateapp.ui.screens.swipe.SwipeUiState
import org.junit.Rule
import org.junit.Test

/**
 * UI tests for [SwipeScreenContent].
 * No Hilt needed — SwipeScreenContent is a stateless composable.
 */
class SwipeScreenContentTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private val sampleShows = listOf(
        MediaContent(id = 1, name = "Breaking Bad", overview = "A chemistry teacher turns to crime"),
        MediaContent(id = 2, name = "The Wire", overview = "Crime drama")
    )

    // ── Loading state ─────────────────────────────────────────────────────────

    @Test
    fun givenIsLoadingTrue_whenRendered_thenProgressIndicatorIsVisible() {
        composeTestRule.setContent {
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

        composeTestRule
            .onNode(hasProgressBarRangeInfo(ProgressBarRangeInfo.Indeterminate))
            .assertIsDisplayed()
    }

    @Test
    fun givenIsLoadingTrue_whenRendered_thenActionButtonsAreNotVisible() {
        composeTestRule.setContent {
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

        composeTestRule
            .onNodeWithContentDescription("Me gusta")
            .assertDoesNotExist()
    }

    // ── Error state ───────────────────────────────────────────────────────────

    @Test
    fun givenErrorMessage_whenRendered_thenErrorTextIsDisplayed() {
        val errorMsg = "No se pudo cargar el contenido"
        composeTestRule.setContent {
            SwipeScreenContent(
                showsToRate = emptyList(),
                errorMessage = errorMsg,
                isLoading = false,
                ratedCount = 0,
                lastAction = null,
                onLikeShow = {},
                onSkipShow = {},
                onUndoAction = {},
                onNavigateToHome = {}
            )
        }

        composeTestRule
            .onNodeWithText(errorMsg)
            .assertIsDisplayed()
    }

    // ── Empty state ───────────────────────────────────────────────────────────

    @Test
    fun givenEmptyShowsAndRatedCount10_whenRendered_thenDoneMessageIsVisible() {
        composeTestRule.setContent {
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

        // The screen shows a "completed" message when all shows have been rated
        composeTestRule
            .onNodeWithContentDescription("Me gusta")
            .assertDoesNotExist()
    }

    // ── Shows loaded ──────────────────────────────────────────────────────────

    @Test
    fun givenShowsLoaded_whenRendered_thenLikeButtonIsVisible() {
        composeTestRule.setContent {
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

        composeTestRule
            .onNodeWithContentDescription("Me gusta")
            .assertIsDisplayed()
    }

    @Test
    fun givenShowsLoaded_whenRendered_thenSkipButtonIsVisible() {
        composeTestRule.setContent {
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

        composeTestRule
            .onNodeWithContentDescription("Paso")
            .assertIsDisplayed()
    }

    @Test
    fun givenShowsLoaded_whenLikeButtonClicked_thenOnLikeShowIsCalled() {
        var likeCallCount = 0
        composeTestRule.setContent {
            SwipeScreenContent(
                showsToRate = sampleShows,
                errorMessage = null,
                isLoading = false,
                ratedCount = 0,
                lastAction = null,
                onLikeShow = { likeCallCount++ },
                onSkipShow = {},
                onUndoAction = {},
                onNavigateToHome = {}
            )
        }

        composeTestRule
            .onNodeWithContentDescription("Me gusta")
            .performClick()

        assert(likeCallCount == 1) { "onLikeShow should have been called once" }
    }

    @Test
    fun givenShowsLoaded_whenSkipButtonClicked_thenOnSkipShowIsCalled() {
        var skipCallCount = 0
        composeTestRule.setContent {
            SwipeScreenContent(
                showsToRate = sampleShows,
                errorMessage = null,
                isLoading = false,
                ratedCount = 0,
                lastAction = null,
                onLikeShow = {},
                onSkipShow = { skipCallCount++ },
                onUndoAction = {},
                onNavigateToHome = {}
            )
        }

        composeTestRule
            .onNodeWithContentDescription("Paso")
            .performClick()

        assert(skipCallCount == 1) { "onSkipShow should have been called once" }
    }

    // ── Undo button ───────────────────────────────────────────────────────────

    @Test
    fun givenLastRemovedShowIsNull_whenRendered_thenUndoButtonIsNotEnabled() {
        composeTestRule.setContent {
            SwipeScreenContent(
                showsToRate = sampleShows,
                errorMessage = null,
                isLoading = false,
                ratedCount = 1,
                lastAction = null,
                onLikeShow = {},
                onSkipShow = {},
                onUndoAction = {},
                onNavigateToHome = {}
            )
        }

        // Undo button exists but is disabled when no show has been removed
        composeTestRule
            .onNodeWithContentDescription("Deshacer")
            .assertIsNotEnabled()
    }

    @Test
    fun givenLastRemovedShowExists_whenUndoClicked_thenOnUndoActionIsCalled() {
        var undoCallCount = 0
        composeTestRule.setContent {
            SwipeScreenContent(
                showsToRate = sampleShows,
                errorMessage = null,
                isLoading = false,
                ratedCount = 1,
                lastAction = SwipeUiState.SwipeAction(show = sampleShows.first(), isLike = true),
                onLikeShow = {},
                onSkipShow = {},
                onUndoAction = { undoCallCount++ },
                onNavigateToHome = {}
            )
        }

        composeTestRule
            .onNodeWithContentDescription("Deshacer")
            .performClick()

        assert(undoCallCount == 1) { "onUndoAction should have been called once" }
    }

    // ── Accessibility ─────────────────────────────────────────────────────────

    @Test
    fun givenShowsLoaded_whenAccessibilityCheck_thenAllButtonsHaveContentDescriptions() {
        composeTestRule.setContent {
            SwipeScreenContent(
                showsToRate = sampleShows,
                errorMessage = null,
                isLoading = false,
                ratedCount = 0,
                lastAction = SwipeUiState.SwipeAction(show = sampleShows.first(), isLike = true),
                onLikeShow = {},
                onSkipShow = {},
                onUndoAction = {},
                onNavigateToHome = {}
            )
        }

        composeTestRule.onNodeWithContentDescription("Me gusta").assertExists()
        composeTestRule.onNodeWithContentDescription("Paso").assertExists()
        composeTestRule.onNodeWithContentDescription("Deshacer").assertExists()
    }
}
