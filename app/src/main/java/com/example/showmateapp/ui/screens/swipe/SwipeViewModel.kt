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

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage

    private val _isLoading = MutableStateFlow(true)
    val isLoading: StateFlow<Boolean> = _isLoading

    fun removeTopShow() {
        val currentList = _shows.value.toMutableList()
        if (currentList.isNotEmpty()) {
            currentList.removeAt(0)
            _shows.value = currentList
        }
    }

    fun loadShows(genreIds: String) {
        viewModelScope.launch {
            _isLoading.value = true
            _errorMessage.value = null
            try {
                _shows.value = repository.getShowsByGenres(genreIds).shuffled()
            } catch (e: Exception) {
                _errorMessage.value = "Hubo un error cargando las series. Inténtalo de nuevo."
            } finally {
                _isLoading.value = false
            }
        }
    }
}