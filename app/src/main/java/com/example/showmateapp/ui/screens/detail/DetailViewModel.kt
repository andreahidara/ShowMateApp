package com.example.showmateapp.ui.screens.detail

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.showmateapp.data.network.MediaContent
import com.example.showmateapp.data.network.SeasonResponse
import com.example.showmateapp.data.repository.ShowRepository
import com.example.showmateapp.data.repository.UserRepository
import com.example.showmateapp.util.Resource
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import javax.inject.Inject
import com.example.showmateapp.domain.usecase.GetRecommendationsUseCase

data class DetailUiState(
    val isLoading: Boolean = true,
    val media: MediaContent? = null,
    val errorMessage: String? = null,
    val isLiked: Boolean = false,
    val isEssential: Boolean = false,
    val isWatched: Boolean = false,
    val userRating: Int? = null,
    val userReview: String = "",
    val isSavingReview: Boolean = false,
    val isReviewSaved: Boolean = false,
    val similarShows: List<MediaContent> = emptyList(),
    val isSimilarLoading: Boolean = true,
    val actionError: String? = null,
    val watchedEpisodes: List<Int> = emptyList(),
    val selectedSeason: SeasonResponse? = null,
    val isSeasonLoading: Boolean = false,
    val customLists: Map<String, List<Int>> = emptyMap(),
    val showAddToListDialog: Boolean = false,
    val showUnlikeConfirm: Boolean = false,
    val showUnwatchConfirm: Boolean = false
)

