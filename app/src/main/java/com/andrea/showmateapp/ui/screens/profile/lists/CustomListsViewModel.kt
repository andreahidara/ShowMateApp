package com.andrea.showmateapp.ui.screens.profile.lists

import android.content.Context
import androidx.compose.runtime.Immutable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.andrea.showmateapp.R
import com.andrea.showmateapp.domain.repository.IInteractionRepository
import com.andrea.showmateapp.domain.repository.IShowRepository
import com.andrea.showmateapp.util.Resource
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

@Immutable
data class CustomListsUiState(
    val lists: Map<String, List<Int>> = emptyMap(),
    val posterPaths: Map<String, String?> = emptyMap(),
    val isLoading: Boolean = true,
    val newListName: String = "",
    val showCreateDialog: Boolean = false,
    val error: String? = null
)

@HiltViewModel
class CustomListsViewModel @Inject constructor(
    private val interactionRepository: IInteractionRepository,
    private val showRepository: IShowRepository,
    @ApplicationContext private val context: Context
) : ViewModel() {

    private val _uiState = MutableStateFlow(CustomListsUiState())
    val uiState: StateFlow<CustomListsUiState> = _uiState.asStateFlow()

    init {
        loadLists()
    }

    fun loadLists() {
        viewModelScope.launch {
            try {
                interactionRepository.getCustomListsFlow().collect { lists ->
                    _uiState.update { it.copy(lists = lists, isLoading = false) }
                    loadPosters(lists)
                }
            } catch (e: Exception) {
                if (e is CancellationException) throw e
                _uiState.update { it.copy(isLoading = false, error = context.getString(R.string.custom_list_error_load)) }
            }
        }
    }

    private fun loadPosters(lists: Map<String, List<Int>>) {
        viewModelScope.launch {
            try {
                val posterEntries = lists.entries.map { (name, ids) ->
                    async {
                        val firstId = ids.firstOrNull() ?: return@async name to null
                        val path = when (val r = showRepository.getShowDetails(firstId)) {
                            is Resource.Success -> r.data.posterPath
                            else -> null
                        }
                        name to path
                    }
                }.awaitAll()
                _uiState.update { it.copy(posterPaths = posterEntries.toMap()) }
            } catch (e: Exception) {
                if (e is CancellationException) throw e
            }
        }
    }

    fun onNewListNameChange(name: String) {
        _uiState.update { it.copy(newListName = name) }
    }

    fun showCreateDialog() {
        _uiState.update { it.copy(showCreateDialog = true) }
    }

    fun hideCreateDialog() {
        _uiState.update { it.copy(showCreateDialog = false, newListName = "") }
    }

    fun createList() {
        val name = _uiState.value.newListName.trim()
        if (name.isBlank()) return
        viewModelScope.launch {
            try {
                interactionRepository.createCustomList(name)
                _uiState.update {
                    it.copy(
                        lists = it.lists + (name to emptyList()),
                        posterPaths = it.posterPaths + (name to null),
                        showCreateDialog = false,
                        newListName = ""
                    )
                }
            } catch (e: Exception) {
                if (e is CancellationException) throw e
                _uiState.update { it.copy(error = context.getString(R.string.custom_list_error_create)) }
            }
        }
    }

    fun deleteList(name: String) {
        viewModelScope.launch {
            try {
                interactionRepository.deleteCustomList(name)
                _uiState.update { it.copy(lists = it.lists - name, posterPaths = it.posterPaths - name) }
            } catch (e: Exception) {
                if (e is CancellationException) throw e
                _uiState.update { it.copy(error = context.getString(R.string.custom_list_error_delete)) }
            }
        }
    }

    fun dismissError() {
        _uiState.update { it.copy(error = null) }
    }
}
