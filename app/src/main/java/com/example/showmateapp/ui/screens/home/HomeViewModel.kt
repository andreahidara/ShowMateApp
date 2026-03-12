package com.example.showmateapp.ui.screens.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.showmateapp.data.network.MediaContent
import com.example.showmateapp.data.repository.ShowRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject
import com.example.showmateapp.domain.usecase.GetRecommendationsUseCase

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val repository: ShowRepository,
    private val getRecommendationsUseCase: GetRecommendationsUseCase
) : ViewModel() {

    private val _trendingShows = MutableStateFlow<List<MediaContent>>(emptyList())
    val trendingShows: StateFlow<List<MediaContent>> = _trendingShows.asStateFlow()

    private val _popularShows = MutableStateFlow<List<MediaContent>>(emptyList())
    val popularShows: StateFlow<List<MediaContent>> = _popularShows.asStateFlow()

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
                val trending = repository.getTrendingShows()
                val popular = repository.getPopularShows()
                _trendingShows.value = getRecommendationsUseCase.scoreShows(trending)
                _popularShows.value = getRecommendationsUseCase.scoreShows(popular)
            } catch (e: Exception) {
                _errorMessage.value = "Error al cargar los datos"
            } finally {
                _isLoading.value = false
            }
        }
    }
}