@HiltViewModel
class DetailViewModel @Inject constructor(
    private val showRepository: ShowRepository,
    private val userRepository: UserRepository,
    private val getRecommendationsUseCase: GetRecommendationsUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(DetailUiState())
    val uiState: StateFlow<DetailUiState> = _uiState.asStateFlow()

    // Serialises rapid like/essential/watched taps so Firestore writes don't race
    private val toggleMutex = Mutex()

    fun loadShowDetails(showId: Int) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            try {
                when (val result = showRepository.getShowDetails(showId)) {
                    is Resource.Success -> {
                        val details = result.data
                        val scoredDetails = getRecommendationsUseCase.scoreShows(listOf(details)).first()
                        _uiState.update { it.copy(media = scoredDetails) }

                        // Await interactions, rating and review in parallel before hiding the loader
                        // so the action buttons show the correct initial state on first render
                        coroutineScope {
                            launch { checkInteractions(scoredDetails.id) }
                            launch { loadUserRating(scoredDetails.id) }
                            launch { loadUserReview(scoredDetails.id) }
                        }

                        // Secondary content is fire-and-forget; it updates the UI when ready
                        launch { loadSimilarShows(showId) }
                        launch { loadCustomLists() }
                        scoredDetails.seasons?.firstOrNull()?.let {
                            launch { loadSeasonDetails(showId, it.seasonNumber) }
                        }
                    }
                    is Resource.Error -> {
                        _uiState.update { it.copy(errorMessage = result.message) }
                    }
                    else -> {}
                }
            } catch (e: Exception) {
                Log.e("DetailViewModel", "Error loading details", e)
                _uiState.update { it.copy(errorMessage = "Error al cargar los detalles.") }
            } finally {
                _uiState.update { it.copy(isLoading = false) }
            }
        }
    }

    private suspend fun checkInteractions(showId: Int) {
        try {
            // Prefer the Room cache for instant reads; fall back to Firestore on first launch
            val localState = userRepository.getLocalInteractionState(showId)
            val profile = userRepository.getUserProfile()
            val episodes = profile?.watchedEpisodes?.get(showId.toString()) ?: emptyList()
            if (localState != null) {
                _uiState.update { it.copy(
                    isLiked = localState.isLiked,
                    isEssential = localState.isEssential,
                    isWatched = localState.isWatched,
                    watchedEpisodes = episodes
                ) }
            } else {
                val favorites = userRepository.getFavorites()
                val essentials = userRepository.getEssentials()
                val watched = userRepository.getWatchedShows()
                val isLiked = favorites.any { it.id == showId }
                val isEssential = essentials.any { it.id == showId }
                val isWatched = watched.any { it.id == showId }
                _uiState.update { it.copy(
                    isLiked = isLiked,
                    isEssential = isEssential,
                    isWatched = isWatched,
                    watchedEpisodes = episodes
                ) }
                // Cache result so future opens skip Firestore
                userRepository.cacheInteractionState(showId, isLiked, isEssential, isWatched)
            }
        } catch (e: Exception) {
            Log.w("DetailViewModel", "Error loading interaction state for $showId", e)
        }
    }

    fun requestToggleLiked() {
        if (_uiState.value.isLiked) {
            _uiState.update { it.copy(showUnlikeConfirm = true) }
        } else {
            toggleLiked()
        }
    }

    fun requestToggleWatched() {
        if (_uiState.value.isWatched) {
            _uiState.update { it.copy(showUnwatchConfirm = true) }
        } else {
            toggleWatched()
        }
    }

    fun cancelConfirm() {
        _uiState.update { it.copy(showUnlikeConfirm = false, showUnwatchConfirm = false) }
    }

    fun toggleLiked() {
        _uiState.update { it.copy(showUnlikeConfirm = false) }
        val currentShow = _uiState.value.media ?: return
        val previousState = _uiState.value.isLiked
        val newState = !previousState
        _uiState.update { it.copy(isLiked = newState) }

        viewModelScope.launch {
            toggleMutex.withLock {
                try {
                    userRepository.toggleFavorite(currentShow, newState)
                    if (newState) {
                        userRepository.trackMediaInteraction(
                            mediaId = currentShow.id,
                            genres = currentShow.safeGenreIds.map { it.toString() },
                            keywords = currentShow.keywords?.results?.map { it.name } ?: emptyList(),
                            actors = currentShow.credits?.cast?.map { it.id } ?: emptyList(),
                            interactionType = UserRepository.InteractionType.Like
                        )
                    }
                } catch (e: Exception) {
                    _uiState.update { it.copy(isLiked = previousState, actionError = "Error al actualizar") }
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
                    userRepository.toggleEssential(currentShow, newState)
                    if (newState) {
                        userRepository.trackMediaInteraction(
                            mediaId = currentShow.id,
                            genres = currentShow.safeGenreIds.map { it.toString() },
                            keywords = currentShow.keywords?.results?.map { it.name } ?: emptyList(),
                            actors = currentShow.credits?.cast?.map { it.id } ?: emptyList(),
                            interactionType = UserRepository.InteractionType.Essential
                        )
                    }
                } catch (e: Exception) {
                    _uiState.update { it.copy(isEssential = previousState, actionError = "Error al actualizar") }
                }
            }
        }
    }

    fun toggleWatched() {
        _uiState.update { it.copy(showUnwatchConfirm = false) }
        val currentShow = _uiState.value.media ?: return
        val previousState = _uiState.value.isWatched
        val newState = !previousState
        _uiState.update { it.copy(isWatched = newState) }

        viewModelScope.launch {
            toggleMutex.withLock {
                try {
                    userRepository.toggleWatched(currentShow, newState)
                    if (newState) {
                        userRepository.trackMediaInteraction(
                            mediaId = currentShow.id,
                            genres = currentShow.safeGenreIds.map { it.toString() },
                            keywords = currentShow.keywords?.results?.map { it.name } ?: emptyList(),
                            actors = currentShow.credits?.cast?.map { it.id } ?: emptyList(),
                            interactionType = UserRepository.InteractionType.Watched
                        )
                    }
                } catch (e: Exception) {
                    _uiState.update { it.copy(isWatched = previousState, actionError = "Error al actualizar") }
                }
            }
        }
    }

    fun toggleEpisodeWatched(episodeId: Int) {
        val showId = _uiState.value.media?.id ?: return
        val currentWatched = _uiState.value.watchedEpisodes.toMutableList()
        
        if (currentWatched.contains(episodeId)) currentWatched.remove(episodeId)
        else currentWatched.add(episodeId)
        
        _uiState.update { it.copy(watchedEpisodes = currentWatched) }

        viewModelScope.launch {
            try {
                userRepository.toggleEpisodeWatched(showId, episodeId)
            } catch (e: Exception) {
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
                val season = showRepository.getSeasonDetails(showId, seasonNumber)
                _uiState.update { it.copy(selectedSeason = season) }
            } catch (e: Exception) {
                Log.e("DetailViewModel", "Error loading season", e)
            } finally {
                _uiState.update { it.copy(isSeasonLoading = false) }
            }
        }
    }

    private suspend fun loadUserRating(showId: Int) {
        val rating = userRepository.getUserRating(showId)
        _uiState.update { it.copy(userRating = rating) }
    }

    private suspend fun loadUserReview(showId: Int) {
        val review = try { userRepository.getReview(showId) } catch (_: Exception) { null }
        _uiState.update { it.copy(
            userReview = review ?: "",
            isReviewSaved = !review.isNullOrBlank()
        ) }
    }

    fun loadCustomLists() {
        viewModelScope.launch {
            try {
                val lists = userRepository.getCustomLists()
                _uiState.update { it.copy(customLists = lists) }
            } catch (_: Exception) {}
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
                userRepository.addToCustomList(listName, showId)
                _uiState.update { it.copy(actionError = "Añadido a «$listName»") }
                loadCustomLists()
            } catch (_: Exception) {
                _uiState.update { it.copy(actionError = "Error al añadir a la lista") }
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
                userRepository.saveReview(showId, text)
                _uiState.update { it.copy(isReviewSaved = true) }
            } catch (e: Exception) {
                _uiState.update { it.copy(actionError = "Error al guardar la reseña") }
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
                userRepository.saveReview(showId, "")
            } catch (e: Exception) {
                _uiState.update { it.copy(actionError = "Error al borrar la reseña") }
            }
        }
    }

    fun rateShow(rating: Int) {
        val currentShow = _uiState.value.media ?: return
        _uiState.update { it.copy(userRating = rating) }

        viewModelScope.launch {
            try {
                userRepository.updateRating(currentShow.id, rating)
                userRepository.trackMediaInteraction(
                    mediaId = currentShow.id,
                    genres = currentShow.safeGenreIds.map { it.toString() },
                    keywords = currentShow.keywords?.results?.map { it.name } ?: emptyList(),
                    actors = currentShow.credits?.cast?.map { it.id } ?: emptyList(),
                    interactionType = UserRepository.InteractionType.Rate(rating)
                )
            } catch (e: Exception) {
                _uiState.update { it.copy(actionError = "Error al guardar la valoración") }
            }
        }
    }

    fun clearRating() {
        val currentShow = _uiState.value.media ?: return
        val previousRating = _uiState.value.userRating
        _uiState.update { it.copy(userRating = null) }
        viewModelScope.launch {
            try {
                userRepository.deleteRating(currentShow.id)
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
                Log.e("DetailViewModel", "Error loading similar shows", e)
            } finally {
                _uiState.update { it.copy(isSimilarLoading = false) }
            }
        }
    }

    fun clearActionError() {
        _uiState.update { it.copy(actionError = null) }
    }
}
