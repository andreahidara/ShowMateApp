package com.example.showmateapp.ui.screens.swipe

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.showmateapp.data.network.MediaContent
import com.example.showmateapp.data.repository.UserRepository
import com.example.showmateapp.data.repository.ShowRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

import com.example.showmateapp.domain.usecase.GetRecommendationsUseCase

@HiltViewModel
class SwipeViewModel @Inject constructor(
    private val userRepository: UserRepository,
    private val getRecommendationsUseCase: GetRecommendationsUseCase
) : ViewModel() {
    
    private val _shows = MutableStateFlow<List<MediaContent>>(emptyList())
    val shows: StateFlow<List<MediaContent>> = _shows

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage

    private val _isLoading = MutableStateFlow(true)
    val isLoading: StateFlow<Boolean> = _isLoading

    fun likeTopShow() {
        val currentList = _shows.value.toMutableList()
        if (currentList.isNotEmpty()) {
            val show = currentList.removeAt(0)
            _shows.value = currentList
            viewModelScope.launch {
                userRepository.toggleFavorite(show)
                userRepository.trackMediaInteraction(
                    mediaId = show.id,
                    genres = show.safeGenreIds.map { it.toString() },
                    keywords = show.keywords?.results?.map { it.name } ?: emptyList(),
                    actors = show.credits?.cast?.map { it.id } ?: emptyList(),
                    interactionType = UserRepository.InteractionType.Like
                )
            }
        }
    }

    fun skipTopShow() {
        val currentList = _shows.value.toMutableList()
        if (currentList.isNotEmpty()) {
            val show = currentList.removeAt(0)
            _shows.value = currentList
            viewModelScope.launch {
                userRepository.trackMediaInteraction(
                    mediaId = show.id,
                    genres = show.safeGenreIds.map { it.toString() },
                    keywords = show.keywords?.results?.map { it.name } ?: emptyList(),
                    actors = show.credits?.cast?.map { it.id } ?: emptyList(),
                    interactionType = UserRepository.InteractionType.Dislike
                )
            }
        }
    }

    fun loadShows() {
        viewModelScope.launch {
            _isLoading.value = true
            _errorMessage.value = null
            try {
                _shows.value = getRecommendationsUseCase.execute()
            } catch (e: Exception) {
                _errorMessage.value = "Hubo un error cargando las series. Inténtalo de nuevo."
            } finally {
                _isLoading.value = false
            }
        }
    }
}
