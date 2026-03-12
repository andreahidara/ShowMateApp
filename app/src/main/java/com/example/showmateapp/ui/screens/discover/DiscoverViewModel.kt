package com.example.showmateapp.ui.screens.discover

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

    private val _isLoading = MutableStateFlow(true)
    val isLoading: StateFlow<Boolean> = _isLoading

    init {
        loadDiscoverContent()
    }

    private fun loadDiscoverContent() {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val profile = userRepository.getUserProfile()
                val sortedGenres = profile?.genreScores?.entries?.sortedByDescending { it.value } ?: emptyList()
                
                // Default Genres if empty
                var topGenreId = "18" // Drama
                var topName = "Drama"
                var secondGenreId = "35" // Comedy
                var secondName = "Comedia"

                if (sortedGenres.isNotEmpty()) {
                    topGenreId = sortedGenres[0].key
                    topName = getGenreName(topGenreId)
                    
                    if (sortedGenres.size > 1) {
                        secondGenreId = sortedGenres[1].key
                        secondName = getGenreName(secondGenreId)
                    }
                }
                
                _topGenreName.value = topName
                _secondGenreName.value = secondName

                val query1 = repository.getShowsByGenres(topGenreId)
                val query2 = repository.getShowsByGenres(secondGenreId)

                // Sort purely randomly for discovery but apply scores
                _topGenreShows.value = getRecommendationsUseCase.scoreShows(query1.shuffled().take(10))
                _secondGenreShows.value = getRecommendationsUseCase.scoreShows(query2.shuffled().take(10))
                
                // Top Match Recommendation from the algorithm
                // Top Match Recommendation from the algorithm
                val recommendations = getRecommendationsUseCase.execute()
                _heroShow.value = recommendations.firstOrNull() ?: query1.randomOrNull() 

                // Context-Aware "Because you watched..." Section
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
            } catch (e: Exception) {
                // Handled in a later fix
            } finally {
                _isLoading.value = false
            }
        }
    }
    
    private fun getGenreName(id: String): String {
        return when (id) {
            "10759" -> "Acción y Aventura"
            "16" -> "Animación"
            "35" -> "Comedia"
            "80" -> "Crimen"
            "99" -> "Documental"
            "18" -> "Drama"
            "10751" -> "Familiar"
            "10762" -> "Infantil"
            "9648" -> "Misterio"
            "10763" -> "Noticias"
            "10764" -> "Reality"
            "10765" -> "Sci-Fi & Fantasía"
            "10766" -> "Soap"
            "10767" -> "Talk"
            "10768" -> "War & Politics"
            "37" -> "Western"
            else -> "Series Variadas"
        }
    }
}
