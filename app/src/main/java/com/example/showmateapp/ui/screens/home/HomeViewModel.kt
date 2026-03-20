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
    val userName: String = "",
    val upNextShows: List<MediaContent> = emptyList(),
    val upNextProgress: Map<Int, Float> = emptyMap(),
    val trendingShows: List<MediaContent> = emptyList(),
    val actionShows: List<MediaContent> = emptyList(),
    val comedyShows: List<MediaContent> = emptyList(),
    val mysteryShows: List<MediaContent> = emptyList(),
    val thisWeekShows: List<MediaContent> = emptyList(),
    val selectedPlatform: String? = null,
    val platformShows: Map<String, List<MediaContent>> = emptyMap(),
    val isPlatformLoading: Boolean = false,
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

    companion object {
        val PLATFORM_PROVIDER_IDS = mapOf(
            "Netflix"    to "8",
            "Prime"      to "9",
            "Disney+"    to "337",
            "Max"        to "384",
            "Paramount+" to "531"
        )
    }

    fun selectPlatform(name: String?) {
        val current = _uiState.value.selectedPlatform
        // Toggle off if already selected
        val newSelection = if (current == name) null else name
        _uiState.update { it.copy(selectedPlatform = newSelection) }
        if (newSelection == null) return

        // Return cached result if already loaded
        if (_uiState.value.platformShows.containsKey(newSelection)) return

        val providerId = PLATFORM_PROVIDER_IDS[newSelection] ?: return
        viewModelScope.launch {
            _uiState.update { it.copy(isPlatformLoading = true) }
            try {
                val result = repository.getShowsOnTheAir(providers = providerId)
                if (result is Resource.Success) {
                    val scored = getRecommendationsUseCase.scoreShows(result.data.take(15))
                    _uiState.update { state ->
                        state.copy(
                            platformShows = state.platformShows + (newSelection to scored),
                            isPlatformLoading = false
                        )
                    }
                } else {
                    _uiState.update { it.copy(isPlatformLoading = false) }
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(isPlatformLoading = false) }
            }
        }
    }

    fun pickWhatToWatchToday() {
        viewModelScope.launch {
            val recommendations = try {
                getRecommendationsUseCase.execute()
            } catch (e: Exception) {
                _uiState.value.trendingShows
            }
            val pick = recommendations
                .filter { it.posterPath != null }
                .maxByOrNull { it.affinityScore }
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
                val trendingDeferred  = async { repository.getTrendingShows() }
                val actionDeferred   = async { repository.discoverShows(genreId = "10759") }
                val comedyDeferred   = async { repository.discoverShows(genreId = "35") }
                val mysteryDeferred  = async { repository.discoverShows(genreId = "9648") }
                val thisWeekDeferred = async { repository.getShowsOnTheAir() }

                val trendingRes  = trendingDeferred.await()
                val actionRes    = actionDeferred.await()
                val comedyRes    = comedyDeferred.await()
                val mysteryRes   = mysteryDeferred.await()
                val thisWeekRes  = thisWeekDeferred.await()

                var upNextList   = emptyList<MediaContent>()
                var trendingList = _uiState.value.trendingShows
                var actionList   = _uiState.value.actionShows
                var comedyList   = _uiState.value.comedyShows
                var mysteryList  = _uiState.value.mysteryShows
                var thisWeekList = _uiState.value.thisWeekShows
                var error: UiText? = null

                when (trendingRes) {
                    is Resource.Success -> trendingList = getRecommendationsUseCase.scoreShows(trendingRes.data)
                    is Resource.Error -> error = UiText.DynamicString(trendingRes.message)
                    else -> {}
                }

                if (actionRes is Resource.Success) actionList = getRecommendationsUseCase.scoreShows(actionRes.data)
                if (comedyRes is Resource.Success) comedyList = getRecommendationsUseCase.scoreShows(comedyRes.data)
                if (mysteryRes is Resource.Success) mysteryList = getRecommendationsUseCase.scoreShows(mysteryRes.data)
                if (thisWeekRes is Resource.Success) thisWeekList = getRecommendationsUseCase.scoreShows(thisWeekRes.data.take(15))

                // Fetch Up Next shows in parallel
                val profile = userRepository.getUserProfile()
                val userName = profile?.username?.takeIf { it.isNotBlank() }
                    ?: userRepository.getCurrentUserEmail()?.substringBefore("@")
                        ?.replaceFirstChar { it.uppercaseChar() }
                    ?: ""
                val recentShowIds = profile?.watchedEpisodes?.keys
                    ?.toList()
                    ?.takeLast(7)
                    ?.mapNotNull { it.toIntOrNull() }
                    ?: emptyList()

                val episodesMap = profile?.watchedEpisodes ?: emptyMap()
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

                val progressMap = upNextList.associate { show ->
                    val watched = episodesMap[show.id.toString()]?.size ?: 0
                    val total = ((show.numberOfSeasons ?: 1) * 10).coerceAtLeast(1)
                    show.id to (watched.toFloat() / total).coerceIn(0f, 1f)
                }

                _uiState.update {
                    it.copy(
                        isLoading = false,
                        isRefreshing = false,
                        userName = userName,
                        upNextShows = upNextList,
                        upNextProgress = progressMap,
                        trendingShows = trendingList,
                        actionShows = actionList,
                        comedyShows = comedyList,
                        mysteryShows = mysteryList,
                        thisWeekShows = thisWeekList,
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
