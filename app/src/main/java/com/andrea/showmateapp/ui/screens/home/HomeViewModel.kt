package com.andrea.showmateapp.ui.screens.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.andrea.showmateapp.data.network.MediaContent
import com.andrea.showmateapp.domain.repository.IInteractionRepository
import com.andrea.showmateapp.domain.repository.IShowRepository
import com.andrea.showmateapp.domain.repository.IUserRepository
import com.andrea.showmateapp.domain.usecase.GetRecommendationsUseCase
import com.andrea.showmateapp.util.ErrorType
import com.andrea.showmateapp.util.PerfTracer
import com.andrea.showmateapp.util.Resource
import com.andrea.showmateapp.util.SnackbarManager
import com.andrea.showmateapp.util.UiText
import com.google.firebase.crashlytics.FirebaseCrashlytics
import dagger.hilt.android.lifecycle.HiltViewModel
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import javax.inject.Inject
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import timber.log.Timber

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val repository: IShowRepository,
    private val getRecommendationsUseCase: GetRecommendationsUseCase,
    private val userRepository: IUserRepository,
    private val interactionRepository: IInteractionRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    private var fetchJob: Job? = null
    private var trendingPage = 1
    private var trendingTotalPages = 1
    private var thisWeekPage = 1
    private var thisWeekTotalPages = 1

    init {
        fetchHomeData(isInitialLoad = true)
    }

    fun onAction(action: HomeAction) {
        when (action) {
            is HomeAction.LoadData -> loadData()
            is HomeAction.Refresh -> refresh()
            is HomeAction.RequestWhatToWatch -> requestWhatToWatch()
            is HomeAction.DismissContextSelector -> dismissContextSelector()
            is HomeAction.PickWhatToWatchToday -> pickWhatToWatchToday(action.mood, action.time)
            is HomeAction.DismissWhatToWatch -> dismissWhatToWatch()
            is HomeAction.LoadMoreTrending -> loadMoreTrending()
            is HomeAction.LoadMoreThisWeek -> loadMoreThisWeek()
            is HomeAction.SelectPlatform -> selectPlatform(action.platform)
            is HomeAction.SwipeLeft -> handleSwipeLeft(action.mediaId)
            is HomeAction.MarkAsWatched -> handleMarkAsWatched(action.mediaId)
        }
    }

    private fun removeShowFromState(mediaId: Int) {
        _uiState.update { state ->
            state.copy(
                upNextShows = state.upNextShows.filter { it.id != mediaId },
                trendingShows = state.trendingShows.filter { it.id != mediaId },
                top10Shows = state.top10Shows.filter { it.id != mediaId },
                newReleasesShows = state.newReleasesShows.filter { it.id != mediaId },
                genres = state.genres.copy(
                    action = state.genres.action.filter { it.id != mediaId },
                    comedy = state.genres.comedy.filter { it.id != mediaId },
                    drama = state.genres.drama.filter { it.id != mediaId }
                ),
                thisWeekShows = state.thisWeekShows.filter { it.id != mediaId },
                whatToWatchToday = state.whatToWatchToday?.takeIf { it.id != mediaId }
            )
        }
    }

    private fun handleSwipeLeft(mediaId: Int) {
        removeShowFromState(mediaId)
        viewModelScope.launch {
            try {
                interactionRepository.trackMediaInteraction(
                    mediaId = mediaId,
                    genres = emptyList(),
                    keywords = emptyList(),
                    actors = emptyList(),
                    narrativeStyles = emptyMap(),
                    creators = emptyList(),
                    interactionType = IInteractionRepository.InteractionType.Dislike
                )
            } catch (e: Exception) {
                Timber.e(e, "Error registerSwipe")
            }
        }
    }

    private fun handleMarkAsWatched(mediaId: Int) {
        removeShowFromState(mediaId)
        viewModelScope.launch {
            try {
                interactionRepository.trackMediaInteraction(
                    mediaId = mediaId,
                    genres = emptyList(),
                    keywords = emptyList(),
                    actors = emptyList(),
                    narrativeStyles = emptyMap(),
                    creators = emptyList(),
                    interactionType = IInteractionRepository.InteractionType.Watched
                )
            } catch (e: Exception) {
                Timber.e(e, "Error markAsWatched")
            }
        }
    }

    private fun loadData() = fetchHomeData(isInitialLoad = true)
    private fun refresh() = fetchHomeData(isInitialLoad = false)

    companion object {
        val PLATFORM_PROVIDER_IDS = mapOf(
            "Netflix" to "8",
            // Amazon Prime Video ES (ID 9 = US only)
            "Prime" to "119",
            "Disney+" to "337",
            // Max (HBO Max) ES
            "Max" to "1899"
        )
    }

    private fun selectPlatform(name: String?) {
        val current = _uiState.value.selectedPlatform
        val newSelection = if (current == name) null else name
        _uiState.update { it.copy(selectedPlatform = newSelection) }

        if (newSelection == null || _uiState.value.platformShows.containsKey(newSelection)) return

        val providerId = PLATFORM_PROVIDER_IDS[newSelection] ?: return
        viewModelScope.launch {
            _uiState.update { it.copy(isPlatformLoading = true) }
            try {
                // Primary: shows airing THIS week on that platform (on_the_air endpoint)
                var result = repository.getShowsOnTheAir(providers = providerId)

                // Fallback: if nothing is on air this week, show popular titles from that platform
                if (result is Resource.Success && result.data.isEmpty()) {
                    result = repository.discoverShows(
                        providers = providerId,
                        watchRegion = "ES",
                        sortBy = "popularity.desc"
                    )
                }

                when (result) {
                    is Resource.Success -> {
                        val scored = getRecommendationsUseCase.scoreShows(result.data.take(15))
                        _uiState.update { it.copy(
                            platformShows = it.platformShows + (newSelection to scored),
                            isPlatformLoading = false
                        ) }
                    }
                    is Resource.Error -> {
                        SnackbarManager.showError(result.type)
                        _uiState.update { it.copy(isPlatformLoading = false) }
                    }
                    else -> _uiState.update { it.copy(isPlatformLoading = false) }
                }
            } catch (e: Exception) {
                if (e is CancellationException) throw e
                SnackbarManager.showError(ErrorType.Unknown)
                _uiState.update { it.copy(isPlatformLoading = false) }
            }
        }
    }

    private fun requestWhatToWatch() {
        _uiState.update { it.copy(showContextSelector = true) }
    }

    private fun dismissContextSelector() {
        _uiState.update { it.copy(showContextSelector = false) }
    }

    private fun pickWhatToWatchToday(mood: MoodOption? = null, time: TimeOption? = null) {
        viewModelScope.launch {
            _uiState.update { it.copy(showContextSelector = false) }
            val currentState = _uiState.value
            val cachedPool = (currentState.trendingShows + currentState.genres.action +
                    currentState.genres.comedy + currentState.genres.drama).distinctBy { it.id }

            val recommendations = cachedPool.ifEmpty {
                try { getRecommendationsUseCase.execute() } catch (e: Exception) { emptyList() }
            }

            var candidates = recommendations.filter { it.posterPath != null }

            mood?.let { m ->
                val filtered = candidates.filter { it.safeGenreIds.any { id -> id in m.genreIds } }
                if (filtered.isNotEmpty()) candidates = filtered
            }

            time?.maxRuntime?.let { max ->
                val filtered = candidates.filter { (it.episodeRunTime?.firstOrNull() ?: Int.MAX_VALUE) <= max }
                if (filtered.isNotEmpty()) candidates = filtered
            }

            val pick = candidates.maxByOrNull { it.affinityScore }
                ?: currentState.trendingShows.firstOrNull { it.posterPath != null }

            _uiState.update { it.copy(whatToWatchToday = pick) }
        }
    }

    private fun dismissWhatToWatch() {
        _uiState.update { it.copy(whatToWatchToday = null) }
    }

    private fun loadMoreTrending() {
        if (_uiState.value.isLoadingMoreTrending) return
        if (trendingPage >= trendingTotalPages) return
        viewModelScope.launch {
            _uiState.update { it.copy(isLoadingMoreTrending = true) }
            try {
                val result = repository.discoverShowsPaged(sortBy = "popularity.desc", page = trendingPage + 1)
                if (result is Resource.Success) {
                    trendingPage++
                    trendingTotalPages = result.data.second
                    val scored = getRecommendationsUseCase.scoreShows(result.data.first)
                    _uiState.update { state ->
                        state.copy(
                            trendingShows = (state.trendingShows + scored).distinctBy { it.id },
                            isLoadingMoreTrending = false
                        )
                    }
                } else {
                    _uiState.update { it.copy(isLoadingMoreTrending = false) }
                }
            } catch (e: Exception) {
                if (e is CancellationException) throw e
                _uiState.update { it.copy(isLoadingMoreTrending = false) }
            }
        }
    }

    private fun loadMoreThisWeek() {
        if (_uiState.value.isLoadingMoreThisWeek) return
        if (thisWeekPage >= thisWeekTotalPages) return
        val fmt = DateTimeFormatter.ISO_LOCAL_DATE
        val today = LocalDate.now().format(fmt)
        val weekLater = LocalDate.now().plusDays(7).format(fmt)
        viewModelScope.launch {
            _uiState.update { it.copy(isLoadingMoreThisWeek = true) }
            try {
                val result = repository.discoverShowsPaged(
                    sortBy = "popularity.desc",
                    page = thisWeekPage + 1,
                    airDateGte = today,
                    airDateLte = weekLater
                )
                if (result is Resource.Success) {
                    thisWeekPage++
                    thisWeekTotalPages = result.data.second
                    val scored = getRecommendationsUseCase.scoreShows(result.data.first)
                    _uiState.update { state ->
                        state.copy(
                            thisWeekShows = (state.thisWeekShows + scored).distinctBy { it.id },
                            isLoadingMoreThisWeek = false
                        )
                    }
                } else {
                    _uiState.update { it.copy(isLoadingMoreThisWeek = false) }
                }
            } catch (e: Exception) {
                if (e is CancellationException) throw e
                _uiState.update { it.copy(isLoadingMoreThisWeek = false) }
            }
        }
    }

    private fun fetchHomeData(isInitialLoad: Boolean) {
        fetchJob?.cancel()
        fetchJob = viewModelScope.launch {
            resetPagination()
            updateLoadingState(isInitialLoad)

            try {
                fetchPhase1()
                fetchPhase2()
            } catch (e: Exception) {
                if (e is CancellationException) throw e
                Timber.e(e, "Error fetching home data")
                FirebaseCrashlytics.getInstance().recordException(e)
                _uiState.update { it.copy(isLoading = false, isRefreshing = false, criticalError = ErrorType.Unknown) }
            }
        }
    }

    private fun resetPagination() {
        trendingPage = 1
        trendingTotalPages = 1
        thisWeekPage = 1
        thisWeekTotalPages = 1
    }

    private fun updateLoadingState(isInitialLoad: Boolean) {
        _uiState.update {
            if (isInitialLoad) {
                it.copy(isLoading = true, errorMessage = null, criticalError = null, belowFoldLoaded = false)
            } else {
                it.copy(isRefreshing = true, errorMessage = null, criticalError = null)
            }
        }
    }

    private suspend fun fetchPhase1() {
        PerfTracer.trace("home_phase1_fetch") { trace ->
            val trendingDef = viewModelScope.async { repository.getTrendingShows() }
            val top10Def = viewModelScope.async { repository.getTrendingThisWeek() }
            val thisWeekDef = viewModelScope.async { repository.getShowsOnTheAir() }
            val profileDef = viewModelScope.async { userRepository.getUserProfile() }

            val trendingRes = trendingDef.await()
            val top10Res = top10Def.await()
            val thisWeekRes = thisWeekDef.await()
            val profile = profileDef.await()

            val trendingList = if (trendingRes is Resource.Success) {
                getRecommendationsUseCase.scoreShows(trendingRes.data)
            } else _uiState.value.trendingShows

            val top10List = if (top10Res is Resource.Success) {
                val bannerIds = trendingList.take(5).map { it.id }.toSet()
                getRecommendationsUseCase.scoreShows(top10Res.data.filter { it.id !in bannerIds }.take(10))
            } else _uiState.value.top10Shows

            val thisWeekList = if (thisWeekRes is Resource.Success) {
                getRecommendationsUseCase.scoreShows(thisWeekRes.data.take(15))
            } else _uiState.value.thisWeekShows

            val userName = profile?.username?.takeIf { it.isNotBlank() }
                ?: userRepository.getCurrentUserEmail()?.substringBefore("@")?.replaceFirstChar { it.uppercaseChar() } ?: ""

            val upNextList = fetchUpNextShows(profile?.watchedEpisodes ?: emptyMap())
            val progressMap = calculateUpNextProgress(upNextList, profile?.watchedEpisodes ?: emptyMap())

            trace?.putMetric("trending_count", trendingList.size.toLong())

            val hasAnyData = trendingList.isNotEmpty() || top10List.isNotEmpty() || thisWeekList.isNotEmpty()
            val error = (trendingRes as? Resource.Error) ?: (top10Res as? Resource.Error) ?: (thisWeekRes as? Resource.Error)

            if (error != null && !hasAnyData) {
                _uiState.update { it.copy(isLoading = false, isRefreshing = false, criticalError = error.type) }
                return@trace
            }

            if (error != null && !_uiState.value.isLoading) {
                SnackbarManager.showError(error.type.defaultMessage)
            }

            _uiState.update {
                it.copy(
                    isLoading = false,
                    isRefreshing = false,
                    isFromCache = error != null && hasAnyData,
                    userName = userName,
                    upNextShows = upNextList,
                    upNextProgress = progressMap,
                    trendingShows = trendingList,
                    top10Shows = top10List,
                    thisWeekShows = thisWeekList
                )
            }
        }
    }

    private suspend fun fetchUpNextShows(watchedEpisodes: Map<String, List<Int>>): List<MediaContent> {
        val recentShowIds = watchedEpisodes.keys.toList().takeLast(7).mapNotNull { it.toIntOrNull() }
        if (recentShowIds.isEmpty()) return emptyList()

        return recentShowIds.map { viewModelScope.async { repository.getShowDetails(it) } }
            .awaitAll()
            .filterIsInstance<Resource.Success<MediaContent>>()
            .map { it.data }
            .filter { it.posterPath != null }
            .distinctBy { it.id }
    }

    private fun calculateUpNextProgress(shows: List<MediaContent>, watchedEpisodes: Map<String, List<Int>>): Map<Int, Float> {
        return shows.associate { show ->
            val watched = watchedEpisodes[show.id.toString()]?.size ?: 0
            val total = ((show.numberOfSeasons ?: 1) * 10).coerceAtLeast(1)
            show.id to (watched.toFloat() / total).coerceIn(0f, 1f)
        }
    }

    private suspend fun fetchPhase2() {
        PerfTracer.trace("home_phase2_fetch") { trace ->
            val threeMonthsAgo = LocalDate.now().minusMonths(3).toString()
            val defs = mapOf(
                "new" to viewModelScope.async { repository.discoverShows(firstAirDateGte = threeMonthsAgo, sortBy = "popularity.desc") },
                "10759" to viewModelScope.async { repository.discoverShows(genreId = "10759") }, // Action
                "35" to viewModelScope.async { repository.discoverShows(genreId = "35") },     // Comedy
                "18" to viewModelScope.async { repository.discoverShows(genreId = "18") }      // Drama
            )

            val results = defs.mapValues { it.value.await() }
            val existingIds = (_uiState.value.trendingShows + _uiState.value.top10Shows).map { it.id }.toSet()

            suspend fun process(res: Resource<List<MediaContent>>?, current: List<MediaContent>, filter: Boolean = false): List<MediaContent> {
                return if (res is Resource.Success) {
                    val list = if (filter) res.data.filter { it.id !in existingIds }.take(15) else res.data
                    getRecommendationsUseCase.scoreShows(list)
                } else current
            }

            val belowFoldTotal = results.values.filterIsInstance<Resource.Success<List<MediaContent>>>().sumOf { it.data.size }.toLong()
            trace?.putMetric("below_fold_total", belowFoldTotal)

            val newReleasesShows = process(results["new"], _uiState.value.newReleasesShows, true)
            val actionList = process(results["10759"], _uiState.value.genres.action)
            val comedyList = process(results["35"], _uiState.value.genres.comedy)
            val dramaList = process(results["18"], _uiState.value.genres.drama)

            _uiState.update { state ->
                state.copy(
                    newReleasesShows = newReleasesShows,
                    genres = HomeGenreShows(
                        action = actionList,
                        comedy = comedyList,
                        drama = dramaList
                    ),
                    belowFoldLoaded = true
                )
            }
        }
    }
}
