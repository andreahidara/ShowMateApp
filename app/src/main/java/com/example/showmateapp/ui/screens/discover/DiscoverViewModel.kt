package com.example.showmateapp.ui.screens.discover

import android.util.Log
import androidx.lifecycle.ViewModel
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
import kotlinx.coroutines.launch
import javax.inject.Inject
import com.example.showmateapp.domain.usecase.GetRecommendationsUseCase
import com.example.showmateapp.util.GenreMapper
import com.example.showmateapp.util.KeywordMapper

data class DiscoverUiState(
    val isLoading: Boolean = true,
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
    val topKeywordLabel: String = ""
)

@HiltViewModel
class DiscoverViewModel @Inject constructor(
    private val repository: ShowRepository,
    private val userRepository: UserRepository,
    private val getRecommendationsUseCase: GetRecommendationsUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(DiscoverUiState())
    val uiState: StateFlow<DiscoverUiState> = _uiState.asStateFlow()

    init {
        loadDiscoverContent()
    }

    fun retry() {
        loadDiscoverContent()
    }

    private fun loadDiscoverContent() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            try {
                val profile = userRepository.getUserProfile()
                val sortedGenres = profile?.genreScores?.entries?.sortedByDescending { it.value } ?: emptyList()

                var topGenreId = "18"
                var topName = "Drama"
                var secondGenreId = "35"
                var secondName = "Comedia"
                var thirdGenreId: String? = null
                var thirdName = ""

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

                _uiState.update { it.copy(topGenreName = topName, secondGenreName = secondName, thirdGenreName = thirdName) }

                val res1 = repository.getShowsByGenres(topGenreId)
                val res2 = repository.getShowsByGenres(secondGenreId)
                val timeTravelRes = repository.discoverShows(keywords = "4363")
                val topRatedRes = repository.discoverShows(
                    genreId = topGenreId,
                    minRating = 8.0f,
                    sortBy = "vote_average.desc"
                )

                if (res1 is Resource.Success) {
                    _uiState.update { it.copy(topGenreShows = getRecommendationsUseCase.scoreShows(res1.data.shuffled().take(10))) }
                }
                if (res2 is Resource.Success) {
                    _uiState.update { it.copy(secondGenreShows = getRecommendationsUseCase.scoreShows(res2.data.shuffled().take(10))) }
                }
                if (timeTravelRes is Resource.Success) {
                    _uiState.update { it.copy(timeTravelShows = getRecommendationsUseCase.scoreShows(timeTravelRes.data.shuffled().take(10))) }
                }
                if (topRatedRes is Resource.Success && topRatedRes.data.isNotEmpty()) {
                    _uiState.update { it.copy(topRatedShows = topRatedRes.data.take(10).sortedByDescending { it.voteAverage }) }
                }
                if (thirdGenreId != null) {
                    val res3 = repository.getShowsByGenres(thirdGenreId)
                    if (res3 is Resource.Success) {
                        _uiState.update { it.copy(thirdGenreShows = getRecommendationsUseCase.scoreShows(res3.data.shuffled().take(10))) }
                    }
                }

                val keywordResult = KeywordMapper.getTopMappedKeyword(
                    profile?.preferredKeywords ?: emptyMap(),
                    excludeKeywordId = "4363"
                )
                if (keywordResult != null) {
                    val (_, kwId, kwLabel) = keywordResult
                    val kwRes = repository.discoverShows(keywords = kwId)
                    if (kwRes is Resource.Success && kwRes.data.isNotEmpty()) {
                        _uiState.update { it.copy(
                            topKeywordLabel = kwLabel,
                            topKeywordShows = getRecommendationsUseCase.scoreShows(kwRes.data.shuffled().take(10))
                        ) }
                    }
                }

                val sortedActors = profile?.preferredActors?.entries?.sortedByDescending { it.value } ?: emptyList()
                if (sortedActors.isNotEmpty()) {
                    val actorIdStr = sortedActors.first().key
                    val actorId = actorIdStr.toIntOrNull()
                    if (actorId != null) {
                        val personRes = repository.getPersonDetails(actorId)
                        if (personRes is Resource.Success) {
                            val actorShowsRes = repository.discoverShows(withCast = actorIdStr)
                            if (actorShowsRes is Resource.Success) {
                                _uiState.update { it.copy(
                                    actorName = personRes.data.name,
                                    actorShows = getRecommendationsUseCase.scoreShows(actorShowsRes.data.shuffled().take(10))
                                ) }
                            }
                        }
                    }
                    if (sortedActors.size > 1) {
                        val secondActorIdStr = sortedActors[1].key
                        val secondActorId = secondActorIdStr.toIntOrNull()
                        if (secondActorId != null) {
                            val personRes2 = repository.getPersonDetails(secondActorId)
                            if (personRes2 is Resource.Success) {
                                val secondActorShowsRes = repository.discoverShows(withCast = secondActorIdStr)
                                if (secondActorShowsRes is Resource.Success) {
                                    _uiState.update { it.copy(
                                        secondActorName = personRes2.data.name,
                                        secondActorShows = getRecommendationsUseCase.scoreShows(secondActorShowsRes.data.shuffled().take(10))
                                    ) }
                                }
                            }
                        }
                    }
                }

                val recommendations = getRecommendationsUseCase.execute()
                val hero = recommendations.firstOrNull() ?: _uiState.value.topGenreShows.randomOrNull()
                _uiState.update { it.copy(heroShow = hero) }

                val likedMedia = profile?.likedMediaIds?.toList() ?: emptyList()
                val topRatedMedia = profile?.ratings?.filterValues { it >= 4f }?.keys?.mapNotNull { it.toIntOrNull() } ?: emptyList()
                val candidates = (likedMedia + topRatedMedia).distinct()
                if (candidates.isNotEmpty()) {
                    val targetId = candidates.random()
                    val details = repository.getShowDetails(targetId)
                    if (details is Resource.Success) {
                        _uiState.update { it.copy(
                            similarToName = details.data.name,
                            similarShows = getRecommendationsUseCase.scoreShows(
                                repository.getSimilarShows(targetId).shuffled().take(10)
                            )
                        ) }
                    }
                }

                if (_uiState.value.heroShow == null && _uiState.value.topGenreShows.isEmpty()) {
                    _uiState.update { it.copy(errorMessage = "No se pudo cargar el contenido. Por favor, reintenta.") }
                }

            } catch (e: Exception) {
                Log.e("DiscoverViewModel", "Error loading discover content", e)
                _uiState.update { it.copy(errorMessage = "Error al cargar el contenido: ${e.localizedMessage ?: "Comprueba tu conexión"}") }
            } finally {
                _uiState.update { it.copy(isLoading = false) }
            }
        }
    }
}
