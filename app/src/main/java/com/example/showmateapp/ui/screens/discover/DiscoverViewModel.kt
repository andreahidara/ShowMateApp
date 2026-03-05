package com.example.showmateapp.ui.screens.discover

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.showmateapp.data.network.TvShow
import com.example.showmateapp.data.repository.TvShowRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class DiscoverViewModel : ViewModel() {
    private val repository = TvShowRepository()

    private val _heroShow = MutableStateFlow<TvShow?>(null)
    val heroShow: StateFlow<TvShow?> = _heroShow

    private val _euphoriaRecommendations = MutableStateFlow<List<TvShow>>(emptyList())
    val euphoriaRecommendations: StateFlow<List<TvShow>> = _euphoriaRecommendations

    private val _hiddenGems = MutableStateFlow<List<TvShow>>(emptyList())
    val hiddenGems: StateFlow<List<TvShow>> = _hiddenGems

    private val _isLoading = MutableStateFlow(true)
    val isLoading: StateFlow<Boolean> = _isLoading

    init {
        loadDiscoverContent()
    }

    private fun loadDiscoverContent() {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val dramas = repository.getTvShowsByGenres("18")
                val mysteries = repository.getTvShowsByGenres("9648")
                val popular = repository.getPopularTvShows()

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
