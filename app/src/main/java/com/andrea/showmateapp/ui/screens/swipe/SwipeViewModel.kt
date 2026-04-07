package com.andrea.showmateapp.ui.screens.swipe

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.andrea.showmateapp.data.network.MediaContent
import com.andrea.showmateapp.domain.repository.IInteractionRepository
import com.andrea.showmateapp.domain.repository.IUserRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

import com.andrea.showmateapp.domain.usecase.AchievementChecker
import com.andrea.showmateapp.domain.usecase.AchievementDefs
import com.andrea.showmateapp.domain.usecase.GetRecommendationsUseCase
import kotlinx.coroutines.CancellationException
import timber.log.Timber
import com.andrea.showmateapp.util.NarrativeStyleMapper

@HiltViewModel
class SwipeViewModel @Inject constructor(
    private val userRepository: IUserRepository,
    private val interactionRepository: IInteractionRepository,
    private val getRecommendationsUseCase: GetRecommendationsUseCase,
    private val achievementChecker: AchievementChecker
) : ViewModel() {

    private val _shows = MutableStateFlow<List<MediaContent>>(emptyList())
    val shows: StateFlow<List<MediaContent>> = _shows.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    private val _isLoading = MutableStateFlow(true)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _ratedCount = MutableStateFlow(0)
    val ratedCount: StateFlow<Int> = _ratedCount.asStateFlow()

    private val _lastRemovedShow = MutableStateFlow<MediaContent?>(null)
    val lastRemovedShow: StateFlow<MediaContent?> = _lastRemovedShow.asStateFlow()

    fun undoLastAction() {
        val show = _lastRemovedShow.value ?: return
        val currentList = _shows.value.toMutableList()
        currentList.add(0, show)
        _shows.value = currentList
        _ratedCount.value = (_ratedCount.value - 1).coerceAtLeast(0)
        _lastRemovedShow.value = null
    }

    fun likeTopShow() {
        val currentList = _shows.value.toMutableList()
        if (currentList.isNotEmpty()) {
            val show = currentList.removeAt(0)
            _lastRemovedShow.value = show
            _shows.value = currentList
            _ratedCount.value++
            viewModelScope.launch {
                interactionRepository.toggleFavorite(show, setLiked = true)
                interactionRepository.trackMediaInteraction(
                    mediaId = show.id,
                    genres = show.safeGenreIds.map { it.toString() },
                    keywords = show.keywordNames,
                    actors = show.credits?.cast?.map { it.id } ?: emptyList(),
                    narrativeStyles = NarrativeStyleMapper.extractStyles(
                        show.keywordNames, show.episodeRunTime?.firstOrNull()
                    ),
                    creators = show.creatorIds,
                    interactionType = IInteractionRepository.InteractionType.Like
                )
                achievementChecker.addXp(AchievementDefs.XP_LIKE_SHOW)
                val profile = runCatching { userRepository.getUserProfile() }.getOrNull()
                if (profile != null) {
                    achievementChecker.evaluate(
                        AchievementChecker.EvalContext(
                            profile = profile,
                            watchedShowVoteCount = show.voteCount,
                            watchedShowOriginCountries = show.originCountry
                        )
                    )
                }
            }
        }
    }

    fun skipTopShow() {
        val currentList = _shows.value.toMutableList()
        if (currentList.isNotEmpty()) {
            val show = currentList.removeAt(0)
            _lastRemovedShow.value = show
            _shows.value = currentList
            _ratedCount.value++
            viewModelScope.launch {
                interactionRepository.trackMediaInteraction(
                    mediaId = show.id,
                    genres = show.safeGenreIds.map { it.toString() },
                    keywords = show.keywordNames,
                    actors = show.credits?.cast?.map { it.id } ?: emptyList(),
                    narrativeStyles = NarrativeStyleMapper.extractStyles(
                        show.keywordNames, show.episodeRunTime?.firstOrNull()
                    ),
                    creators = show.creatorIds,
                    interactionType = IInteractionRepository.InteractionType.Dislike
                )
            }
        }
    }

    fun loadShows(forceReload: Boolean = false) {
        if (!forceReload && _shows.value.isNotEmpty()) {
            _isLoading.value = false
            return
        }
        viewModelScope.launch {
            _isLoading.value = true
            _errorMessage.value = null
            try {
                _shows.value = getRecommendationsUseCase.execute()
            } catch (e: Exception) {
                if (e is CancellationException) throw e
                Timber.e(e, "Error loading shows")
                _errorMessage.value = "Hubo un error cargando las series: ${e.localizedMessage ?: "Inténtalo de nuevo"}"
            } finally {
                _isLoading.value = false
            }
        }
    }
}
