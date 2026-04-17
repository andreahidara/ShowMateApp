package com.andrea.showmateapp.ui.screens.search

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import com.andrea.showmateapp.data.model.MediaContent
import com.andrea.showmateapp.data.network.TmdbApiService
import com.andrea.showmateapp.domain.repository.IShowRepository
import com.andrea.showmateapp.domain.usecase.GetRecommendationsUseCase
import com.andrea.showmateapp.util.MainDispatcherRule
import com.andrea.showmateapp.util.Resource
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class SearchViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val showRepository: IShowRepository = mockk(relaxed = true)
    private val tmdbApiService: TmdbApiService = mockk(relaxed = true)
    private val getRecommendationsUseCase: GetRecommendationsUseCase = mockk(relaxed = true)
    private val dataStore: DataStore<Preferences> = mockk(relaxed = true)

    private fun viewModel(): SearchViewModel {
        val fakePreferences = mockk<Preferences>(relaxed = true)
        every { dataStore.data } returns flowOf(fakePreferences)
        return SearchViewModel(showRepository, tmdbApiService, getRecommendationsUseCase, dataStore)
    }

    // --- Filter state ---

    @Test
    fun `updateGenre sets selectedGenre and activates filter`() = runTest {
        val vm = viewModel()
        advanceUntilIdle()

        vm.updateGenre("18")

        assertEquals("18", vm.selectedGenre.value)
        assertTrue(vm.isFilterActive.value)
    }

    @Test
    fun `updateGenre with null deactivates filter when no other filters set`() = runTest {
        val vm = viewModel()
        advanceUntilIdle()

        vm.updateGenre("18")
        vm.updateGenre(null)

        assertNull(vm.selectedGenre.value)
        assertFalse(vm.isFilterActive.value)
    }

    @Test
    fun `updateRating sets selectedRating and activates filter`() = runTest {
        val vm = viewModel()
        advanceUntilIdle()

        vm.updateRating(7.5f)

        assertEquals(7.5f, vm.selectedRating.value)
        assertTrue(vm.isFilterActive.value)
    }

    @Test
    fun `updatePlatform sets selectedPlatform and activates filter`() = runTest {
        val vm = viewModel()
        advanceUntilIdle()

        vm.updatePlatform("8")

        assertEquals("8", vm.selectedPlatform.value)
        assertTrue(vm.isFilterActive.value)
    }

    @Test
    fun `updateYearRange with non-default values activates filter`() = runTest {
        val vm = viewModel()
        advanceUntilIdle()

        vm.updateYearRange(2000, SearchViewModel.CURRENT_YEAR)

        assertEquals(2000, vm.yearFrom.value)
        assertTrue(vm.isFilterActive.value)
    }

    @Test
    fun `clearFilters resets all filter state`() = runTest {
        val vm = viewModel()
        advanceUntilIdle()

        vm.updateGenre("18")
        vm.updateRating(8f)
        vm.updatePlatform("337")
        vm.clearFilters()

        assertNull(vm.selectedGenre.value)
        assertNull(vm.selectedRating.value)
        assertNull(vm.selectedPlatform.value)
        assertEquals(SearchViewModel.MIN_YEAR, vm.yearFrom.value)
        assertEquals(SearchViewModel.CURRENT_YEAR, vm.yearTo.value)
        assertFalse(vm.isFilterActive.value)
    }

    // --- Search mode switching ---

    @Test
    fun `setSearchMode to same mode does not clear results`() = runTest {
        coEvery { getRecommendationsUseCase.scoreShows(any()) } returns emptyList()
        coEvery { showRepository.getPopularShows() } returns Resource.Success(emptyList())
        val vm = viewModel()
        advanceUntilIdle()

        vm.setSearchMode(SearchMode.TITLE)

        assertEquals(SearchMode.TITLE, vm.searchMode.value)
    }

    @Test
    fun `setSearchMode to ACTOR changes mode`() = runTest {
        val vm = viewModel()
        advanceUntilIdle()

        vm.setSearchMode(SearchMode.ACTOR)

        assertEquals(SearchMode.ACTOR, vm.searchMode.value)
    }

    @Test
    fun `setSearchMode to CREATOR changes mode`() = runTest {
        val vm = viewModel()
        advanceUntilIdle()

        vm.setSearchMode(SearchMode.CREATOR)

        assertEquals(SearchMode.CREATOR, vm.searchMode.value)
    }

    // --- Suggestions ---

    @Test
    fun `updateSuggestions with query shorter than 2 chars returns empty`() = runTest {
        val vm = viewModel()
        advanceUntilIdle()

        vm.updateSuggestions("A")

        assertTrue(vm.suggestions.value.isEmpty())
    }

    @Test
    fun `updateSuggestions filters trending shows by name`() = runTest {
        val shows = listOf(
            MediaContent(id = 1, name = "Breaking Bad", voteAverage = 9.5f),
            MediaContent(id = 2, name = "Better Call Saul", voteAverage = 9.0f),
            MediaContent(id = 3, name = "Ozark", voteAverage = 8.4f)
        )
        coEvery { showRepository.getPopularShows() } returns Resource.Success(shows)
        coEvery { getRecommendationsUseCase.scoreShows(any()) } answers { firstArg() }

        val vm = viewModel()
        advanceUntilIdle()

        vm.updateSuggestions("break")

        assertEquals(1, vm.suggestions.value.size)
        assertEquals("Breaking Bad", vm.suggestions.value.first().name)
    }

    @Test
    fun `updateSuggestions returns at most 3 results`() = runTest {
        val shows = (1..10).map { MediaContent(id = it, name = "Drama $it", voteAverage = 8f) }
        coEvery { showRepository.getPopularShows() } returns Resource.Success(shows)
        coEvery { getRecommendationsUseCase.scoreShows(any()) } answers { firstArg() }

        val vm = viewModel()
        advanceUntilIdle()

        vm.updateSuggestions("drama")

        assertTrue(vm.suggestions.value.size <= 3)
    }

    // --- Recent searches ---

    @Test
    fun `removeRecentSearch removes entry from list`() = runTest {
        val vm = viewModel()
        advanceUntilIdle()

        vm.removeRecentSearch("breaking bad")

        assertFalse(vm.recentSearches.value.contains("breaking bad"))
    }

    @Test
    fun `clearRecentSearches empties the list`() = runTest {
        val vm = viewModel()
        advanceUntilIdle()

        vm.clearRecentSearches()

        assertTrue(vm.recentSearches.value.isEmpty())
    }
}
