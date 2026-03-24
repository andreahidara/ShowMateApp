package com.example.showmateapp.ui.screens.discover

import android.content.Context
import android.util.Log
import com.google.firebase.crashlytics.FirebaseCrashlytics
import androidx.lifecycle.ViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import com.example.showmateapp.R
import androidx.lifecycle.viewModelScope
import com.example.showmateapp.data.network.MediaContent
import com.example.showmateapp.data.repository.UserRepository
import com.example.showmateapp.data.repository.ShowRepository
import com.example.showmateapp.util.Resource
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.async
import kotlinx.coroutines.launch
import javax.inject.Inject
import com.example.showmateapp.domain.usecase.GetRecommendationsUseCase
import com.example.showmateapp.util.GenreMapper
import com.example.showmateapp.util.KeywordMapper
import com.example.showmateapp.util.NarrativeStyleMapper
import com.example.showmateapp.util.TemporalPatternAnalyzer
import java.time.DayOfWeek
import java.time.LocalDate

data class DiscoverUiState(
    val isLoading: Boolean = true,
    val isRefreshing: Boolean = false,
    val errorMessage: String? = null,
    val heroShow: MediaContent? = null,
    val topGenreShows: List<MediaContent> = emptyList(),
    val topGenreName: String = "",
    val secondGenreShows: List<MediaContent> = emptyList(),
    val secondGenreName: String = "",
    val thirdGenreShows: List<MediaContent> = emptyList(),
    val thirdGenreName: String = "",
    val similarShows: List<MediaContent> = emptyList(),
    val similarToName: String = "",
    val timeTravelShows: List<MediaContent> = emptyList(),
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
    val explorationShows: List<MediaContent> = emptyList(),
    val explorationGenreName: String = "",
    val creatorShows: List<MediaContent> = emptyList(),
    val creatorName: String = "",
    val collaborativeShows: List<MediaContent> = emptyList()
)

