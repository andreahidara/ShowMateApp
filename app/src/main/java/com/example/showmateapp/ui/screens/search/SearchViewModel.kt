package com.example.showmateapp.ui.screens.search

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.showmateapp.data.network.MediaContent
import com.example.showmateapp.data.repository.ShowRepository
import com.example.showmateapp.domain.usecase.GetRecommendationsUseCase
import com.example.showmateapp.util.Resource
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

enum class SearchMode(val label: String) {
    TITLE("Título"),
    ACTOR("Actor"),
    CREATOR("Creador/Director")
}

@HiltViewModel
class SearchViewModel @Inject constructor(
    private val showRepository: ShowRepository,
    private val getRecommendationsUseCase: GetRecommendationsUseCase,
    @ApplicationContext private val context: Context
) : ViewModel() {

    companion object {
        const val MIN_YEAR = 1990
        private const val PREFS_NAME = "search_prefs"
        private const val KEY_RECENT = "recent_searches"
        private const val MAX_RECENT = 6
        val CURRENT_YEAR: Int = java.time.Year.now().value
        val AVAILABLE_GENRES = listOf(
            "10759" to "Acción", "16" to "Animación", "35" to "Comedia",
            "80" to "Crimen", "99" to "Docu", "18" to "Drama",
            "10751" to "Familia", "10762" to "Kids", "9648" to "Misterio",
            "10765" to "Sci-Fi"
        )
    }

    private val _searchResults = MutableStateFlow<List<MediaContent>>(emptyList())
    val searchResults: StateFlow<List<MediaContent>> = _searchResults.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    private val _trendingShows = MutableStateFlow<List<MediaContent>>(emptyList())
    val trendingShows: StateFlow<List<MediaContent>> = _trendingShows.asStateFlow()

    private val _selectedGenre = MutableStateFlow<String?>(null)
    val selectedGenre: StateFlow<String?> = _selectedGenre.asStateFlow()

    private val _yearFrom = MutableStateFlow(MIN_YEAR)
    val yearFrom: StateFlow<Int> = _yearFrom.asStateFlow()

    private val _yearTo = MutableStateFlow(CURRENT_YEAR)
    val yearTo: StateFlow<Int> = _yearTo.asStateFlow()

    private val _selectedRating = MutableStateFlow<Float?>(null)
    val selectedRating: StateFlow<Float?> = _selectedRating.asStateFlow()

    private val _isFilterActive = MutableStateFlow(false)
    val isFilterActive: StateFlow<Boolean> = _isFilterActive.asStateFlow()

    private val _recentSearches = MutableStateFlow<List<String>>(emptyList())
    val recentSearches: StateFlow<List<String>> = _recentSearches.asStateFlow()

    private val _suggestions = MutableStateFlow<List<MediaContent>>(emptyList())
    val suggestions: StateFlow<List<MediaContent>> = _suggestions.asStateFlow()

    private val _searchMode = MutableStateFlow(SearchMode.TITLE)
    val searchMode: StateFlow<SearchMode> = _searchMode.asStateFlow()

    private val prefs by lazy { context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE) }

    private var searchJob: Job? = null

    init {
        loadTrendingShows()
        _recentSearches.value = loadRecentSearches()
    }

    private fun loadRecentSearches(): List<String> {
        val raw = prefs.getString(KEY_RECENT, "") ?: ""
        return if (raw.isBlank()) emptyList() else raw.split("|||").filter { it.isNotBlank() }
    }

    private fun saveRecentQuery(query: String) {
        val trimmed = query.trim()
        if (trimmed.isBlank()) return
        val updated = (listOf(trimmed) + _recentSearches.value.filter { it != trimmed }).take(MAX_RECENT)
        _recentSearches.value = updated
        prefs.edit().putString(KEY_RECENT, updated.joinToString("|||")).apply()
    }

    fun removeRecentSearch(query: String) {
        val updated = _recentSearches.value.filter { it != query }
        _recentSearches.value = updated
        prefs.edit().putString(KEY_RECENT, updated.joinToString("|||")).apply()
    }

    fun clearRecentSearches() {
        _recentSearches.value = emptyList()
        prefs.edit().remove(KEY_RECENT).apply()
    }

    fun setSearchMode(mode: SearchMode) {
        _searchMode.value = mode
    }

    private fun loadTrendingShows() {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val popularRes = showRepository.getPopularShows()
                if (popularRes is Resource.Success) {
                    _trendingShows.value = getRecommendationsUseCase.scoreShows(popularRes.data)
                }
            } catch (_: Exception) {
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun updateSuggestions(query: String) {
        if (query.length < 2) {
            _suggestions.value = emptyList()
            return
        }
        val q = query.trim().lowercase()
        _suggestions.value = _trendingShows.value
            .filter { it.name.lowercase().contains(q) }
            .take(3)
    }

    fun searchMedia(query: String) {
        searchJob?.cancel()

        if (query.isBlank() && !_isFilterActive.value) {
            _searchResults.value = emptyList()
            _suggestions.value = emptyList()
            _errorMessage.value = null
            return
        }

        searchJob = viewModelScope.launch {
            // Debounce sólo para búsquedas por texto; los filtros sin texto no necesitan esperar
            if (query.isNotBlank()) delay(500)

            _isLoading.value = true
            _errorMessage.value = null
            try {
                if (query.isNotBlank()) {
                    val result = showRepository.searchShows(query)
                    when (result) {
                        is Resource.Success -> {
                            _suggestions.value = emptyList()
                            _searchResults.value = getRecommendationsUseCase.scoreShows(result.data)
                            saveRecentQuery(query)
                            if (_searchResults.value.isEmpty()) {
                                _errorMessage.value = "No se encontraron resultados para '$query'"
                            }
                        }
                        is Resource.Error -> {
                            _errorMessage.value = result.message
                            _searchResults.value = emptyList()
                        }
                        else -> {}
                    }
                } else if (_isFilterActive.value) {
                    applyFilters()
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
        searchMedia("")
    }

    fun updateYearRange(from: Int, to: Int) {
        _yearFrom.value = from
        _yearTo.value = to
        checkFilterStatus()
        searchMedia("")
    }

    fun updateRating(rating: Float?) {
        _selectedRating.value = rating
        checkFilterStatus()
        searchMedia("")
    }

    fun clearFilters() {
        _selectedGenre.value = null
        _yearFrom.value = MIN_YEAR
        _yearTo.value = CURRENT_YEAR
        _selectedRating.value = null
        _isFilterActive.value = false
        _searchResults.value = emptyList()
    }

    private fun checkFilterStatus() {
        _isFilterActive.value = _selectedGenre.value != null ||
                              _yearFrom.value > MIN_YEAR ||
                              _yearTo.value < CURRENT_YEAR ||
                              _selectedRating.value != null
    }

    private suspend fun applyFilters() {
        val firstAirDateGte = if (_yearFrom.value > MIN_YEAR) "${_yearFrom.value}-01-01" else null
        val firstAirDateLte = if (_yearTo.value < CURRENT_YEAR) "${_yearTo.value}-12-31" else null
        val result = showRepository.discoverShows(
            genreId = _selectedGenre.value,
            minRating = _selectedRating.value,
            firstAirDateGte = firstAirDateGte,
            firstAirDateLte = firstAirDateLte
        )
        
        when (result) {
            is Resource.Success -> {
                _searchResults.value = getRecommendationsUseCase.scoreShows(result.data)
            }
            is Resource.Error -> {
                _errorMessage.value = result.message
                _searchResults.value = emptyList()
            }
            else -> {}
        }
    }
}
