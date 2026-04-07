package com.andrea.showmateapp.ui.screens.home

import com.andrea.showmateapp.data.network.MediaContent
import com.andrea.showmateapp.data.repository.ShowRepository
import com.andrea.showmateapp.data.repository.UserRepository
import com.andrea.showmateapp.domain.usecase.GetRecommendationsUseCase
import com.andrea.showmateapp.util.MainDispatcherRule
import com.andrea.showmateapp.util.Resource
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.junit.MockitoJUnitRunner
import org.mockito.kotlin.*

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(MockitoJUnitRunner::class)
class HomeViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val repository: ShowRepository = mock()
    private val getRecommendationsUseCase: GetRecommendationsUseCase = mock()
    private val userRepository: UserRepository = mock()

    @Test
    fun `fetchHomeData updates state properly on success`() = runTest {
        val mockMedia = listOf(MediaContent(id = 1, name = "Breaking Bad"))

        whenever(repository.getTrendingShows()).thenReturn(Resource.Success(mockMedia))
        whenever(repository.getTrendingThisWeek()).thenReturn(Resource.Success(emptyList()))
        whenever(repository.discoverShows(anyOrNull(), anyOrNull(), anyOrNull(), anyOrNull(), anyOrNull(), anyOrNull(), anyOrNull(), anyOrNull(), anyOrNull(), anyOrNull(), anyOrNull())).thenReturn(Resource.Success(emptyList()))
        whenever(repository.getShowsOnTheAir(anyOrNull())).thenReturn(Resource.Success(emptyList()))
        
        whenever(getRecommendationsUseCase.scoreShows(any())).thenAnswer { it.arguments[0] }
        whenever(userRepository.getUserProfile()).thenReturn(null)
        whenever(userRepository.getCurrentUserEmail()).thenReturn("andrea@showmate.com")

        val viewModel = HomeViewModel(repository, getRecommendationsUseCase, userRepository)

        val state = viewModel.uiState.value
        assertFalse(state.isLoading)
        assertEquals("Andrea", state.userName)
        assertEquals(1, state.trendingShows.size)
        assertEquals("Breaking Bad", state.trendingShows[0].name)
    }

    @Test
    fun `selectPlatform fetches and updates platformShows`() = runTest {
        val mockMedia = listOf(MediaContent(id = 2, name = "Stranger Things"))
        
        whenever(repository.getTrendingShows()).thenReturn(Resource.Success(emptyList()))
        whenever(repository.getTrendingThisWeek()).thenReturn(Resource.Success(emptyList()))
        whenever(repository.discoverShows(anyOrNull(), anyOrNull(), anyOrNull(), anyOrNull(), anyOrNull(), anyOrNull(), anyOrNull(), anyOrNull(), anyOrNull(), anyOrNull(), anyOrNull())).thenReturn(Resource.Success(emptyList()))
        whenever(repository.getShowsOnTheAir(anyOrNull())).thenReturn(Resource.Success(emptyList()))
        whenever(userRepository.getUserProfile()).thenReturn(null)
        whenever(userRepository.getCurrentUserEmail()).thenReturn("andrea@showmate.com")
        
        whenever(repository.getShowsOnTheAir("8")).thenReturn(Resource.Success(mockMedia))
        whenever(getRecommendationsUseCase.scoreShows(any())).thenAnswer { it.arguments[0] }

        val viewModel = HomeViewModel(repository, getRecommendationsUseCase, userRepository)

        // Select Netflix (id "8")
        viewModel.onAction(com.andrea.showmateapp.ui.screens.home.HomeAction.SelectPlatform("Netflix"))

        val state = viewModel.uiState.value
        assertEquals("Netflix", state.selectedPlatform)
        assertTrue(state.platformShows.containsKey("Netflix"))
        assertEquals(1, state.platformShows["Netflix"]?.size)
        assertEquals("Stranger Things", state.platformShows["Netflix"]?.get(0)?.name)
    }
}
