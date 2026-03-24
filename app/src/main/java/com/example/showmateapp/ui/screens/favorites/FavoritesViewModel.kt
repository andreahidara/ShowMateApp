package com.example.showmateapp.ui.screens.favorites

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.showmateapp.data.model.toDomain
import com.example.showmateapp.data.network.MediaContent
import com.example.showmateapp.data.repository.UserRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

enum class SortOption(val label: String) {
    DATE_ADDED("Recientes"),
    RATING("Valoración"),
    NAME("Nombre")
}

enum class FavoriteTab(val label: String) {
    LIKED("Me gusta"),
    ESSENTIAL("Imprescindibles"),
    WATCHED("Vistas"),
    WATCHLIST("Quiero ver")
}

data class FavoritesUiState(
    val favorites: List<MediaContent> = emptyList(),
    val essentials: List<MediaContent> = emptyList(),
    val watched: List<MediaContent> = emptyList(),
    val watchlist: List<MediaContent> = emptyList(),
    val selectedTab: FavoriteTab = FavoriteTab.LIKED,
    val sortOption: SortOption = SortOption.DATE_ADDED,
    val isLoading: Boolean = false
)

@HiltViewModel
class FavoritesViewModel @Inject constructor(
    private val userRepository: UserRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(FavoritesUiState())
    val uiState: StateFlow<FavoritesUiState> = _uiState.asStateFlow()

    private var rawFavorites: List<MediaContent> = emptyList()
    private var rawEssentials: List<MediaContent> = emptyList()
    private var rawWatched: List<MediaContent> = emptyList()

    init {
        viewModelScope.launch {
            userRepository.getLikedShowsFlow().collect { entities ->
                rawFavorites = entities.map { it.toDomain() }
                _uiState.update { it.copy(favorites = applySorting(rawFavorites, it.sortOption)) }
            }
        }
        viewModelScope.launch {
            userRepository.getWatchedShowsFlow().collect { entities ->
                rawWatched = entities.map { it.toDomain() }
                _uiState.update { it.copy(watched = applySorting(rawWatched, it.sortOption)) }
            }
        }
        viewModelScope.launch {
            userRepository.syncFavoritesAndWatchedToRoom()
            loadEssentials()
            loadWatchlist()
        }
    }

    private fun loadEssentials() {
        viewModelScope.launch {
            val result = userRepository.getEssentials()
            rawEssentials = result
            _uiState.update { it.copy(essentials = applySorting(rawEssentials, it.sortOption)) }
        }
    }

    private fun loadWatchlist() {
        viewModelScope.launch {
            val result = userRepository.getWatchlist()
            _uiState.update { it.copy(watchlist = applySorting(result, it.sortOption)) }
        }
    }

    fun selectTab(tab: FavoriteTab) {
        _uiState.update { it.copy(selectedTab = tab) }
        when (tab) {
            FavoriteTab.ESSENTIAL -> loadEssentials()
            FavoriteTab.WATCHLIST -> loadWatchlist()
            else -> {}
        }
    }

    fun setSortOption(sort: SortOption) {
        _uiState.update { state ->
            state.copy(
                sortOption = sort,
                favorites = applySorting(rawFavorites, sort),
                essentials = applySorting(rawEssentials, sort),
                watched = applySorting(rawWatched, sort)
            )
        }
    }

    private fun applySorting(list: List<MediaContent>, sort: SortOption): List<MediaContent> {
        return when (sort) {
            SortOption.DATE_ADDED -> list
            SortOption.RATING     -> list.sortedByDescending { it.voteAverage }
            SortOption.NAME       -> list.sortedBy { it.name }
        }
    }
}
