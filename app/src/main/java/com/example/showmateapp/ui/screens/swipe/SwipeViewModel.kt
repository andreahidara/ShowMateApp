package com.example.showmateapp.ui.screens.swipe

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.showmateapp.data.network.Movie
import com.example.showmateapp.data.repository.MovieRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class SwipeViewModel : ViewModel() {
    private val repository = MovieRepository()
    
    private val _shows = MutableStateFlow<List<Movie>>(emptyList())
    val shows: StateFlow<List<Movie>> = _shows

    fun removeTopShow() {
        val currentList = _shows.value.toMutableList()
        if (currentList.isNotEmpty()) {
            currentList.removeAt(0)
            _shows.value = currentList
        }
    }

    fun loadShows(genreIds: String) {
        viewModelScope.launch {
            _shows.value = repository.getShowsByGenres(genreIds)
        }
    }
}