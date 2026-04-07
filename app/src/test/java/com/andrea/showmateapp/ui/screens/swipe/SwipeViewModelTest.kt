package com.andrea.showmateapp.ui.screens.swipe

import com.andrea.showmateapp.data.network.MediaContent
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
import org.junit.Assert.*
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

    // ── loadShows ─────────────────────────────────────────────────────────────

    @Test
    fun `loadShows sets shows from use case`() = runTest {
        coEvery { getRecommendationsUseCase.execute() } returns sampleShows

        val vm = viewModel()
        vm.loadShows(forceReload = true)
        advanceUntilIdle()

        assertEquals(sampleShows, vm.shows.value)
    }

    @Test
    fun `loadShows sets isLoading false after completion`() = runTest {
        coEvery { getRecommendationsUseCase.execute() } returns sampleShows

        val vm = viewModel()
        vm.loadShows(forceReload = true)
        advanceUntilIdle()

        assertFalse(vm.isLoading.value)
    }

    @Test
    fun `loadShows with non-empty list skips when not forced`() = runTest {
        coEvery { getRecommendationsUseCase.execute() } returns sampleShows

        val vm = viewModel()
        vm.loadShows(forceReload = true)
        advanceUntilIdle()

        // Now call without force — list should remain the same (no second network call)
        val before = vm.shows.value
        vm.loadShows(forceReload = false)
        advanceUntilIdle()

        assertEquals(before, vm.shows.value)
        assertFalse(vm.isLoading.value)
    }

    @Test
    fun `loadShows on error sets errorMessage`() = runTest {
        coEvery { getRecommendationsUseCase.execute() } throws RuntimeException("Network error")

        val vm = viewModel()
        vm.loadShows(forceReload = true)
        advanceUntilIdle()

        assertNotNull(vm.errorMessage.value)
        assertTrue(vm.errorMessage.value!!.contains("error", ignoreCase = true))
    }

    @Test
    fun `loadShows on error clears loading state`() = runTest {
        coEvery { getRecommendationsUseCase.execute() } throws RuntimeException("Timeout")

        val vm = viewModel()
        vm.loadShows(forceReload = true)
        advanceUntilIdle()

        assertFalse(vm.isLoading.value)
    }

    // ── likeTopShow ───────────────────────────────────────────────────────────

    @Test
    fun `likeTopShow removes first show from list`() = runTest {
        coEvery { getRecommendationsUseCase.execute() } returns sampleShows

        val vm = viewModel()
        vm.loadShows(forceReload = true)
        advanceUntilIdle()

        vm.likeTopShow()
        advanceUntilIdle()

        assertEquals(sampleShows.drop(1), vm.shows.value)
    }

    @Test
    fun `likeTopShow increments ratedCount`() = runTest {
        coEvery { getRecommendationsUseCase.execute() } returns sampleShows

        val vm = viewModel()
        vm.loadShows(forceReload = true)
        advanceUntilIdle()

        vm.likeTopShow()
        advanceUntilIdle()

        assertEquals(1, vm.ratedCount.value)
    }

    @Test
    fun `likeTopShow saves removed show as lastRemovedShow`() = runTest {
        coEvery { getRecommendationsUseCase.execute() } returns sampleShows

        val vm = viewModel()
        vm.loadShows(forceReload = true)
        advanceUntilIdle()

        vm.likeTopShow()
        advanceUntilIdle()

        assertEquals(sampleShows.first(), vm.lastRemovedShow.value)
    }

    @Test
    fun `likeTopShow on empty list does nothing`() = runTest {
        val vm = viewModel()
        // shows are empty by default
        vm.likeTopShow()

        assertEquals(0, vm.ratedCount.value)
        assertNull(vm.lastRemovedShow.value)
    }

    // ── skipTopShow ───────────────────────────────────────────────────────────

    @Test
    fun `skipTopShow removes first show from list`() = runTest {
        coEvery { getRecommendationsUseCase.execute() } returns sampleShows

        val vm = viewModel()
        vm.loadShows(forceReload = true)
        advanceUntilIdle()

        vm.skipTopShow()
        advanceUntilIdle()

        assertEquals(sampleShows.drop(1), vm.shows.value)
    }

    @Test
    fun `skipTopShow increments ratedCount`() = runTest {
        coEvery { getRecommendationsUseCase.execute() } returns sampleShows

        val vm = viewModel()
        vm.loadShows(forceReload = true)
        advanceUntilIdle()

        vm.skipTopShow()
        advanceUntilIdle()

        assertEquals(1, vm.ratedCount.value)
    }

    @Test
    fun `skipTopShow saves removed show as lastRemovedShow`() = runTest {
        coEvery { getRecommendationsUseCase.execute() } returns sampleShows

        val vm = viewModel()
        vm.loadShows(forceReload = true)
        advanceUntilIdle()

        vm.skipTopShow()
        advanceUntilIdle()

        assertEquals(sampleShows.first(), vm.lastRemovedShow.value)
    }

    // ── undoLastAction ────────────────────────────────────────────────────────

    @Test
    fun `undoLastAction restores removed show to front`() = runTest {
        coEvery { getRecommendationsUseCase.execute() } returns sampleShows

        val vm = viewModel()
        vm.loadShows(forceReload = true)
        advanceUntilIdle()

        vm.likeTopShow()
        advanceUntilIdle()
        vm.undoLastAction()

        assertEquals(sampleShows.first(), vm.shows.value.first())
    }

    @Test
    fun `undoLastAction decrements ratedCount`() = runTest {
        coEvery { getRecommendationsUseCase.execute() } returns sampleShows

        val vm = viewModel()
        vm.loadShows(forceReload = true)
        advanceUntilIdle()

        vm.likeTopShow()
        advanceUntilIdle()
        assertEquals(1, vm.ratedCount.value)

        vm.undoLastAction()
        assertEquals(0, vm.ratedCount.value)
    }

    @Test
    fun `undoLastAction clears lastRemovedShow`() = runTest {
        coEvery { getRecommendationsUseCase.execute() } returns sampleShows

        val vm = viewModel()
        vm.loadShows(forceReload = true)
        advanceUntilIdle()

        vm.likeTopShow()
        advanceUntilIdle()
        assertNotNull(vm.lastRemovedShow.value)

        vm.undoLastAction()
        assertNull(vm.lastRemovedShow.value)
    }

    @Test
    fun `undoLastAction with no removed show does nothing`() = runTest {
        val vm = viewModel()
        vm.undoLastAction()

        assertEquals(0, vm.ratedCount.value)
        assertNull(vm.lastRemovedShow.value)
    }

    @Test
    fun `ratedCount does not go below zero after undo`() = runTest {
        coEvery { getRecommendationsUseCase.execute() } returns sampleShows

        val vm = viewModel()
        vm.loadShows(forceReload = true)
        advanceUntilIdle()

        vm.likeTopShow()
        advanceUntilIdle()
        vm.undoLastAction()
        // Calling undo again when no show is set should not decrement below 0
        vm.undoLastAction()

        assertTrue(vm.ratedCount.value >= 0)
    }
}
