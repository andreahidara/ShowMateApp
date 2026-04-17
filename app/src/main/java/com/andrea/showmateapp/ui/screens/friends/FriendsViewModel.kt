package com.andrea.showmateapp.ui.screens.friends

import androidx.compose.runtime.Immutable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.andrea.showmateapp.data.model.ActivityEvent
import com.andrea.showmateapp.data.model.FriendInfo
import com.andrea.showmateapp.data.model.FriendRequest
import com.andrea.showmateapp.data.model.UserProfile
import com.andrea.showmateapp.domain.repository.ISocialRepository
import com.andrea.showmateapp.domain.repository.IUserRepository
import com.andrea.showmateapp.domain.usecase.AchievementChecker
import com.andrea.showmateapp.domain.usecase.AchievementDefs
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

enum class FriendsTab { FRIENDS, REQUESTS, FEED, DISCOVER }

enum class FriendsMode { COMPARE, GROUP }

@Immutable
data class FriendsUiState(
    val tab: FriendsTab = FriendsTab.FRIENDS,
    val friends: List<FriendInfo> = emptyList(),
    val isFriendsLoading: Boolean = false,
    val incomingRequests: List<FriendRequest> = emptyList(),
    val outgoingRequests: List<FriendRequest> = emptyList(),
    val isRequestsLoading: Boolean = false,
    val unreadRequestCount: Int = 0,
    val activityFeed: List<ActivityEvent> = emptyList(),
    val isFeedLoading: Boolean = false,
    val searchQuery: String = "",
    val searchResults: List<UserProfile> = emptyList(),
    val isSearching: Boolean = false,
    val suggestions: List<UserProfile> = emptyList(),
    val isSuggestionsLoading: Boolean = false,
    val sentRequestUids: Set<String> = emptySet(),
    val errorMessage: String? = null,
    val successMessage: String? = null,
    val groupMembers: List<String> = emptyList(),
    val groupAddError: String? = null
)

