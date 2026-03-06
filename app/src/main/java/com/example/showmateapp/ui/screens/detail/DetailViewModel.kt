package com.example.showmateapp.ui.screens.detail

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.showmateapp.data.network.TvShow
import com.example.showmateapp.data.repository.TvShowRepository
import com.example.showmateapp.data.repository.FirestoreRepository
import com.example.showmateapp.domain.usecase.UpdateUserInterestsUseCase
import com.google.firebase.auth.FirebaseAuth
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class DetailViewModel @Inject constructor(
    private val tvShowRepository: TvShowRepository,
    private val firestoreRepository: FirestoreRepository,
    private val updateUserInterestsUseCase: UpdateUserInterestsUseCase,
    private val auth: FirebaseAuth
) : ViewModel() {

    private val _tvShow = MutableStateFlow<TvShow?>(null)
    val tvShow: StateFlow<TvShow?> = _tvShow

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
                val details = tvShowRepository.getTvShowDetails(showId)
                _tvShow.value = details
                checkIfFavorite(details.id)
            } catch (e: Exception) {
                _errorMessage.value = "Error al cargar los detalles."
            } finally {
                _isLoading.value = false
            }
        }
    }

    private suspend fun checkIfFavorite(showId: Int) {
        try {
            val favorites = firestoreRepository.getFavorites()
            _isFavorite.value = favorites.any { it.id == showId }
        } catch (e: Exception) {
            _isFavorite.value = false
        }
    }

    fun toggleFavorite() {
        val currentShow = _tvShow.value ?: return

        viewModelScope.launch {
            try {
                val isNowFav = firestoreRepository.toggleFavorite(currentShow)
                _isFavorite.value = isNowFav

                if (isNowFav) {
                    updateUserInterestsUseCase.execute(currentShow, true)
                }
            } catch (e: Exception) {
                // Handle error
            }
        }
    }
}
