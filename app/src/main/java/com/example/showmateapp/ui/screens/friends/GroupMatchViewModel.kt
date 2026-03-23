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

data class GroupMatchItem(
    val media: MediaContent,
    val likedByCount: Int,
    val totalMembers: Int
)

data class GroupMatchUiState(
    val isLoading: Boolean = true,
    val members: List<String> = emptyList(),
    val strictMatches: List<GroupMatchItem> = emptyList(),
    val softMatches: List<GroupMatchItem> = emptyList(),
    val errorMessage: String? = null
)

@HiltViewModel
class GroupMatchViewModel @Inject constructor(
    private val userRepository: UserRepository,
    private val showRepository: ShowRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(GroupMatchUiState())
    val uiState: StateFlow<GroupMatchUiState> = _uiState.asStateFlow()

    fun loadGroupMatches(memberEmails: List<String>) {
        _uiState.value = GroupMatchUiState(isLoading = true, members = memberEmails)
        viewModelScope.launch {
            try {
                val myProfileDeferred = async { userRepository.getUserProfile() }
                val friendDeferreds = memberEmails.map { email ->
                    async { userRepository.getFriendProfile(email) }
                }
                val myProfile = myProfileDeferred.await()
                val allProfiles = listOfNotNull(myProfile) + friendDeferreds.awaitAll().filterNotNull()
                val totalMembers = allProfiles.size

                if (totalMembers < 2) {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        errorMessage = "No se pudieron cargar los perfiles del grupo"
                    )
                    return@launch
                }

                val showLikedCounts = mutableMapOf<Int, Int>()
                allProfiles.forEach { profile ->
                    (profile.likedMediaIds + profile.essentialMediaIds).toSet().forEach { id ->
                        showLikedCounts[id] = (showLikedCounts[id] ?: 0) + 1
                    }
                }

                val strictIds = showLikedCounts.filter { it.value == totalMembers }.keys.toList()
                val softIds = showLikedCounts
                    .filter { it.value >= maxOf(2, totalMembers / 2) && it.value < totalMembers }
                    .entries.sortedByDescending { it.value }.map { it.key }

                // Fetch strict + soft in a single parallel batch to halve network time
                val allIds = (strictIds.take(20) + softIds.take(20)).toSet()
                val detailsMap = allIds
                    .map { id -> async { id to showRepository.getShowDetails(id) } }
                    .awaitAll()
                    .associate { it }

                fun toItem(id: Int): GroupMatchItem? =
                    (detailsMap[id] as? Resource.Success)?.data?.let { media ->
                        GroupMatchItem(media, showLikedCounts[id] ?: 0, totalMembers)
                    }

                val strictShows = strictIds.take(20).mapNotNull(::toItem)
                    .sortedByDescending { it.media.voteAverage }

                // Sort by likedByCount first, then voteAverage as tiebreaker
                val softShows = softIds.take(20).mapNotNull(::toItem)
                    .sortedWith(compareByDescending<GroupMatchItem> { it.likedByCount }
                        .thenByDescending { it.media.voteAverage })

                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    strictMatches = strictShows,
                    softMatches = softShows,
                    errorMessage = null
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    errorMessage = "Error al buscar matches del grupo"
                )
            }
        }
    }
}
