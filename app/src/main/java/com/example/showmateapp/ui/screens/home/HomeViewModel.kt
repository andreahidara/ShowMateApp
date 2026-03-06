package com.example.showmateapp.ui.screens.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.showmateapp.data.network.TvShow
import com.example.showmateapp.data.repository.TvShowRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val repository: TvShowRepository
) : ViewModel() {

    private val _trendingShows = MutableStateFlow<List<TvShow>>(emptyList())
    val trendingShows: StateFlow<List<TvShow>> = _trendingShows.asStateFlow()

    private val _popularShows = MutableStateFlow<List<TvShow>>(emptyList())
    val popularShows: StateFlow<List<TvShow>> = _popularShows.asStateFlow()

    private val _isLoading = MutableStateFlow(true)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    init {
        loadData()
    }

    fun loadData() {
        viewModelScope.launch {
            _isLoading.value = true
            _errorMessage.value = null
            try {
                _trendingShows.value = repository.getTrendingTvShows()
                _popularShows.value = repository.getPopularTvShows()
            } catch (e: Exception) {
                _errorMessage.value = "Error al cargar los datos"
            } finally {
                _isLoading.value = false
            }
        }
    }
}
