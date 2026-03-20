package com.example.showmateapp.data.repository

import com.example.showmateapp.data.local.ShowDao
import com.example.showmateapp.data.model.MediaEntity
import com.example.showmateapp.data.network.MediaContent
import com.example.showmateapp.data.network.TmdbApiService
import com.example.showmateapp.util.Resource
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import java.io.IOException

class ShowRepositoryTest {

    private val apiService: TmdbApiService = mock()
    private val showDao: ShowDao = mock()
    private lateinit var repository: ShowRepository

    @Before
    fun setup() {
        repository = ShowRepository(apiService, showDao)
    }

    // ── searchShows ────────────────────────────────────────────────────────────

    @Test
    fun `searchShows returns success when API responds`() = runTest {
        val fakeResponse = com.example.showmateapp.data.model.MediaResponse(
            results = listOf(MediaContent(id = 1, name = "Breaking Bad"))
        )
        whenever(apiService.searchMedia("breaking bad")).thenReturn(fakeResponse)

        val result = repository.searchShows("breaking bad")

        assertTrue(result is Resource.Success)
        assertEquals(1, (result as Resource.Success).data.first().id)
    }

    @Test
    fun `searchShows returns error on IOException`() = runTest {
        whenever(apiService.searchMedia(any())).thenThrow(IOException("No network"))

        val result = repository.searchShows("anything")

        assertTrue(result is Resource.Error)
    }

    // ── getPopularShows ────────────────────────────────────────────────────────

    @Test
    fun `getPopularShows returns cached data when network fails`() = runTest {
        whenever(apiService.getPopularMedia()).thenThrow(IOException("timeout"))
        val cached = listOf(
            MediaEntity(id = 5, name = "Cached Show", overview = "", posterPath = "", category = "popular")
        )
        whenever(showDao.getShowsByCategory("popular")).thenReturn(cached)

        val result = repository.getPopularShows()

        assertTrue(result is Resource.Success)
        assertEquals(5, (result as Resource.Success).data.first().id)
    }

    @Test
    fun `getPopularShows returns error when cache is also empty`() = runTest {
        whenever(apiService.getPopularMedia()).thenThrow(IOException("timeout"))
        whenever(showDao.getShowsByCategory("popular")).thenReturn(emptyList())

        val result = repository.getPopularShows()

        assertTrue(result is Resource.Error)
    }

    // ── getShowDetails ─────────────────────────────────────────────────────────

    @Test
    fun `getShowDetails returns success and caches result`() = runTest {
        val detail = MediaContent(id = 42, name = "Stranger Things")
        whenever(apiService.getMediaDetails(42)).thenReturn(detail)
        whenever(showDao.insertShows(any())).thenReturn(Unit)

        val result = repository.getShowDetails(42)

        assertTrue(result is Resource.Success)
        assertEquals(42, (result as Resource.Success).data.id)
        verify(showDao).insertShows(any())
    }

    @Test
    fun `getShowDetails falls back to Room when network fails`() = runTest {
        whenever(apiService.getMediaDetails(42)).thenThrow(IOException("offline"))
        val cached = MediaEntity(id = 42, name = "Cached Detail", overview = "", posterPath = "", category = "details")
        whenever(showDao.getShowById(42)).thenReturn(cached)

        val result = repository.getShowDetails(42)

        assertTrue(result is Resource.Success)
        assertEquals(42, (result as Resource.Success).data.id)
    }

    @Test
    fun `getShowDetails returns error when network fails and cache is empty`() = runTest {
        whenever(apiService.getMediaDetails(99)).thenThrow(IOException("offline"))
        whenever(showDao.getShowById(99)).thenReturn(null)

        val result = repository.getShowDetails(99)

        assertTrue(result is Resource.Error)
    }
}
