package com.example.showmateapp.ui.screens.friends

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
import kotlinx.coroutines.launch
import javax.inject.Inject

data class FriendsUiState(
    val query: String = "",
    val isLoading: Boolean = false,
    val commonShows: List<MediaContent> = emptyList(),
    val errorMessage: String? = null,
    val searchDone: Boolean = false
)

@HiltViewModel
class FriendsViewModel @Inject constructor(
    private val userRepository: UserRepository,
    private val showRepository: ShowRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(FriendsUiState())
    val uiState: StateFlow<FriendsUiState> = _uiState.asStateFlow()

    fun compareWithFriend(email: String) {
        if (email.isBlank()) return
        _uiState.value = FriendsUiState(query = email, isLoading = true)
        viewModelScope.launch {
            try {
                val commonIds = userRepository.compareWithFriend(email)
                if (commonIds.isEmpty()) {
                    _uiState.value = _uiState.value.copy(isLoading = false, searchDone = true)
                    return@launch
                }
                // Fetch all show details in parallel
                val shows = commonIds.take(20)
                    .map { id -> async { showRepository.getShowDetails(id) } }
                    .awaitAll()
                    .mapNotNull { (it as? Resource.Success)?.data }
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
}
