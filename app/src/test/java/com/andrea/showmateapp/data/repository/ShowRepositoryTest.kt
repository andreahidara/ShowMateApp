package com.andrea.showmateapp.data.repository

import com.andrea.showmateapp.data.local.ShowDao
import com.andrea.showmateapp.data.model.MediaEntity
import com.andrea.showmateapp.data.model.MediaResponse
import com.andrea.showmateapp.data.network.MediaContent
import com.andrea.showmateapp.data.network.TmdbApiService
import com.andrea.showmateapp.util.Resource
import java.io.IOException
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.anyOrNull
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

class ShowRepositoryTest {

    private val apiService: TmdbApiService = mock()
    private val showDao: ShowDao = mock()
    private lateinit var repository: ShowRepository

    private fun fakeResponse(vararg items: MediaContent) = MediaResponse(
        page = 1,
        results = items.toList(),
        total_pages = 1,
        total_results = items.size
    )

    @Before
    fun setup() {
        repository = ShowRepository(apiService, showDao)
    }


    @Test
    fun `searchShows returns success when API responds`() = runTest {
        whenever(apiService.searchMedia(any(), any(), any())).thenReturn(
            fakeResponse(MediaContent(id = 1, name = "Breaking Bad"))
        )

        val result = repository.searchShows("breaking bad")

        assertTrue(result is Resource.Success)
        assertEquals(1, (result as Resource.Success).data.first().id)
    }

    @Test
    fun `searchShows returns error when API throws`() = runTest {
        whenever(apiService.searchMedia(any(), any(), any())).thenAnswer { throw IOException("No network") }

        val result = repository.searchShows("anything")

        assertTrue(result is Resource.Error)
    }


    @Test
    fun `getPopularShows returns cached data when network fails`() = runTest {
        whenever(apiService.getPopularMedia(any(), any())).thenAnswer { throw IOException("timeout") }
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
        whenever(apiService.getPopularMedia(any(), any())).thenAnswer { throw IOException("timeout") }
        whenever(showDao.getShowsByCategory("popular")).thenReturn(emptyList())

        val result = repository.getPopularShows()

        assertTrue(result is Resource.Error)
    }


    @Test
    fun `getShowDetails returns success and caches result`() = runTest {
        val detail = MediaContent(id = 42, name = "Stranger Things")
        whenever(apiService.getMediaDetails(any(), any(), anyOrNull())).thenReturn(detail)
        whenever(showDao.insertShows(any())).thenReturn(Unit)

        val result = repository.getShowDetails(42)

        assertTrue(result is Resource.Success)
        assertEquals(42, (result as Resource.Success).data.id)
        verify(showDao).insertShows(any())
    }

    @Test
    fun `getShowDetails falls back to Room when network fails`() = runTest {
        whenever(apiService.getMediaDetails(any(), any(), anyOrNull())).thenAnswer { throw IOException("offline") }
        val cached = MediaEntity(id = 42, name = "Cached Detail", overview = "", posterPath = "", category = "liked")
        whenever(showDao.getLikedShowById(42)).thenReturn(cached)

        val result = repository.getShowDetails(42)

        assertTrue(result is Resource.Success)
        assertEquals(42, (result as Resource.Success).data.id)
    }

    @Test
    fun `getShowDetails returns error when network fails and cache is empty`() = runTest {
        whenever(apiService.getMediaDetails(any(), any(), anyOrNull())).thenAnswer { throw IOException("offline") }
        whenever(showDao.getLikedShowById(99)).thenReturn(null)
        whenever(showDao.getWatchedShowById(99)).thenReturn(null)

        val result = repository.getShowDetails(99)

        assertTrue(result is Resource.Error)
    }
}
