package com.example.showmateapp.ui.screens.onboarding

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.showmateapp.data.repository.ShowRepository
import com.example.showmateapp.data.repository.UserRepository
import com.example.showmateapp.util.Resource
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class OnboardingViewModel @Inject constructor(
    private val userRepository: UserRepository,
    private val showRepository: ShowRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(OnboardingUiState())
    val uiState = _uiState.asStateFlow()

    init {
        loadGenrePosters()
    }

    private fun loadGenrePosters() {
        viewModelScope.launch {
            val genres = _uiState.value.availableGenres.keys.toList()
            val deferred = genres.map { genreId ->
                async {
                    val result = showRepository.discoverShows(
                        genreId = genreId,
                        sortBy = "popularity.desc"
                    )
                    val posterPath = when (result) {
                        is Resource.Success -> result.data.firstOrNull()?.posterPath
                        else -> null
                    }
                    genreId to posterPath
                }
            }
            val posters = deferred.awaitAll().toMap()
            _uiState.value = _uiState.value.copy(genrePosters = posters)
        }
    }

    fun toggleGenre(genreId: String) {
        val currentSelected = _uiState.value.selectedGenres
        val newSelected = if (currentSelected.contains(genreId)) {
            currentSelected - genreId
        } else {
            currentSelected + genreId
        }
        _uiState.value = _uiState.value.copy(selectedGenres = newSelected)
    }

    fun saveInterests() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            userRepository.saveOnboardingInterests(
                genres = _uiState.value.selectedGenres.toList()
            )
            _uiState.value = _uiState.value.copy(isLoading = false, isComplete = true)
        }
    }
}
