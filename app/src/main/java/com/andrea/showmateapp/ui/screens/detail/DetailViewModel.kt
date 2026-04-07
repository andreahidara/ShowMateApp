package com.andrea.showmateapp.ui.screens.detail

import timber.log.Timber
import com.google.firebase.crashlytics.FirebaseCrashlytics
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.andrea.showmateapp.data.model.UserProfile
import com.andrea.showmateapp.data.network.MediaContent
import com.andrea.showmateapp.data.network.SeasonResponse
import com.andrea.showmateapp.domain.repository.IInteractionRepository
import com.andrea.showmateapp.domain.repository.IShowRepository
import com.andrea.showmateapp.domain.repository.IUserRepository
import com.andrea.showmateapp.domain.usecase.AchievementChecker
import com.andrea.showmateapp.util.NarrativeStyleMapper
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import com.andrea.showmateapp.util.Resource
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import javax.inject.Inject
import com.andrea.showmateapp.data.model.RecommendationReason
import com.andrea.showmateapp.domain.usecase.GetRecommendationsUseCase
import com.andrea.showmateapp.util.UiText
import com.andrea.showmateapp.R

data class DetailUiState(
    val isLoading: Boolean = true,
    val media: MediaContent? = null,
    val errorMessage: UiText? = null,
    val isLiked: Boolean = false,
    val isEssential: Boolean = false,
    val isWatched: Boolean = false,
    val isInWatchlist: Boolean = false,
    val userRating: Int? = null,
    val userReview: String = "",
    val isSavingReview: Boolean = false,
    val isReviewSaved: Boolean = false,
    val similarShows: List<MediaContent> = emptyList(),
    val isSimilarLoading: Boolean = true,
    val actionError: UiText? = null,
    val snackbarMessage: UiText? = null,
    val watchedEpisodes: List<Int> = emptyList(),
    val selectedSeason: SeasonResponse? = null,
    val isSeasonLoading: Boolean = false,
    val customLists: Map<String, List<Int>> = emptyMap(),
    val showAddToListDialog: Boolean = false
)

