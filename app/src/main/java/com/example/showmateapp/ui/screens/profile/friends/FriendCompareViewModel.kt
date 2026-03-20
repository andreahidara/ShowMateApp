package com.example.showmateapp.ui.screens.profile.friends

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.showmateapp.data.network.MediaContent
import com.example.showmateapp.data.repository.ShowRepository
import com.example.showmateapp.data.repository.UserRepository
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

data class FriendCompareUiState(
    val friendEmail: String = "",
    val commonShows: List<MediaContent> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null,
    val hasSearched: Boolean = false
)

@HiltViewModel
class FriendCompareViewModel @Inject constructor(
    private val userRepository: UserRepository,
    private val showRepository: ShowRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(FriendCompareUiState())
    val uiState: StateFlow<FriendCompareUiState> = _uiState.asStateFlow()

    fun onEmailChange(email: String) {
        _uiState.update { it.copy(friendEmail = email, error = null) }
    }

    fun compare() {
        val email = _uiState.value.friendEmail.trim()
        if (email.isBlank()) {
            _uiState.update { it.copy(error = "Introduce el email de tu amigo") }
            return
        }
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null, commonShows = emptyList()) }
            val commonIds = userRepository.compareWithFriend(email)
            if (commonIds.isEmpty()) {
                _uiState.update { it.copy(isLoading = false, hasSearched = true, commonShows = emptyList()) }
                return@launch
            }
            val shows = commonIds.map { id ->
                async {
                    when (val result = showRepository.getShowDetails(id)) {
                        is Resource.Success -> result.data
                        else -> null
                    }
                }
            }.awaitAll().filterNotNull()
            _uiState.update { it.copy(isLoading = false, hasSearched = true, commonShows = shows) }
        }
    }
}
