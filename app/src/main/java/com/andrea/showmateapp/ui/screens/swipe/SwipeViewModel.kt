package com.andrea.showmateapp.ui.screens.swipe

import androidx.compose.runtime.Immutable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.andrea.showmateapp.data.model.MediaContent
import com.andrea.showmateapp.domain.repository.IInteractionRepository
import com.andrea.showmateapp.domain.repository.IUserRepository
import com.andrea.showmateapp.domain.usecase.AchievementChecker
import com.andrea.showmateapp.domain.usecase.AchievementDefs
import com.andrea.showmateapp.domain.usecase.GetRecommendationsUseCase
import com.andrea.showmateapp.util.NarrativeStyleMapper
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import timber.log.Timber

@Immutable
data class SwipeUiState(
    val shows: List<MediaContent> = emptyList(),
    val isLoading: Boolean = true,
    val ratedCount: Int = 0,
    val lastAction: SwipeAction? = null,
    val errorMessage: String? = null
) {
    data class SwipeAction(val show: MediaContent, val isLike: Boolean)
}

sealed interface SwipeEffect {
    data class ShowError(val message: String) : SwipeEffect
}

@HiltViewModel
class SwipeViewModel @Inject constructor(
    private val userRepository: IUserRepository,
    private val interactionRepository: IInteractionRepository,
    private val getRecommendationsUseCase: GetRecommendationsUseCase,
    private val achievementChecker: AchievementChecker
) : ViewModel() {

    private val _uiState = MutableStateFlow(SwipeUiState())
    val uiState: StateFlow<SwipeUiState> = _uiState.asStateFlow()

    private val _effects = Channel<SwipeEffect>(Channel.BUFFERED)
    val effects = _effects.receiveAsFlow()

    fun loadShows(forceReload: Boolean = false) {
        if (!forceReload && _uiState.value.shows.isNotEmpty()) {
            _uiState.update { it.copy(isLoading = false) }
            return
        }
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            try {
                val newShows = getRecommendationsUseCase.execute()
                _uiState.update { it.copy(shows = newShows, isLoading = false) }
            } catch (e: Exception) {
                if (e is CancellationException) throw e
                Timber.e(e, "Error loading shows")
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        errorMessage = "Hubo un error cargando las series: " +
                            "${e.localizedMessage ?: "Inténtalo de nuevo"}"
                    )
                }
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
        if (_uiState.value.shows.size < 5) loadShows(false)

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
                        lastAction = null
                    )
                }
                _effects.trySend(SwipeEffect.ShowError("No se pudo guardar. Revisa tu conexión."))
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
        if (_uiState.value.shows.size < 5) loadShows(false)

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
        if (_uiState.value.shows.size < 5) loadShows(false)

        viewModelScope.launch {
            try {
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
            } catch (e: Exception) {
                if (e is CancellationException) throw e
                _uiState.update { state ->
                    state.copy(
                        shows = listOf(show) + state.shows,
                        ratedCount = (state.ratedCount - 1).coerceAtLeast(0),
                        lastAction = null
                    )
                }
                _effects.trySend(SwipeEffect.ShowError("No se pudo guardar. Revisa tu conexión."))
            }
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
            if (action.isLike) {
                interactionRepository.toggleFavorite(action.show, setLiked = false)
            } else {
                interactionRepository.toggleDislike(action.show, setDisliked = false)
            }
        }
    }
}