@HiltViewModel
class FriendsViewModel @Inject constructor(
    private val socialRepository: ISocialRepository,
    private val userRepository: IUserRepository,
    private val achievementChecker: AchievementChecker
) : ViewModel() {

    private val _uiState = MutableStateFlow(FriendsUiState())
    val uiState: StateFlow<FriendsUiState> = _uiState.asStateFlow()

    @Suppress("ktlint:standard:property-naming")
    private val _rawSearchQuery = MutableStateFlow("")

    init {
        loadFriends()
        loadPendingCount()
        setupDebouncedSearch()
    }

    @OptIn(FlowPreview::class)
    private fun setupDebouncedSearch() {
        viewModelScope.launch {
            _rawSearchQuery.debounce(500L).collectLatest { query ->
                if (query.length >= 2) {
                    _uiState.update { it.copy(isSearching = true) }
                    val results = try {
                        socialRepository.searchByUsername(query)
                    } catch (
                        e: Exception
                    ) {
                        if (e is CancellationException) throw e
                        emptyList()
                    }
                    _uiState.update { it.copy(isSearching = false, searchResults = results) }
                } else {
                    _uiState.update { it.copy(searchResults = emptyList(), isSearching = false) }
                }
            }
        }
    }

    fun setTab(tab: FriendsTab) {
        _uiState.update { it.copy(tab = tab) }
        when (tab) {
            FriendsTab.FRIENDS -> loadFriends()
            FriendsTab.REQUESTS -> loadRequests()
            FriendsTab.FEED -> loadFeed()
            FriendsTab.DISCOVER -> if (_uiState.value.suggestions.isEmpty()) loadSuggestions()
        }
    }

    fun loadFriends() {
        if (_uiState.value.isFriendsLoading) return
        viewModelScope.launch {
            _uiState.update { it.copy(isFriendsLoading = true) }
            val friends = try {
                socialRepository.getFriends()
            } catch (
                e: Exception
            ) {
                if (e is CancellationException) throw e
                emptyList()
            }
            _uiState.update { it.copy(isFriendsLoading = false, friends = friends) }
        }
    }

    private fun loadPendingCount() {
        viewModelScope.launch {
            val count = try {
                socialRepository.getPendingRequestCount()
            } catch (
                e: Exception
            ) {
                if (e is CancellationException) throw e
                0
            }
            _uiState.update { it.copy(unreadRequestCount = count) }
        }
    }

    fun loadRequests() {
        viewModelScope.launch {
            _uiState.update { it.copy(isRequestsLoading = true) }
            try {
                val incoming = socialRepository.getIncomingRequests()
                val outgoing = socialRepository.getOutgoingRequests()
                _uiState.update {
                    it.copy(
                        isRequestsLoading = false,
                        incomingRequests = incoming,
                        outgoingRequests = outgoing,
                        unreadRequestCount = incoming.size
                    )
                }
            } catch (e: Exception) {
                if (e is CancellationException) throw e
                _uiState.update { it.copy(isRequestsLoading = false) }
            }
        }
    }

    fun acceptRequest(request: FriendRequest) {
        viewModelScope.launch {
            try {
                socialRepository.acceptFriendRequest(request.id, request.fromUid)
                _uiState.update {
                    it.copy(
                        successMessage = "¡Ahora sois amigos!",
                        incomingRequests = it.incomingRequests.filter { r -> r.id != request.id },
                        unreadRequestCount = (it.unreadRequestCount - 1).coerceAtLeast(0)
                    )
                }
                loadFriends()

                runCatching { achievementChecker.addXp(AchievementDefs.XP_ADD_FRIEND) }
                val profile = runCatching { userRepository.getUserProfile() }.getOrNull()
                if (profile != null) {
                    runCatching {
                        achievementChecker.evaluate(AchievementChecker.EvalContext(profile = profile))
                    }
                }
            } catch (e: Exception) {
                if (e is CancellationException) throw e
                _uiState.update { it.copy(errorMessage = "No se pudo aceptar la solicitud") }
            }
        }
    }

    fun rejectRequest(requestId: String) {
        viewModelScope.launch {
            try {
                socialRepository.rejectFriendRequest(requestId)
                _uiState.update {
                    it.copy(
                        incomingRequests = it.incomingRequests.filter { r -> r.id != requestId },
                        unreadRequestCount = (it.unreadRequestCount - 1).coerceAtLeast(0)
                    )
                }
            } catch (e: Exception) {
                if (e is CancellationException) throw e
            }
        }
    }

    fun cancelOutgoingRequest(requestId: String) {
        viewModelScope.launch {
            try {
                socialRepository.rejectFriendRequest(requestId)
                _uiState.update { it.copy(outgoingRequests = it.outgoingRequests.filter { r -> r.id != requestId }) }
            } catch (e: Exception) {
                if (e is CancellationException) throw e
            }
        }
    }

    fun loadFeed() {
        if (_uiState.value.isFeedLoading) return
        viewModelScope.launch {
            _uiState.update { it.copy(isFeedLoading = true) }
            try {
                val friendUids = _uiState.value.friends.map { it.uid }.ifEmpty {
                    socialRepository.getFriends().also { loaded ->
                        _uiState.update { it.copy(friends = loaded) }
                    }.map { it.uid }
                }
                val feed = socialRepository.getFriendActivityFeed(friendUids)
                _uiState.update { it.copy(isFeedLoading = false, activityFeed = feed) }
            } catch (e: Exception) {
                if (e is CancellationException) throw e
                _uiState.update { it.copy(isFeedLoading = false) }
            }
        }
    }

    fun loadSuggestions() {
        if (_uiState.value.isSuggestionsLoading) return
        viewModelScope.launch {
            _uiState.update { it.copy(isSuggestionsLoading = true) }
            val suggestions = try {
                socialRepository.getSuggestedFriends()
            } catch (
                e: Exception
            ) {
                if (e is CancellationException) throw e
                emptyList()
            }
            _uiState.update { it.copy(isSuggestionsLoading = false, suggestions = suggestions) }
        }
    }

    fun onSearchQueryChange(query: String) {
        _uiState.update { it.copy(searchQuery = query) }
        _rawSearchQuery.value = query
    }

    fun sendFriendRequest(toUid: String, toUsername: String) {
        viewModelScope.launch {
            val success = try {
                socialRepository.sendFriendRequest(toUid, toUsername)
            } catch (
                e: Exception
            ) {
                if (e is CancellationException) throw e
                false
            }
            if (success) {
                _uiState.update {
                    it.copy(
                        sentRequestUids = it.sentRequestUids + toUid,
                        successMessage = "Solicitud enviada a $toUsername"
                    )
                }
            } else {
                _uiState.update { it.copy(errorMessage = "No se pudo enviar la solicitud") }
            }
        }
    }

    fun removeFriend(uid: String) {
        viewModelScope.launch {
            try {
                socialRepository.removeFriend(uid)
                _uiState.update { it.copy(friends = it.friends.filter { f -> f.uid != uid }) }
            } catch (e: Exception) {
                if (e is CancellationException) throw e
            }
        }
    }

    fun addFriendToGroup(friend: FriendInfo) {
        val current = _uiState.value.groupMembers
        if (current.any { it == friend.email }) {
            _uiState.update { it.copy(groupAddError = "${friend.username} ya está en el grupo") }
            return
        }
        if (current.size >= 4) {
            _uiState.update { it.copy(groupAddError = "Máximo 4 amigos por grupo") }
            return
        }
        _uiState.update { it.copy(groupMembers = current + friend.email, groupAddError = null) }
    }

    fun removeGroupMember(email: String) =
        _uiState.update { it.copy(groupMembers = it.groupMembers.filter { m -> m != email }) }

    fun clearGroupError() = _uiState.update { it.copy(groupAddError = null) }

    fun dismissError() = _uiState.update { it.copy(errorMessage = null) }
    fun dismissSuccess() = _uiState.update { it.copy(successMessage = null) }

    fun getCurrentUid() = socialRepository.getCurrentUid()
}
