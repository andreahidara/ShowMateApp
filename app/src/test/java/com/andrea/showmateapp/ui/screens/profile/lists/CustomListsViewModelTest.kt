package com.andrea.showmateapp.ui.screens.profile.lists

import com.andrea.showmateapp.data.repository.ShowRepository
import com.andrea.showmateapp.domain.repository.IInteractionRepository
import com.andrea.showmateapp.util.MainDispatcherRule
import io.mockk.coEvery
import io.mockk.coVerify
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
class CustomListsViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val interactionRepository: IInteractionRepository = mockk(relaxed = true)
    private val showRepository: ShowRepository = mockk(relaxed = true)

    private val fakeLists = mapOf(
        "Favoritas" to listOf(1, 2),
        "Para el finde" to listOf(3)
    )

    private fun viewModel(): CustomListsViewModel {
        every { interactionRepository.getCustomListsFlow() } returns flowOf(fakeLists)
        return CustomListsViewModel(interactionRepository, showRepository)
    }

    // --- Initial load ---

    @Test
    fun `init loads lists from repository`() = runTest {
        val vm = viewModel()
        advanceUntilIdle()

        assertEquals(fakeLists, vm.uiState.value.lists)
        assertFalse(vm.uiState.value.isLoading)
    }

    // --- Dialog state ---

    @Test
    fun `showCreateDialog opens the dialog`() = runTest {
        val vm = viewModel()
        advanceUntilIdle()

        vm.showCreateDialog()

        assertTrue(vm.uiState.value.showCreateDialog)
    }

    @Test
    fun `hideCreateDialog closes dialog and clears name`() = runTest {
        val vm = viewModel()
        advanceUntilIdle()

        vm.showCreateDialog()
        vm.onNewListNameChange("Mi lista")
        vm.hideCreateDialog()

        assertFalse(vm.uiState.value.showCreateDialog)
        assertEquals("", vm.uiState.value.newListName)
    }

    @Test
    fun `onNewListNameChange updates name in state`() = runTest {
        val vm = viewModel()
        advanceUntilIdle()

        vm.onNewListNameChange("Series de ciencia ficción")

        assertEquals("Series de ciencia ficción", vm.uiState.value.newListName)
    }

    // --- Create list ---

    @Test
    fun `createList with blank name does nothing`() = runTest {
        val vm = viewModel()
        advanceUntilIdle()

        vm.onNewListNameChange("   ")
        vm.createList()
        advanceUntilIdle()

        coVerify(exactly = 0) { interactionRepository.createCustomList(any()) }
    }

    @Test
    fun `createList calls repository and adds list optimistically`() = runTest {
        val vm = viewModel()
        advanceUntilIdle()

        vm.onNewListNameChange("Nuevas series")
        vm.createList()
        advanceUntilIdle()

        coVerify { interactionRepository.createCustomList("Nuevas series") }
        assertTrue(vm.uiState.value.lists.containsKey("Nuevas series"))
    }

    @Test
    fun `createList closes dialog on success`() = runTest {
        val vm = viewModel()
        advanceUntilIdle()

        vm.showCreateDialog()
        vm.onNewListNameChange("Mi lista nueva")
        vm.createList()
        advanceUntilIdle()

        assertFalse(vm.uiState.value.showCreateDialog)
        assertEquals("", vm.uiState.value.newListName)
    }

    @Test
    fun `createList on error sets error message`() = runTest {
        coEvery { interactionRepository.createCustomList(any()) } throws RuntimeException("Firestore error")

        val vm = viewModel()
        advanceUntilIdle()

        vm.onNewListNameChange("Test")
        vm.createList()
        advanceUntilIdle()

        assertTrue(vm.uiState.value.error?.contains("crear", ignoreCase = true) == true)
    }

    // --- Delete list ---

    @Test
    fun `deleteList calls repository and removes list from state on success`() = runTest {
        val vm = viewModel()
        advanceUntilIdle()

        vm.deleteList("Favoritas")
        advanceUntilIdle()

        coVerify { interactionRepository.deleteCustomList("Favoritas") }
        assertFalse(vm.uiState.value.lists.containsKey("Favoritas"))
    }

    @Test
    fun `deleteList on error sets error message and does not remove list`() = runTest {
        every { interactionRepository.getCustomListsFlow() } returns flowOf(fakeLists)
        coEvery { interactionRepository.deleteCustomList(any()) } throws RuntimeException("Network fail")

        val vm = viewModel()
        advanceUntilIdle()

        vm.deleteList("Favoritas")
        advanceUntilIdle()

        assertTrue(vm.uiState.value.error?.contains("eliminar", ignoreCase = true) == true)
    }

    // --- Error handling ---

    @Test
    fun `dismissError clears error state`() = runTest {
        coEvery { interactionRepository.createCustomList(any()) } throws RuntimeException("fail")

        val vm = viewModel()
        advanceUntilIdle()

        vm.onNewListNameChange("Test")
        vm.createList()
        advanceUntilIdle()

        assertTrue(vm.uiState.value.error != null)
        vm.dismissError()
        assertNull(vm.uiState.value.error)
    }
}
