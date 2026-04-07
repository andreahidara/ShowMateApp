package com.andrea.showmateapp.ui.screens.home

import androidx.compose.runtime.Immutable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.andrea.showmateapp.data.network.MediaContent
import com.andrea.showmateapp.domain.repository.IShowRepository
import com.andrea.showmateapp.domain.repository.IUserRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch
import kotlinx.coroutines.async
import timber.log.Timber
import kotlinx.coroutines.awaitAll
import javax.inject.Inject
import com.andrea.showmateapp.domain.usecase.GetRecommendationsUseCase
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import com.andrea.showmateapp.util.ErrorType
import com.andrea.showmateapp.util.PerfTracer
import com.andrea.showmateapp.util.Resource
import com.andrea.showmateapp.util.SnackbarManager
import com.andrea.showmateapp.util.UiText
import com.andrea.showmateapp.R
import com.google.firebase.crashlytics.FirebaseCrashlytics

@Immutable
data class HomeUiState(
    val isLoading: Boolean = false,
    val isRefreshing: Boolean = false,
    val belowFoldLoaded: Boolean = false,
    val userName: String = "",
    val upNextShows: List<MediaContent> = emptyList(),
    val upNextProgress: Map<Int, Float> = emptyMap(),
    val trendingShows: List<MediaContent> = emptyList(),
    val top10Shows: List<MediaContent> = emptyList(),
    val newReleasesShows: List<MediaContent> = emptyList(),
    val actionShows: List<MediaContent> = emptyList(),
    val comedyShows: List<MediaContent> = emptyList(),
    val mysteryShows: List<MediaContent> = emptyList(),
    val dramaShows: List<MediaContent> = emptyList(),
    val scifiShows: List<MediaContent> = emptyList(),
    val thisWeekShows: List<MediaContent> = emptyList(),
    val selectedPlatform: String? = null,
    val platformShows: Map<String, List<MediaContent>> = emptyMap(),
    val isPlatformLoading: Boolean = false,
    val errorMessage: UiText? = null,
    val criticalError: ErrorType? = null,
    val showContextSelector: Boolean = false,
    val whatToWatchToday: MediaContent? = null,
    val isLoadingMoreTrending: Boolean = false,
    val isLoadingMoreThisWeek: Boolean = false
)

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val repository: IShowRepository,
    private val getRecommendationsUseCase: GetRecommendationsUseCase,
    private val userRepository: IUserRepository
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
        }
    }

    private fun loadData() = fetchHomeData(isInitialLoad = true)
    private fun refresh() = fetchHomeData(isInitialLoad = false)

    companion object {
        val PLATFORM_PROVIDER_IDS = mapOf(
            "Netflix"    to "8",
            "Prime"      to "9",
            "Disney+"    to "337",
            "Max"        to "384",
            "Paramount+" to "531"
        )
    }

    private fun selectPlatform(name: String?) {
        val current = _uiState.value.selectedPlatform
        val newSelection = if (current == name) null else name
        _uiState.update { it.copy(selectedPlatform = newSelection) }
        if (newSelection == null) return

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
                    val errorType = (result as? Resource.Error)?.type ?: ErrorType.Unknown
                    SnackbarManager.showError(errorType)
                    _uiState.update { it.copy(isPlatformLoading = false) }
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
            val cachedPool = (currentState.trendingShows + currentState.actionShows +
                              currentState.comedyShows + currentState.mysteryShows).distinctBy { it.id }
            val recommendations = cachedPool.takeIf { it.isNotEmpty() } ?: try {
                getRecommendationsUseCase.execute()
            } catch (e: Exception) {
                if (e is CancellationException) throw e
                emptyList()
            }

            var candidates = recommendations.filter { it.posterPath != null }

            if (mood != null) {
                val moodFiltered = candidates.filter { show ->
                    show.safeGenreIds.any { it in mood.genreIds }
                }
                if (moodFiltered.isNotEmpty()) candidates = moodFiltered
            }

            if (time?.maxRuntime != null) {
                val runtimeFiltered = candidates.filter { show ->
                    val runtime = show.episodeRunTime?.firstOrNull() ?: Int.MAX_VALUE
                    runtime <= time.maxRuntime
                }
                if (runtimeFiltered.isNotEmpty()) candidates = runtimeFiltered
            }

            val pick = candidates.maxByOrNull { it.affinityScore }
                ?: _uiState.value.trendingShows.filter { it.posterPath != null }.firstOrNull()
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
            trendingPage = 1; trendingTotalPages = 1
            thisWeekPage = 1; thisWeekTotalPages = 1
            _uiState.update {
                if (isInitialLoad)
                    it.copy(isLoading = true, errorMessage = null, criticalError = null, belowFoldLoaded = false)
                else
                    it.copy(isRefreshing = true, errorMessage = null, criticalError = null)
            }

            try {
                val phase1Trace = PerfTracer.start("home_phase1_fetch")

                val trendingDeferred  = async { repository.getTrendingShows() }
                val top10Deferred     = async { repository.getTrendingThisWeek() }
                val thisWeekDeferred  = async { repository.getShowsOnTheAir() }
                val profileDeferred   = async { userRepository.getUserProfile() }

                val trendingRes  = trendingDeferred.await()
                val top10Res     = top10Deferred.await()
                val thisWeekRes  = thisWeekDeferred.await()
                val profile      = profileDeferred.await()

                var trendingList = _uiState.value.trendingShows
                var top10List    = _uiState.value.top10Shows
                var thisWeekList = _uiState.value.thisWeekShows
                var upNextList   = emptyList<MediaContent>()
                var error: UiText? = null

                when (trendingRes) {
                    is Resource.Success -> trendingList = getRecommendationsUseCase.scoreShows(trendingRes.data)
                    is Resource.Error   -> error = UiText.DynamicString(trendingRes.effectiveMessage)
                    else -> {}
                }
                if (top10Res is Resource.Success) {
                    val bannerIds = trendingList.take(5).map { it.id }.toSet()
                    top10List = getRecommendationsUseCase.scoreShows(
                        top10Res.data.filter { it.id !in bannerIds }.take(10)
                    )
                }
                if (thisWeekRes is Resource.Success) {
                    thisWeekList = getRecommendationsUseCase.scoreShows(thisWeekRes.data.take(15))
                }

                val userName = profile?.username?.takeIf { it.isNotBlank() }
                    ?: userRepository.getCurrentUserEmail()?.substringBefore("@")
                        ?.replaceFirstChar { it.uppercaseChar() }
                    ?: ""

                val episodesMap    = profile?.watchedEpisodes ?: emptyMap()
                val recentShowIds  = profile?.watchedEpisodes?.keys
                    ?.toList()?.takeLast(7)?.mapNotNull { it.toIntOrNull() } ?: emptyList()

                if (recentShowIds.isNotEmpty()) {
                    upNextList = recentShowIds.map { async { repository.getShowDetails(it) } }
                        .awaitAll()
                        .filterIsInstance<Resource.Success<MediaContent>>()
                        .map { it.data }
                        .filter { it.posterPath != null }
                        .distinctBy { it.id }
                }
                val progressMap = upNextList.associate { show ->
                    val watched = episodesMap[show.id.toString()]?.size ?: 0
                    val total   = ((show.numberOfSeasons ?: 1) * 10).coerceAtLeast(1)
                    show.id to (watched.toFloat() / total).coerceIn(0f, 1f)
                }

                phase1Trace.putMetric("trending_count", trendingList.size.toLong())
                runCatching { phase1Trace.stop() }

                val hasAnyData = trendingList.isNotEmpty() || top10List.isNotEmpty()
                val errorType = when {
                    trendingRes is Resource.Error -> trendingRes.type
                    top10Res    is Resource.Error -> top10Res.type
                    else -> null
                }

                if (error != null && !hasAnyData) {
                    _uiState.update {
                        it.copy(isLoading = false, isRefreshing = false, criticalError = errorType ?: ErrorType.Unknown)
                    }
                    return@launch
                }

                if (error != null && !isInitialLoad) {
                    SnackbarManager.showError(errorType?.defaultMessage ?: "No se pudo actualizar todo el contenido.")
                }

                _uiState.update {
                    it.copy(
                        isLoading    = false,
                        isRefreshing = false,
                        criticalError = null,
                        errorMessage  = null,
                        userName      = userName,
                        upNextShows   = upNextList,
                        upNextProgress = progressMap,
                        trendingShows = trendingList,
                        top10Shows    = top10List,
                        thisWeekShows = thisWeekList
                    )
                }

                launch {
                    PerfTracer.trace("home_phase2_fetch") { trace ->
                        val threeMonthsAgo   = java.time.LocalDate.now().minusMonths(3).toString()
                        val newReleasesDeferred = async { repository.discoverShows(firstAirDateGte = threeMonthsAgo, sortBy = "popularity.desc") }
                        val actionDeferred      = async { repository.discoverShows(genreId = "10759") }
                        val comedyDeferred      = async { repository.discoverShows(genreId = "35") }
                        val mysteryDeferred     = async { repository.discoverShows(genreId = "9648") }
                        val dramaDeferred       = async { repository.discoverShows(genreId = "18") }
                        val scifiDeferred       = async { repository.discoverShows(genreId = "10765") }

                        val newReleasesRes = newReleasesDeferred.await()
                        val actionRes      = actionDeferred.await()
                        val comedyRes      = comedyDeferred.await()
                        val mysteryRes     = mysteryDeferred.await()
                        val dramaRes       = dramaDeferred.await()
                        val scifiRes       = scifiDeferred.await()

                        val existingIds    = (_uiState.value.trendingShows + _uiState.value.top10Shows).map { it.id }.toSet()
                        val newReleasesList = if (newReleasesRes is Resource.Success)
                            getRecommendationsUseCase.scoreShows(newReleasesRes.data.filter { it.id !in existingIds }.take(15))
                        else _uiState.value.newReleasesShows

                        val actionList  = if (actionRes  is Resource.Success) getRecommendationsUseCase.scoreShows(actionRes.data)  else _uiState.value.actionShows
                        val comedyList  = if (comedyRes  is Resource.Success) getRecommendationsUseCase.scoreShows(comedyRes.data)  else _uiState.value.comedyShows
                        val mysteryList = if (mysteryRes is Resource.Success) getRecommendationsUseCase.scoreShows(mysteryRes.data) else _uiState.value.mysteryShows
                        val dramaList   = if (dramaRes   is Resource.Success) getRecommendationsUseCase.scoreShows(dramaRes.data)   else _uiState.value.dramaShows
                        val scifiList   = if (scifiRes   is Resource.Success) getRecommendationsUseCase.scoreShows(scifiRes.data)   else _uiState.value.scifiShows

                        trace.putMetric("below_fold_total", (actionList.size + comedyList.size + mysteryList.size).toLong())

                        _uiState.update {
                            it.copy(
                                newReleasesShows = newReleasesList,
                                actionShows      = actionList,
                                comedyShows      = comedyList,
                                mysteryShows     = mysteryList,
                                dramaShows       = dramaList,
                                scifiShows       = scifiList,
                                belowFoldLoaded  = true
                            )
                        }
                    }
                }

            } catch (e: Exception) {
                if (e is CancellationException) throw e
                Timber.e(e, "Error fetching home data")
                FirebaseCrashlytics.getInstance().recordException(e)
                _uiState.update { it.copy(isLoading = false, isRefreshing = false, criticalError = ErrorType.Unknown) }
            }
        }
    }
}
