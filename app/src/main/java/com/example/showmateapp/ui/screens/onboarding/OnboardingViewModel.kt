package com.example.showmateapp.ui.screens.onboarding

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.showmateapp.data.repository.UserRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class OnboardingViewModel @Inject constructor(
    private val userRepository: UserRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(OnboardingUiState())
    val uiState = _uiState.asStateFlow()

    // genreId corresponds to the TMDB Genre ID
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

            // Usamos la nueva función para dar peso fuerte inicial a los géneros elegidos
            userRepository.saveOnboardingInterests(
                genres = _uiState.value.selectedGenres.toList()
            )

            _uiState.value = _uiState.value.copy(isLoading = false, isComplete = true)
        }
    }
}
