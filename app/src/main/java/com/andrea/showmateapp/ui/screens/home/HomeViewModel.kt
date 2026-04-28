package com.andrea.showmateapp.ui.screens.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.andrea.showmateapp.data.model.*
import com.andrea.showmateapp.domain.repository.IInteractionRepository
import com.andrea.showmateapp.domain.repository.IShowRepository
import com.andrea.showmateapp.domain.repository.IUserRepository
import com.andrea.showmateapp.domain.usecase.GetRecommendationsUseCase
import com.andrea.showmateapp.util.ErrorType
import com.andrea.showmateapp.util.NarrativeStyleMapper
import com.andrea.showmateapp.util.PerfTracer
import com.andrea.showmateapp.util.Resource
import com.andrea.showmateapp.util.SnackbarManager
import com.andrea.showmateapp.util.UiText
import com.andrea.showmateapp.di.IoDispatcher
import com.google.firebase.crashlytics.FirebaseCrashlytics
import dagger.hilt.android.lifecycle.HiltViewModel
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import javax.inject.Inject
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import timber.log.Timber

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val repository: IShowRepository,
    private val getRecommendationsUseCase: GetRecommendationsUseCase,
    private val userRepository: IUserRepository,
    private val interactionRepository: IInteractionRepository,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher
) : ViewModel() {

    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    private var fetchJob: Job? = null
    private var trendingPage = 1
    private var trendingTotalPages = Int.MAX_VALUE
    private var thisWeekPage = 1
    private var thisWeekTotalPages = Int.MAX_VALUE

    init {
        fetchHomeData(isInitialLoad = true)
        observeInteractions()
    }

    private fun HomeUiState.filterShows(predicate: (MediaContent) -> Boolean) = copy(
        upNextShows = upNextShows.filter(predicate),
        trendingShows = trendingShows.filter(predicate),
        top10Shows = top10Shows.filter(predicate),
        newReleasesShows = newReleasesShows.filter(predicate),
        thisWeekShows = thisWeekShows.filter(predicate),
        genres = genres.copy(
            action = genres.action.filter(predicate),
            comedy = genres.comedy.filter(predicate),
            drama = genres.drama.filter(predicate),
            sciFi = genres.sciFi.filter(predicate),
            mystery = genres.mystery.filter(predicate)
        ),
        platformShows = platformShows.mapValues { (_, shows) -> shows.filter(predicate) },
        whatToWatchToday = whatToWatchToday?.takeIf(predicate)
    )

    private fun observeInteractions() {
        viewModelScope.launch {
            try {
                interactionRepository.getInteractedMediaIdsFlow().collect { interactedIds ->
                    if (interactedIds.isEmpty()) return@collect
                    _uiState.update { it.filterShows { show -> show.id !in interactedIds } }
                }
            } catch (e: Exception) {
                if (e is CancellationException) throw e
                Timber.e(e, "observeInteractions flow failed")
            }
        }

        viewModelScope.launch {
            userRepository.getUserProfileFlow().collect { profile ->
                if (profile != null && !_uiState.value.isLoading && !_uiState.value.isRefreshing) {
                    rescoreExistingContent()
                }
            }
        }
    }

    private fun rescoreExistingContent() {
        viewModelScope.launch {
            val state = _uiState.value
            val upNextScored = getRecommendationsUseCase.scoreShows(state.upNextShows)
            val trendingScored = getRecommendationsUseCase.scoreShows(state.trendingShows)
            val top10Scored = getRecommendationsUseCase.scoreShows(state.top10Shows)
            val newReleasesScored = getRecommendationsUseCase.scoreShows(state.newReleasesShows)
            val thisWeekScored = getRecommendationsUseCase.scoreShows(state.thisWeekShows)

            val actionScored = getRecommendationsUseCase.scoreShows(state.genres.action)
            val comedyScored = getRecommendationsUseCase.scoreShows(state.genres.comedy)
            val dramaScored = getRecommendationsUseCase.scoreShows(state.genres.drama)
            val sciFiScored = getRecommendationsUseCase.scoreShows(state.genres.sciFi)
            val mysteryScored = getRecommendationsUseCase.scoreShows(state.genres.mystery)

            val platformScored = state.platformShows.mapValues { getRecommendationsUseCase.scoreShows(it.value) }
            val whatToWatchScored = state.whatToWatchToday?.let { getRecommendationsUseCase.scoreForDetail(it) }

            _uiState.update { s ->
                s.copy(
                    upNextShows = upNextScored,
                    trendingShows = trendingScored,
                    top10Shows = top10Scored,
                    newReleasesShows = newReleasesScored,
                    thisWeekShows = thisWeekScored,
                    genres = s.genres.copy(
                        action = actionScored,
                        comedy = comedyScored,
                        drama = dramaScored,
                        sciFi = sciFiScored,
                        mystery = mysteryScored
                    ),
                    platformShows = platformScored,
                    whatToWatchToday = whatToWatchScored
                )
            }
        }
    }

    private fun findShowInState(mediaId: Int): com.andrea.showmateapp.data.model.MediaContent? {
        val state = _uiState.value
        return state.trendingShows.find { it.id == mediaId }
            ?: state.top10Shows.find { it.id == mediaId }
            ?: state.newReleasesShows.find { it.id == mediaId }
            ?: state.thisWeekShows.find { it.id == mediaId }
            ?: state.genres.action.find { it.id == mediaId }
            ?: state.genres.comedy.find { it.id == mediaId }
            ?: state.genres.drama.find { it.id == mediaId }
            ?: state.genres.sciFi.find { it.id == mediaId }
            ?: state.genres.mystery.find { it.id == mediaId }
            ?: state.whatToWatchToday?.takeIf { it.id == mediaId }
            ?: state.platformShows.values.flatten().find { it.id == mediaId }
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
        _uiState.update { it.filterShows { show -> show.id != mediaId } }
    }

    private fun handleSwipeLeft(mediaId: Int) = trackAndRemove(mediaId, IInteractionRepository.InteractionType.Dislike)
    private fun handleMarkAsWatched(mediaId: Int) = trackAndRemove(mediaId, IInteractionRepository.InteractionType.Watched)

    private fun trackAndRemove(mediaId: Int, interactionType: IInteractionRepository.InteractionType) {
        val show = findShowInState(mediaId)
        removeShowFromState(mediaId)
        viewModelScope.launch {
            try {
                interactionRepository.trackMediaInteraction(
                    mediaId = mediaId,
                    genres = show?.safeGenreIds?.map { it.toString() } ?: emptyList(),
                    keywords = show?.keywordNames ?: emptyList(),
                    actors = show?.credits?.cast?.map { it.id } ?: emptyList(),
                    narrativeStyles = show?.let {
                        NarrativeStyleMapper.extractStyles(it.keywordNames, it.episodeRunTime?.firstOrNull())
                    } ?: emptyMap(),
                    creators = show?.creatorIds ?: emptyList(),
                    interactionType = interactionType
                )
            } catch (e: Exception) {
                Timber.e(e, "Error trackAndRemove $interactionType")
            }
        }
    }

    private fun loadData() = fetchHomeData(isInitialLoad = true)
    private fun refresh() = fetchHomeData(isInitialLoad = false)

    companion object {
        val PLATFORM_PROVIDER_IDS = mapOf(
            "Netflix" to "8",
            "Prime" to "119",
            "Disney+" to "337",
            "Max" to "1899",
            "Apple TV+" to "350"
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
                val excludedIds = interactionRepository.getExcludedMediaIds()
                var result = repository.getShowsOnTheAir(providers = providerId)

                if (result is Resource.Success && result.data.isEmpty()) {
                    result = repository.discoverShows(
                        providers = providerId,
                        watchRegion = "ES",
                        sortBy = "popularity.desc",
                        excludedIds = excludedIds.toList()
                    )
                }

                when (result) {
                    is Resource.Success -> {
                        val filtered = result.data.filter { it.id !in excludedIds }
                        val scored = getRecommendationsUseCase.scoreShows(filtered.take(15))
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
                    currentState.genres.comedy + currentState.genres.drama +
                    currentState.genres.sciFi + currentState.genres.mystery).distinctBy { it.id }

            val recommendations = if (cachedPool.isNotEmpty()) {
                getRecommendationsUseCase.scoreShows(cachedPool)
            } else {
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

            val pick = candidates.sortedByDescending { it.affinityScore }.firstOrNull()
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
            withContext(ioDispatcher) {
                _uiState.update { it.copy(isLoadingMoreTrending = true) }
                try {
                    val excludedIds = interactionRepository.getExcludedMediaIds()
                    val result = repository.discoverShowsPaged(sortBy = "popularity.desc", page = trendingPage + 1)
                    if (result is Resource.Success) {
                        trendingPage++
                        trendingTotalPages = result.data.second
                        val filtered = result.data.first.filter { it.id !in excludedIds }
                        val scored = getRecommendationsUseCase.scoreShows(filtered)
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
    }

    private fun loadMoreThisWeek() {
        if (_uiState.value.isLoadingMoreThisWeek) return
        if (thisWeekPage >= thisWeekTotalPages) return
        val fmt = DateTimeFormatter.ISO_LOCAL_DATE
        val today = LocalDate.now().format(fmt)
        val weekLater = LocalDate.now().plusDays(7).format(fmt)
        viewModelScope.launch {
            withContext(ioDispatcher) {
                _uiState.update { it.copy(isLoadingMoreThisWeek = true) }
                try {
                    val excludedIds = interactionRepository.getExcludedMediaIds()
                    val result = repository.discoverShowsPaged(
                        sortBy = "popularity.desc",
                        page = thisWeekPage + 1,
                        airDateGte = today,
                        airDateLte = weekLater
                    )
                    if (result is Resource.Success) {
                        thisWeekPage++
                        thisWeekTotalPages = result.data.second
                        val filtered = result.data.first.filter { it.id !in excludedIds }
                        val scored = getRecommendationsUseCase.scoreShows(filtered)
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
    }

    private fun fetchHomeData(isInitialLoad: Boolean) {
        fetchJob?.cancel()
        fetchJob = viewModelScope.launch {
            withContext(ioDispatcher) {
                resetPagination()
                updateLoadingState(isInitialLoad)

                try {
                    val profile = userRepository.getUserProfile()
                    fetchPhase1(profile)
                    fetchPhase2()
                } catch (e: Exception) {
                    if (e is CancellationException) throw e
                    Timber.e(e, "Error fetching home data")
                    FirebaseCrashlytics.getInstance().recordException(e)
                    _uiState.update { it.copy(isLoading = false, isRefreshing = false, criticalError = ErrorType.Unknown) }
                }
            }
        }
    }

    private fun resetPagination() {
        trendingPage = 1
        trendingTotalPages = Int.MAX_VALUE
        thisWeekPage = 1
        thisWeekTotalPages = Int.MAX_VALUE
    }

    private fun updateLoadingState(isInitialLoad: Boolean) {
        _uiState.update {
            if (isInitialLoad) {
                it.copy(isLoading = true, errorMessage = null, criticalError = null)
            } else {
                it.copy(isRefreshing = true, errorMessage = null, criticalError = null,
                    platformShows = emptyMap(), selectedPlatform = null)
            }
        }
    }

    private suspend fun fetchPhase1(profile: UserProfile?) = withContext(ioDispatcher) {
        PerfTracer.trace("home_phase1_fetch") { trace ->
            val excludedIds = interactionRepository.getExcludedMediaIds()
            val trendingDef = async { repository.getTrendingShows() }
            val top10Def = async { repository.getTrendingThisWeek() }
            val thisWeekDef = async { repository.getShowsOnTheAir() }

            val trendingRes = trendingDef.await()
            val top10Res = top10Def.await()
            val thisWeekRes = thisWeekDef.await()

            val trendingList = if (trendingRes is Resource.Success) {
                val filtered = trendingRes.data.filter { it.id !in excludedIds }
                getRecommendationsUseCase.scoreShows(filtered)
            } else _uiState.value.trendingShows

            val top10List = if (top10Res is Resource.Success) {
                val bannerIds = trendingList.take(5).map { it.id }.toSet()
                val filtered = top10Res.data.filter { it.id !in bannerIds && it.id !in excludedIds }
                getRecommendationsUseCase.scoreShows(filtered.take(10))
            } else _uiState.value.top10Shows

            val thisWeekList = if (thisWeekRes is Resource.Success) {
                val filtered = thisWeekRes.data.filter { it.id !in excludedIds }
                getRecommendationsUseCase.scoreShows(filtered.take(15))
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

    private suspend fun fetchUpNextShows(watchedEpisodes: Map<String, List<Int>>): List<MediaContent> = withContext(ioDispatcher) {
        val recentShowIds = watchedEpisodes.keys.toList().takeLast(12).mapNotNull { it.toIntOrNull() }
        if (recentShowIds.isEmpty()) return@withContext emptyList()

        val fullyWatchedIds = interactionRepository.getWatchedMediaIds().toSet()

        val shows = recentShowIds
            .filter { it !in fullyWatchedIds }
            .map { id -> async { repository.getShowDetails(id) } }
            .awaitAll()
            .filterIsInstance<Resource.Success<MediaContent>>()
            .map { it.data }
            .filter { it.posterPath != null }
            .distinctBy { it.id }

        val finishedStatuses = setOf("Ended", "Canceled", "Cancelled")

        shows.filter { show ->
            val watchedCount = watchedEpisodes[show.id.toString()]?.size ?: 0
            val totalEpisodes = show.seasons
                ?.filter { it.seasonNumber > 0 }
                ?.sumOf { it.episodeCount }
                ?: ((show.numberOfSeasons ?: 1) * 10)

            val isFinishedStatus = show.status in finishedStatuses
            val caughtUp = watchedCount >= totalEpisodes && totalEpisodes > 0

            if (isFinishedStatus) !caughtUp else (watchedCount > 0 && !caughtUp)
        }.let {
            getRecommendationsUseCase.scoreShows(it)
        }
    }

    private fun calculateUpNextProgress(shows: List<MediaContent>, watchedEpisodes: Map<String, List<Int>>): Map<Int, Float> {
        return shows.associate { show ->
            val watched = watchedEpisodes[show.id.toString()]?.size ?: 0
            val total = (show.seasons
                ?.filter { it.seasonNumber > 0 }
                ?.sumOf { it.episodeCount }
                ?.takeIf { it > 0 }
                ?: ((show.numberOfSeasons ?: 1) * 10)).coerceAtLeast(1)
            show.id to (watched.toFloat() / total).coerceIn(0f, 1f)
        }
    }

    private suspend fun fetchPhase2() = withContext(ioDispatcher) {
        PerfTracer.trace("home_phase2_fetch") { trace ->
            val excludedIds = interactionRepository.getExcludedMediaIds()
            val profile = userRepository.getUserProfile()
            val threeMonthsAgo = LocalDate.now().minusMonths(3).toString()

            val userGenres = profile?.genreScores?.filter { it.value > 0 }?.keys ?: emptySet()

            val defs = mutableMapOf<String, kotlinx.coroutines.Deferred<Resource<List<MediaContent>>>>()
            val defs2 = mutableMapOf<String, kotlinx.coroutines.Deferred<Resource<Pair<List<MediaContent>, Int>>>>()

            defs["new"] = async { repository.discoverShows(firstAirDateGte = threeMonthsAgo, sortBy = "popularity.desc") }

            val genresToFetch = mutableListOf<String>()
            if (userGenres.contains("10759")) genresToFetch.add("10759")
            if (userGenres.contains("35")) genresToFetch.add("35")
            if (userGenres.contains("18")) genresToFetch.add("18")
            if (userGenres.contains("10765")) genresToFetch.add("10765")
            if (userGenres.contains("9648")) genresToFetch.add("9648")

            if (genresToFetch.size < 3) {
                if (!genresToFetch.contains("18")) genresToFetch.add("18")
                if (!genresToFetch.contains("35")) genresToFetch.add("35")
            }

            genresToFetch.forEach { gid ->
                defs2[gid] = async { repository.discoverShowsPaged(genreId = gid, page = 1) }
                defs2[gid + "_p2"] = async { repository.discoverShowsPaged(genreId = gid, page = 2) }
                defs2[gid + "_p3"] = async { repository.discoverShowsPaged(genreId = gid, page = 3) }
            }

            val resultsNew = defs["new"]?.await()
            val resultsGenres = defs2.mapValues { it.value.await() }

            val totalShowsFound = resultsGenres.values.filterIsInstance<Resource.Success<Pair<List<MediaContent>, Int>>>().sumOf { it.data.first.size }
            trace?.putMetric("below_fold_genres_total", totalShowsFound.toLong())

            val existingIds = (_uiState.value.trendingShows + _uiState.value.top10Shows).map { it.id }.toSet()

            suspend fun process(res: Resource<List<MediaContent>>?, current: List<MediaContent>, filter: Boolean = false): List<MediaContent> {
                return if (res is Resource.Success) {
                    val baseList = res.data.filter { it.id !in excludedIds }
                    val list = if (filter) baseList.filter { it.id !in existingIds }.take(30) else baseList.take(30)
                    getRecommendationsUseCase.scoreShows(list)
                } else if (res == null) emptyList() else current
            }

            suspend fun processPaged(gid: String, current: List<MediaContent>): List<MediaContent> {
                val r1 = resultsGenres[gid]
                val r2 = resultsGenres[gid + "_p2"]
                val r3 = resultsGenres[gid + "_p3"]
                val list1 = (r1 as? Resource.Success)?.data?.first ?: emptyList()
                val list2 = (r2 as? Resource.Success)?.data?.first ?: emptyList()
                val list3 = (r3 as? Resource.Success)?.data?.first ?: emptyList()
                val combined = (list1 + list2 + list3).distinctBy { it.id }.filter { it.id !in excludedIds }
                return if (combined.isEmpty()) current else getRecommendationsUseCase.scoreShows(combined.take(40))
            }

            val newReleasesShows = process(resultsNew, _uiState.value.newReleasesShows, true)
            val actionList = processPaged("10759", _uiState.value.genres.action)
            val comedyList = processPaged("35", _uiState.value.genres.comedy)
            val dramaList = processPaged("18", _uiState.value.genres.drama)
            val sciFiList = processPaged("10765", _uiState.value.genres.sciFi)
            val mysteryList = processPaged("9648", _uiState.value.genres.mystery)

            _uiState.update { state ->
                state.copy(
                    newReleasesShows = newReleasesShows,
                    genres = HomeGenreShows(
                        action = actionList,
                        comedy = comedyList,
                        drama = dramaList,
                        sciFi = sciFiList,
                        mystery = mysteryList
                    )
                )
            }
        }
    }
}
