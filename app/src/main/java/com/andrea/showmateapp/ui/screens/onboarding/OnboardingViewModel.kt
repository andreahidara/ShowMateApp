package com.andrea.showmateapp.ui.screens.onboarding

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.andrea.showmateapp.data.repository.ShowRepository
import com.andrea.showmateapp.di.AppPrefsDataStore
import com.andrea.showmateapp.domain.repository.ISocialRepository
import com.andrea.showmateapp.domain.repository.IUserRepository
import com.andrea.showmateapp.util.Resource
import com.google.firebase.messaging.FirebaseMessaging
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

@HiltViewModel
class OnboardingViewModel @Inject constructor(
    private val userRepository: IUserRepository,
    private val showRepository: ShowRepository,
    private val socialRepository: ISocialRepository,
    @AppPrefsDataStore private val dataStore: DataStore<Preferences>
) : ViewModel() {

    private val _uiState = MutableStateFlow(OnboardingUiState())
    val uiState = _uiState.asStateFlow()

    init {
        loadGenrePosters()
    }

    private fun loadGenrePosters() {
        viewModelScope.launch {
            val genres = _uiState.value.availableGenres.keys.toList()

            val deferred = genres.map { genreId ->
                async {
                    val result = showRepository.discoverShows(
                        genreId = genreId,
                        sortBy = "popularity.desc",
                        minRating = 6f // Solo series con buena puntuación
                    )
                    val candidates = when (result) {
                        is Resource.Success -> result.data
                            .filter { show ->
                                !show.posterPath.isNullOrBlank() &&
                                show.safeGenreIds.contains(genreId.toIntOrNull() ?: -1) // Doble comprobación de género
                            }
                            .map { it.posterPath!! }
                        else -> emptyList()
                    }
                    genreId to candidates
                }
            }
            val candidatesPerGenre = deferred.awaitAll()

            val usedPosters = mutableSetOf<String>()
            val posters = mutableMapOf<String, String?>()
            for ((genreId, candidates) in candidatesPerGenre) {
                val unique = candidates.firstOrNull { it !in usedPosters }
                posters[genreId] = unique
                if (unique != null) usedPosters.add(unique)
            }

            _uiState.update { it.copy(genrePosters = posters) }
        }
    }

    fun toggleGenre(genreId: String) {
        val current = _uiState.value.selectedGenres
        val updated = if (current.contains(genreId)) current - genreId else current + genreId
        _uiState.update { it.copy(selectedGenres = updated) }
    }

    private fun loadPopularShows() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoadingShows = true) }
            val (popular, trending) = awaitAll(
                async { showRepository.getPopularShows() },
                async { showRepository.getTrendingThisWeek() }
            )
            val combined = buildList {
                if (popular is Resource.Success) addAll(popular.data)
                if (trending is Resource.Success) addAll(trending.data)
            }
                .distinctBy { it.id }
                .filter { !it.posterPath.isNullOrBlank() }
                .take(20)
            _uiState.update { it.copy(popularShows = combined, isLoadingShows = false) }
        }
    }

    fun toggleWatched(showId: Int) {
        _uiState.update { state ->
            val watched = state.watchedShowIds.toMutableSet()
            val loved = state.lovedShowIds.toMutableSet()
            if (watched.contains(showId)) {
                watched.remove(showId)
                loved.remove(showId)
            } else {
                watched.add(showId)
            }
            state.copy(watchedShowIds = watched, lovedShowIds = loved)
        }
    }

    fun toggleLoved(showId: Int) {
        if (!_uiState.value.watchedShowIds.contains(showId)) return
        _uiState.update { state ->
            val loved = state.lovedShowIds.toMutableSet()
            if (loved.contains(showId)) loved.remove(showId) else loved.add(showId)
            state.copy(lovedShowIds = loved)
        }
    }

    fun setEpisodeLengthPref(pref: EpisodeLengthPref) = _uiState.update { it.copy(episodeLengthPref = pref) }

    fun setStatusPref(pref: StatusPref) = _uiState.update { it.copy(statusPref = pref) }

    fun setDubbedPref(pref: DubbedPref) = _uiState.update { it.copy(dubbedPref = pref) }

    fun advance() {
        val state = _uiState.value
        val nextStep = state.step + 1
        _uiState.update { it.copy(step = nextStep) }

        when (nextStep) {
            2 -> loadPopularShows()
            4 -> runAnalysis()
        }
    }

    fun goBack() {
        val current = _uiState.value.step
        if (current > 1) _uiState.update { it.copy(step = current - 1) }
    }

    private fun runAnalysis() {
        viewModelScope.launch {
            repeat(4) { phase ->
                _uiState.update { it.copy(analyzePhase = phase) }
                delay(900)
            }
            val personality = computePersonality()
            _uiState.update { it.copy(personality = personality, step = 5) }
        }
    }

    private fun computePersonality(): OnboardingPersonalityType {
        val state = _uiState.value
        val genres = state.selectedGenres
        val watched = state.watchedShowIds.size

        if (watched >= 12) return OnboardingPersonalityType.MARATHON
        if (genres.size >= 5) return OnboardingPersonalityType.ECLECTIC

        return when {
            genres.contains("80") || genres.contains("9648") -> OnboardingPersonalityType.DETECTIVE
            genres.contains("10765") -> OnboardingPersonalityType.VISIONARY
            genres.contains("35") -> OnboardingPersonalityType.OPTIMIST
            genres.contains("18") -> OnboardingPersonalityType.EMPATH
            genres.contains("10759") -> OnboardingPersonalityType.ADRENALINE
            genres.contains("99") -> OnboardingPersonalityType.CURIOUS
            else -> OnboardingPersonalityType.ECLECTIC
        }
    }

    fun completeOnboarding() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            val state = _uiState.value

            runCatching {
                userRepository.saveOnboardingInterests(
                    genres = state.selectedGenres.toList(),
                    watchedShows = state.popularShows.filter { it.id in state.watchedShowIds },
                    lovedShows = state.popularShows.filter { it.id in state.lovedShowIds },
                    preferShortEpisodes = when (state.episodeLengthPref) {
                        EpisodeLengthPref.SHORT -> true
                        EpisodeLengthPref.LONG -> false
                        else -> null
                    },
                    preferFinishedShows = when (state.statusPref) {
                        StatusPref.FINISHED -> true
                        StatusPref.ONGOING -> false
                        else -> null
                    },
                    preferDubbed = when (state.dubbedPref) {
                        DubbedPref.DUBBED -> true
                        DubbedPref.VO -> false
                        else -> null
                    }
                )
            }

            // Siempre guardar localmente, aunque Firebase haya fallado
            runCatching {
                dataStore.edit { prefs -> prefs[booleanPreferencesKey("onboarding_completed")] = true }
            }

            runCatching {
                val token = FirebaseMessaging.getInstance().token.await()
                socialRepository.saveDeviceToken(token)
            }

            _uiState.update { it.copy(isLoading = false, isComplete = true) }
        }
    }
}
