package com.andrea.showmateapp.ui.screens.friends

import androidx.compose.runtime.Immutable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.andrea.showmateapp.data.model.GroupFilters
import com.andrea.showmateapp.data.model.GroupSession
import com.andrea.showmateapp.data.model.MemberVoteDoc
import com.andrea.showmateapp.data.model.VoteType
import com.andrea.showmateapp.data.model.MediaContent
import com.andrea.showmateapp.data.repository.ShowRepository
import com.andrea.showmateapp.domain.repository.IAchievementRepository
import com.andrea.showmateapp.domain.repository.IGroupSessionRepository
import com.andrea.showmateapp.domain.repository.IUserRepository
import com.andrea.showmateapp.domain.usecase.AchievementChecker
import com.andrea.showmateapp.domain.usecase.AchievementDefs
import com.andrea.showmateapp.util.Resource
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

enum class GroupPhase { LOADING, LOBBY, VOTING, MATCH_FOUND, NO_MATCH }

@Immutable
data class GroupMatchUiState(
    val phase: GroupPhase = GroupPhase.LOADING,
    val session: GroupSession? = null,
    val candidates: List<MediaContent> = emptyList(),
    val myVotes: MemberVoteDoc = MemberVoteDoc(),
    val allVotes: Map<String, MemberVoteDoc> = emptyMap(),
    val currentIndex: Int = 0,
    val myVetoUsed: Boolean = false,
    val matchedMedia: MediaContent? = null,
    val showMatchCelebration: Boolean = false,
    val showFiltersSheet: Boolean = false,
    val filters: GroupFilters = GroupFilters(),
    val nightTitle: String = "",
    val showNightTitleDialog: Boolean = false,
    val isComputingCandidates: Boolean = false,
    val errorMessage: String? = null,
    val members: List<String> = emptyList()
) {
    val currentCandidate: MediaContent? get() = candidates.getOrNull(currentIndex)
    val isVotingDone: Boolean get() = currentIndex >= candidates.size && candidates.isNotEmpty()
    val votingProgress: Float get() = if (candidates.isEmpty()) {
        0f
    } else {
        (currentIndex.toFloat() / candidates.size).coerceIn(0f, 1f)
    }

    fun memberVoteCount(email: String): Int = allVotes[email]?.let { v -> v.yes.size + v.no.size + v.maybe.size } ?: 0
}

