package com.example.showmateapp.ui.screens.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.showmateapp.data.network.MediaContent
import com.example.showmateapp.data.repository.ShowRepository
import com.example.showmateapp.data.repository.UserRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import javax.inject.Inject
import com.example.showmateapp.domain.usecase.GetRecommendationsUseCase
import com.example.showmateapp.util.Resource
import com.example.showmateapp.util.UiText
import com.example.showmateapp.R
import android.util.Log

data class HomeUiState(
    val isLoading: Boolean = false,
    val isRefreshing: Boolean = false,
    val upNextShows: List<MediaContent> = emptyList(),
    val trendingShows: List<MediaContent> = emptyList(),
    val actionShows: List<MediaContent> = emptyList(),
    val comedyShows: List<MediaContent> = emptyList(),
    val popularInSpain: List<MediaContent> = emptyList(),
    val mysteryShows: List<MediaContent> = emptyList(),
    val errorMessage: UiText? = null,
    val whatToWatchToday: MediaContent? = null
)

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val repository: ShowRepository,
    private val getRecommendationsUseCase: GetRecommendationsUseCase,
    private val userRepository: UserRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    private var fetchJob: Job? = null

    init {
        fetchHomeData(isInitialLoad = true)
    }

    fun loadData() = fetchHomeData(isInitialLoad = true)
    fun refresh() = fetchHomeData(isInitialLoad = false)

    fun pickWhatToWatchToday() {
        viewModelScope.launch {
            val recommendations = try {
                getRecommendationsUseCase.execute()
            } catch (e: Exception) {
                _uiState.value.trendingShows
            }
            val pick = recommendations.filter { it.posterPath != null }.randomOrNull()
                ?: _uiState.value.trendingShows.filter { it.posterPath != null }.firstOrNull()
            _uiState.update { it.copy(whatToWatchToday = pick) }
        }
    }

    fun dismissWhatToWatch() {
        _uiState.update { it.copy(whatToWatchToday = null) }
    }

    private fun fetchHomeData(isInitialLoad: Boolean) {
        fetchJob?.cancel() // Cancel any in-flight fetch before starting a new one
        fetchJob = viewModelScope.launch {
            _uiState.update { 
                if (isInitialLoad) it.copy(isLoading = true, errorMessage = null)
                else it.copy(isRefreshing = true, errorMessage = null)
            }

            try {
                val trendingDeferred = async { repository.getTrendingShows() }
                val actionDeferred = async { repository.discoverShows(genreId = "10759") } // Action & Adventure
                val comedyDeferred = async { repository.discoverShows(genreId = "35") } // Comedy
                val spainDeferred = async { repository.discoverShows(watchRegion = "ES", sortBy = "popularity.desc") }
                val mysteryDeferred = async { repository.discoverShows(genreId = "9648") } // Mystery
                
                val trendingRes = trendingDeferred.await()
                val actionRes = actionDeferred.await()
                val comedyRes = comedyDeferred.await()
                val spainRes = spainDeferred.await()
                val mysteryRes = mysteryDeferred.await()
                
                var upNextList = _uiState.value.upNextShows
                var trendingList = _uiState.value.trendingShows
                var actionList = _uiState.value.actionShows
                var comedyList = _uiState.value.comedyShows
                var spainList = _uiState.value.popularInSpain
                var mysteryList = _uiState.value.mysteryShows
                var error: UiText? = null

                when (trendingRes) {
                    is Resource.Success -> trendingList = getRecommendationsUseCase.scoreShows(trendingRes.data)
                    is Resource.Error -> error = UiText.DynamicString(trendingRes.message)
                    else -> {}
                }

                if (actionRes is Resource.Success) actionList = getRecommendationsUseCase.scoreShows(actionRes.data)
                if (comedyRes is Resource.Success) comedyList = getRecommendationsUseCase.scoreShows(comedyRes.data)
                if (spainRes is Resource.Success) spainList = getRecommendationsUseCase.scoreShows(spainRes.data)
                if (mysteryRes is Resource.Success) mysteryList = getRecommendationsUseCase.scoreShows(mysteryRes.data)

                // Fetch Up Next shows in parallel
                val profile = userRepository.getUserProfile()
                val recentShowIds = profile?.watchedEpisodes?.keys
                    ?.toList()
                    ?.takeLast(7)
                    ?.mapNotNull { it.toIntOrNull() }
                    ?: emptyList()

                if (recentShowIds.isNotEmpty()) {
                    val deferred = recentShowIds.map { showId ->
                        async { repository.getShowDetails(showId) }
                    }
                    upNextList = deferred.awaitAll()
                        .filterIsInstance<Resource.Success<MediaContent>>()
                        .map { it.data }
                        .filter { it.posterPath != null }
                        .distinctBy { it.id }
                }

                _uiState.update { 
                    it.copy(
                        isLoading = false,
                        isRefreshing = false,
                        upNextShows = upNextList,
                        trendingShows = trendingList,
                        actionShows = actionList,
                        comedyShows = comedyList,
                        popularInSpain = spainList,
                        mysteryShows = mysteryList,
                        errorMessage = error
                    ) 
                }

            } catch (e: Exception) {
                Log.e("HomeViewModel", "Error fetching home data", e)
                val errorRes = if (isInitialLoad) R.string.error_unexpected_data else R.string.error_refresh_data
                _uiState.update { 
                    it.copy(
                        isLoading = false, 
                        isRefreshing = false, 
                        errorMessage = UiText.StringResource(errorRes)
                    ) 
                }
            }
        }
    }
}
