package com.example.showmateapp.ui.screens.search

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.showmateapp.data.network.MediaContent
import com.example.showmateapp.data.repository.ShowRepository
import com.example.showmateapp.util.Resource
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SearchViewModel @Inject constructor(
    private val showRepository: ShowRepository
) : ViewModel() {

    private val _searchResults = MutableStateFlow<List<MediaContent>>(emptyList())
    val searchResults: StateFlow<List<MediaContent>> = _searchResults

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage

    private val _trendingShows = MutableStateFlow<List<MediaContent>>(emptyList())
    val trendingShows: StateFlow<List<MediaContent>> = _trendingShows

    private val _selectedGenre = MutableStateFlow<String?>(null)
    val selectedGenre: StateFlow<String?> = _selectedGenre

    private val _selectedYear = MutableStateFlow<Int?>(null)
    val selectedYear: StateFlow<Int?> = _selectedYear

    private val _selectedRating = MutableStateFlow<Float?>(null)
    val selectedRating: StateFlow<Float?> = _selectedRating

    private val _isFilterActive = MutableStateFlow(false)
    val isFilterActive: StateFlow<Boolean> = _isFilterActive

    private var searchJob: Job? = null

    init {
        loadTrendingShows()
    }

    private fun loadTrendingShows() {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val popular = showRepository.getPopularShows()
                _trendingShows.value = popular
            } catch (_: Exception) {
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun searchMedia(query: String) {
        searchJob?.cancel()

        if (query.isBlank() && !_isFilterActive.value) {
            _searchResults.value = emptyList()
            _errorMessage.value = null
            return
        }

        searchJob = viewModelScope.launch {
            delay(500)

            _isLoading.value = true
            _errorMessage.value = null
            try {
                if (query.isNotBlank()) {
                    _searchResults.value = showRepository.searchShows(query)
                } else if (_isFilterActive.value) {
                    applyFilters()
                }

                if (_searchResults.value.isEmpty() && _isLoading.value) {
                    _errorMessage.value = if (query.isNotBlank()) {
                        "No se encontraron resultados para '$query'"
                    } else {
                        "No hay series que coincidan con los filtros"
                    }
                }
            } catch (e: Exception) {
                _errorMessage.value = "Error: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun updateGenre(genreId: String?) {
        _selectedGenre.value = genreId
        checkFilterStatus()
        applyFiltersWithDebounce()
    }

    fun updateYear(year: Int?) {
        _selectedYear.value = year
        checkFilterStatus()
        applyFiltersWithDebounce()
    }

    fun updateRating(rating: Float?) {
        _selectedRating.value = rating
        checkFilterStatus()
        applyFiltersWithDebounce()
    }

    fun clearFilters() {
        _selectedGenre.value = null
        _selectedYear.value = null
        _selectedRating.value = null
        _isFilterActive.value = false
        _searchResults.value = emptyList()
    }

    private fun checkFilterStatus() {
        _isFilterActive.value = _selectedGenre.value != null || 
                              _selectedYear.value != null || 
                              _selectedRating.value != null
    }

    private fun applyFiltersWithDebounce() {
        searchMedia("")
    }

    private suspend fun applyFilters() {
        val result = showRepository.discoverShows(
            genreId = _selectedGenre.value,
            year = _selectedYear.value,
            minRating = _selectedRating.value
        )
        
        when (result) {
            is Resource.Success -> {
                _searchResults.value = result.data
            }
            is Resource.Error -> {
                _errorMessage.value = result.message
                _searchResults.value = emptyList()
            }
            else -> {}
        }
    }
}
