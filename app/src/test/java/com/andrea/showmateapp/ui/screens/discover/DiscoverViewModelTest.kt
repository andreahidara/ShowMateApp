package com.andrea.showmateapp.ui.screens.discover

import android.content.Context
import com.andrea.showmateapp.data.model.MediaContent
import com.andrea.showmateapp.domain.repository.IShowRepository
import com.andrea.showmateapp.domain.repository.IUserRepository
import com.andrea.showmateapp.domain.usecase.GetRecommendationsUseCase
import com.andrea.showmateapp.util.MainDispatcherRule
import com.andrea.showmateapp.util.NetworkMonitor
import com.andrea.showmateapp.util.Resource
import com.google.firebase.crashlytics.FirebaseCrashlytics
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.unmockkAll
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class DiscoverViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val repository: IShowRepository = mockk(relaxed = true)
    private val userRepository: IUserRepository = mockk(relaxed = true)
    private val getRecommendationsUseCase: GetRecommendationsUseCase = mockk(relaxed = true)
    private val networkMonitor: NetworkMonitor = mockk()
    private val interactionRepository: com.andrea.showmateapp.domain.repository.IInteractionRepository = mockk(relaxed = true)
    private val context: Context = mockk(relaxed = true)

    private val mockMedia = listOf(
        MediaContent(id = 1, name = "Breaking Bad", posterPath = "/bb.jpg"),
        MediaContent(id = 2, name = "The Wire", posterPath = "/tw.jpg")
    )

    @Before
    fun setup() {
        mockkStatic(FirebaseCrashlytics::class)
        val mockCrashlytics = mockk<FirebaseCrashlytics>(relaxed = true)
        every { FirebaseCrashlytics.getInstance() } returns mockCrashlytics

        // Default: online
        every { networkMonitor.isOnline } returns flowOf(true)

        // Default stubs
        coEvery { userRepository.getUserProfile() } returns null
        coEvery { repository.getShowsByGenres(any(), any()) } returns Resource.Success(emptyList())
        coEvery {
            repository.discoverShows(any(), any(), any(), any(), any(), any(), any(), any(), any(), any(), any(), any())
        } returns Resource.Success(emptyList())
        coEvery { getRecommendationsUseCase.execute() } returns emptyList()
        coEvery { getRecommendationsUseCase.scoreShows(any(), any()) } answers { firstArg() }
        every { context.getString(any()) } returns ""
    }

    @After
    fun teardown() {
        unmockkAll()
    }

    private fun createViewModel() = DiscoverViewModel(
        repository = repository,
        userRepository = userRepository,
        interactionRepository = interactionRepository,
        getRecommendationsUseCase = getRecommendationsUseCase,
        networkMonitor = networkMonitor,
        context = context
    )

    @Test
    fun `loadDiscoverContent success sets isLoading false and no errorMessage`() = runTest {
        coEvery { getRecommendationsUseCase.execute() } returns mockMedia
        coEvery { getRecommendationsUseCase.scoreShows(any(), any()) } answers { firstArg() }

        val vm = createViewModel()
        advanceUntilIdle()

        assertFalse(vm.uiState.value.isLoading)
        assertNull(vm.uiState.value.errorMessage)
    }

    @Test
    fun `loadDiscoverContent populates topGenreShows`() = runTest {
        coEvery { repository.discoverShowsPaged(any(), any(), any(), any(), any()) } returns Resource.Success(mockMedia to 1)
        coEvery { getRecommendationsUseCase.scoreShows(any(), any()) } answers { firstArg() }

        val vm = createViewModel()
        advanceUntilIdle()

        assertTrue(vm.uiState.value.topGenreShows.isNotEmpty())
    }

    @Test
    fun `loadDiscoverContent with no content sets errorMessage`() = runTest {
        // Todas las llamadas devuelven listas vacías → hero=null, topGenreShows vacío → errorMessage != null
        coEvery { repository.getShowsByGenres(any(), any()) } returns Resource.Success(emptyList())
        coEvery { getRecommendationsUseCase.execute() } returns emptyList()
        coEvery { getRecommendationsUseCase.scoreShows(any(), any()) } returns emptyList()

        val vm = createViewModel()
        advanceUntilIdle()

        assertNotNull(vm.uiState.value.errorMessage)
        assertFalse(vm.uiState.value.isLoading)
    }

    @Test
    fun `isFromCache is true when offline and content loads`() = runTest {
        every { networkMonitor.isOnline } returns flowOf(false)
        coEvery { repository.getShowsByGenres("18", emptyList()) } returns Resource.Success(mockMedia)
        coEvery { getRecommendationsUseCase.scoreShows(any(), any()) } answers { firstArg() }
        coEvery { getRecommendationsUseCase.execute() } returns mockMedia

        val vm = createViewModel()
        advanceUntilIdle()

        assertTrue(vm.uiState.value.isFromCache)
    }

    @Test
    fun `isFromCache is false when online`() = runTest {
        every { networkMonitor.isOnline } returns flowOf(true)
        coEvery { getRecommendationsUseCase.execute() } returns mockMedia
        coEvery { getRecommendationsUseCase.scoreShows(any(), any()) } answers { firstArg() }

        val vm = createViewModel()
        advanceUntilIdle()

        assertFalse(vm.uiState.value.isFromCache)
    }

    @Test
    fun `retry after no-content error clears errorMessage when content loads`() = runTest {
        // Primera carga: sin contenido → errorMessage
        coEvery { getRecommendationsUseCase.execute() } returns emptyList()
        coEvery { getRecommendationsUseCase.scoreShows(any(), any()) } returns emptyList()
        val vm = createViewModel()
        advanceUntilIdle()
        assertNotNull(vm.uiState.value.errorMessage)

        // Retry con contenido real
        coEvery { getRecommendationsUseCase.execute() } returns mockMedia
        coEvery { getRecommendationsUseCase.scoreShows(any(), any()) } answers { firstArg() }
        vm.retry()
        advanceUntilIdle()

        assertFalse(vm.uiState.value.isLoading)
        assertNull(vm.uiState.value.errorMessage)
    }

    @Test
    fun `refresh sets isRefreshing false after completion`() = runTest {
        val vm = createViewModel()
        advanceUntilIdle()

        vm.refresh()
        advanceUntilIdle()

        assertFalse(vm.uiState.value.isRefreshing)
    }

    @Test
    fun `loadMoreTopGenre does nothing when already at last page`() = runTest {
        val vm = createViewModel()
        advanceUntilIdle()

        // topGenrePage == topGenreTotalPages == 1 por defecto → no carga más
        vm.loadMoreTopGenre()
        advanceUntilIdle()

        assertFalse(vm.uiState.value.isLoadingMoreTopGenre)
    }
}
