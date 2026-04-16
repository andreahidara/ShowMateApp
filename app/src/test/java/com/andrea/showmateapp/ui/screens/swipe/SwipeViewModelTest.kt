package com.andrea.showmateapp.ui.screens.swipe

import com.andrea.showmateapp.data.model.MediaContent
import com.andrea.showmateapp.domain.repository.IInteractionRepository
import com.andrea.showmateapp.domain.repository.IUserRepository
import com.andrea.showmateapp.domain.usecase.AchievementChecker
import com.andrea.showmateapp.domain.usecase.GetRecommendationsUseCase
import com.andrea.showmateapp.util.MainDispatcherRule
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class SwipeViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val userRepository: IUserRepository = mockk(relaxed = true)
    private val interactionRepository: IInteractionRepository = mockk(relaxed = true)
    private val getRecommendationsUseCase: GetRecommendationsUseCase = mockk(relaxed = true)
    private val achievementChecker: AchievementChecker = mockk(relaxed = true)

    private val sampleShows = listOf(
        MediaContent(id = 1, name = "Breaking Bad"),
        MediaContent(id = 2, name = "The Wire"),
        MediaContent(id = 3, name = "Ozark")
    )

    private fun viewModel() = SwipeViewModel(
        userRepository,
        interactionRepository,
        getRecommendationsUseCase,
        achievementChecker
    )

    private val SwipeViewModel.shows get() = uiState.value.shows
    private val SwipeViewModel.isLoading get() = uiState.value.isLoading
    private val SwipeViewModel.ratedCount get() = uiState.value.ratedCount
    private val SwipeViewModel.lastAction get() = uiState.value.lastAction
    private val SwipeViewModel.errorMessage get() = uiState.value.errorMessage

    @Test
    fun `loadShows sets shows from use case`() = runTest {
        coEvery { getRecommendationsUseCase.execute() } returns sampleShows

        val vm = viewModel()
        vm.loadShows(forceReload = true)
        advanceUntilIdle()

        assertEquals(sampleShows, vm.shows)
    }

    @Test
    fun `loadShows sets isLoading false after completion`() = runTest {
        coEvery { getRecommendationsUseCase.execute() } returns sampleShows

        val vm = viewModel()
        vm.loadShows(forceReload = true)
        advanceUntilIdle()

        assertFalse(vm.isLoading)
    }

    @Test
    fun `loadShows with non-empty list skips when not forced`() = runTest {
        coEvery { getRecommendationsUseCase.execute() } returns sampleShows

        val vm = viewModel()
        vm.loadShows(forceReload = true)
        advanceUntilIdle()

        val before = vm.shows
        vm.loadShows(forceReload = false)
        advanceUntilIdle()

        assertEquals(before, vm.shows)
        assertFalse(vm.isLoading)
    }

    @Test
    fun `loadShows on error sets errorMessage`() = runTest {
        coEvery { getRecommendationsUseCase.execute() } throws RuntimeException("Network error")

        val vm = viewModel()
        vm.loadShows(forceReload = true)
        advanceUntilIdle()

        assertNotNull(vm.errorMessage)
        assertTrue(vm.errorMessage!!.contains("error", ignoreCase = true))
    }

    @Test
    fun `loadShows on error clears loading state`() = runTest {
        coEvery { getRecommendationsUseCase.execute() } throws RuntimeException("Timeout")

        val vm = viewModel()
        vm.loadShows(forceReload = true)
        advanceUntilIdle()

        assertFalse(vm.isLoading)
    }

    @Test
    fun `likeTopShow removes first show from list`() = runTest {
        coEvery { getRecommendationsUseCase.execute() } returns sampleShows

        val vm = viewModel()
        vm.loadShows(forceReload = true)
        advanceUntilIdle()

        vm.likeTopShow()
        advanceUntilIdle()

        assertEquals(sampleShows.drop(1), vm.shows)
    }

    @Test
    fun `likeTopShow increments ratedCount`() = runTest {
        coEvery { getRecommendationsUseCase.execute() } returns sampleShows

        val vm = viewModel()
        vm.loadShows(forceReload = true)
        advanceUntilIdle()

        vm.likeTopShow()
        advanceUntilIdle()

        assertEquals(1, vm.ratedCount)
    }

    @Test
    fun `likeTopShow saves removed show as lastAction`() = runTest {
        coEvery { getRecommendationsUseCase.execute() } returns sampleShows

        val vm = viewModel()
        vm.loadShows(forceReload = true)
        advanceUntilIdle()

        vm.likeTopShow()
        advanceUntilIdle()

        assertEquals(sampleShows.first(), vm.lastAction?.show)
        assertTrue(vm.lastAction?.isLike == true)
    }

    @Test
    fun `likeTopShow on empty list does nothing`() = runTest {
        val vm = viewModel()
        vm.likeTopShow()

        assertEquals(0, vm.ratedCount)
        assertNull(vm.lastAction)
    }

    @Test
    fun `skipTopShow removes first show from list`() = runTest {
        coEvery { getRecommendationsUseCase.execute() } returns sampleShows

        val vm = viewModel()
        vm.loadShows(forceReload = true)
        advanceUntilIdle()

        vm.skipTopShow()
        advanceUntilIdle()

        assertEquals(sampleShows.drop(1), vm.shows)
    }

    @Test
    fun `skipTopShow increments ratedCount`() = runTest {
        coEvery { getRecommendationsUseCase.execute() } returns sampleShows

        val vm = viewModel()
        vm.loadShows(forceReload = true)
        advanceUntilIdle()

        vm.skipTopShow()
        advanceUntilIdle()

        assertEquals(1, vm.ratedCount)
    }

    @Test
    fun `skipTopShow stores last action as not liked`() = runTest {
        coEvery { getRecommendationsUseCase.execute() } returns sampleShows

        val vm = viewModel()
        vm.loadShows(forceReload = true)
        advanceUntilIdle()

        vm.skipTopShow()
        advanceUntilIdle()

        assertEquals(sampleShows.first(), vm.lastAction?.show)
        assertTrue(vm.lastAction?.isLike == false)
    }

    @Test
    fun `undoLastAction restores removed show to front`() = runTest {
        coEvery { getRecommendationsUseCase.execute() } returns sampleShows

        val vm = viewModel()
        vm.loadShows(forceReload = true)
        advanceUntilIdle()

        vm.likeTopShow()
        advanceUntilIdle()
        vm.undoLastAction()

        assertEquals(sampleShows.first(), vm.shows.first())
    }

    @Test
    fun `undoLastAction decrements ratedCount`() = runTest {
        coEvery { getRecommendationsUseCase.execute() } returns sampleShows

        val vm = viewModel()
        vm.loadShows(forceReload = true)
        advanceUntilIdle()

        vm.likeTopShow()
        advanceUntilIdle()
        assertEquals(1, vm.ratedCount)

        vm.undoLastAction()
        assertEquals(0, vm.ratedCount)
    }

    @Test
    fun `undoLastAction clears lastAction`() = runTest {
        coEvery { getRecommendationsUseCase.execute() } returns sampleShows

        val vm = viewModel()
        vm.loadShows(forceReload = true)
        advanceUntilIdle()

        vm.likeTopShow()
        advanceUntilIdle()
        assertNotNull(vm.lastAction)

        vm.undoLastAction()
        assertNull(vm.lastAction)
    }

    @Test
    fun `undoLastAction with no action does nothing`() = runTest {
        val vm = viewModel()
        vm.undoLastAction()

        assertEquals(0, vm.ratedCount)
        assertNull(vm.lastAction)
    }

    @Test
    fun `ratedCount does not go below zero after double undo`() = runTest {
        coEvery { getRecommendationsUseCase.execute() } returns sampleShows

        val vm = viewModel()
        vm.loadShows(forceReload = true)
        advanceUntilIdle()

        vm.likeTopShow()
        advanceUntilIdle()
        vm.undoLastAction()
        vm.undoLastAction()

        assertTrue(vm.ratedCount >= 0)
    }
}