@HiltViewModel
class DetailViewModel @Inject constructor(
    private val showRepository: IShowRepository,
    private val userRepository: IUserRepository,
    private val interactionRepository: IInteractionRepository,
    private val getRecommendationsUseCase: GetRecommendationsUseCase,
    private val achievementChecker: AchievementChecker
) : ViewModel() {

    private val _uiState = MutableStateFlow(DetailUiState())
    val uiState: StateFlow<DetailUiState> = _uiState.asStateFlow()

    private val _whyFactors = MutableStateFlow<List<RecommendationReason>>(emptyList())
    val whyFactors: StateFlow<List<RecommendationReason>> = _whyFactors.asStateFlow()

    private val _showWhyDialog = MutableStateFlow(false)
    val showWhyDialog: StateFlow<Boolean> = _showWhyDialog.asStateFlow()

    fun showWhyDialog() { _showWhyDialog.value = true }
    fun dismissWhyDialog() { _showWhyDialog.value = false }

    private val toggleMutex = Mutex()

    fun loadShowDetails(showId: Int) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            try {
                when (val result = showRepository.getShowDetails(showId)) {
                    is Resource.Success -> {
                        val details = result.data

                        val profile = try { userRepository.getUserProfile() } catch (e: Exception) {
                            Timber.w(e, "Could not load profile"); null
                        }

                        val scoredDetails = getRecommendationsUseCase.scoreShows(listOf(details)).firstOrNull() ?: details
                        _uiState.update { it.copy(media = scoredDetails) }

                        coroutineScope {
                            launch { checkInteractions(scoredDetails.id, profile) }
                            launch { loadUserRating(scoredDetails.id) }
                            launch { loadUserReview(scoredDetails.id) }
                        }

                        if (scoredDetails.reasons.isNotEmpty()) {
                            _whyFactors.value = scoredDetails.reasons
                        }

                        launch { loadSimilarShows(showId) }
                        launch { loadCustomLists() }
                        scoredDetails.seasons?.firstOrNull()?.let {
                            launch { loadSeasonDetails(showId, it.seasonNumber) }
                        }
                    }
                    is Resource.Error -> {
                        _uiState.update { it.copy(errorMessage = UiText.DynamicString(result.effectiveMessage)) }
                    }
                    else -> {}
                }
            } catch (e: Exception) {
                if (e is CancellationException) throw e
                Timber.e(e, "Error loading details")
                FirebaseCrashlytics.getInstance().recordException(e)
                _uiState.update { it.copy(errorMessage = UiText.StringResource(R.string.error_unexpected_data)) }
            } finally {
                _uiState.update { it.copy(isLoading = false) }
            }
        }
    }

    private suspend fun checkInteractions(showId: Int, cachedProfile: UserProfile? = null) {
        try {
            val localState = interactionRepository.getLocalInteractionState(showId)
            if (localState != null) {
                val episodes = try {
                    val profile = cachedProfile ?: userRepository.getUserProfile()
                    profile?.watchedEpisodes?.get(showId.toString()) ?: emptyList()
                } catch (e: Exception) {
                    if (e is CancellationException) throw e
                    emptyList()
                }
                _uiState.update { it.copy(
                    isLiked = localState.isLiked,
                    isEssential = localState.isEssential,
                    isWatched = localState.isWatched,
                    isInWatchlist = localState.isInWatchlist,
                    watchedEpisodes = episodes
                ) }
            } else {
                val profile = cachedProfile ?: userRepository.getUserProfile()
                val episodes = profile?.watchedEpisodes?.get(showId.toString()) ?: emptyList()
                val isLiked = profile?.likedMediaIds?.contains(showId)
                    ?: interactionRepository.getFavorites().any { it.id == showId }
                val isEssential = profile?.essentialMediaIds?.contains(showId)
                    ?: interactionRepository.getEssentials().any { it.id == showId }
                val isWatched = showId in interactionRepository.getWatchedMediaIds()
                val isInWatchlist = interactionRepository.isInWatchlist(showId)
                _uiState.update { it.copy(
                    isLiked = isLiked,
                    isEssential = isEssential,
                    isWatched = isWatched,
                    isInWatchlist = isInWatchlist,
                    watchedEpisodes = episodes
                ) }
                interactionRepository.cacheInteractionState(showId, isLiked, isEssential, isWatched)
            }
        } catch (e: Exception) {
            if (e is CancellationException) throw e
            Timber.w(e, "Error loading interaction state for %d", showId)
        }
    }

    fun toggleLiked() {
        val currentShow = _uiState.value.media ?: return
        val previousState = _uiState.value.isLiked
        val newState = !previousState
        _uiState.update { it.copy(isLiked = newState) }

        viewModelScope.launch {
            toggleMutex.withLock {
                try {
                    interactionRepository.toggleFavorite(currentShow, newState)
                    if (newState) {
                        trackInteraction(currentShow, IInteractionRepository.InteractionType.Like)
                        launchAchievementEvaluate()
                    }
                } catch (e: Exception) {
                    if (e is CancellationException) throw e
                    _uiState.update { it.copy(isLiked = previousState, actionError = UiText.StringResource(R.string.error_update_failed)) }
                }
            }
        }
    }

    fun toggleEssential() {
        val currentShow = _uiState.value.media ?: return
        val previousState = _uiState.value.isEssential
        val newState = !previousState
        _uiState.update { it.copy(isEssential = newState) }

        viewModelScope.launch {
            toggleMutex.withLock {
                try {
                    interactionRepository.toggleEssential(currentShow, newState)
                    if (newState) {
                        trackInteraction(currentShow, IInteractionRepository.InteractionType.Essential)
                        launchAchievementEvaluate()
                    }
                } catch (e: Exception) {
                    if (e is CancellationException) throw e
                    _uiState.update { it.copy(isEssential = previousState, actionError = UiText.StringResource(R.string.error_update_failed)) }
                }
            }
        }
    }

    fun toggleWatched() {
        val currentShow = _uiState.value.media ?: return
        val previousState = _uiState.value.isWatched
        val newState = !previousState
        _uiState.update { it.copy(isWatched = newState) }

        viewModelScope.launch {
            toggleMutex.withLock {
                try {
                    interactionRepository.toggleWatched(currentShow, newState)
                    markAllSeasonsWatched(currentShow, newState)
                    if (newState) {
                        trackInteraction(currentShow, IInteractionRepository.InteractionType.Watched)
                        launchAchievementEvaluate()
                    }
                } catch (e: Exception) {
                    if (e is CancellationException) throw e
                    _uiState.update { it.copy(isWatched = previousState, actionError = UiText.StringResource(R.string.error_update_failed)) }
                }
            }
        }
    }

    private suspend fun markAllSeasonsWatched(show: MediaContent, setWatched: Boolean) {
        val seasons = show.seasons?.filter { it.seasonNumber > 0 } ?: return
        if (!setWatched) {
            interactionRepository.setAllEpisodesWatched(show.id, emptyList())
            _uiState.update { it.copy(watchedEpisodes = emptyList()) }
            return
        }
        val allEpisodeIds = coroutineScope {
            seasons.map { season ->
                async {
                    runCatching { showRepository.getSeasonDetails(show.id, season.seasonNumber) }
                        .getOrNull()
                        ?.let { if (it is Resource.Success) it.data.episodes.map { ep -> ep.id } else emptyList() }
                        ?: emptyList()
                }
            }.flatMap { it.await() }
        }
        if (allEpisodeIds.isNotEmpty()) {
            interactionRepository.setAllEpisodesWatched(show.id, allEpisodeIds)
            _uiState.update { it.copy(watchedEpisodes = allEpisodeIds) }
        }
    }

    fun toggleWatchlist() {
        val currentShow = _uiState.value.media ?: return
        val previousState = _uiState.value.isInWatchlist
        val newState = !previousState
        _uiState.update { it.copy(isInWatchlist = newState) }

        viewModelScope.launch {
            toggleMutex.withLock {
                try {
                    interactionRepository.toggleWatchlist(currentShow, newState)
                } catch (e: Exception) {
                    if (e is CancellationException) throw e
                    _uiState.update { it.copy(isInWatchlist = previousState, actionError = UiText.StringResource(R.string.error_update_failed)) }
                }
            }
        }
    }

    fun toggleEpisodeWatched(episodeId: Int, markPrevious: Boolean = false) {
        val showId = _uiState.value.media?.id ?: return
        val currentWatched = _uiState.value.watchedEpisodes.toMutableList()
        val season = _uiState.value.selectedSeason

        if (markPrevious && season != null) {
            val epIndex = season.episodes.indexOfFirst { it.id == episodeId }
            if (epIndex != -1) {
                val toMark = season.episodes.take(epIndex + 1).map { it.id }
                val allMarked = toMark.all { it in currentWatched }

                if (allMarked) {
                    currentWatched.removeAll(toMark)
                } else {
                    toMark.forEach { if (it !in currentWatched) currentWatched.add(it) }
                }
            }
        } else {
            if (currentWatched.contains(episodeId)) currentWatched.remove(episodeId)
            else currentWatched.add(episodeId)
        }

        _uiState.update { it.copy(watchedEpisodes = currentWatched) }

        viewModelScope.launch {
            try {
                if (markPrevious && season != null) {
                    interactionRepository.setAllEpisodesWatched(showId, currentWatched)
                } else {
                    val isNowWatched = interactionRepository.toggleEpisodeWatched(showId, episodeId)
                    if (isNowWatched) {
                        launchAchievementEvaluate()
                        runCatching { userRepository.recordViewingSession(showId, 1) }
                    }
                }
            } catch (e: Exception) {
                if (e is CancellationException) throw e
                checkInteractions(showId)
            }
        }
    }

    fun toggleSeasonWatched() {
        val showId = _uiState.value.media?.id ?: return
        val season = _uiState.value.selectedSeason ?: return
        val currentWatched = _uiState.value.watchedEpisodes.toMutableSet()
        val seasonEpIds = season.episodes.map { it.id }

        val isSeasonCompleted = seasonEpIds.all { it in currentWatched }

        if (isSeasonCompleted) {
            currentWatched.removeAll(seasonEpIds.toSet())
        } else {
            currentWatched.addAll(seasonEpIds)
        }

        val newList = currentWatched.toList()
        _uiState.update { it.copy(watchedEpisodes = newList) }

        viewModelScope.launch {
            try {
                interactionRepository.setAllEpisodesWatched(showId, newList)
                if (!isSeasonCompleted) {
                    launchAchievementEvaluate()
                    runCatching { userRepository.recordViewingSession(showId, seasonEpIds.size) }
                }
            } catch (e: Exception) {
                if (e is CancellationException) throw e
                checkInteractions(showId)
            }
        }
    }

    fun markNextEpisodeWatched() {
        val season = _uiState.value.selectedSeason ?: return
        val watched = _uiState.value.watchedEpisodes
        val next = season.episodes.firstOrNull { it.id !in watched } ?: return
        toggleEpisodeWatched(next.id)
    }

    fun loadSeasonDetails(showId: Int, seasonNumber: Int) {
        viewModelScope.launch {
            _uiState.update { it.copy(isSeasonLoading = true) }
            try {
                val result = showRepository.getSeasonDetails(showId, seasonNumber)
                if (result is Resource.Success) {
                    _uiState.update { it.copy(selectedSeason = result.data) }
                } else {
                    Timber.e("Error loading season: %s", (result as? Resource.Error)?.effectiveMessage)
                }
            } catch (e: Exception) {
                Timber.e(e, "Error loading season")
            } finally {
                _uiState.update { it.copy(isSeasonLoading = false) }
            }
        }
    }

    private suspend fun loadUserRating(showId: Int) {
        val rating = interactionRepository.getUserRating(showId)
        _uiState.update { it.copy(userRating = rating) }
    }

    private suspend fun loadUserReview(showId: Int) {
        val review = try { interactionRepository.getReview(showId) } catch (e: Exception) { if (e is CancellationException) throw e; null }
        _uiState.update { it.copy(
            userReview = review ?: "",
            isReviewSaved = !review.isNullOrBlank()
        ) }
    }

    fun loadCustomLists() {
        viewModelScope.launch {
            try {
                val lists = interactionRepository.getCustomLists()
                _uiState.update { it.copy(customLists = lists) }
            } catch (e: Exception) { if (e is CancellationException) throw e;}
        }
    }

    fun showAddToListDialog() {
        if (_uiState.value.customLists.isEmpty()) loadCustomLists()
        _uiState.update { it.copy(showAddToListDialog = true) }
    }

    fun hideAddToListDialog() {
        _uiState.update { it.copy(showAddToListDialog = false) }
    }

    fun addToList(listName: String) {
        val showId = _uiState.value.media?.id ?: return
        _uiState.update { it.copy(showAddToListDialog = false) }
        viewModelScope.launch {
            try {
                interactionRepository.addToCustomList(listName, showId)
                _uiState.update { it.copy(snackbarMessage = UiText.StringResource(R.string.detail_added_to_list, listName)) }
                loadCustomLists()
            } catch (e: Exception) { if (e is CancellationException) throw e;
                _uiState.update { it.copy(actionError = UiText.StringResource(R.string.error_add_to_list_failed)) }
            }
        }
    }

    fun onReviewTextChange(text: String) {
        _uiState.update { it.copy(userReview = text, isReviewSaved = false) }
    }

    fun saveReview() {
        val showId = _uiState.value.media?.id ?: return
        val text = _uiState.value.userReview.trim()
        _uiState.update { it.copy(isSavingReview = true, isReviewSaved = false) }
        viewModelScope.launch {
            try {
                interactionRepository.saveReview(showId, text)
                _uiState.update { it.copy(isReviewSaved = true) }
            } catch (e: Exception) {
                _uiState.update { it.copy(actionError = UiText.StringResource(R.string.error_save_review_failed)) }
            } finally {
                _uiState.update { it.copy(isSavingReview = false) }
            }
        }
    }

    fun deleteReview() {
        val showId = _uiState.value.media?.id ?: return
        _uiState.update { it.copy(userReview = "", isReviewSaved = false) }
        viewModelScope.launch {
            try {
                interactionRepository.saveReview(showId, "")
            } catch (e: Exception) {
                _uiState.update { it.copy(actionError = UiText.StringResource(R.string.error_delete_review_failed)) }
            }
        }
    }

    fun rateShow(rating: Int) {
        val currentShow = _uiState.value.media ?: return
        _uiState.update { it.copy(userRating = rating) }

        viewModelScope.launch {
            try {
                interactionRepository.updateRating(currentShow.id, rating)
                trackInteraction(currentShow, IInteractionRepository.InteractionType.Rate(rating))
                launchAchievementEvaluate()
            } catch (e: Exception) {
                _uiState.update { it.copy(actionError = UiText.StringResource(R.string.error_rate_failed)) }
            }
        }
    }

    fun clearRating() {
        val currentShow = _uiState.value.media ?: return
        val previousRating = _uiState.value.userRating
        _uiState.update { it.copy(userRating = null) }
        viewModelScope.launch {
            try {
                interactionRepository.deleteRating(currentShow.id)
            } catch (e: Exception) {
                _uiState.update { it.copy(userRating = previousRating) }
            }
        }
    }

    private fun loadSimilarShows(showId: Int) {
        viewModelScope.launch {
            _uiState.update { it.copy(isSimilarLoading = true) }
            try {
                val similar = showRepository.getSimilarShows(showId)
                val scoredSimilar = getRecommendationsUseCase.scoreShows(similar)
                _uiState.update { it.copy(similarShows = scoredSimilar) }
            } catch (e: Exception) {
                Timber.e(e, "Error loading similar shows")
            } finally {
                _uiState.update { it.copy(isSimilarLoading = false) }
            }
        }
    }

    private fun launchAchievementEvaluate() {
        val media = _uiState.value.media ?: return
        viewModelScope.launch {
            val profile = runCatching { userRepository.getUserProfile() }.getOrNull() ?: return@launch
            val today = LocalDate.now().format(DateTimeFormatter.ISO_LOCAL_DATE)
            val episodesToday = profile.viewingHistory
                .filter { it.startsWith(today) }
                .sumOf { raw -> raw.split(":").getOrNull(2)?.toIntOrNull() ?: 0 }
            runCatching {
                achievementChecker.evaluate(
                    AchievementChecker.EvalContext(
                        profile                    = profile,
                        episodesToday              = episodesToday,
                        watchedShowVoteCount       = media.voteCount,
                        watchedShowOriginCountries = media.originCountry
                    )
                )
            }
        }
    }

    fun clearActionError() {
        _uiState.update { it.copy(actionError = null) }
    }

    fun clearSnackbarMessage() {
        _uiState.update { it.copy(snackbarMessage = null) }
    }

    private suspend fun trackInteraction(show: MediaContent, type: IInteractionRepository.InteractionType) {
        interactionRepository.trackMediaInteraction(
            mediaId = show.id,
            genres = show.safeGenreIds.map { it.toString() },
            keywords = show.keywordNames,
            actors = show.credits?.cast?.map { it.id } ?: emptyList(),
            narrativeStyles = NarrativeStyleMapper.extractStyles(show.keywordNames, show.episodeRunTime?.firstOrNull()),
            creators = show.creatorIds,
            interactionType = type
        )
    }

}
