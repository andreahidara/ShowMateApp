package com.example.showmateapp.ui.screens.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.showmateapp.data.network.TvShow
import com.example.showmateapp.data.repository.TvShowRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class HomeViewModel : ViewModel() {
    private val repository = TvShowRepository()

    private val _trendingShows = MutableStateFlow<List<TvShow>>(emptyList())
    val trendingShows: StateFlow<List<TvShow>> = _trendingShows

    private val _popularShows = MutableStateFlow<List<TvShow>>(emptyList())
    val popularShows: StateFlow<List<TvShow>> = _popularShows

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage

    private val _isLoading = MutableStateFlow(true)
    val isLoading: StateFlow<Boolean> = _isLoading

    init {
        loadHomeContent()
    }

    fun loadHomeContent() {
        viewModelScope.launch {
            _isLoading.value = true
            _errorMessage.value = null
            try {
                _trendingShows.value = repository.getTrendingTvShows()
                _popularShows.value = repository.getPopularTvShows()
            } catch (e: Exception) {
                _errorMessage.value = "Error de conexión. Comprueba tu internet."
            } finally {
                _isLoading.value = false
            }
        }
    }
}
