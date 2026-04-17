package com.andrea.showmateapp.ui.screens.profile.friends

import android.util.Patterns
import androidx.compose.runtime.Immutable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.andrea.showmateapp.data.model.UserProfile
import com.andrea.showmateapp.data.model.MediaContent
import com.andrea.showmateapp.data.repository.ShowRepository
import com.andrea.showmateapp.domain.repository.IUserRepository
import com.andrea.showmateapp.util.GenreMapper
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

@Immutable
data class FriendCompareUiState(
    val friendEmail: String = "",
    val commonShows: List<MediaContent> = emptyList(),
    val compatibilityScore: Int? = null,
    val isLoading: Boolean = false,
    val error: String? = null,
    val hasSearched: Boolean = false
)

@HiltViewModel
class FriendCompareViewModel @Inject constructor(
    private val userRepository: IUserRepository,
    private val showRepository: ShowRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(FriendCompareUiState())
    val uiState: StateFlow<FriendCompareUiState> = _uiState.asStateFlow()

    fun initWithEmail(email: String) {
        if (email.isBlank() || _uiState.value.hasSearched) return
        _uiState.update { it.copy(friendEmail = email) }
        compare()
    }

    fun onEmailChange(email: String) {
        _uiState.update { it.copy(friendEmail = email, error = null) }
    }

    fun compare() {
        val email = _uiState.value.friendEmail.trim()
        if (email.isBlank()) {
            _uiState.update { it.copy(error = "Introduce el email de tu amigo") }
            return
        }
        if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            _uiState.update { it.copy(error = "Introduce un email válido") }
            return
        }
        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    isLoading = true,
                    error = null,
                    commonShows = emptyList(),
                    compatibilityScore = null
                )
            }
            try {
                val myProfileDeferred = async { userRepository.getUserProfile() }
                val friendProfileDeferred = async { userRepository.getFriendProfile(email) }
                val myProfile = myProfileDeferred.await()
                val friendProfile = friendProfileDeferred.await()

                if (friendProfile == null) {
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            hasSearched = true,
                            error = "No se encontró ningún usuario con ese email"
                        )
                    }
                    return@launch
                }

                val commonIds = run {
                    val myLiked = myProfile?.likedMediaIds?.toSet() ?: emptySet()
                    val friendLiked = friendProfile.likedMediaIds.toSet()
                    (myLiked intersect friendLiked).toList()
                }

                val compatibility = if (myProfile != null) calculateCompatibility(myProfile, friendProfile) else null

                val shows = if (commonIds.isNotEmpty()) showRepository.getShowDetailsInParallel(commonIds) else emptyList()

                _uiState.update {
                    it.copy(isLoading = false, hasSearched = true, commonShows = shows, compatibilityScore = compatibility)
                }
            } catch (e: Exception) {
                if (e is CancellationException) throw e
                _uiState.update {
                    it.copy(isLoading = false, hasSearched = true, error = "Error al comparar. Revisa tu conexión.")
                }
            }
        }
    }

    private fun calculateCompatibility(mine: UserProfile, friend: UserProfile): Int =
        (GenreMapper.jaccardSimilarity(mine.genreScores, friend.genreScores) * 100)
            .toInt().coerceIn(0, 100)
}