@HiltViewModel
class GroupMatchViewModel @Inject constructor(
    private val userRepository: IUserRepository,
    private val showRepository: ShowRepository,
    private val groupSessionRepository: IGroupSessionRepository,
    private val achievementChecker: AchievementChecker,
    private val achievementRepository: IAchievementRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(GroupMatchUiState())
    val uiState: StateFlow<GroupMatchUiState> = _uiState.asStateFlow()

    private var sessionId: String? = null
    private var myEmail: String? = null

    fun loadGroupMatches(memberEmails: List<String>) {
        if (_uiState.value.phase != GroupPhase.LOADING) return
        viewModelScope.launch {
            myEmail = userRepository.getCurrentUserEmail()
            _uiState.update { it.copy(members = memberEmails) }
            try {
                val session = groupSessionRepository.createSession(memberEmails)
                sessionId = session.id
                _uiState.update { it.copy(phase = GroupPhase.LOBBY, session = session) }

                startSessionObserver(session.id)
                startVotesObserver(session.id)

                if (session.hostEmail == myEmail) {
                    computeAndSaveCandidates(session.memberEmails, _uiState.value.filters)
                }
            } catch (e: Exception) {
                if (e is CancellationException) throw e
                _uiState.update { it.copy(phase = GroupPhase.LOBBY, errorMessage = "Error al crear la sala") }
            }
        }
    }

    private fun startSessionObserver(id: String) {
        viewModelScope.launch {
            groupSessionRepository.observeSession(id).collectLatest { session ->
                if (session == null) return@collectLatest
                val phase = when (session.status) {
                    GroupSession.STATUS_LOBBY -> GroupPhase.LOBBY
                    GroupSession.STATUS_VOTING -> GroupPhase.VOTING
                    GroupSession.STATUS_FINISHED -> if (session.matchedMediaId != 0) {
                        GroupPhase.MATCH_FOUND
                    } else {
                        GroupPhase.NO_MATCH
                    }
                    else -> _uiState.value.phase
                }
                _uiState.update { it.copy(session = session, phase = phase) }

                if (session.candidateIds.isNotEmpty() && _uiState.value.candidates.isEmpty()) {
                    loadCandidatesFromIds(session.candidateIds)
                }
                if (session.matchedMediaId != 0 &&
                    _uiState.value.phase != GroupPhase.MATCH_FOUND
                ) {
                    val matched = _uiState.value.candidates
                        .firstOrNull { it.id == session.matchedMediaId }
                        ?: fetchSingle(session.matchedMediaId)
                    _uiState.update {
                        it.copy(
                            matchedMedia = matched,
                            phase = GroupPhase.MATCH_FOUND,
                            showMatchCelebration = true
                        )
                    }
                    viewModelScope.launch {
                        runCatching { achievementChecker.addXp(AchievementDefs.XP_GROUP_MATCH) }
                        runCatching {
                            val count = achievementRepository.incrementAndGetGroupMatchCount()
                            achievementChecker.onGroupMatchCompleted(count)
                        }
                    }
                }
            }
        }
    }

    private fun startVotesObserver(id: String) {
        viewModelScope.launch {
            groupSessionRepository.observeAllVotes(id).collect { votes ->
                _uiState.update { it.copy(allVotes = votes) }
                checkForMatch(votes)
            }
        }
    }

    private suspend fun computeAndSaveCandidates(memberEmails: List<String>, filters: GroupFilters) {
        _uiState.update { it.copy(isComputingCandidates = true) }
        try {
            val allProfiles = coroutineScope {
                memberEmails.map { email ->
                    async { userRepository.getFriendProfile(email) }
                }.awaitAll().filterNotNull() + listOfNotNull(userRepository.getUserProfile())
            }

            val allSeenIds = allProfiles.flatMap { it.likedMediaIds + it.essentialMediaIds }.toSet()
            val groupGenreScores = mutableMapOf<String, Float>()
            allProfiles.forEach { profile ->
                profile.genreScores.forEach { (genre, score) ->
                    groupGenreScores[genre] = (groupGenreScores[genre] ?: 0f) + score
                }
            }
            val topGenres = groupGenreScores.entries
                .sortedByDescending { it.value }
                .take(3)
                .joinToString(",") { it.key }

            val discoveryResults = showRepository.getDetailedRecommendations(topGenres)
            val filteredCandidates = discoveryResults
                .filter { it.id !in allSeenIds }
                .filter { show ->
                    if (filters.maxEpisodeDuration > 0) {
                        val rt = show.episodeRunTime?.firstOrNull() ?: 0
                        if (rt in 1..Int.MAX_VALUE && rt > filters.maxEpisodeDuration) return@filter false
                    }
                    if (filters.excludedGenreIds.isNotEmpty()) {
                        if (show.safeGenreIds.any { it in filters.excludedGenreIds }) return@filter false
                    }
                    true
                }
                .distinctBy { it.id }
                .take(20)

            sessionId?.let { groupSessionRepository.updateCandidates(it, filteredCandidates.map { s -> s.id }) }
        } catch (e: Exception) {
            if (e is CancellationException) throw e
            _uiState.update { it.copy(errorMessage = "Error cargando candidatos") }
        } finally {
            _uiState.update { it.copy(isComputingCandidates = false) }
        }
    }

    private suspend fun loadCandidatesFromIds(ids: List<Int>) {
        val shows = showRepository.getShowDetailsInParallel(ids)
        _uiState.update { it.copy(candidates = shows) }
    }

    private suspend fun fetchSingle(mediaId: Int): MediaContent? =
        (showRepository.getShowDetails(mediaId) as? Resource.Success)?.data

    fun startVoting() {
        viewModelScope.launch {
            sessionId?.let { id ->
                try {
                    groupSessionRepository.updateSessionStatus(id, GroupSession.STATUS_VOTING)
                } catch (e: Exception) {
                    if (e is CancellationException) throw e
                    _uiState.update { it.copy(errorMessage = "Error al iniciar la votación") }
                }
            }
        }
    }

    fun updateFilters(filters: GroupFilters) {
        _uiState.update { it.copy(filters = filters, showFiltersSheet = false) }
        if (_uiState.value.session?.hostEmail == myEmail) {
            val members = _uiState.value.session?.memberEmails ?: _uiState.value.members
            viewModelScope.launch { computeAndSaveCandidates(members, filters) }
        }
    }

    fun voteYes() = castVote(VoteType.YES)
    fun voteNo() = castVote(VoteType.NO)
    fun voteMaybe() = castVote(VoteType.MAYBE)
    fun voteSuperLike() {
        if (_uiState.value.myVotes.superLikeId != 0) return
        castVote(VoteType.SUPER_LIKE)
    }

    private fun castVote(type: VoteType) {
        val email = myEmail ?: return
        val candidate = _uiState.value.currentCandidate ?: return

        viewModelScope.launch {
            sessionId?.let { id ->
                try {
                    groupSessionRepository.submitVote(id, email, candidate.id, type)
                } catch (e: Exception) {
                    if (e is CancellationException) throw e
                }
            }
        }

        val newVotes = when (type) {
            VoteType.YES -> _uiState.value.myVotes.copy(yes = _uiState.value.myVotes.yes + candidate.id)
            VoteType.NO -> _uiState.value.myVotes.copy(no = _uiState.value.myVotes.no + candidate.id)
            VoteType.MAYBE -> _uiState.value.myVotes.copy(maybe = _uiState.value.myVotes.maybe + candidate.id)
            VoteType.SUPER_LIKE -> _uiState.value.myVotes.copy(superLikeId = candidate.id)
        }
        _uiState.update { it.copy(myVotes = newVotes, currentIndex = it.currentIndex + 1) }

        if (_uiState.value.isVotingDone) {
            viewModelScope.launch {
                sessionId?.let { id -> groupSessionRepository.setMemberReady(id, email) }
            }
        }
    }

    fun useVeto() {
        val email = myEmail ?: return
        val candidate = _uiState.value.currentCandidate ?: return
        if (_uiState.value.myVetoUsed) return

        viewModelScope.launch {
            sessionId?.let { id ->
                try {
                    groupSessionRepository.submitVeto(id, email, candidate.id)
                } catch (e: Exception) {
                    if (e is CancellationException) throw e
                }
            }
        }
        _uiState.update { it.copy(myVetoUsed = true, currentIndex = it.currentIndex + 1) }
    }

    private fun checkForMatch(allVotes: Map<String, MemberVoteDoc>) {
        if (_uiState.value.phase == GroupPhase.MATCH_FOUND) return
        val session = _uiState.value.session ?: return
        if (session.status != GroupSession.STATUS_VOTING) return
        val candidates = _uiState.value.candidates
        if (candidates.isEmpty()) return

        val vetoed = session.vetoes.values.filter { it != 0 }.toSet()

        val superLikeCounts = mutableMapOf<Int, Int>()
        allVotes.values.forEach { v ->
            if (v.superLikeId != 0) {
                superLikeCounts[v.superLikeId] = (superLikeCounts[v.superLikeId] ?: 0) + 1
            }
        }

        val instantMatchId = superLikeCounts.entries.find { it.value >= 2 }?.key

        if (instantMatchId != null && instantMatchId !in vetoed) {
            val matched = candidates.firstOrNull { it.id == instantMatchId }
            if (matched != null) {
                executeMatch(matched)
                return
            }
        }

        for (candidate in candidates) {
            if (candidate.id in vetoed) continue
            val allPositive = session.memberEmails.all { email ->
                val v = allVotes[email]
                v != null && (candidate.id in v.yes || candidate.id in v.maybe || candidate.id == v.superLikeId)
            }
            if (allPositive) {
                executeMatch(candidate)
                return
            }
        }
    }

    private fun executeMatch(candidate: MediaContent) {
        viewModelScope.launch {
            sessionId?.let { id ->
                runCatching { groupSessionRepository.setMatch(id, candidate.id) }
            }
        }
        _uiState.update {
            it.copy(
                matchedMedia = candidate,
                phase = GroupPhase.MATCH_FOUND,
                showMatchCelebration = true
            )
        }
    }

    fun showNightTitleDialog() = _uiState.update { it.copy(showNightTitleDialog = true) }
    fun dismissNightTitleDialog() = _uiState.update { it.copy(showNightTitleDialog = false) }

    fun saveNightTitle(title: String) {
        _uiState.update { it.copy(nightTitle = title, showNightTitleDialog = false) }
        viewModelScope.launch {
            sessionId?.let { id ->
                try {
                    groupSessionRepository.saveNightTitle(id, title)
                } catch (e: Exception) {
                    if (e is CancellationException) throw e
                }
            }
        }
    }

    fun showFilters() = _uiState.update { it.copy(showFiltersSheet = true) }
    fun hideFilters() = _uiState.update { it.copy(showFiltersSheet = false) }
    fun dismissError() = _uiState.update { it.copy(errorMessage = null) }
    fun dismissCelebration() = _uiState.update { it.copy(showMatchCelebration = false) }

    fun isHost(): Boolean = myEmail != null && myEmail == _uiState.value.session?.hostEmail
}

