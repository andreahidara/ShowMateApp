package com.andrea.showmateapp.ui.screens.friends.sharedlist

import androidx.compose.runtime.Immutable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.andrea.showmateapp.data.model.NowWatching
import com.andrea.showmateapp.data.model.SharedList
import com.andrea.showmateapp.data.model.MediaContent
import com.andrea.showmateapp.domain.repository.ISharedListRepository
import com.andrea.showmateapp.domain.repository.IShowRepository
import com.andrea.showmateapp.domain.repository.IUserRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull

@Immutable
data class SharedListsUiState(
    val isLoading: Boolean = false,
    val friendNowWatching: List<NowWatching> = emptyList(),
    val successMessage: String? = null,
    val error: String? = null
)

@Immutable
data class CollabListUiState(
    val isLoading: Boolean = false,
    val list: SharedList? = null,
    val shows: List<MediaContent> = emptyList(),
    val error: String? = null
)

@HiltViewModel
class SharedListViewModel @Inject constructor(
    private val sharedListRepo: ISharedListRepository,
    private val showRepository: IShowRepository,
    private val userRepo: IUserRepository
) : ViewModel() {

    val myLists: StateFlow<List<SharedList>> = sharedListRepo.observeMySharedLists()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _uiState = MutableStateFlow(SharedListsUiState())
    val uiState: StateFlow<SharedListsUiState> = _uiState.asStateFlow()

    private val _collabState = MutableStateFlow(CollabListUiState())
    val collabState: StateFlow<CollabListUiState> = _collabState.asStateFlow()

    init {
        loadFriendsNowWatching()
    }

    private fun loadFriendsNowWatching() {
        viewModelScope.launch {
            try {
                val profile = userRepo.getUserProfile() ?: return@launch
                val nowWatching = sharedListRepo.getFriendsNowWatching(profile.friendIds)
                _uiState.value = _uiState.value.copy(friendNowWatching = nowWatching)
            } catch (e: Exception) {
                if (e is CancellationException) throw e
            }
        }
    }

    fun createList(name: String, memberUids: List<String>, memberUsernames: List<String>) {
        viewModelScope.launch {
            try {
                val listId = sharedListRepo.createSharedList(name, memberUids, memberUsernames)
                if (listId != null) {
                    _uiState.value = _uiState.value.copy(successMessage = "Lista \"$name\" creada")
                } else {
                    _uiState.value = _uiState.value.copy(error = "Error al crear la lista")
                }
            } catch (e: Exception) {
                if (e is CancellationException) throw e
                _uiState.value = _uiState.value.copy(error = "Error al crear la lista")
            }
        }
    }

    fun deleteList(listId: String) {
        viewModelScope.launch {
            try {
                sharedListRepo.deleteSharedList(listId)
            } catch (e: Exception) {
                if (e is CancellationException) throw e
            }
        }
    }

    fun dismissSuccess() {
        _uiState.value = _uiState.value.copy(successMessage = null)
    }

    fun loadCollabList(listId: String) {
        _collabState.value = CollabListUiState(isLoading = true)
        viewModelScope.launch {
            try {
                val lists = if (myLists.value.isNotEmpty()) {
                    myLists.value
                } else {
                    withTimeoutOrNull(5000L) { myLists.first { it.isNotEmpty() } } ?: emptyList()
                }
                val list = lists.find { it.listId == listId }
                if (list != null) {
                    val shows = if (list.showIds.isNotEmpty()) {
                        showRepository.getShowDetailsInParallel(list.showIds.map { it.toInt() })
                    } else {
                        emptyList()
                    }
                    _collabState.value = CollabListUiState(list = list, shows = shows)
                } else {
                    _collabState.value = CollabListUiState(error = "Lista no encontrada")
                }
            } catch (e: Exception) {
                if (e is CancellationException) throw e
                _collabState.value = CollabListUiState(error = e.message)
            }
        }
    }

    fun removeShowFromList(listId: String, showId: Int) {
        val previous = _collabState.value.shows
        _collabState.value = _collabState.value.copy(shows = previous.filter { it.id != showId })
        viewModelScope.launch {
            try {
                sharedListRepo.removeShowFromSharedList(listId, showId)
            } catch (e: Exception) {
                if (e is CancellationException) throw e
                _collabState.value = _collabState.value.copy(shows = previous)
            }
        }
    }

    fun setNowWatching(showId: Int, showName: String, posterPath: String?) {
        viewModelScope.launch { sharedListRepo.setNowWatching(showId, showName, posterPath) }
    }

    fun clearNowWatching() {
        viewModelScope.launch { sharedListRepo.clearNowWatching() }
    }
}

