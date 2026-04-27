package com.andrea.showmateapp.ui.screens.search

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import androidx.paging.cachedIn
import com.andrea.showmateapp.R
import com.andrea.showmateapp.data.model.*
import com.andrea.showmateapp.data.network.TmdbApiService
import com.andrea.showmateapp.data.network.TmdbSearchPagingSource
import com.andrea.showmateapp.domain.repository.IShowRepository
import com.andrea.showmateapp.domain.repository.IUserRepository
import com.andrea.showmateapp.domain.usecase.GetRecommendationsUseCase
import com.andrea.showmateapp.util.Resource
import com.andrea.showmateapp.util.UiText
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

enum class SearchMode(val label: String) {
    TITLE("Título"),
    ACTOR("Actor"),
    CREATOR("Creador/Director")
}

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class SearchViewModel @Inject constructor(
    private val showRepository: IShowRepository,
    private val userRepository: IUserRepository,
    private val tmdbApiService: TmdbApiService,
    private val getRecommendationsUseCase: GetRecommendationsUseCase,
    private val dataStore: DataStore<Preferences>
) : ViewModel() {

    companion object {
        const val MIN_YEAR = 1990
        private val KEY_RECENT = stringPreferencesKey("recent_searches")
        private const val MAX_RECENT = 6
        val CURRENT_YEAR: Int = java.time.Year.now().value
        val AVAILABLE_GENRES = listOf(
            "10759" to "Acción", "16" to "Animación", "35" to "Comedia",
            "80" to "Crimen", "99" to "Docu", "18" to "Drama",
            "10751" to "Familia", "10762" to "Kids", "9648" to "Misterio",
            "10765" to "Sci-Fi"
        )
        val AVAILABLE_PLATFORMS = listOf(
            "8" to "Netflix",
            "119" to "Prime",
            "337" to "Disney+",
            "1899" to "Max",
            "350" to "Apple TV+",
            "531" to "Paramount+"
        )
    }

    private val _searchResults = MutableStateFlow<List<MediaContent>>(emptyList())
    val searchResults: StateFlow<List<MediaContent>> = _searchResults.asStateFlow()

    private val _personSearchResults = MutableStateFlow<List<PersonSearchResult>>(emptyList())
    val personSearchResults: StateFlow<List<PersonSearchResult>> = _personSearchResults.asStateFlow()

    @Suppress("ktlint:standard:property-naming")
    private val _pagingQuery = MutableStateFlow("")
    val searchPagingData: Flow<PagingData<MediaContent>> = _pagingQuery
        .flatMapLatest { q ->
            if (q.isBlank()) {
                kotlinx.coroutines.flow.flowOf(PagingData.empty())
            } else {
                Pager(
                    config = PagingConfig(pageSize = 20, enablePlaceholders = false),
                    pagingSourceFactory = {
                        TmdbSearchPagingSource(tmdbApiService, q, getRecommendationsUseCase)
                    }
                ).flow
            }
        }
        .cachedIn(viewModelScope)

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _errorMessage = MutableStateFlow<UiText?>(null)
    val errorMessage: StateFlow<UiText?> = _errorMessage.asStateFlow()

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

    private val _selectedPlatform = MutableStateFlow<String?>(null)
    val selectedPlatform: StateFlow<String?> = _selectedPlatform.asStateFlow()

    private val _isFilterActive = MutableStateFlow(false)
    val isFilterActive: StateFlow<Boolean> = _isFilterActive.asStateFlow()

    private val _recentSearches = MutableStateFlow<List<String>>(emptyList())
    val recentSearches: StateFlow<List<String>> = _recentSearches.asStateFlow()

    private val _suggestions = MutableStateFlow<List<MediaContent>>(emptyList())
    val suggestions: StateFlow<List<MediaContent>> = _suggestions.asStateFlow()

    private val _searchMode = MutableStateFlow(SearchMode.TITLE)
    val searchMode: StateFlow<SearchMode> = _searchMode.asStateFlow()

    private val _trendingPeople = MutableStateFlow<List<PersonSearchResult>>(emptyList())
    val trendingPeople: StateFlow<List<PersonSearchResult>> = _trendingPeople.asStateFlow()

    private var searchJob: Job? = null

    init {
        loadTrendingShows()
        viewModelScope.launch {
            try {
                val raw = dataStore.data.map { it[KEY_RECENT] ?: "" }.first()
                _recentSearches.value = if (raw.isBlank()) emptyList() else raw.split("|||").filter { it.isNotBlank() }
            } catch (e: Exception) {
                if (e is CancellationException) throw e
            }
        }

        viewModelScope.launch {
            userRepository.getUserProfileFlow().collect { profile ->
                if (profile != null && !_isLoading.value) {
                    rescoreExistingContent()
                }
            }
        }
    }

    private fun rescoreExistingContent() {
        viewModelScope.launch {
            val trendingScored = getRecommendationsUseCase.scoreShows(_trendingShows.value)
            val suggestionsScored = getRecommendationsUseCase.scoreShows(_suggestions.value)
            val resultsScored = getRecommendationsUseCase.scoreShows(_searchResults.value)
            
            _trendingShows.value = trendingScored
            _suggestions.value = suggestionsScored
            _searchResults.value = resultsScored
        }
    }

    private fun saveRecentQuery(query: String) {
        val trimmed = query.trim()
        if (trimmed.isBlank()) return
        val updated = (listOf(trimmed) + _recentSearches.value.filter { it != trimmed }).take(MAX_RECENT)
        _recentSearches.value = updated
        viewModelScope.launch {
            dataStore.edit { it[KEY_RECENT] = updated.joinToString("|||") }
        }
    }

    fun removeRecentSearch(query: String) {
        val updated = _recentSearches.value.filter { it != query }
        _recentSearches.value = updated
        viewModelScope.launch {
            dataStore.edit { it[KEY_RECENT] = updated.joinToString("|||") }
        }
    }

    fun clearRecentSearches() {
        _recentSearches.value = emptyList()
        viewModelScope.launch {
            dataStore.edit { it.remove(KEY_RECENT) }
        }
    }

    fun setSearchMode(mode: SearchMode) {
        val prevMode = _searchMode.value
        _searchMode.value = mode

        if (prevMode != mode) {
            _searchResults.value = emptyList()
            _personSearchResults.value = emptyList()
            _pagingQuery.value = ""
            _errorMessage.value = null

            if (mode == SearchMode.ACTOR || mode == SearchMode.CREATOR) {
                loadTrendingPeople(mode)
            }
        }
    }

    private fun filterPeopleByMode(people: List<PersonSearchResult>, mode: SearchMode) = when (mode) {
        SearchMode.ACTOR -> people.filter { it.knownForDepartment?.lowercase() == "acting" }
        SearchMode.CREATOR -> people.filter {
            val dept = it.knownForDepartment?.lowercase() ?: ""
            dept == "directing" || dept == "writing" || dept == "production" || dept == "creator"
        }
        else -> people
    }

    private fun loadTrendingPeople(mode: SearchMode) {
        viewModelScope.launch {
            try {
                val response = tmdbApiService.getTrendingPeople()
                _trendingPeople.value = filterPeopleByMode(response.results, mode).take(20)
            } catch (e: Exception) {
                if (e is CancellationException) throw e
            }
        }
    }

    private fun loadTrendingShows() {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val popularRes = showRepository.getPopularShows()
                if (popularRes is Resource.Success) {
                    _trendingShows.value = getRecommendationsUseCase.scoreShows(popularRes.data)
                }
            } catch (e: Exception) {
                if (e is CancellationException) throw e
                _errorMessage.value = UiText.StringResource(R.string.error_unexpected_data)
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
        val trimmedQuery = query.trim()

        if (trimmedQuery.isEmpty() && !_isFilterActive.value) {
            _searchResults.value = emptyList()
            _suggestions.value = emptyList()
            _errorMessage.value = null
            return
        }

        if (trimmedQuery.isNotEmpty() && trimmedQuery.length < 2) {
            _errorMessage.value = UiText.StringResource(R.string.error_search_too_short)
            _searchResults.value = emptyList()
            _personSearchResults.value = emptyList()
            _suggestions.value = emptyList()
            return
        }

        searchJob = viewModelScope.launch {
            if (query.isNotBlank()) delay(500)

            _errorMessage.value = null

            if (query.isNotBlank() && _searchMode.value == SearchMode.TITLE) {
                _suggestions.value = emptyList()
                _searchResults.value = emptyList()
                _pagingQuery.value = query.trim()
                saveRecentQuery(query)
                return@launch
            }

            _isLoading.value = true
            try {
                if (query.isNotBlank()) {
                    val result = showRepository.searchPerson(query)
                    if (result is Resource.Success) {
                        _personSearchResults.value = filterPeopleByMode(result.data, _searchMode.value)
                        saveRecentQuery(query)
                    } else if (result is Resource.Error) {
                        _errorMessage.value = result.message?.let { UiText.DynamicString(it) } ?: UiText.StringResource(R.string.error_unknown)
                    }
                } else if (_isFilterActive.value) {
                    applyFilters()
                }
            } catch (e: Exception) {
                if (e is CancellationException) throw e
                _errorMessage.value = UiText.DynamicString("Error: ${e.message}")
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

    fun updatePlatform(platformId: String?) {
        _selectedPlatform.value = platformId
        checkFilterStatus()
        searchMedia("")
    }

    fun clearFilters() {
        _selectedGenre.value = null
        _selectedPlatform.value = null
        _yearFrom.value = MIN_YEAR
        _yearTo.value = CURRENT_YEAR
        _selectedRating.value = null
        _isFilterActive.value = false
        _searchResults.value = emptyList()
        _pagingQuery.value = ""
    }

    private fun checkFilterStatus() {
        _isFilterActive.value = _selectedGenre.value != null ||
            _selectedPlatform.value != null ||
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
            firstAirDateLte = firstAirDateLte,
            providers = _selectedPlatform.value,
            watchRegion = if (_selectedPlatform.value != null) "ES" else null
        )

        when (result) {
            is Resource.Success -> {
                _searchResults.value = getRecommendationsUseCase.scoreShows(result.data)
            }
            is Resource.Error -> {
                _errorMessage.value = result.message?.let { UiText.DynamicString(it) }
                    ?: UiText.StringResource(R.string.error_unexpected_data)
                _searchResults.value = emptyList()
            }
            else -> {}
        }
    }
}

