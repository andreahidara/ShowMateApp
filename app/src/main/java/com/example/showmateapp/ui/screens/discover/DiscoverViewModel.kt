package com.example.showmateapp.ui.screens.discover

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.showmateapp.data.network.MediaContent
import com.example.showmateapp.data.repository.UserRepository
import com.example.showmateapp.data.repository.ShowRepository
import com.example.showmateapp.util.Resource
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject
import com.example.showmateapp.domain.usecase.GetRecommendationsUseCase
import com.example.showmateapp.util.GenreMapper

@HiltViewModel
class DiscoverViewModel @Inject constructor(
    private val repository: ShowRepository,
    private val userRepository: UserRepository,
    private val getRecommendationsUseCase: GetRecommendationsUseCase
) : ViewModel() {

    private val _heroShow = MutableStateFlow<MediaContent?>(null)
    val heroShow: StateFlow<MediaContent?> = _heroShow

    private val _topGenreShows = MutableStateFlow<List<MediaContent>>(emptyList())
    val topGenreShows: StateFlow<List<MediaContent>> = _topGenreShows
    
    private val _secondGenreShows = MutableStateFlow<List<MediaContent>>(emptyList())
    val secondGenreShows: StateFlow<List<MediaContent>> = _secondGenreShows
    
    private val _topGenreName = MutableStateFlow("")
    val topGenreName: StateFlow<String> = _topGenreName

    private val _secondGenreName = MutableStateFlow("")
    val secondGenreName: StateFlow<String> = _secondGenreName

    private val _similarShows = MutableStateFlow<List<MediaContent>>(emptyList())
    val similarShows: StateFlow<List<MediaContent>> = _similarShows

    private val _similarToName = MutableStateFlow("")
    val similarToName: StateFlow<String> = _similarToName

    private val _timeTravelShows = MutableStateFlow<List<MediaContent>>(emptyList())
    val timeTravelShows: StateFlow<List<MediaContent>> = _timeTravelShows

    private val _actorShows = MutableStateFlow<List<MediaContent>>(emptyList())
    val actorShows: StateFlow<List<MediaContent>> = _actorShows

    private val _actorName = MutableStateFlow("")
    val actorName: StateFlow<String> = _actorName

    private val _isLoading = MutableStateFlow(true)
    val isLoading: StateFlow<Boolean> = _isLoading

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage

    init {
        loadDiscoverContent()
    }

    fun retry() {
        loadDiscoverContent()
    }

    private fun loadDiscoverContent() {
        viewModelScope.launch {
            _isLoading.value = true
            _errorMessage.value = null
            try {
                val profile = userRepository.getUserProfile()
                val sortedGenres = profile?.genreScores?.entries?.sortedByDescending { it.value } ?: emptyList()
                
                var topGenreId = "18"
                var topName = "Drama"
                var secondGenreId = "35"
                var secondName = "Comedia"

                if (sortedGenres.isNotEmpty()) {
                    topGenreId = sortedGenres[0].key
                    topName = GenreMapper.getGenreName(topGenreId)

                    if (sortedGenres.size > 1) {
                        secondGenreId = sortedGenres[1].key
                        secondName = GenreMapper.getGenreName(secondGenreId)
                    }
                }
                
                _topGenreName.value = topName
                _secondGenreName.value = secondName

                val res1 = repository.getShowsByGenres(topGenreId)
                val res2 = repository.getShowsByGenres(secondGenreId)
                val timeTravelRes = repository.discoverShows(keywords = "4363")

                if (res1 is Resource.Success) {
                    _topGenreShows.value = getRecommendationsUseCase.scoreShows(res1.data.shuffled().take(10))
                }
                if (res2 is Resource.Success) {
                    _secondGenreShows.value = getRecommendationsUseCase.scoreShows(res2.data.shuffled().take(10))
                }
                if (timeTravelRes is Resource.Success) {
                    _timeTravelShows.value = getRecommendationsUseCase.scoreShows(timeTravelRes.data.shuffled().take(10))
                }
                
                val sortedActors = profile?.preferredActors?.entries?.sortedByDescending { it.value } ?: emptyList()
                if (sortedActors.isNotEmpty()) {
                    val actorIdStr = sortedActors.first().key
                    val actorId = actorIdStr.toIntOrNull()
                    if (actorId != null) {
                        val personRes = repository.getPersonDetails(actorId)
                        if (personRes is Resource.Success) {
                            _actorName.value = personRes.data.name
                            val actorShowsRes = repository.discoverShows(withCast = actorIdStr)
                            if (actorShowsRes is Resource.Success) {
                                _actorShows.value = getRecommendationsUseCase.scoreShows(actorShowsRes.data.shuffled().take(10))
                            }
                        }
                    }
                }
                
                val recommendations = getRecommendationsUseCase.execute()
                _heroShow.value = recommendations.firstOrNull() ?: _topGenreShows.value.randomOrNull()

                val likedMedia = profile?.likedMediaIds?.toList() ?: emptyList()
                val topRatedMedia = profile?.ratings?.filterValues { it >= 4f }?.keys?.mapNotNull { it.toIntOrNull() } ?: emptyList()
                val candidates = (likedMedia + topRatedMedia).distinct()
                
                if (candidates.isNotEmpty()) {
                    val targetId = candidates.random()
                    val details = repository.getShowDetails(targetId)
                    if (details is Resource.Success) {
                        _similarToName.value = details.data.name
                        _similarShows.value = getRecommendationsUseCase.scoreShows(
                            repository.getSimilarShows(targetId).shuffled().take(10)
                        )
                    }
                }

                if (_heroShow.value == null && _topGenreShows.value.isEmpty()) {
                    _errorMessage.value = "No se pudo cargar el contenido. Por favor, reintenta."
                }
                
            } catch (e: Exception) {
                Log.e("DiscoverViewModel", "Error loading discover content", e)
                _errorMessage.value = "Error al cargar el contenido: ${e.localizedMessage ?: "Comprueba tu conexión"}"
            } finally {
                _isLoading.value = false
            }
        }
    }
    
}
