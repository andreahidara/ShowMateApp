package com.example.showmateapp.ui.screens.detail

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.showmateapp.data.network.Movie
import com.example.showmateapp.data.network.RetrofitClient
import com.example.showmateapp.data.repository.FirestoreRepository
import com.example.showmateapp.BuildConfig
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class DetailViewModel : ViewModel() {
    private val apiService = RetrofitClient.apiService
    private val token = BuildConfig.TMDB_API_TOKEN
    private val firestoreRepository = FirestoreRepository()

    private val _movie = MutableStateFlow<Movie?>(null)
    val movie: StateFlow<Movie?> = _movie

    private val _isLoading = MutableStateFlow(true)
    val isLoading: StateFlow<Boolean> = _isLoading

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage

    private val _isFavorite = MutableStateFlow(false)
    val isFavorite: StateFlow<Boolean> = _isFavorite

    fun loadShowDetails(showId: Int) {
        viewModelScope.launch {
            _isLoading.value = true
            _errorMessage.value = null
            try {
                val response = apiService.getTvShowDetails(token, showId)
                _movie.value = response
                checkIfFavorite(response.id)
            } catch (e: Exception) {
                _errorMessage.value = "Error al cargar los detalles. Inténtalo de nuevo."
            } finally {
                _isLoading.value = false
            }
        }
    }

    private suspend fun checkIfFavorite(showId: Int) {
        val favorites = firestoreRepository.getFavorites()
        _isFavorite.value = favorites.any { it.id == showId }
    }

    fun toggleFavorite() {
        val currentMovie = _movie.value ?: return
        viewModelScope.launch {
            val isNowFav = firestoreRepository.toggleFavorite(currentMovie)
            _isFavorite.value = isNowFav
        }
    }
}
