package com.example.showmateapp.ui.screens.profile.lists

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.toRoute
import com.example.showmateapp.data.network.MediaContent
import com.example.showmateapp.data.repository.ShowRepository
import com.example.showmateapp.data.repository.UserRepository
import com.example.showmateapp.ui.navigation.Screen
import com.example.showmateapp.util.Resource
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ListDetailUiState(
    val listName: String = "",
    val shows: List<MediaContent> = emptyList(),
    val isLoading: Boolean = true,
    val error: String? = null
)

@HiltViewModel
class ListDetailViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val showRepository: ShowRepository,
    private val userRepository: UserRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(ListDetailUiState())
    val uiState: StateFlow<ListDetailUiState> = _uiState.asStateFlow()

    init {
        val route = savedStateHandle.toRoute<Screen.ListDetail>()
        _uiState.update { it.copy(listName = route.listName) }
        loadShows(route.listName)
    }

    private fun loadShows(listName: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            try {
                val lists = userRepository.getCustomLists()
                val ids = lists[listName] ?: emptyList()
                val shows = ids.map { id ->
                    async {
                        when (val result = showRepository.getShowDetails(id)) {
                            is Resource.Success -> result.data
                            else -> null
                        }
                    }
                }.awaitAll().filterNotNull()
                _uiState.update { it.copy(shows = shows, isLoading = false) }
            } catch (e: Exception) {
                _uiState.update { it.copy(isLoading = false, error = "Error al cargar las series") }
            }
        }
    }

    fun removeFromList(showId: Int) {
        val listName = _uiState.value.listName
        viewModelScope.launch {
            try {
                userRepository.removeFromCustomList(listName, showId)
                _uiState.update { it.copy(shows = it.shows.filter { s -> s.id != showId }) }
            } catch (_: Exception) {}
        }
    }
}
