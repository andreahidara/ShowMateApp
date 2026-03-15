package com.example.showmateapp.domain.usecase

import com.example.showmateapp.data.model.UserProfile
import com.example.showmateapp.data.network.MediaContent
import com.example.showmateapp.data.repository.ShowRepository
import com.example.showmateapp.data.repository.UserRepository
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.mockito.Mock
import org.mockito.Mockito.`when`
import org.mockito.MockitoAnnotations

class GetRecommendationsUseCaseTest {

    @Mock
    private lateinit var userRepository: UserRepository

    @Mock
    private lateinit var showRepository: ShowRepository

    private lateinit var getRecommendationsUseCase: GetRecommendationsUseCase

    @Before
    fun setup() {
        MockitoAnnotations.openMocks(this)
        getRecommendationsUseCase = GetRecommendationsUseCase(userRepository, showRepository)
    }

    @Test
    fun `execute returns popular shows when user profile is null`() = runBlocking {
        `when`(userRepository.getUserProfile()).thenReturn(null)
        val popularShows = listOf(MediaContent(id = 1, name = "Popular Show"))
        `when`(showRepository.getPopularShows()).thenReturn(popularShows)

        val result = getRecommendationsUseCase.execute()

        assertEquals(popularShows, result)
    }

    @Test
    fun `execute returns popular shows when user has no genre scores`() = runBlocking {
        val emptyProfile = UserProfile(userId = "1", genreScores = emptyMap())
        `when`(userRepository.getUserProfile()).thenReturn(emptyProfile)
        val popularShows = listOf(MediaContent(id = 1, name = "Popular Show"))
        `when`(showRepository.getPopularShows()).thenReturn(popularShows)

        val result = getRecommendationsUseCase.execute()

        assertEquals(popularShows, result)
    }

    @Test
    fun `test recommendation excludes disliked media`() = runBlocking {
        val userProfile = UserProfile(
            userId = "testUser",
            genreScores = mapOf("28" to 5f),
            dislikedMediaIds = listOf(2)
        )
        `when`(userRepository.getUserProfile()).thenReturn(userProfile)
        `when`(userRepository.getWatchedShows()).thenReturn(emptyList())

        val candidates = listOf(
            MediaContent(id = 1, name = "Show 1", genreIds = listOf(28), popularity = 100f),
            MediaContent(id = 2, name = "Show 2 (Disliked)", genreIds = listOf(28), popularity = 100f),
            MediaContent(id = 3, name = "Show 3", genreIds = listOf(28), popularity = 100f)
        )
        `when`(showRepository.getDetailedRecommendations()).thenReturn(candidates)

        val result = getRecommendationsUseCase.execute()

        assertEquals(2, result.size)
        assertTrue(result.none { it.id == 2 })
    }
}
