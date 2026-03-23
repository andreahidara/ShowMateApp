package com.example.showmateapp.ui.screens.swipe

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.showmateapp.data.network.MediaContent
import com.example.showmateapp.data.repository.UserRepository
import com.example.showmateapp.data.repository.ShowRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

import com.example.showmateapp.domain.usecase.GetRecommendationsUseCase
import com.example.showmateapp.util.NarrativeStyleMapper
import android.util.Log

@HiltViewModel
class SwipeViewModel @Inject constructor(
    private val userRepository: UserRepository,
    private val getRecommendationsUseCase: GetRecommendationsUseCase
) : ViewModel() {

    private val _shows = MutableStateFlow<List<MediaContent>>(emptyList())
    val shows: StateFlow<List<MediaContent>> = _shows.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    private val _isLoading = MutableStateFlow(true)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    // Persists across tab switches unlike composable remember state
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
                userRepository.toggleFavorite(show, setLiked = true)
                userRepository.trackMediaInteraction(
                    mediaId = show.id,
                    genres = show.safeGenreIds.map { it.toString() },
                    keywords = show.keywordNames,
                    actors = show.credits?.cast?.map { it.id } ?: emptyList(),
                    narrativeStyles = NarrativeStyleMapper.extractStyles(
                        show.keywordNames, show.episodeRunTime?.firstOrNull()
                    ),
                    creators = show.creatorIds,
                    interactionType = UserRepository.InteractionType.Like
                )
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
                userRepository.trackMediaInteraction(
                    mediaId = show.id,
                    genres = show.safeGenreIds.map { it.toString() },
                    keywords = show.keywordNames,
                    actors = show.credits?.cast?.map { it.id } ?: emptyList(),
                    narrativeStyles = NarrativeStyleMapper.extractStyles(
                        show.keywordNames, show.episodeRunTime?.firstOrNull()
                    ),
                    creators = show.creatorIds,
                    interactionType = UserRepository.InteractionType.Dislike
                )
            }
        }
    }

    fun loadShows(forceReload: Boolean = false) {
        // Don't reset the card stack if the user just switched tabs and comes back
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
                Log.e("SwipeViewModel", "Error loading shows", e)
                _errorMessage.value = "Hubo un error cargando las series: ${e.localizedMessage ?: "Inténtalo de nuevo"}"
            } finally {
                _isLoading.value = false
            }
        }
    }
}
