package com.andrea.showmateapp.ui.screens.home

import com.andrea.showmateapp.data.network.MediaContent
import com.andrea.showmateapp.data.repository.ShowRepository
import com.andrea.showmateapp.data.repository.UserRepository
import com.andrea.showmateapp.domain.repository.IInteractionRepository
import com.andrea.showmateapp.domain.usecase.GetRecommendationsUseCase
import com.andrea.showmateapp.util.MainDispatcherRule
import com.andrea.showmateapp.util.PerfTracer
import com.andrea.showmateapp.util.Resource
import com.google.firebase.crashlytics.FirebaseCrashlytics
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.mockkStatic
import io.mockk.unmockkAll
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.anyOrNull
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever

@OptIn(ExperimentalCoroutinesApi::class)
class HomeViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val repository: ShowRepository = mock()
    private val getRecommendationsUseCase: GetRecommendationsUseCase = mock()
    private val userRepository: UserRepository = mock()
    private val interactionRepository: IInteractionRepository = mock()

    @Before
    fun setup() {
        mockkObject(PerfTracer)
        every { PerfTracer.start(any()) } returns null

        mockkStatic(FirebaseCrashlytics::class)
        val mockCrashlytics = mockk<FirebaseCrashlytics>(relaxed = true)
        every { FirebaseCrashlytics.getInstance() } returns mockCrashlytics
    }

    @After
    fun teardown() {
        unmockkAll()
    }

    @Test
    fun `fetchHomeData updates state properly on success`() = runTest {
        val mockMedia = listOf(MediaContent(id = 1, name = "Breaking Bad"))

        whenever(repository.getTrendingShows()).thenReturn(Resource.Success(mockMedia))
        whenever(repository.getTrendingThisWeek()).thenReturn(Resource.Success(emptyList()))
        whenever(
            repository.discoverShows(
                anyOrNull(), anyOrNull(), anyOrNull(), anyOrNull(), anyOrNull(), anyOrNull(),
                anyOrNull(), anyOrNull(), anyOrNull(), anyOrNull(), anyOrNull(), any()
            )
        ).thenReturn(Resource.Success(emptyList()))
        whenever(repository.getShowsOnTheAir(anyOrNull())).thenReturn(Resource.Success(emptyList()))

        whenever(getRecommendationsUseCase.scoreShows(any())).thenAnswer { it.arguments[0] }
        whenever(userRepository.getUserProfile()).thenReturn(null)
        whenever(userRepository.getCurrentUserEmail()).thenReturn("andrea@showmate.com")

        val viewModel = HomeViewModel(repository, getRecommendationsUseCase, userRepository, interactionRepository)

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
        whenever(
            repository.discoverShows(
                anyOrNull(), anyOrNull(), anyOrNull(), anyOrNull(), anyOrNull(), anyOrNull(),
                anyOrNull(), anyOrNull(), anyOrNull(), anyOrNull(), anyOrNull(), any()
            )
        ).thenReturn(Resource.Success(emptyList()))
        whenever(repository.getShowsOnTheAir(anyOrNull())).thenReturn(Resource.Success(emptyList()))
        whenever(userRepository.getUserProfile()).thenReturn(null)
        whenever(userRepository.getCurrentUserEmail()).thenReturn("andrea@showmate.com")

        whenever(repository.getShowsOnTheAir("8")).thenReturn(Resource.Success(mockMedia))
        whenever(getRecommendationsUseCase.scoreShows(any())).thenAnswer { it.arguments[0] }

        val viewModel = HomeViewModel(repository, getRecommendationsUseCase, userRepository, interactionRepository)

        viewModel.onAction(com.andrea.showmateapp.ui.screens.home.HomeAction.SelectPlatform("Netflix"))

        val state = viewModel.uiState.value
        assertEquals("Netflix", state.selectedPlatform)
        assertTrue(state.platformShows.containsKey("Netflix"))
        assertEquals(1, state.platformShows["Netflix"]?.size)
        assertEquals("Stranger Things", state.platformShows["Netflix"]?.get(0)?.name)
    }
}
