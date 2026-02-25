package com.example.showmateapp.ui.screens.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.showmateapp.data.network.Movie
import com.example.showmateapp.data.repository.MovieRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class HomeViewModel : ViewModel() {
    private val repository = MovieRepository()

    private val _trendingShows = MutableStateFlow<List<Movie>>(emptyList())
    val trendingShows: StateFlow<List<Movie>> = _trendingShows

    private val _popularShows = MutableStateFlow<List<Movie>>(emptyList())
    val popularShows: StateFlow<List<Movie>> = _popularShows

    init {
        loadHomeContent()
    }

    private fun loadHomeContent() {
        viewModelScope.launch {
            _trendingShows.value = repository.getTrendingShows()
            _popularShows.value = repository.getPopularShows()
        }
    }
}
