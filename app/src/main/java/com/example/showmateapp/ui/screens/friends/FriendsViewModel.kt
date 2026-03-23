package com.example.showmateapp.ui.screens.friends

import android.util.Patterns
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.showmateapp.data.network.MediaContent
import com.example.showmateapp.data.repository.ShowRepository
import com.example.showmateapp.data.repository.UserRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

enum class FriendsMode { COMPARE, GROUP }

data class FriendsUiState(
    val mode: FriendsMode = FriendsMode.COMPARE,
    val isLoading: Boolean = false,
    val commonShows: List<MediaContent> = emptyList(),
    val errorMessage: String? = null,
    val searchDone: Boolean = false,
    val groupMembers: List<String> = emptyList(),
    val groupAddError: String? = null
)

@HiltViewModel
class FriendsViewModel @Inject constructor(
    private val userRepository: UserRepository,
    private val showRepository: ShowRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(FriendsUiState())
    val uiState: StateFlow<FriendsUiState> = _uiState.asStateFlow()

    fun setMode(mode: FriendsMode) {
        // Preserve groupMembers when toggling back to GROUP mode
        _uiState.value = _uiState.value.copy(
            mode = mode,
            isLoading = false,
            commonShows = emptyList(),
            errorMessage = null,
            searchDone = false,
            groupAddError = null
        )
    }

    fun compareWithFriend(email: String) {
        if (email.isBlank()) return
        if (!Patterns.EMAIL_ADDRESS.matcher(email.trim()).matches()) {
            _uiState.value = _uiState.value.copy(errorMessage = "Introduce un email válido")
            return
        }
        _uiState.value = _uiState.value.copy(isLoading = true, errorMessage = null)
        viewModelScope.launch {
            try {
                val commonIds = userRepository.compareWithFriend(email)
                if (commonIds.isEmpty()) {
                    _uiState.value = _uiState.value.copy(isLoading = false, searchDone = true)
                    return@launch
                }
                val shows = showRepository.getShowDetailsInParallel(commonIds)
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    searchDone = true,
                    commonShows = shows,
                    errorMessage = null
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    searchDone = true,
                    errorMessage = "No se pudo comparar con ese usuario"
                )
            }
        }
    }

    fun addGroupMember(email: String) {
        val trimmed = email.trim()
        if (trimmed.isBlank()) return
        if (!Patterns.EMAIL_ADDRESS.matcher(trimmed).matches()) {
            _uiState.value = _uiState.value.copy(groupAddError = "Introduce un email válido")
            return
        }
        val current = _uiState.value.groupMembers
        if (current.any { it.equals(trimmed, ignoreCase = true) }) {
            _uiState.value = _uiState.value.copy(groupAddError = "Ya está en el grupo")
            return
        }
        if (current.size >= 4) {
            _uiState.value = _uiState.value.copy(groupAddError = "Máximo 4 amigos por grupo")
            return
        }
        viewModelScope.launch {
            val exists = userRepository.userExists(trimmed)
            if (!exists) {
                _uiState.value = _uiState.value.copy(groupAddError = "Usuario no encontrado")
            } else {
                // Re-read state after the suspend call to avoid race condition
                _uiState.value = _uiState.value.copy(
                    groupMembers = _uiState.value.groupMembers + trimmed,
                    groupAddError = null
                )
            }
        }
    }

    fun removeGroupMember(email: String) {
        _uiState.value = _uiState.value.copy(
            groupMembers = _uiState.value.groupMembers.filter { it != email },
            groupAddError = null
        )
    }

    fun clearGroupError() {
        _uiState.value = _uiState.value.copy(groupAddError = null)
    }
}
