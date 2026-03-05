package com.example.showmateapp.ui.screens.search

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.showmateapp.data.network.TvShow
import com.example.showmateapp.data.repository.TvShowRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class SearchViewModel : ViewModel() {
    private val repository = TvShowRepository()

    private val _searchResults = MutableStateFlow<List<TvShow>>(emptyList())
    val searchResults: StateFlow<List<TvShow>> = _searchResults

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage

    fun searchShows(query: String) {
        if (query.isBlank()) return

        viewModelScope.launch {
            _isLoading.value = true
            _errorMessage.value = null
            try {
                _searchResults.value = repository.searchTvShows(query)
                if (_searchResults.value.isEmpty()) {
                    _errorMessage.value = "No se encontraron resultados para '$query'"
                }
            } catch (e: Exception) {
                _errorMessage.value = "Error al buscar: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }
}
