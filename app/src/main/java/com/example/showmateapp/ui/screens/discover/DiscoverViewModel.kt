package com.example.showmateapp.ui.screens.discover

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.showmateapp.data.network.Movie
import com.example.showmateapp.data.repository.MovieRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class DiscoverViewModel : ViewModel() {
    private val repository = MovieRepository()

    private val _heroShow = MutableStateFlow<Movie?>(null)
    val heroShow: StateFlow<Movie?> = _heroShow

    private val _euphoriaRecommendations = MutableStateFlow<List<Movie>>(emptyList())
    val euphoriaRecommendations: StateFlow<List<Movie>> = _euphoriaRecommendations

    private val _hiddenGems = MutableStateFlow<List<Movie>>(emptyList())
    val hiddenGems: StateFlow<List<Movie>> = _hiddenGems

    private val _isLoading = MutableStateFlow(true)
    val isLoading: StateFlow<Boolean> = _isLoading

    init {
        loadDiscoverContent()
    }

    private fun loadDiscoverContent() {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                // To avoid making too many specific endpoints for this example,
                // we'll use combinations of existing genres (18 = Drama, 9648 = Mystery)
                val dramas = repository.getShowsByGenres("18")
                val mysteries = repository.getShowsByGenres("9648")
                val popular = repository.getPopularShows()

                _euphoriaRecommendations.value = dramas.shuffled().take(10)
                _hiddenGems.value = mysteries.shuffled().take(10)
                _heroShow.value = popular.randomOrNull()
            } catch (e: Exception) {
                // Handled in a later fix
            } finally {
                _isLoading.value = false
            }
        }
    }
}
