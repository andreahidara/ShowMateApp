package com.andrea.showmateapp.ui.screens.discover

import android.content.Context
import androidx.compose.runtime.Immutable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.andrea.showmateapp.R
import com.andrea.showmateapp.data.model.MediaContent
import com.andrea.showmateapp.domain.repository.IInteractionRepository
import com.andrea.showmateapp.domain.repository.IShowRepository
import com.andrea.showmateapp.domain.repository.IUserRepository
import com.andrea.showmateapp.domain.usecase.GetRecommendationsUseCase
import com.andrea.showmateapp.util.GenreMapper
import com.andrea.showmateapp.util.KeywordMapper
import com.andrea.showmateapp.util.NarrativeStyleMapper
import com.andrea.showmateapp.util.NetworkMonitor
import com.andrea.showmateapp.util.Resource
import com.andrea.showmateapp.util.ExplorationEngine
import com.andrea.showmateapp.util.TemporalPatternAnalyzer
import com.andrea.showmateapp.data.model.UserProfile
import com.google.firebase.crashlytics.FirebaseCrashlytics
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import java.time.DayOfWeek
import java.time.LocalDate
import javax.inject.Inject
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import timber.log.Timber

@Immutable
data class DiscoverUiState(
    val isLoading: Boolean = true,
    val isRefreshing: Boolean = false,
    val isFromCache: Boolean = false,
    val errorMessage: String? = null,
    val heroShow: MediaContent? = null,
    val topGenreShows: List<MediaContent> = emptyList(),
    val topGenreName: String = "",
    val topGenreSubtitle: String = "",
    val isLoadingMoreTopGenre: Boolean = false,
    val secondGenreShows: List<MediaContent> = emptyList(),
    val secondGenreName: String = "",
    val secondGenreSubtitle: String = "",
    val isLoadingMoreSecondGenre: Boolean = false,
    val thirdGenreShows: List<MediaContent> = emptyList(),
    val thirdGenreName: String = "",
    val thirdGenreSubtitle: String = "",
    val similarShows: List<MediaContent> = emptyList(),
    val similarToName: String = "",
    val actorShows: List<MediaContent> = emptyList(),
    val actorName: String = "",
    val secondActorShows: List<MediaContent> = emptyList(),
    val secondActorName: String = "",
    val topRatedShows: List<MediaContent> = emptyList(),
    val topKeywordShows: List<MediaContent> = emptyList(),
    val topKeywordLabel: String = "",
    val contextPicksShows: List<MediaContent> = emptyList(),
    val contextPicksTitle: String = "",
    val dayOfWeekShows: List<MediaContent> = emptyList(),
    val dayOfWeekTitle: String = "",
    val narrativeStyleShows: List<MediaContent> = emptyList(),
    val narrativeStyleLabel: String = "",
    val hiddenGemShows: List<MediaContent> = emptyList(),
    val moodSectionShows: List<MediaContent> = emptyList(),
    val moodSectionTitle: String = "",
    val creatorShows: List<MediaContent> = emptyList(),
    val creatorName: String = "",
    val collaborativeShows: List<MediaContent> = emptyList(),
    val explorationShows: List<MediaContent> = emptyList(),
    val explorationGenreName: String = "",
    val timeTravelShows: List<MediaContent> = emptyList(),
    val secondKeywordShows: List<MediaContent> = emptyList(),
    val secondKeywordLabel: String = ""
)

