package com.example.showmateapp.ui.screens.detail

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.showmateapp.data.network.MediaContent
import com.example.showmateapp.data.repository.ShowRepository
import com.example.showmateapp.data.repository.UserRepository
import com.example.showmateapp.domain.usecase.UpdateUserInterestsUseCase
import com.example.showmateapp.util.Resource
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject
import com.example.showmateapp.domain.usecase.GetRecommendationsUseCase

@HiltViewModel
class DetailViewModel @Inject constructor(
    private val showRepository: ShowRepository,
    private val userRepository: UserRepository,
    private val updateUserInterestsUseCase: UpdateUserInterestsUseCase,
    private val getRecommendationsUseCase: GetRecommendationsUseCase
) : ViewModel() {

    private val _media = MutableStateFlow<MediaContent?>(null)
    val media: StateFlow<MediaContent?> = _media

    private val _isLoading = MutableStateFlow(true)
    val isLoading: StateFlow<Boolean> = _isLoading

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage

    private val _isFavorite = MutableStateFlow(false)
    val isFavorite: StateFlow<Boolean> = _isFavorite

    private val _isWatched = MutableStateFlow(false)
    val isWatched: StateFlow<Boolean> = _isWatched

    private val _userRating = MutableStateFlow<Int?>(null)
    val userRating: StateFlow<Int?> = _userRating

    fun loadShowDetails(showId: Int) {
        viewModelScope.launch {
            _isLoading.value = true
            _errorMessage.value = null
            try {
                when (val result = showRepository.getShowDetails(showId)) {
                    is Resource.Success -> {
                        val details = result.data
                        val scoredDetails = getRecommendationsUseCase.scoreShows(listOf(details)).first()
                        _media.value = scoredDetails
                        checkIfFavorite(scoredDetails.id)
                        checkIfWatched(scoredDetails.id)
                        loadUserRating(scoredDetails.id)
                    }
                    is Resource.Error -> {
                        _errorMessage.value = result.message
                    }
                    is Resource.Loading -> {
                        _isLoading.value = true
                    }
                }
            } catch (_: Exception) {
                _errorMessage.value = "Error al cargar los detalles."
            } finally {
                _isLoading.value = false
            }
        }
    }

    private suspend fun checkIfFavorite(showId: Int) {
        try {
            val favorites = userRepository.getFavorites()
            _isFavorite.value = favorites.any { it.id == showId }
        } catch (_: Exception) {
            _isFavorite.value = false
        }
    }

    private suspend fun checkIfWatched(showId: Int) {
        try {
            val watchedList = userRepository.getWatchedShows()
            _isWatched.value = watchedList.any { it.id == showId }
        } catch (_: Exception) {
            _isWatched.value = false
        }
    }

    fun toggleFavorite() {
        val currentShow = _media.value ?: return

        viewModelScope.launch {
            try {
                val isNowFav = userRepository.toggleFavorite(currentShow)
                _isFavorite.value = isNowFav

                val interactionType = if (isNowFav) UserRepository.InteractionType.Like else UserRepository.InteractionType.Dislike
                userRepository.trackMediaInteraction(
                    mediaId = currentShow.id,
                    genres = currentShow.safeGenreIds.map { it.toString() },
                    keywords = currentShow.keywords?.results?.map { it.name } ?: emptyList(),
                    actors = currentShow.credits?.cast?.map { it.id } ?: emptyList(),
                    interactionType = interactionType
                )
            } catch (_: Exception) {
                // Handle error
            }
        }
    }

    fun toggleWatched() {
        val currentShow = _media.value ?: return

        viewModelScope.launch {
            try {
                val isNowWatched = userRepository.toggleWatched(currentShow)
                _isWatched.value = isNowWatched

                if (isNowWatched) {
                    userRepository.trackMediaInteraction(
                        mediaId = currentShow.id,
                        genres = currentShow.safeGenreIds.map { it.toString() },
                        keywords = currentShow.keywords?.results?.map { it.name } ?: emptyList(),
                        actors = currentShow.credits?.cast?.map { it.id } ?: emptyList(),
                        interactionType = UserRepository.InteractionType.Watched
                    )
                }
            } catch (_: Exception) {
                // Handle error
            }
        }
    }

    fun clearRating() {
        val currentShow = _media.value ?: return
        viewModelScope.launch {
            try {
                userRepository.deleteRating(currentShow.id)
                _userRating.value = null
            } catch (_: Exception) {}
        }
    }

    private suspend fun loadUserRating(showId: Int) {
        _userRating.value = userRepository.getUserRating(showId)
    }

    fun rateShow(rating: Int) {
        val currentShow = _media.value ?: return
        viewModelScope.launch {
            userRepository.updateRating(currentShow.id, rating)
            _userRating.value = rating
            
            userRepository.trackMediaInteraction(
                mediaId = currentShow.id,
                genres = currentShow.safeGenreIds.map { it.toString() },
                keywords = currentShow.keywords?.results?.map { it.name } ?: emptyList(),
                actors = currentShow.credits?.cast?.map { it.id } ?: emptyList(),
                interactionType = UserRepository.InteractionType.Rate(rating)
            )
        }
    }
}
