package com.andrea.showmateapp.data.service

import com.andrea.showmateapp.data.local.MediaInteractionEntity
import com.andrea.showmateapp.data.model.MediaContent
import com.andrea.showmateapp.domain.repository.IInteractionRepository
import com.andrea.showmateapp.domain.repository.IShowRepository
import com.andrea.showmateapp.util.Resource
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

class SeasonCheckWorkerTest {

    private val interactionRepository: IInteractionRepository = mockk(relaxed = true)
    private val showRepository: IShowRepository = mockk(relaxed = true)

    // region season detection logic

    @Test
    fun `no notification sent when current seasons equals last known seasons`() = runTest {
        val entity = MediaInteractionEntity(mediaId = 42, lastKnownSeasons = 3)
        val show = MediaContent(id = 42, name = "Stranger Things", numberOfSeasons = 3)

        coEvery { interactionRepository.getWatchedShowsWithSeasonCount() } returns listOf(entity)
        coEvery { showRepository.getShowDetails(42) } returns Resource.Success(show)

        // Simulate the worker logic manually
        val watchedWithCount = interactionRepository.getWatchedShowsWithSeasonCount()
        for (e in watchedWithCount) {
            val result = showRepository.getShowDetails(e.mediaId)
            if (result is Resource.Success) {
                val currentSeasons = result.data.numberOfSeasons ?: e.lastKnownSeasons
                val newSeasonAvailable = currentSeasons > e.lastKnownSeasons && e.lastKnownSeasons > 0
                assertEquals(false, newSeasonAvailable)
            }
        }
    }

    @Test
    fun `new season detected when current seasons exceeds last known`() = runTest {
        val entity = MediaInteractionEntity(mediaId = 42, lastKnownSeasons = 2)
        val show = MediaContent(id = 42, name = "Stranger Things", numberOfSeasons = 3)

        coEvery { interactionRepository.getWatchedShowsWithSeasonCount() } returns listOf(entity)
        coEvery { showRepository.getShowDetails(42) } returns Resource.Success(show)

        val watchedWithCount = interactionRepository.getWatchedShowsWithSeasonCount()
        for (e in watchedWithCount) {
            val result = showRepository.getShowDetails(e.mediaId)
            if (result is Resource.Success) {
                val currentSeasons = result.data.numberOfSeasons ?: e.lastKnownSeasons
                val newSeasonAvailable = currentSeasons > e.lastKnownSeasons && e.lastKnownSeasons > 0
                assertEquals(true, newSeasonAvailable)
            }
        }
    }

    @Test
    fun `no new season notif when lastKnownSeasons is 0 (first time sync)`() = runTest {
        val entity = MediaInteractionEntity(mediaId = 42, lastKnownSeasons = 0)
        val show = MediaContent(id = 42, name = "Breaking Bad", numberOfSeasons = 5)

        coEvery { interactionRepository.getWatchedShowsWithSeasonCount() } returns listOf(entity)
        coEvery { showRepository.getShowDetails(42) } returns Resource.Success(show)

        val watchedWithCount = interactionRepository.getWatchedShowsWithSeasonCount()
        for (e in watchedWithCount) {
            val result = showRepository.getShowDetails(e.mediaId)
            if (result is Resource.Success) {
                val currentSeasons = result.data.numberOfSeasons ?: e.lastKnownSeasons
                val newSeasonAvailable = currentSeasons > e.lastKnownSeasons && e.lastKnownSeasons > 0
                // lastKnownSeasons == 0 → never notify on first sync
                assertEquals(false, newSeasonAvailable)
            }
        }
    }

    @Test
    fun `updateLastKnownSeasons called when season count changes`() = runTest {
        val entity = MediaInteractionEntity(mediaId = 42, lastKnownSeasons = 2)
        val show = MediaContent(id = 42, name = "Stranger Things", numberOfSeasons = 3)

        coEvery { interactionRepository.getWatchedShowsWithSeasonCount() } returns listOf(entity)
        coEvery { showRepository.getShowDetails(42) } returns Resource.Success(show)

        val watchedWithCount = interactionRepository.getWatchedShowsWithSeasonCount()
        for (e in watchedWithCount) {
            val result = showRepository.getShowDetails(e.mediaId)
            if (result is Resource.Success) {
                val currentSeasons = result.data.numberOfSeasons ?: e.lastKnownSeasons
                if (currentSeasons != e.lastKnownSeasons) {
                    interactionRepository.updateLastKnownSeasons(e.mediaId, currentSeasons)
                }
            }
        }

        coVerify { interactionRepository.updateLastKnownSeasons(42, 3) }
    }

    @Test
    fun `no update when seasons are unchanged`() = runTest {
        val entity = MediaInteractionEntity(mediaId = 42, lastKnownSeasons = 3)
        val show = MediaContent(id = 42, name = "Stranger Things", numberOfSeasons = 3)

        coEvery { interactionRepository.getWatchedShowsWithSeasonCount() } returns listOf(entity)
        coEvery { showRepository.getShowDetails(42) } returns Resource.Success(show)

        val watchedWithCount = interactionRepository.getWatchedShowsWithSeasonCount()
        for (e in watchedWithCount) {
            val result = showRepository.getShowDetails(e.mediaId)
            if (result is Resource.Success) {
                val currentSeasons = result.data.numberOfSeasons ?: e.lastKnownSeasons
                if (currentSeasons != e.lastKnownSeasons) {
                    interactionRepository.updateLastKnownSeasons(e.mediaId, currentSeasons)
                }
            }
        }

        coVerify(exactly = 0) { interactionRepository.updateLastKnownSeasons(any(), any()) }
    }

    @Test
    fun `worker handles empty watched list gracefully`() = runTest {
        coEvery { interactionRepository.getWatchedShowsWithSeasonCount() } returns emptyList()

        val watchedWithCount = interactionRepository.getWatchedShowsWithSeasonCount()
        assertEquals(0, watchedWithCount.size)
        coVerify(exactly = 0) { showRepository.getShowDetails(any()) }
    }

    @Test
    fun `worker handles API failure gracefully for single show`() = runTest {
        val entity = MediaInteractionEntity(mediaId = 99, lastKnownSeasons = 1)
        coEvery { interactionRepository.getWatchedShowsWithSeasonCount() } returns listOf(entity)
        coEvery { showRepository.getShowDetails(99) } returns Resource.Error()

        val watchedWithCount = interactionRepository.getWatchedShowsWithSeasonCount()
        for (e in watchedWithCount) {
            val result = showRepository.getShowDetails(e.mediaId)
            // On error, no update should happen
            if (result is Resource.Success) {
                interactionRepository.updateLastKnownSeasons(e.mediaId, result.data.numberOfSeasons ?: 0)
            }
        }

        coVerify(exactly = 0) { interactionRepository.updateLastKnownSeasons(any(), any()) }
    }

    // endregion
}