@HiltViewModel
class DiscoverViewModel @Inject constructor(
    private val repository: IShowRepository,
    private val userRepository: IUserRepository,
    private val interactionRepository: IInteractionRepository,
    private val getRecommendationsUseCase: GetRecommendationsUseCase,
    private val networkMonitor: NetworkMonitor,
    @ApplicationContext private val context: Context
) : ViewModel() {

    private val _uiState = MutableStateFlow(DiscoverUiState())
    val uiState: StateFlow<DiscoverUiState> = _uiState.asStateFlow()

    private var currentTopGenreId = "18"
    private var topGenrePage = 1
    private var topGenreTotalPages = 1
    private var currentSecondGenreId = "35"
    private var secondGenrePage = 1
    private var secondGenreTotalPages = 1

    init {
        loadDiscoverContent()
        observeInteractions()
    }

    private fun observeInteractions() {
        viewModelScope.launch {
            try {
                interactionRepository.getInteractedMediaIdsFlow().collect { interactedIds ->
                    if (interactedIds.isEmpty()) return@collect

                    _uiState.update { state ->
                        state.copy(
                            heroShow = state.heroShow?.takeIf { it.id !in interactedIds },
                            topGenreShows = state.topGenreShows.filter { it.id !in interactedIds },
                            secondGenreShows = state.secondGenreShows.filter { it.id !in interactedIds },
                            thirdGenreShows = state.thirdGenreShows.filter { it.id !in interactedIds },
                            similarShows = state.similarShows.filter { it.id !in interactedIds },
                            actorShows = state.actorShows.filter { it.id !in interactedIds },
                            secondActorShows = state.secondActorShows.filter { it.id !in interactedIds },
                            topRatedShows = state.topRatedShows.filter { it.id !in interactedIds },
                            topKeywordShows = state.topKeywordShows.filter { it.id !in interactedIds },
                            secondKeywordShows = state.secondKeywordShows.filter { it.id !in interactedIds },
                            contextPicksShows = state.contextPicksShows.filter { it.id !in interactedIds },
                            dayOfWeekShows = state.dayOfWeekShows.filter { it.id !in interactedIds },
                            narrativeStyleShows = state.narrativeStyleShows.filter { it.id !in interactedIds },
                            hiddenGemShows = state.hiddenGemShows.filter { it.id !in interactedIds },
                            moodSectionShows = state.moodSectionShows.filter { it.id !in interactedIds },
                            creatorShows = state.creatorShows.filter { it.id !in interactedIds },
                            collaborativeShows = state.collaborativeShows.filter { it.id !in interactedIds },
                            explorationShows = state.explorationShows.filter { it.id !in interactedIds },
                            timeTravelShows = state.timeTravelShows.filter { it.id !in interactedIds }
                        )
                    }
                }
            } catch (e: Exception) {
                if (e is CancellationException) throw e
                Timber.e(e, "observeInteractions flow failed")
            }
        }

        viewModelScope.launch {
            userRepository.getUserProfileFlow()
                .distinctUntilChanged { old: UserProfile?, new: UserProfile? ->
                    old?.genreScores == new?.genreScores &&
                    old?.narrativeStyleScores == new?.narrativeStyleScores &&
                    old?.preferredActors == new?.preferredActors
                }
                .collect { profile ->
                    if (profile != null && !_uiState.value.isLoading && !_uiState.value.isRefreshing) {
                        rescoreExistingContent(profile)
                    }
                }
        }
    }

    private fun rescoreExistingContent(cachedProfile: UserProfile? = null) {
        viewModelScope.launch {
            val currentState = _uiState.value

            val heroScored = currentState.heroShow?.let { getRecommendationsUseCase.scoreForDetail(it) }
            val topGenreScored = getRecommendationsUseCase.scoreShows(currentState.topGenreShows, cachedProfile)
            val secondGenreScored = getRecommendationsUseCase.scoreShows(currentState.secondGenreShows, cachedProfile)
            val thirdGenreScored = getRecommendationsUseCase.scoreShows(currentState.thirdGenreShows, cachedProfile)
            val similarScored = getRecommendationsUseCase.scoreShows(currentState.similarShows, cachedProfile)
            val actorScored = getRecommendationsUseCase.scoreShows(currentState.actorShows, cachedProfile)
            val secondActorScored = getRecommendationsUseCase.scoreShows(currentState.secondActorShows, cachedProfile)
            val topRatedScored = getRecommendationsUseCase.scoreShows(currentState.topRatedShows, cachedProfile)
            val topKeywordScored = getRecommendationsUseCase.scoreShows(currentState.topKeywordShows, cachedProfile)
            val contextPicksScored = getRecommendationsUseCase.scoreShows(currentState.contextPicksShows, cachedProfile)
            val dayOfWeekScored = getRecommendationsUseCase.scoreShows(currentState.dayOfWeekShows, cachedProfile)
            val narrativeStyleScored = getRecommendationsUseCase.scoreShows(currentState.narrativeStyleShows, cachedProfile)
            val hiddenGemScored = getRecommendationsUseCase.scoreShows(currentState.hiddenGemShows, cachedProfile)
            val moodSectionScored = getRecommendationsUseCase.scoreShows(currentState.moodSectionShows, cachedProfile)
            val creatorScored = getRecommendationsUseCase.scoreShows(currentState.creatorShows, cachedProfile)
            val collaborativeScored = getRecommendationsUseCase.scoreShows(currentState.collaborativeShows, cachedProfile)
            val explorationScored = getRecommendationsUseCase.scoreShows(currentState.explorationShows, cachedProfile)
            val timeTravelScored = getRecommendationsUseCase.scoreShows(currentState.timeTravelShows, cachedProfile)

            _uiState.update { state ->
                state.copy(
                    heroShow = heroScored,
                    topGenreShows = topGenreScored,
                    secondGenreShows = secondGenreScored,
                    thirdGenreShows = thirdGenreScored,
                    similarShows = similarScored,
                    actorShows = actorScored,
                    secondActorShows = secondActorScored,
                    topRatedShows = topRatedScored,
                    topKeywordShows = topKeywordScored,
                    contextPicksShows = contextPicksScored,
                    dayOfWeekShows = dayOfWeekScored,
                    narrativeStyleShows = narrativeStyleScored,
                    hiddenGemShows = hiddenGemScored,
                    moodSectionShows = moodSectionScored,
                    creatorShows = creatorScored,
                    collaborativeShows = collaborativeScored,
                    explorationShows = explorationScored,
                    timeTravelShows = timeTravelScored
                )
            }
        }
    }

    fun retry() {
        loadDiscoverContent(isRefresh = false)
    }

    fun refresh() {
        loadDiscoverContent(isRefresh = true)
    }

    private fun getGenreSubtitle(genreId: String): String = when (genreId) {
        "10759" -> context.getString(R.string.discover_genre_action)
        "16" -> context.getString(R.string.discover_genre_animation)
        "35" -> context.getString(R.string.discover_genre_comedy)
        "80" -> context.getString(R.string.discover_genre_crime)
        "99" -> context.getString(R.string.discover_genre_documentary)
        "18" -> context.getString(R.string.discover_genre_drama)
        "10751" -> context.getString(R.string.discover_genre_family)
        "10762" -> context.getString(R.string.discover_genre_kids)
        "9648" -> context.getString(R.string.discover_genre_mystery)
        "10763" -> context.getString(R.string.discover_genre_news)
        "10764" -> context.getString(R.string.discover_genre_reality)
        "10765" -> context.getString(R.string.discover_genre_scifi)
        "10766" -> context.getString(R.string.discover_genre_soap)
        "10767" -> context.getString(R.string.discover_genre_talk)
        "10768" -> context.getString(R.string.discover_genre_war)
        "37" -> context.getString(R.string.discover_genre_western)
        else -> context.getString(R.string.discover_genre_comedy)
    }

    fun loadMoreTopGenre() = loadMoreGenre(
        isAlreadyLoading = { _uiState.value.isLoadingMoreTopGenre },
        currentPage = { topGenrePage },
        totalPages = { topGenreTotalPages },
        genreId = { currentTopGenreId },
        setLoading = { _uiState.update { it.copy(isLoadingMoreTopGenre = true) } },
        onSuccess = { newPage, newTotal, scored ->
            topGenrePage = newPage
            topGenreTotalPages = newTotal
            _uiState.update { state -> state.copy(topGenreShows = (state.topGenreShows + scored).distinctBy { it.id }, isLoadingMoreTopGenre = false) }
        },
        onError = { _uiState.update { it.copy(isLoadingMoreTopGenre = false) } }
    )

    fun loadMoreSecondGenre() = loadMoreGenre(
        isAlreadyLoading = { _uiState.value.isLoadingMoreSecondGenre },
        currentPage = { secondGenrePage },
        totalPages = { secondGenreTotalPages },
        genreId = { currentSecondGenreId },
        setLoading = { _uiState.update { it.copy(isLoadingMoreSecondGenre = true) } },
        onSuccess = { newPage, newTotal, scored ->
            secondGenrePage = newPage
            secondGenreTotalPages = newTotal
            _uiState.update { state -> state.copy(secondGenreShows = (state.secondGenreShows + scored).distinctBy { it.id }, isLoadingMoreSecondGenre = false) }
        },
        onError = { _uiState.update { it.copy(isLoadingMoreSecondGenre = false) } }
    )

    private fun loadMoreGenre(
        isAlreadyLoading: () -> Boolean,
        currentPage: () -> Int,
        totalPages: () -> Int,
        genreId: () -> String,
        setLoading: () -> Unit,
        onSuccess: (newPage: Int, newTotal: Int, scored: List<MediaContent>) -> Unit,
        onError: () -> Unit
    ) {
        if (isAlreadyLoading()) return
        if (currentPage() >= totalPages()) return
        viewModelScope.launch {
            setLoading()
            try {
                val result = repository.discoverShowsPaged(genreId = genreId(), sortBy = "popularity.desc", page = currentPage() + 1)
                if (result is Resource.Success) {
                    onSuccess(currentPage() + 1, result.data.second, getRecommendationsUseCase.scoreShows(result.data.first))
                } else {
                    onError()
                }
            } catch (e: Exception) {
                if (e is CancellationException) throw e
                onError()
            }
        }
    }

    private fun loadDiscoverContent(isRefresh: Boolean = false) {
        viewModelScope.launch {
            topGenrePage = 1
            topGenreTotalPages = 1
            secondGenrePage = 1
            secondGenreTotalPages = 1
            _uiState.update {
                if (isRefresh) {
                    it.copy(isRefreshing = true, errorMessage = null)
                } else {
                    it.copy(isLoading = true, errorMessage = null)
                }
            }
            try {
                val profile = userRepository.getUserProfile()
                val sortedGenres = profile?.genreScores?.entries?.sortedByDescending { it.value } ?: emptyList()

                if (profile != null && (sortedGenres.isEmpty() || sortedGenres.all { it.value <= 0f })) {
                    Timber.w("Genre scores desynchronized or empty for user ${profile.userId}. Possible onboarding/profile sync issue.")
                }

                var topGenreId = "18"
                var topName = "Drama"
                var topSubtitle = ""
                var secondGenreId = "35"
                var secondName = "Comedia"
                var secondSubtitle = ""
                var thirdGenreId: String? = null
                var thirdName = ""
                var thirdSubtitle = ""
                if (sortedGenres.isNotEmpty()) {
                    topGenreId = sortedGenres[0].key
                    topName = GenreMapper.getGenreName(topGenreId)
                    topSubtitle = getGenreSubtitle(topGenreId)
                    if (sortedGenres.size > 1) {
                        secondGenreId = sortedGenres[1].key
                    } else {

                        secondGenreId = when (topGenreId) {
                            "18" -> "35"; "35" -> "18"; "10765" -> "10759"
                            "9648" -> "80"; "80" -> "9648"; "10759" -> "53"
                            "53" -> "9648"; "16" -> "35"; "99" -> "18"
                            else -> "18"
                        }
                    }
                    secondName = GenreMapper.getGenreName(secondGenreId)
                    secondSubtitle = getGenreSubtitle(secondGenreId)
                    if (sortedGenres.size > 2) {
                        thirdGenreId = sortedGenres[2].key
                        thirdName = GenreMapper.getGenreName(thirdGenreId!!)
                        thirdSubtitle = getGenreSubtitle(thirdGenreId!!)
                    }
                }

                currentTopGenreId = topGenreId
                currentSecondGenreId = secondGenreId

                val kwResult = KeywordMapper.getTopMappedKeyword(
                    profile?.preferredKeywords ?: emptyMap()
                )
                val kwId = kwResult?.second
                val kwLabel = kwResult?.third

                val sortedActors = profile?.preferredActors?.entries?.sortedByDescending { it.value } ?: emptyList()
                val actorIdStr1 = sortedActors.getOrNull(0)?.key
                val actorId1 = actorIdStr1?.toIntOrNull()
                val actorIdStr2 = sortedActors.getOrNull(1)?.key
                val actorId2 = actorIdStr2?.toIntOrNull()

                val likedMedia = profile?.likedMediaIds?.toList() ?: emptyList()
                val topRatedMedia = profile?.ratings?.filterValues { it >= 4f }
                    ?.keys?.mapNotNull { it.toIntOrNull() } ?: emptyList()
                val targetId = (likedMedia + topRatedMedia).distinct().randomOrNull()

                val topNarrativeStyle = profile?.narrativeStyleScores?.filter { it.value > 0 }?.maxByOrNull { it.value }
                val (nsGenreId, nsKeyword) = if (topNarrativeStyle != null) {
                    narrativeStyleToDiscoverParams(
                        topNarrativeStyle.key
                    )
                } else {
                    null to null
                }
                val (moodGenreId, moodTitle) = if (topNarrativeStyle != null) {
                    narrativeStyleToMoodSection(
                        topNarrativeStyle.key
                    )
                } else {
                    null to ""
                }

                val creatorEntry = profile?.preferredCreators?.filter { it.value > 0 }?.maxByOrNull { it.value }
                val creatorId = creatorEntry?.key?.toIntOrNull()

                val res1Def = async { repository.discoverShowsPaged(genreId = topGenreId, page = 1) }
                val res1Page2Def = async { repository.discoverShowsPaged(genreId = topGenreId, page = 2) }
                val res2Def = async { repository.discoverShowsPaged(genreId = secondGenreId, page = 1) }
                val res2Page2Def = async { repository.discoverShowsPaged(genreId = secondGenreId, page = 2) }
                val topRatedDef =
                    async {
                        repository.discoverShows(
                            genreId = topGenreId,
                            minRating = 7.5f,
                            sortBy = "vote_average.desc"
                        )
                    }
                val res3Def = thirdGenreId?.let { id -> async { repository.getShowsByGenres(id) } }
                val person1Def = actorId1?.let { id -> async { repository.getPersonDetails(id) } }
                val actor1ShowsDef = actorIdStr1?.let { id -> async { repository.discoverShows(withCast = id) } }
                val person2Def = actorId2?.let { id -> async { repository.getPersonDetails(id) } }
                val actor2ShowsDef = actorIdStr2?.let { id -> async { repository.discoverShows(withCast = id) } }

                val kwResults = KeywordMapper.getTopMappedKeywords(profile?.preferredKeywords ?: emptyMap(), limit = 2)
                val firstKw = kwResults.getOrNull(0)
                val secondKw = kwResults.getOrNull(1)

                val kw1ShowsDef = firstKw?.let { it -> async { repository.discoverShows(keywords = it.second) } }
                val kw2ShowsDef = secondKw?.let { it -> async { repository.discoverShows(keywords = it.second) } }
                val timeTravelDef = async { repository.discoverShows(keywords = "4363") }

                val unexploredGenreId = ExplorationEngine.unexploredGenres(profile ?: UserProfile(""), setOf(10759, 16, 35, 80, 99, 18, 10751, 9648, 10765, 37, 53)).randomOrNull()
                val explorationShowsDef = unexploredGenreId?.let { id -> async { repository.discoverShows(genreId = id.toString(), minRating = 7.0f) } }

                val recsDef = async { getRecommendationsUseCase.execute() }
                val targetDetailsDef = targetId?.let { id -> async { repository.getShowDetails(id) } }
                val similarShowsDef = targetId?.let { id -> async { repository.getSimilarShows(id) } }
                val nsShowsDef = if (nsGenreId != null || nsKeyword != null) {
                    async {
                        repository.discoverShows(genreId = nsGenreId, keywords = nsKeyword)
                    }
                } else {
                    null
                }
                val moodShowsDef = if (moodGenreId != null && moodTitle.isNotEmpty()) {
                    async {
                        repository.discoverShows(genreId = moodGenreId, minRating = 7.0f)
                    }
                } else {
                    null
                }
                val creatorPersonDef = creatorId?.let { id -> async { repository.getPersonDetails(id) } }
                val creatorShowsDef = creatorEntry?.key?.let { key ->
                    async {
                        repository.discoverShows(
                            withCrew = key
                        )
                    }
                }

                val res1Res = res1Def.await()
                val res1Page2Res = res1Page2Def.await()
                val res2Res = res2Def.await()
                val res2Page2Res = res2Page2Def.await()

                val res1List = (res1Res as? Resource.Success)?.data?.first ?: emptyList()
                val res1Page2List = (res1Page2Res as? Resource.Success)?.data?.first ?: emptyList()
                val topGenreFullList = (res1List + res1Page2List).distinctBy { it.id }

                val res2List = (res2Res as? Resource.Success)?.data?.first ?: emptyList()
                val res2Page2List = (res2Page2Res as? Resource.Success)?.data?.first ?: emptyList()
                val secondGenreFullList = (res2List + res2Page2List).distinctBy { it.id }

                val topRatedRes = topRatedDef.await()
                val res3 = res3Def?.await()
                val person1 = person1Def?.await()
                val actor1Shows = actor1ShowsDef?.await()
                val person2 = person2Def?.await()
                val actor2Shows = actor2ShowsDef?.await()
                val recommendations = recsDef.await()
                val targetDetails = targetDetailsDef?.await()
                val similarShowsRaw = similarShowsDef?.await()
                val nsShows = nsShowsDef?.await()
                val moodShows = moodShowsDef?.await()
                val creatorPerson = creatorPersonDef?.await()
                val creatorShowsRaw = creatorShowsDef?.await()
                val kw1ShowsRes = kw1ShowsDef?.await()
                val kw2ShowsRes = kw2ShowsDef?.await()
                val timeTravelRes = timeTravelDef.await()
                val explorationRes = explorationShowsDef?.await()

                val topGenreShows = getRecommendationsUseCase.scoreShows(
                    topGenreFullList.shuffled().take(20), profile
                )

                val secondGenreShows = getRecommendationsUseCase.scoreShows(
                    secondGenreFullList.shuffled().take(20), profile
                )
                val topRatedShows = if (topRatedRes is Resource.Success<List<MediaContent>> && topRatedRes.data.isNotEmpty()) {
                    getRecommendationsUseCase.scoreShows(topRatedRes.data.take(20), profile)
                } else {
                    emptyList()
                }
                val thirdGenreShows = if (res3 is Resource.Success<List<MediaContent>>) {
                    getRecommendationsUseCase.scoreShows(
                        res3.data.shuffled().take(20), profile
                    )
                } else {
                    emptyList()
                }
                val topKeywordShows = if (kw1ShowsRes is Resource.Success<List<MediaContent>> && kw1ShowsRes.data.isNotEmpty()) {
                    getRecommendationsUseCase.scoreShows(
                        kw1ShowsRes.data.shuffled().take(20), profile
                    )
                } else {
                    emptyList()
                }
                val secondKeywordShows = if (kw2ShowsRes is Resource.Success<List<MediaContent>> && kw2ShowsRes.data.isNotEmpty()) {
                    getRecommendationsUseCase.scoreShows(
                        kw2ShowsRes.data.shuffled().take(20), profile
                    )
                } else {
                    emptyList()
                }
                val timeTravelShows = if (timeTravelRes is Resource.Success<List<MediaContent>> && timeTravelRes.data.isNotEmpty()) {
                    getRecommendationsUseCase.scoreShows(
                        timeTravelRes.data.shuffled().take(20), profile
                    )
                } else {
                    emptyList()
                }
                val explorationShows = if (explorationRes is Resource.Success<List<MediaContent>> && explorationRes.data.isNotEmpty()) {
                    getRecommendationsUseCase.scoreShows(
                        explorationRes.data.shuffled().take(20), profile
                    )
                } else {
                    emptyList()
                }
                val explorationGenreName = unexploredGenreId?.let { GenreMapper.getGenreName(it.toString()) } ?: ""

                val actorName =
                    if (person1 is Resource.Success && actor1Shows is Resource.Success) person1.data.name else ""
                val actorShows = if (person1 is Resource.Success && actor1Shows is Resource.Success) {
                    getRecommendationsUseCase.scoreShows(
                        actor1Shows.data.shuffled().take(20), profile
                    )
                } else {
                    emptyList()
                }
                val secondActorName =
                    if (person2 is Resource.Success && actor2Shows is Resource.Success) person2.data.name else ""
                val secondActorShows = if (person2 is Resource.Success && actor2Shows is Resource.Success) {
                    getRecommendationsUseCase.scoreShows(
                        actor2Shows.data.shuffled().take(20), profile
                    )
                } else {
                    emptyList()
                }

                val hero = recommendations.firstOrNull() ?: topGenreShows.randomOrNull()
                val similarToName = if (targetDetails is Resource.Success) targetDetails.data.name else ""
                val similarShows = if (targetDetails is Resource.Success && !similarShowsRaw.isNullOrEmpty()) {
                    getRecommendationsUseCase.scoreShows(
                        similarShowsRaw.shuffled().take(20), profile
                    )
                } else {
                    emptyList()
                }

                val hiddenGems = recommendations.filter { it.voteCount in 1..499 && it.affinityScore >= 5.5f }.take(20)

                val temporalPattern = TemporalPatternAnalyzer.analyze(profile?.viewingHistory ?: emptyList())
                val isBinger = temporalPattern.avgEpisodesPerSession >= 3f
                val contextPicks = if (isBinger) {
                    recommendations.filter { (it.numberOfSeasons ?: 0) >= 3 }.take(20)
                } else {
                    recommendations
                        .filter { (it.numberOfSeasons ?: 1) <= 2 && it.status in listOf("Ended", "Canceled") }
                        .take(20)
                }
                val contextTitle = if (contextPicks.isNotEmpty()) {
                    (
                        if (isBinger) {
                            context.getString(
                                R.string.discover_context_binger
                            )
                        } else {
                            context.getString(R.string.discover_context_casual)
                        }
                        )
                } else {
                    ""
                }

                val (dayTitle, dayShows) = buildDayOfWeekSection(recommendations)

                val narrativeStyleLabel = if (topNarrativeStyle != null) {
                    context.getString(
                        R.string.discover_narrative_style_prefix,
                        NarrativeStyleMapper.getStyleLabel(topNarrativeStyle.key)
                    )
                } else {
                    ""
                }
                val narrativeStyleShows = if (nsShows is Resource.Success && nsShows.data.isNotEmpty()) {
                    getRecommendationsUseCase.scoreShows(
                        nsShows.data.shuffled().take(10), profile
                    )
                } else {
                    emptyList()
                }

                val moodSectionShows = if (moodShows is Resource.Success && moodShows.data.isNotEmpty()) {
                    getRecommendationsUseCase.scoreShows(
                        moodShows.data.shuffled().take(10), profile
                    )
                } else {
                    emptyList()
                }

                val creatorLoaded = creatorPerson is Resource.Success &&
                    creatorShowsRaw is Resource.Success && creatorShowsRaw.data.isNotEmpty()
                val creatorName = if (creatorLoaded) {
                    (creatorPerson as Resource.Success).data.name
                } else {
                    ""
                }
                val creatorShowsList = if (creatorLoaded) {
                    val raw = (creatorShowsRaw as Resource.Success).data
                    getRecommendationsUseCase.scoreShows(raw.shuffled().take(10), profile)
                } else {
                    emptyList()
                }

                val heroId = hero?.id
                val collaborativeShows = recommendations.filter {
                    it.id != heroId && it.voteCount in 100..4999
                }.sortedByDescending { it.affinityScore }.take(12)

                val isOnline = networkMonitor.isOnline.firstOrNull() ?: true
                _uiState.update { current ->
                    val hasContent = hero != null || topGenreShows.isNotEmpty() ||
                        secondGenreShows.isNotEmpty() || topRatedShows.isNotEmpty()
                    current.copy(
                        isLoading = false,
                        isRefreshing = false,
                        isFromCache = !isOnline && hasContent,
                        heroShow = hero,
                        topGenreName = topName,
                        topGenreSubtitle = topSubtitle,
                        secondGenreName = secondName,
                        secondGenreSubtitle = secondSubtitle,
                        thirdGenreName = thirdName,
                        thirdGenreSubtitle = thirdSubtitle,
                        topGenreShows = topGenreShows,
                        secondGenreShows = secondGenreShows,
                        thirdGenreShows = thirdGenreShows,
                        topRatedShows = topRatedShows,
                        topKeywordLabel = kwLabel ?: "",
                        topKeywordShows = topKeywordShows,
                        actorName = actorName,
                        actorShows = actorShows,
                        secondActorName = secondActorName,
                        secondActorShows = secondActorShows,
                        similarToName = similarToName,
                        similarShows = similarShows,
                        hiddenGemShows = hiddenGems,
                        contextPicksTitle = contextTitle,
                        contextPicksShows = contextPicks,
                        dayOfWeekTitle = dayTitle,
                        dayOfWeekShows = dayShows,
                        narrativeStyleLabel = narrativeStyleLabel,
                        narrativeStyleShows = narrativeStyleShows,
                        moodSectionTitle = moodTitle,
                        moodSectionShows = moodSectionShows,
                        creatorName = creatorName,
                        creatorShows = creatorShowsList,
                        collaborativeShows = collaborativeShows,
                        timeTravelShows = timeTravelShows,
                        explorationShows = explorationShows,
                        explorationGenreName = explorationGenreName,
                        secondKeywordLabel = secondKw?.third ?: "",
                        secondKeywordShows = secondKeywordShows,
                        errorMessage = if (!hasContent) {
                            "No se pudo cargar el contenido. Por favor, reintenta."
                        } else {
                            null
                        }
                    )
                }
            } catch (e: Exception) {
                if (e is CancellationException) throw e
                Timber.e(e, "Error loading discover content")
                FirebaseCrashlytics.getInstance().recordException(e)
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        isRefreshing = false,
                        errorMessage = "Error al cargar el contenido. Comprueba tu conexión e inténtalo de nuevo."
                    )
                }
            }
        }
    }

    private fun buildDayOfWeekSection(recommendations: List<MediaContent>): Pair<String, List<MediaContent>> {
        return when (LocalDate.now().dayOfWeek) {
            DayOfWeek.MONDAY, DayOfWeek.TUESDAY, DayOfWeek.WEDNESDAY -> {
                val shows = recommendations.filter {
                    (it.episodeRunTime?.firstOrNull() ?: 45) <= 30
                }.take(10)
                context.getString(R.string.discover_day_after_work) to shows
            }
            DayOfWeek.THURSDAY -> {
                val shows = recommendations.filter {
                    (it.episodeRunTime?.firstOrNull() ?: 45) <= 45
                }.take(10)
                context.getString(R.string.discover_day_friday_eve) to shows
            }
            DayOfWeek.FRIDAY -> {
                val shows = recommendations.filter {
                    it.safeGenreIds.any { id -> id in listOf(10759, 53, 9648) }
                }.take(10)
                context.getString(R.string.discover_day_friday) to shows
            }
            DayOfWeek.SATURDAY -> {
                val shows = recommendations.filter { (it.numberOfSeasons ?: 0) >= 2 }.take(10)
                context.getString(R.string.discover_day_saturday) to shows
            }
            DayOfWeek.SUNDAY -> {
                val shows = recommendations.filter {
                    it.status in listOf("Ended", "Canceled") && (it.numberOfSeasons ?: 4) <= 3
                }.take(10)
                context.getString(R.string.discover_day_sunday) to shows
            }
            else -> "" to emptyList()
        }
    }

    private fun narrativeStyleToDiscoverParams(style: String): Pair<String?, String?> = when (style) {
        "narrativa_compleja" -> Pair("9648", null)
        "protagonista_detective" -> Pair("80", null)
        "protagonista_antihero" -> Pair("18|80", null)
        "protagonista_genio" -> Pair("18", null)
        "tono_oscuro" -> Pair("53", null)
        "tono_emocional" -> Pair("18", null)
        "tono_ligero" -> Pair("35", null)
        "ritmo_intenso" -> Pair("10759", null)
        "ritmo_lento" -> Pair("18", null)
        "ritmo_episodico" -> Pair("35", null)
        "ritmo_largo" -> Pair("18|80", null)
        else -> Pair(null, null)
    }

    private fun narrativeStyleToMoodSection(style: String): Pair<String?, String> = when (style) {
        "tono_oscuro" -> Pair("53|80", context.getString(R.string.discover_mood_suspense))
        "tono_emocional" -> Pair("18", context.getString(R.string.discover_mood_emotional))
        "tono_ligero" -> Pair("35", context.getString(R.string.discover_mood_light))
        "ritmo_intenso" -> Pair("10759", context.getString(R.string.discover_mood_action))
        "narrativa_compleja" -> Pair("9648", context.getString(R.string.discover_mood_mystery))
        "protagonista_detective" -> Pair("80", context.getString(R.string.discover_mood_crime))
        "protagonista_antihero" -> Pair("18|80", context.getString(R.string.discover_mood_antihero))
        "protagonista_genio" -> Pair("18", context.getString(R.string.discover_mood_genius))
        "ritmo_lento" -> Pair("18", context.getString(R.string.discover_mood_slow))
        else -> Pair(null, "")
    }
}