@HiltViewModel
class DiscoverViewModel @Inject constructor(
    private val repository: ShowRepository,
    private val userRepository: UserRepository,
    private val getRecommendationsUseCase: GetRecommendationsUseCase,
    @ApplicationContext private val context: Context
) : ViewModel() {

    private val _uiState = MutableStateFlow(DiscoverUiState())
    val uiState: StateFlow<DiscoverUiState> = _uiState.asStateFlow()

    init {
        loadDiscoverContent()
    }

    fun retry() {
        loadDiscoverContent(isRefresh = false)
    }

    fun refresh() {
        loadDiscoverContent(isRefresh = true)
    }

    private fun loadDiscoverContent(isRefresh: Boolean = false) {
        viewModelScope.launch {
            _uiState.update {
                if (isRefresh) it.copy(isRefreshing = true, errorMessage = null)
                else it.copy(isLoading = true, errorMessage = null)
            }
            try {
                val profile = userRepository.getUserProfile()
                val sortedGenres = profile?.genreScores?.entries?.sortedByDescending { it.value } ?: emptyList()

                var topGenreId = "18"; var topName = "Drama"
                var secondGenreId = "35"; var secondName = "Comedia"
                var thirdGenreId: String? = null; var thirdName = ""
                if (sortedGenres.isNotEmpty()) {
                    topGenreId = sortedGenres[0].key
                    topName = GenreMapper.getGenreName(topGenreId)
                    if (sortedGenres.size > 1) {
                        secondGenreId = sortedGenres[1].key
                        secondName = GenreMapper.getGenreName(secondGenreId)
                    }
                    if (sortedGenres.size > 2) {
                        thirdGenreId = sortedGenres[2].key
                        thirdName = GenreMapper.getGenreName(thirdGenreId)
                    }
                }

                val kwResult   = KeywordMapper.getTopMappedKeyword(profile?.preferredKeywords ?: emptyMap(), excludeKeywordId = "4363")
                val kwId       = kwResult?.second
                val kwLabel    = kwResult?.third

                val sortedActors  = profile?.preferredActors?.entries?.sortedByDescending { it.value } ?: emptyList()
                val actorIdStr1   = sortedActors.getOrNull(0)?.key
                val actorId1      = actorIdStr1?.toIntOrNull()
                val actorIdStr2   = sortedActors.getOrNull(1)?.key
                val actorId2      = actorIdStr2?.toIntOrNull()

                val likedMedia    = profile?.likedMediaIds?.toList() ?: emptyList()
                val topRatedMedia = profile?.ratings?.filterValues { it >= 4f }?.keys?.mapNotNull { it.toIntOrNull() } ?: emptyList()
                val targetId      = (likedMedia + topRatedMedia).distinct().randomOrNull()

                val topNarrativeStyle = profile?.narrativeStyleScores?.filter { it.value > 0 }?.maxByOrNull { it.value }
                val (nsGenreId, nsKeyword) = if (topNarrativeStyle != null) narrativeStyleToDiscoverParams(topNarrativeStyle.key) else null to null
                val (moodGenreId, moodTitle) = if (topNarrativeStyle != null) narrativeStyleToMoodSection(topNarrativeStyle.key) else null to ""

                val creatorEntry  = profile?.preferredCreators?.filter { it.value > 0 }?.maxByOrNull { it.value }
                val creatorId     = creatorEntry?.key?.toIntOrNull()

                val totalScore    = profile?.genreScores?.values?.filter { it > 0 }?.sum() ?: 0f
                val topScore      = sortedGenres.firstOrNull()?.value ?: 0f
                val underexploredGenre = if (totalScore > 0 && sortedGenres.isNotEmpty() && topScore / totalScore > 0.45f) {
                    profile?.genreScores?.filter { it.value > 0 }?.entries
                        ?.sortedBy { it.value }
                        ?.firstOrNull { it.key != sortedGenres.first().key }
                } else null

                val res1Def          = async { repository.getShowsByGenres(topGenreId) }
                val res2Def          = async { repository.getShowsByGenres(secondGenreId) }
                val timeTravelDef    = async { repository.discoverShows(keywords = "4363") }
                val topRatedDef      = async { repository.discoverShows(genreId = topGenreId, minRating = 8.0f, sortBy = "vote_average.desc") }
                val res3Def          = thirdGenreId?.let { id -> async { repository.getShowsByGenres(id) } }
                val kwShowsDef       = kwId?.let { id -> async { repository.discoverShows(keywords = id) } }
                val person1Def       = actorId1?.let { id -> async { repository.getPersonDetails(id) } }
                val actor1ShowsDef   = actorIdStr1?.let { id -> async { repository.discoverShows(withCast = id) } }
                val person2Def       = actorId2?.let { id -> async { repository.getPersonDetails(id) } }
                val actor2ShowsDef   = actorIdStr2?.let { id -> async { repository.discoverShows(withCast = id) } }
                val recsDef          = async { getRecommendationsUseCase.execute() }
                val targetDetailsDef = targetId?.let { id -> async { repository.getShowDetails(id) } }
                val similarShowsDef  = targetId?.let { id -> async { repository.getSimilarShows(id) } }
                val nsShowsDef       = if (nsGenreId != null || nsKeyword != null) async { repository.discoverShows(genreId = nsGenreId, keywords = nsKeyword) } else null
                val moodShowsDef     = if (moodGenreId != null && moodTitle.isNotEmpty()) async { repository.discoverShows(genreId = moodGenreId, minRating = 7.0f) } else null
                val creatorPersonDef = creatorId?.let { id -> async { repository.getPersonDetails(id) } }
                val creatorShowsDef  = creatorEntry?.key?.let { key -> async { repository.discoverShows(withCrew = key) } }
                val explorationDef   = underexploredGenre?.key?.let { key -> async { repository.getShowsByGenres(key) } }

                val res1            = res1Def.await()
                val res2            = res2Def.await()
                val timeTravelRes   = timeTravelDef.await()
                val topRatedRes     = topRatedDef.await()
                val res3            = res3Def?.await()
                val kwShows         = kwShowsDef?.await()
                val person1         = person1Def?.await()
                val actor1Shows     = actor1ShowsDef?.await()
                val person2         = person2Def?.await()
                val actor2Shows     = actor2ShowsDef?.await()
                val recommendations = recsDef.await()
                val targetDetails   = targetDetailsDef?.await()
                val similarShowsRaw = similarShowsDef?.await()
                val nsShows         = nsShowsDef?.await()
                val moodShows       = moodShowsDef?.await()
                val creatorPerson   = creatorPersonDef?.await()
                val creatorShowsRaw = creatorShowsDef?.await()
                val explorationRaw  = explorationDef?.await()

                val topGenreShows    = if (res1 is Resource.Success) getRecommendationsUseCase.scoreShows(res1.data.shuffled().take(10)) else emptyList()
                val secondGenreShows = if (res2 is Resource.Success) getRecommendationsUseCase.scoreShows(res2.data.shuffled().take(10)) else emptyList()
                val timeTravelShows  = if (timeTravelRes is Resource.Success) getRecommendationsUseCase.scoreShows(timeTravelRes.data.shuffled().take(10)) else emptyList()
                val topRatedShows    = if (topRatedRes is Resource.Success && topRatedRes.data.isNotEmpty()) topRatedRes.data.take(10).sortedByDescending { it.voteAverage } else emptyList()
                val thirdGenreShows  = if (res3 is Resource.Success) getRecommendationsUseCase.scoreShows(res3.data.shuffled().take(10)) else emptyList()
                val topKeywordShows  = if (kwShows is Resource.Success && kwShows.data.isNotEmpty()) getRecommendationsUseCase.scoreShows(kwShows.data.shuffled().take(10)) else emptyList()

                val actorName        = if (person1 is Resource.Success && actor1Shows is Resource.Success) person1.data.name else ""
                val actorShows       = if (person1 is Resource.Success && actor1Shows is Resource.Success) getRecommendationsUseCase.scoreShows(actor1Shows.data.shuffled().take(10)) else emptyList()
                val secondActorName  = if (person2 is Resource.Success && actor2Shows is Resource.Success) person2.data.name else ""
                val secondActorShows = if (person2 is Resource.Success && actor2Shows is Resource.Success) getRecommendationsUseCase.scoreShows(actor2Shows.data.shuffled().take(10)) else emptyList()

                val hero            = recommendations.firstOrNull() ?: topGenreShows.randomOrNull()
                val similarToName   = if (targetDetails is Resource.Success) targetDetails.data.name else ""
                val similarShows    = if (targetDetails is Resource.Success && !similarShowsRaw.isNullOrEmpty()) getRecommendationsUseCase.scoreShows(similarShowsRaw.shuffled().take(10)) else emptyList()

                val hiddenGems      = recommendations.filter { it.voteCount in 1..499 && it.affinityScore >= 5.5f }.take(10)

                val temporalPattern = TemporalPatternAnalyzer.analyze(profile?.viewingHistory ?: emptyList())
                val isBinger        = temporalPattern.avgEpisodesPerSession >= 3f
                val contextPicks    = if (isBinger) recommendations.filter { (it.numberOfSeasons ?: 0) >= 3 }.take(10)
                                      else recommendations.filter { (it.numberOfSeasons ?: 1) <= 2 && it.status in listOf("Ended", "Canceled") }.take(10)
                val contextTitle    = if (contextPicks.isNotEmpty()) (if (isBinger) context.getString(R.string.discover_context_binger) else context.getString(R.string.discover_context_casual)) else ""

                val (dayTitle, dayShows) = buildDayOfWeekSection(recommendations)

                val narrativeStyleLabel = if (topNarrativeStyle != null) "Porque te gusta: ${NarrativeStyleMapper.getStyleLabel(topNarrativeStyle.key)}" else ""
                val narrativeStyleShows = if (nsShows is Resource.Success && nsShows.data.isNotEmpty()) getRecommendationsUseCase.scoreShows(nsShows.data.shuffled().take(10)) else emptyList()

                val moodSectionShows = if (moodShows is Resource.Success && moodShows.data.isNotEmpty()) getRecommendationsUseCase.scoreShows(moodShows.data.shuffled().take(10)) else emptyList()

                val explorationGenreName = underexploredGenre?.key?.let { GenreMapper.getGenreName(it) } ?: ""
                val explorationShowsList = if (explorationRaw is Resource.Success && explorationRaw.data.isNotEmpty()) getRecommendationsUseCase.scoreShows(explorationRaw.data.shuffled().take(10)) else emptyList()

                val creatorName      = if (creatorPerson is Resource.Success && creatorShowsRaw is Resource.Success && creatorShowsRaw.data.isNotEmpty()) creatorPerson.data.name else ""
                val creatorShowsList = if (creatorPerson is Resource.Success && creatorShowsRaw is Resource.Success && creatorShowsRaw.data.isNotEmpty()) getRecommendationsUseCase.scoreShows(creatorShowsRaw.data.shuffled().take(10)) else emptyList()

                val heroId             = hero?.id
                val collaborativeShows = recommendations.filter { it.id != heroId && it.voteCount in 100..4999 }.sortedByDescending { it.affinityScore }.take(12)

                _uiState.update { current ->
                    val hasContent = hero != null || topGenreShows.isNotEmpty()
                    current.copy(
                        isLoading = false,
                        isRefreshing = false,
                        heroShow = hero,
                        topGenreName = topName,
                        secondGenreName = secondName,
                        thirdGenreName = thirdName,
                        topGenreShows = topGenreShows,
                        secondGenreShows = secondGenreShows,
                        thirdGenreShows = thirdGenreShows,
                        timeTravelShows = timeTravelShows,
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
                        explorationGenreName = explorationGenreName,
                        explorationShows = explorationShowsList,
                        creatorName = creatorName,
                        creatorShows = creatorShowsList,
                        collaborativeShows = collaborativeShows,
                        errorMessage = if (!hasContent) "No se pudo cargar el contenido. Por favor, reintenta." else null
                    )
                }

            } catch (e: Exception) {
                Log.e("DiscoverViewModel", "Error loading discover content", e)
                FirebaseCrashlytics.getInstance().recordException(e)
                _uiState.update { it.copy(isLoading = false, isRefreshing = false, errorMessage = "Error al cargar el contenido: ${e.localizedMessage ?: "Comprueba tu conexión"}") }
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
        "narrativa_compleja"     -> Pair("9648",  null)
        "protagonista_detective" -> Pair("80",    null)
        "protagonista_antihero"  -> Pair("18|80", null)
        "protagonista_genio"     -> Pair("18",    null)
        "tono_oscuro"            -> Pair("53",    null)
        "tono_emocional"         -> Pair("18",    null)
        "tono_ligero"            -> Pair("35",    null)
        "ritmo_intenso"          -> Pair("10759", null)
        "ritmo_lento"            -> Pair("18",    null)
        "ritmo_episodico"        -> Pair("35",    null)
        "ritmo_largo"            -> Pair("18|80", null)
        else                     -> Pair(null,    null)
    }

    private fun narrativeStyleToMoodSection(style: String): Pair<String?, String> = when (style) {
        "tono_oscuro"            -> Pair("53|80",  "Para una noche de suspense")
        "tono_emocional"         -> Pair("18",     "Para llorar un buen rato")
        "tono_ligero"            -> Pair("35",     "Para reírte esta noche")
        "ritmo_intenso"          -> Pair("10759",  "Noche de acción")
        "narrativa_compleja"     -> Pair("9648",   "Thrillers psicológicos")
        "protagonista_detective" -> Pair("80",     "Crimen y misterio")
        "protagonista_antihero"  -> Pair("18|80",  "Antihéroes y dilemas morales")
        "protagonista_genio"     -> Pair("18",     "Genios en pantalla")
        "ritmo_lento"            -> Pair("18",     "Para saborear con calma")
        else                     -> Pair(null,     "")
    }
}
