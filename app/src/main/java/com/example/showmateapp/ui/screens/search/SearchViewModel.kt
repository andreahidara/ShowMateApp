package com.example.showmateapp.ui.screens.search

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.showmateapp.BuildConfig
import com.example.showmateapp.data.network.Movie
import com.example.showmateapp.data.network.RetrofitClient
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class SearchViewModel : ViewModel() {
    private val apiService = RetrofitClient.apiService
    private val token = BuildConfig.TMDB_API_TOKEN

    private val _searchResults = MutableStateFlow<List<Movie>>(emptyList())
    val searchResults: StateFlow<List<Movie>> = _searchResults

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage

    fun searchShows(query: String) {
        if (query.isBlank()) {
            _searchResults.value = emptyList()
            return
        }

        viewModelScope.launch {
            _isLoading.value = true
            _errorMessage.value = null
            try {
                val response = apiService.searchShows(token, query)
                _searchResults.value = response.results
            } catch (e: Exception) {
                _errorMessage.value = "Failed to search: ${e.localizedMessage}"
                _searchResults.value = emptyList()
            } finally {
                _isLoading.value = false
            }
        }
    }
}
