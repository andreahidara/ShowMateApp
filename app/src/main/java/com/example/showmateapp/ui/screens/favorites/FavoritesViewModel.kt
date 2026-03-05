package com.example.showmateapp.ui.screens.favorites

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.showmateapp.data.network.TvShow
import com.example.showmateapp.data.repository.FirestoreRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class FavoritesViewModel : ViewModel() {
    private val repository = FirestoreRepository()

    private val _favorites = MutableStateFlow<List<TvShow>>(emptyList())
    val favorites: StateFlow<List<TvShow>> = _favorites

    private val _isLoading = MutableStateFlow(true)
    val isLoading: StateFlow<Boolean> = _isLoading

    init {
        loadFavorites()
    }

    fun loadFavorites() {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                _favorites.value = repository.getFavorites()
            } catch (e: Exception) {
                // Ignore for now
            } finally {
                _isLoading.value = false
            }
        }
    }
}
