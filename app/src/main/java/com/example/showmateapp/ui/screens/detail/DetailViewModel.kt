package com.example.showmateapp.ui.screens.detail

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.showmateapp.data.model.UserProfile
import com.example.showmateapp.data.network.MediaContent
import com.example.showmateapp.data.network.SeasonResponse
import com.example.showmateapp.data.repository.ShowRepository
import com.example.showmateapp.data.repository.UserRepository
import com.example.showmateapp.util.GenreMapper
import com.example.showmateapp.util.NarrativeStyleMapper
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

data class WhyFactor(val label: String, val score: Float, val emoji: String)

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
    val snackbarMessage: String? = null,  // mensajes informativos no-error (éxito, confirmación)
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

    private val _whyFactors = MutableStateFlow<List<WhyFactor>>(emptyList())
    val whyFactors: StateFlow<List<WhyFactor>> = _whyFactors.asStateFlow()

    private val _showWhyDialog = MutableStateFlow(false)
    val showWhyDialog: StateFlow<Boolean> = _showWhyDialog.asStateFlow()

    fun showWhyDialog() { _showWhyDialog.value = true }
    fun dismissWhyDialog() { _showWhyDialog.value = false }

    // Serializa pulsaciones rápidas de like/esencial/visto para que las escrituras en Firestore no compitan
    private val toggleMutex = Mutex()

    fun loadShowDetails(showId: Int) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            try {
                when (val result = showRepository.getShowDetails(showId)) {
                    is Resource.Success -> {
                        val details = result.data

                        // Carga el perfil una sola vez — se reutiliza para scoring, interacciones y WhyFactors
                        val profile = try { userRepository.getUserProfile() } catch (e: Exception) {
                            Log.w("DetailViewModel", "Could not load profile", e); null
                        }

                        val scoredDetails = getRecommendationsUseCase.scoreShows(listOf(details)).firstOrNull() ?: details
                        _uiState.update { it.copy(media = scoredDetails) }

                        coroutineScope {
                            launch { checkInteractions(scoredDetails.id, profile) }
                            launch { loadUserRating(scoredDetails.id) }
                            launch { loadUserReview(scoredDetails.id) }
                        }

                        if (profile != null) {
                            _whyFactors.value = buildWhyFactors(scoredDetails, profile)
                        }

                        // Contenido secundario: fire-and-forget, actualiza la UI cuando esté listo
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

    private suspend fun checkInteractions(showId: Int, cachedProfile: UserProfile? = null) {
        try {
            val localState = userRepository.getLocalInteractionState(showId)
            val profile = cachedProfile ?: userRepository.getUserProfile()
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
                // Guarda en caché para que aperturas futuras no necesiten llamar a Firestore
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
                    if (newState) trackInteraction(currentShow, UserRepository.InteractionType.Like)
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
                    if (newState) trackInteraction(currentShow, UserRepository.InteractionType.Essential)
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
                    if (newState) trackInteraction(currentShow, UserRepository.InteractionType.Watched)
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
                val result = showRepository.getSeasonDetails(showId, seasonNumber)
                if (result is Resource.Success) {
                    _uiState.update { it.copy(selectedSeason = result.data) }
                } else {
                    Log.e("DetailViewModel", "Error loading season: ${(result as? Resource.Error)?.message}")
                }
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
                _uiState.update { it.copy(snackbarMessage = "Añadido a «$listName»") }
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
                trackInteraction(currentShow, UserRepository.InteractionType.Rate(rating))
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

    fun clearSnackbarMessage() {
        _uiState.update { it.copy(snackbarMessage = null) }
    }

    private suspend fun trackInteraction(show: MediaContent, type: UserRepository.InteractionType) {
        userRepository.trackMediaInteraction(
            mediaId = show.id,
            genres = show.safeGenreIds.map { it.toString() },
            keywords = show.keywordNames,
            actors = show.credits?.cast?.map { it.id } ?: emptyList(),
            narrativeStyles = NarrativeStyleMapper.extractStyles(show.keywordNames, show.episodeRunTime?.firstOrNull()),
            creators = show.creatorIds,
            interactionType = type
        )
    }

    private fun buildWhyFactors(show: MediaContent, profile: UserProfile): List<WhyFactor> {
        val factors = mutableListOf<WhyFactor>()

        // Genre factor
        val maxGenre = profile.genreScores.values.maxOrNull()?.coerceAtLeast(1f) ?: 1f
        val genreMatch = show.safeGenreIds.mapNotNull { id ->
            profile.genreScores[id.toString()]
        }.maxOrNull() ?: 0f
        val genreScore = (genreMatch / maxGenre).coerceIn(0f, 1f)
        if (genreScore > 0.1f) {
            val topGenreName = show.safeGenreIds
                .mapNotNull { id -> profile.genreScores[id.toString()]?.let { id to it } }
                .maxByOrNull { it.second }?.first
                ?.let { GenreMapper.getGenreName(it.toString()) }
                ?: "tu género favorito"
            factors.add(WhyFactor("Género: $topGenreName", genreScore, "🎭"))
        }

        // Narrative style factor
        val keywords = show.keywords?.results?.map { it.name } ?: emptyList()
        val runtime = show.episodeRunTime?.firstOrNull()
        val showStyles = NarrativeStyleMapper.extractStyles(keywords, runtime)
        val maxNarrative = profile.narrativeStyleScores.values.maxOrNull()?.coerceAtLeast(1f) ?: 1f
        val narrativeScore = showStyles.entries.mapNotNull { (style, rel) ->
            (profile.narrativeStyleScores[style] ?: 0f) / maxNarrative * rel
        }.maxOrNull() ?: 0f
        if (narrativeScore > 0.1f) {
            val topStyle = showStyles.entries
                .mapNotNull { (style, rel) -> profile.narrativeStyleScores[style]?.let { style to it * rel } }
                .maxByOrNull { it.second }?.first
                ?.let { NarrativeStyleMapper.getStyleLabel(it) } ?: "tu estilo narrativo"
            factors.add(WhyFactor("Narrativa: $topStyle", narrativeScore.coerceIn(0f, 1f), "📖"))
        }

        // Actor factor
        val maxActor = profile.preferredActors.values.maxOrNull()?.coerceAtLeast(1f) ?: 1f
        val actorScore = (show.credits?.cast?.mapNotNull { actor ->
            profile.preferredActors[actor.id.toString()]
        }?.maxOrNull() ?: 0f) / maxActor
        if (actorScore > 0.1f) {
            val topActor = show.credits?.cast
                ?.mapNotNull { a -> profile.preferredActors[a.id.toString()]?.let { a.name to it } }
                ?.maxByOrNull { it.second }?.first ?: "tu actor favorito"
            factors.add(WhyFactor("Actor: $topActor", actorScore.coerceIn(0f, 1f), "🎬"))
        }

        // Creator factor
        val maxCreator = profile.preferredCreators.values.maxOrNull()?.coerceAtLeast(1f) ?: 1f
        val creatorScore = (show.credits?.crew
            ?.filter { it.job in listOf("Creator", "Executive Producer", "Showrunner", "Series Director") }
            ?.mapNotNull { c -> profile.preferredCreators[c.id.toString()] }
            ?.maxOrNull() ?: 0f) / maxCreator
        if (creatorScore > 0.1f) {
            val topCreator = show.credits?.crew
                ?.filter { it.job in listOf("Creator", "Executive Producer", "Showrunner", "Series Director") }
                ?.mapNotNull { c -> profile.preferredCreators[c.id.toString()]?.let { c.name to it } }
                ?.maxByOrNull { it.second }?.first ?: "el creador"
            factors.add(WhyFactor("Creador: $topCreator", creatorScore.coerceIn(0f, 1f), "🎯"))
        }

        // Global quality factor
        val bayesian = if ((show.voteCount + 150f) > 0)
            ((show.voteCount / (show.voteCount + 150f)) * show.voteAverage + (150f / (show.voteCount + 150f)) * 6.5f)
        else 6.5f
        val globalScore = ((bayesian - 5f) / 5f).coerceIn(0f, 1f)
        if (globalScore > 0.3f) {
            factors.add(WhyFactor("Valoración global: ${"%.1f".format(show.voteAverage)}★", globalScore, "⭐"))
        }

        return factors.sortedByDescending { it.score }.take(4)
    }
}
