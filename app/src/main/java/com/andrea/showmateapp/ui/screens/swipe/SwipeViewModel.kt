package com.andrea.showmateapp.ui.screens.swipe

import androidx.compose.runtime.Immutable
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.andrea.showmateapp.data.model.MediaContent
import com.andrea.showmateapp.domain.repository.IInteractionRepository
import com.andrea.showmateapp.domain.repository.IUserRepository
import com.andrea.showmateapp.domain.usecase.AchievementChecker
import com.andrea.showmateapp.domain.usecase.AchievementDefs
import com.andrea.showmateapp.domain.usecase.GetRecommendationsUseCase
import com.andrea.showmateapp.R
import com.andrea.showmateapp.di.AppPrefsDataStore
import com.andrea.showmateapp.util.AppPrefsKeys
import com.andrea.showmateapp.util.NarrativeStyleMapper
import com.andrea.showmateapp.util.UiText
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import timber.log.Timber

@Immutable
data class SwipeUiState(
    val shows: List<MediaContent> = emptyList(),
    val isLoading: Boolean = true,
    val ratedCount: Int = 0,
    val lastAction: SwipeAction? = null,
    val errorMessage: UiText? = null
) {
    data class SwipeAction(val show: MediaContent, val isLike: Boolean)
}

@HiltViewModel
class SwipeViewModel @Inject constructor(
    private val userRepository: IUserRepository,
    private val interactionRepository: IInteractionRepository,
    private val getRecommendationsUseCase: GetRecommendationsUseCase,
    private val achievementChecker: AchievementChecker,
    @AppPrefsDataStore private val dataStore: DataStore<Preferences>
) : ViewModel() {

    private val _uiState = MutableStateFlow(SwipeUiState())
    val uiState: StateFlow<SwipeUiState> = _uiState.asStateFlow()

    fun loadShows(forceReload: Boolean = false) {
        if (!forceReload && _uiState.value.shows.isNotEmpty()) {
            _uiState.update { it.copy(isLoading = false) }
            return
        }
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            try {
                interactionRepository.syncFavoritesAndWatchedToRoom()
                val newShows = getRecommendationsUseCase.execute()
                _uiState.update { it.copy(shows = newShows, isLoading = false) }
            } catch (e: Exception) {
                if (e is CancellationException) throw e
                Timber.e(e, "Error loading shows")
                _uiState.update {
                    it.copy(isLoading = false, errorMessage = UiText.StringResource(R.string.swipe_error_load))
                }
            }
        }
    }

    fun completeCalibration(onSuccess: () -> Unit) {
        viewModelScope.launch {
            try {
                userRepository.completeCalibration()
                // Update local DataStore as well
                dataStore.edit { prefs -> prefs[AppPrefsKeys.KEY_CALIBRATION] = true }
                onSuccess()
            } catch (e: Exception) {
                if (e is CancellationException) throw e
                Timber.e(e, "Error completing calibration")
                _uiState.update { it.copy(errorMessage = UiText.StringResource(R.string.swipe_error_complete)) }
            }
        }
    }

    fun likeTopShow() {
        val show = _uiState.value.shows.firstOrNull() ?: return

        _uiState.update { state ->
            state.copy(
                shows = state.shows.drop(1),
                ratedCount = state.ratedCount + 1,
                lastAction = SwipeUiState.SwipeAction(show, isLike = true)
            )
        }

        viewModelScope.launch {
            try {
                interactionRepository.toggleFavorite(show, setLiked = true)
                interactionRepository.trackMediaInteraction(
                    mediaId = show.id,
                    genres = show.safeGenreIds.map { it.toString() },
                    keywords = show.keywordNames,
                    actors = show.credits?.cast?.map { it.id } ?: emptyList(),
                    narrativeStyles = NarrativeStyleMapper.extractStyles(
                        show.keywordNames,
                        show.episodeRunTime?.firstOrNull()
                    ),
                    creators = show.creatorIds,
                    interactionType = IInteractionRepository.InteractionType.Like
                )
                achievementChecker.addXp(AchievementDefs.XP_LIKE_SHOW)

                if (_uiState.value.shows.size < 5) refillShows()

                runCatching { userRepository.getUserProfile() }.getOrNull()?.let { profile ->
                    achievementChecker.evaluate(
                        AchievementChecker.EvalContext(
                            profile = profile,
                            voteCount = show.voteCount,
                            countries = show.originCountry
                        )
                    )
                }
            } catch (e: Exception) {
                if (e is CancellationException) throw e
                _uiState.update { state ->
                    state.copy(
                        shows = listOf(show) + state.shows,
                        ratedCount = (state.ratedCount - 1).coerceAtLeast(0),
                        lastAction = null,
                        errorMessage = UiText.StringResource(R.string.swipe_error_save)
                    )
                }
            }
        }
    }

    fun skipTopShow() {
        val show = _uiState.value.shows.firstOrNull() ?: return

        _uiState.update { state ->
            state.copy(
                shows = state.shows.drop(1),
                ratedCount = state.ratedCount + 1,
                lastAction = SwipeUiState.SwipeAction(show, isLike = false)
            )
        }

        viewModelScope.launch {
            try {
                interactionRepository.toggleDislike(show, setDisliked = true)
                interactionRepository.trackMediaInteraction(
                    mediaId = show.id,
                    genres = show.safeGenreIds.map { it.toString() },
                    keywords = show.keywordNames,
                    actors = show.credits?.cast?.map { it.id } ?: emptyList(),
                    narrativeStyles = NarrativeStyleMapper.extractStyles(
                        show.keywordNames,
                        show.episodeRunTime?.firstOrNull()
                    ),
                    creators = show.creatorIds,
                    interactionType = IInteractionRepository.InteractionType.Dislike
                )
                if (_uiState.value.shows.size < 5) refillShows()
            } catch (e: Exception) {
                if (e is CancellationException) throw e
                _uiState.update { state ->
                    state.copy(
                        shows = listOf(show) + state.shows,
                        ratedCount = (state.ratedCount - 1).coerceAtLeast(0)
                    )
                }
            }
        }
    }

    fun essentialTopShow() {
        val show = _uiState.value.shows.firstOrNull() ?: return

        _uiState.update { state ->
            state.copy(
                shows = state.shows.drop(1),
                ratedCount = state.ratedCount + 1,
                lastAction = SwipeUiState.SwipeAction(show, isLike = true)
            )
        }

        viewModelScope.launch {
            try {
                interactionRepository.toggleFavorite(show, setLiked = true)
                interactionRepository.toggleEssential(show, setEssential = true)
                interactionRepository.trackMediaInteraction(
                    mediaId = show.id,
                    genres = show.safeGenreIds.map { it.toString() },
                    keywords = show.keywordNames,
                    actors = show.credits?.cast?.map { it.id } ?: emptyList(),
                    narrativeStyles = NarrativeStyleMapper.extractStyles(
                        show.keywordNames,
                        show.episodeRunTime?.firstOrNull()
                    ),
                    creators = show.creatorIds,
                    interactionType = IInteractionRepository.InteractionType.Essential
                )
                achievementChecker.addXp(AchievementDefs.XP_LIKE_SHOW)
                if (_uiState.value.shows.size < 5) refillShows()
            } catch (e: Exception) {
                if (e is CancellationException) throw e
                _uiState.update { state ->
                    state.copy(
                        shows = listOf(show) + state.shows,
                        ratedCount = (state.ratedCount - 1).coerceAtLeast(0),
                        lastAction = null,
                        errorMessage = UiText.StringResource(R.string.swipe_error_save)
                    )
                }
            }
        }
    }

    private suspend fun refillShows() {
        try {
            val newShows = getRecommendationsUseCase.execute()
            if (newShows.isEmpty()) return
            _uiState.update { state ->
                val currentIds = state.shows.map { it.id }.toHashSet()
                state.copy(shows = state.shows + newShows.filter { it.id !in currentIds })
            }
        } catch (e: Exception) {
            if (e is CancellationException) throw e
        }
    }

    fun undoLastAction() {
        val action = _uiState.value.lastAction ?: return
        _uiState.update { state ->
            state.copy(
                shows = listOf(action.show) + state.shows,
                ratedCount = (state.ratedCount - 1).coerceAtLeast(0),
                lastAction = null
            )
        }
        viewModelScope.launch {
            try {
                if (action.isLike) {
                    interactionRepository.toggleFavorite(action.show, setLiked = false)
                } else {
                    interactionRepository.toggleDislike(action.show, setDisliked = false)
                }
            } catch (e: Exception) {
                if (e is CancellationException) throw e
                Timber.e(e, "Error undoing swipe action")
            }
        }
    }
}
