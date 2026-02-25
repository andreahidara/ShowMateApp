package com.example.showmateapp.ui.screens.detail

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.showmateapp.data.network.Movie
import com.example.showmateapp.data.repository.MovieRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class DetailViewModel : ViewModel() {
    private val repository = MovieRepository()

    private val _show = MutableStateFlow<Movie?>(null)
    val show: StateFlow<Movie?> = _show

    fun loadShowDetails(showId: Int) {
        viewModelScope.launch {
            // Since TMDB discover returns Movie objects, we'll try to find it 
            // from some source or fetch if there was a separate endpoint.
            // For now, we'll assume we pass enough data or use a simplified fetch.
            // Note: TMDB has a specific GET /tv/{tv_id} endpoint.
            // To keep it simple, we'll just use what we have or add the endpoint if needed.
        }
    }
}
