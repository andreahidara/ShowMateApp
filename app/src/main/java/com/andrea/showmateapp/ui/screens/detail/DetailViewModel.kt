package com.andrea.showmateapp.ui.screens.detail

import androidx.compose.runtime.Immutable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.andrea.showmateapp.R
import com.andrea.showmateapp.data.model.ReasonType
import com.andrea.showmateapp.data.model.RecommendationReason
import com.andrea.showmateapp.data.model.UserProfile
import com.andrea.showmateapp.data.model.MediaContent
import com.andrea.showmateapp.data.model.SeasonResponse
import com.andrea.showmateapp.domain.repository.IInteractionRepository
import com.andrea.showmateapp.domain.repository.IShowRepository
import com.andrea.showmateapp.domain.repository.IUserRepository
import com.andrea.showmateapp.domain.usecase.AchievementChecker
import com.andrea.showmateapp.domain.usecase.GetRecommendationsUseCase
import com.andrea.showmateapp.util.BaseUiState
import com.andrea.showmateapp.util.GenreMapper
import com.andrea.showmateapp.util.NarrativeStyleMapper
import com.andrea.showmateapp.util.Resource
import com.andrea.showmateapp.util.UiText
import com.google.firebase.crashlytics.FirebaseCrashlytics
import dagger.hilt.android.lifecycle.HiltViewModel
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import javax.inject.Inject
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import timber.log.Timber

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

    fun showWhyDialog() {
        _showWhyDialog.value = true
    }
    fun dismissWhyDialog() {
        _showWhyDialog.value = false
    }

    private val toggleMutex = Mutex()

    fun refresh(showId: Int) {
        viewModelScope.launch {
            _uiState.update { it.copy(isRefreshing = true, errorMessage = null) }
            try {
                when (val result = showRepository.getShowDetails(showId)) {
                    is Resource.Success -> {
                        val scoredDetails = getRecommendationsUseCase.scoreForDetail(result.data)
                        _uiState.update { it.copy(media = scoredDetails) }
                        loadUserRating(scoredDetails.id)
                        loadUserReview(scoredDetails.id)
                    }
                    is Resource.Error -> {
                        _uiState.update { it.copy(errorMessage = UiText.DynamicString(result.effectiveMessage)) }
                    }
                    else -> {}
                }
            } catch (e: Exception) {
                if (e is CancellationException) throw e
                _uiState.update { it.copy(errorMessage = UiText.StringResource(R.string.error_unexpected_data)) }
            } finally {
                _uiState.update { it.copy(isRefreshing = false) }
            }
        }
    }

    fun loadShowDetails(showId: Int) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            try {
                when (val result = showRepository.getShowDetails(showId)) {
                    is Resource.Success -> {
                        val details = result.data

                        val profile = try {
                            userRepository.getUserProfile()
                        } catch (e: Exception) {
                            Timber.w(e, "Could not load profile")
                            null
                        }

                        val scoredDetails = getRecommendationsUseCase.scoreForDetail(details)
                        _uiState.update { it.copy(media = scoredDetails) }

                        checkInteractions(scoredDetails.id, profile)
                        loadUserRating(scoredDetails.id)
                        loadUserReview(scoredDetails.id)

                        _whyFactors.value = scoredDetails.reasons.ifEmpty {
                            buildFallbackReasons(scoredDetails, profile)
                        }

                        loadCustomLists()
                        scoredDetails.seasons?.firstOrNull { it.seasonNumber > 0 }?.let {
                            loadSeasonDetails(showId, it.seasonNumber)
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
                _uiState.update {
                    it.copy(
                        isLiked = localState.isLiked,
                        isEssential = localState.isEssential,
                        isWatched = localState.isWatched,
                        isInWatchlist = localState.isInWatchlist,
                        watchedEpisodes = episodes
                    )
                }
            } else {
                val profile = cachedProfile ?: userRepository.getUserProfile()
                val episodes = profile?.watchedEpisodes?.get(showId.toString()) ?: emptyList()
                val isLiked = profile?.likedMediaIds?.contains(showId)
                    ?: interactionRepository.getFavorites().any { it.id == showId }
                val isEssential = profile?.essentialMediaIds?.contains(showId)
                    ?: interactionRepository.getEssentials().any { it.id == showId }
                val isWatched = showId in interactionRepository.getWatchedMediaIds()
                val isInWatchlist = interactionRepository.isInWatchlist(showId)
                _uiState.update {
                    it.copy(
                        isLiked = isLiked,
                        isEssential = isEssential,
                        isWatched = isWatched,
                        isInWatchlist = isInWatchlist,
                        watchedEpisodes = episodes
                    )
                }
                interactionRepository.cacheInteractionState(showId, isLiked, isEssential, isWatched)
            }
        } catch (e: Exception) {
            if (e is CancellationException) throw e
            Timber.w(e, "Error loading interaction state for %d", showId)
        }
    }

    private suspend fun updateInteraction(
        current: Boolean,
        action: suspend (Boolean) -> Unit,
        updateState: (DetailUiState, Boolean) -> DetailUiState,
        onSuccess: (suspend () -> Unit)? = null
    ) {
        val newState = !current
        toggleMutex.withLock {
            try {
                action(newState)
                onSuccess?.invoke()
            } catch (e: Exception) {
                if (e is CancellationException) throw e
                _uiState.update { updateState(it, current) }
            }
        }
    }

    fun toggleLiked() {
        val currentShow = _uiState.value.media ?: return
        val current = _uiState.value.isLiked
        _uiState.update { it.copy(isLiked = !current) }

        viewModelScope.launch {
            updateInteraction(
                current = current,
                action = { interactionRepository.toggleFavorite(currentShow, it) },
                updateState = { state, old -> state.copy(isLiked = old, actionError = UiText.StringResource(R.string.error_update_failed)) },
                onSuccess = {
                    if (!current) {
                        trackInteraction(currentShow, IInteractionRepository.InteractionType.Like)
                        launchAchievementEvaluate()
                    }
                }
            )
        }
    }

    fun toggleEssential() {
        val currentShow = _uiState.value.media ?: return
        val current = _uiState.value.isEssential
        _uiState.update { it.copy(isEssential = !current) }

        viewModelScope.launch {
            updateInteraction(
                current = current,
                action = { interactionRepository.toggleEssential(currentShow, it) },
                updateState = { state, old -> state.copy(isEssential = old, actionError = UiText.StringResource(R.string.error_update_failed)) },
                onSuccess = {
                    if (!current) {
                        trackInteraction(currentShow, IInteractionRepository.InteractionType.Essential)
                        launchAchievementEvaluate()
                    }
                }
            )
        }
    }

    fun toggleWatched() {
        val currentShow = _uiState.value.media ?: return
        val current = _uiState.value.isWatched
        _uiState.update { it.copy(isWatched = !current) }

        viewModelScope.launch {
            updateInteraction(
                current = current,
                action = {
                    interactionRepository.toggleWatched(currentShow, it)
                    markAllSeasonsWatched(currentShow, it)
                },
                updateState = { state, old -> state.copy(isWatched = old, actionError = UiText.StringResource(R.string.error_update_failed)) },
                onSuccess = {
                    if (!current) {
                        trackInteraction(currentShow, IInteractionRepository.InteractionType.Watched)
                        val totalEps = currentShow.seasons?.sumOf { it.episodeCount } ?: (currentShow.numberOfSeasons ?: 1) * 10
                        runCatching { userRepository.recordViewingSession(currentShow.id, totalEps) }
                        launchAchievementEvaluate()
                    }
                }
            )
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
            }.awaitAll().flatten()
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
                    _uiState.update {
                        it.copy(
                            isInWatchlist = previousState,
                            actionError = UiText.StringResource(R.string.error_update_failed)
                        )
                    }
                }
            }
        }
    }

    fun toggleEpisodeWatched(episodeId: Int, markPrevious: Boolean = false) {
        val showId = _uiState.value.media?.id ?: return
        val currentWatched = _uiState.value.watchedEpisodes.toMutableList()
        val oldWatchedCount = currentWatched.size
        val season = _uiState.value.selectedSeason

        if (markPrevious && season != null) {
            val epIndex = season.episodes.indexOfFirst { it.id == episodeId }
            if (epIndex != -1) {
                val toMark = season.episodes.take(epIndex + 1).map { it.id }
                if (toMark.all { it in currentWatched }) currentWatched.removeAll(toMark)
                else toMark.forEach { if (it !in currentWatched) currentWatched.add(it) }
            }
        } else {
            if (!currentWatched.remove(episodeId)) currentWatched.add(episodeId)
        }

        _uiState.update { it.copy(watchedEpisodes = currentWatched) }

        viewModelScope.launch {
            try {
                if (markPrevious && season != null) {
                    interactionRepository.setAllEpisodesWatched(showId, currentWatched)
                    val delta = currentWatched.size - oldWatchedCount
                    if (delta > 0) {
                        runCatching { userRepository.recordViewingSession(showId, delta) }
                        checkAutoMarkWatched(showId, currentWatched)
                    }
                } else {
                    if (interactionRepository.toggleEpisodeWatched(showId, episodeId)) {
                        launchAchievementEvaluate()
                        runCatching { userRepository.recordViewingSession(showId, 1) }
                        checkAutoMarkWatched(showId, _uiState.value.watchedEpisodes)
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

        val isCompleted = seasonEpIds.all { it in currentWatched }
        if (isCompleted) currentWatched.removeAll(seasonEpIds.toSet())
        else currentWatched.addAll(seasonEpIds)

        val newList = currentWatched.toList()
        _uiState.update { it.copy(watchedEpisodes = newList) }

        viewModelScope.launch {
            try {
                interactionRepository.setAllEpisodesWatched(showId, newList)
                if (!isCompleted) {
                    launchAchievementEvaluate()
                    runCatching { userRepository.recordViewingSession(showId, seasonEpIds.size) }
                    checkAutoMarkWatched(showId, newList)
                }
            } catch (e: Exception) {
                if (e is CancellationException) throw e
                checkInteractions(showId)
            }
        }
    }

    private suspend fun checkAutoMarkWatched(showId: Int, watchedEpisodes: List<Int>) {
        if (_uiState.value.isWatched) return
        val show = _uiState.value.media ?: return
        val seasons = show.seasons?.filter { it.seasonNumber > 0 } ?: return
        val allEpisodeIds = coroutineScope {
            seasons.map { season ->
                async {
                    runCatching { showRepository.getSeasonDetails(showId, season.seasonNumber) }
                        .getOrNull()
                        ?.let { if (it is Resource.Success) it.data.episodes.map { ep -> ep.id } else emptyList() }
                        ?: emptyList()
                }
            }.awaitAll().flatten()
        }
        if (allEpisodeIds.isNotEmpty() && watchedEpisodes.containsAll(allEpisodeIds)) {
            interactionRepository.toggleWatched(show, true)
            _uiState.update { it.copy(isWatched = true) }
            trackInteraction(show, IInteractionRepository.InteractionType.Watched)
            launchAchievementEvaluate()
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
        val review = try {
            interactionRepository.getReview(showId)
        } catch (e: Exception) {
            if (e is CancellationException) throw e
            null
        }
        _uiState.update {
            it.copy(
                userReview = review ?: "",
                isReviewSaved = !review.isNullOrBlank()
            )
        }
    }

    fun loadCustomLists() {
        viewModelScope.launch {
            try {
                val lists = interactionRepository.getCustomLists()
                _uiState.update { it.copy(customLists = lists) }
            } catch (e: Exception) {
                if (e is CancellationException) throw e
            }
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
                _uiState.update {
                    it.copy(
                        snackbarMessage = UiText.StringResource(R.string.detail_added_to_list, listName)
                    )
                }
                loadCustomLists()
            } catch (e: Exception) {
                if (e is CancellationException) throw e
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
                if (e is CancellationException) throw e
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
                if (e is CancellationException) throw e
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
                if (e is CancellationException) throw e
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
                if (e is CancellationException) throw e
                _uiState.update { it.copy(userRating = previousRating) }
            }
        }
    }

    fun loadSimilarShowsIfNeeded(showId: Int) {
        if (_uiState.value.similarShows.isNotEmpty() || _uiState.value.isSimilarLoading) return
        loadSimilarShows(showId)
    }

    private fun loadSimilarShows(showId: Int) {
        viewModelScope.launch {
            _uiState.update { it.copy(isSimilarLoading = true) }
            try {
                var similar = showRepository.getSimilarShows(showId)

                if (similar.isEmpty()) {

                    delay(500)
                    similar = showRepository.getSimilarShows(showId)
                }

                if (similar.isEmpty()) {
                    Timber.i("Similar shows empty after retry, attempting genre-based fallback for $showId")
                    val genres = _uiState.value.media?.safeGenreIds?.joinToString(",")
                    if (!genres.isNullOrEmpty()) {
                        val fallback = showRepository.getShowsByGenres(genres)
                        if (fallback is Resource.Success) {
                            similar = fallback.data.filter { it.id != showId }
                        }
                    }
                }

                val scoredSimilar = getRecommendationsUseCase.scoreShows(similar)
                _uiState.update { it.copy(similarShows = scoredSimilar) }
            } catch (e: Exception) {
                if (e is CancellationException) throw e
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
                        profile = profile,
                        episodesToday = episodesToday,
                        voteCount = media.voteCount,
                        countries = media.originCountry
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

    private fun buildFallbackReasons(show: MediaContent, profile: UserProfile?): List<RecommendationReason> {
        val reasons = mutableListOf<RecommendationReason>()

        if (profile != null) {
            val topGenreId = profile.genreScores
                .entries.sortedByDescending { it.value }
                .firstOrNull { entry -> show.safeGenreIds.any { it.toString() == entry.key } }
                ?.key
            if (topGenreId != null) {
                reasons += RecommendationReason(
                    type = ReasonType.GENRE,
                    weight = 0.7f,
                    description = UiText.DynamicString("Coincide con tu preferencia de ${GenreMapper.getGenreName(topGenreId)}"),
                    iconEmoji = "🎭"
                )
            }

            val actorMatch = show.credits?.cast?.map { it.id.toString() }
                ?.firstOrNull { profile.preferredActors.containsKey(it) }
            if (actorMatch != null) {
                val actorName = show.credits?.cast?.firstOrNull { it.id.toString() == actorMatch }?.name ?: ""
                reasons += RecommendationReason(
                    type = ReasonType.ACTOR,
                    weight = 0.6f,
                    description = UiText.DynamicString("Protagonizada por $actorName, que te gusta"),
                    iconEmoji = "🎬"
                )
            }
        }

        if (show.voteAverage >= 7.5f && show.voteCount > 1000) {
            reasons += RecommendationReason(
                type = ReasonType.TRENDING,
                weight = 0.5f,
                description = UiText.DynamicString("Altamente valorada por la comunidad"),
                iconEmoji = "⭐"
            )
        }

        if (show.voteAverage >= 7.0f && show.voteCount in 100..4999) {
            reasons += RecommendationReason(
                type = ReasonType.HIDDEN_GEM,
                weight = 0.5f,
                description = UiText.DynamicString("Joya oculta con gran valoración"),
                iconEmoji = "💎"
            )
        }

        return reasons.distinctBy { it.type }.take(3)
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
