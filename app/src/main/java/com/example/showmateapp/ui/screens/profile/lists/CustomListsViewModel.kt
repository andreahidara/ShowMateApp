package com.example.showmateapp.ui.screens.profile.lists

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.showmateapp.data.repository.UserRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class CustomListsUiState(
    val lists: Map<String, List<Int>> = emptyMap(),
    val isLoading: Boolean = true,
    val newListName: String = "",
    val showCreateDialog: Boolean = false,
    val error: String? = null
)

@HiltViewModel
class CustomListsViewModel @Inject constructor(
    private val userRepository: UserRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(CustomListsUiState())
    val uiState: StateFlow<CustomListsUiState> = _uiState.asStateFlow()

    init {
        loadLists()
    }

    fun loadLists() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            val lists = userRepository.getCustomLists()
            _uiState.update { it.copy(lists = lists, isLoading = false) }
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
            userRepository.createCustomList(name)
            _uiState.update { it.copy(lists = it.lists + (name to emptyList()), showCreateDialog = false, newListName = "") }
        }
    }

    fun deleteList(name: String) {
        viewModelScope.launch {
            userRepository.deleteCustomList(name)
            loadLists()
        }
    }
}
