package com.andrea.showmateapp.ui.screens.detail

import androidx.compose.runtime.Immutable
import com.andrea.showmateapp.data.model.MediaContent
import com.andrea.showmateapp.data.model.SeasonResponse
import com.andrea.showmateapp.util.BaseUiState
import com.andrea.showmateapp.util.UiText

@Immutable
data class DetailUiState(
    override val isLoading: Boolean = true,
    val media: MediaContent? = null,
    override val errorMessage: UiText? = null,
    val isLiked: Boolean = false,
    val isEssential: Boolean = false,
    val isWatched: Boolean = false,
    val isInWatchlist: Boolean = false,
    val userRating: Int? = null,
    val userReview: String = "",
    val isSavingReview: Boolean = false,
    val isReviewSaved: Boolean = false,
    val similarShows: List<MediaContent> = emptyList(),
    val isSimilarLoading: Boolean = true,
    val actionError: UiText? = null,
    val snackbarMessage: UiText? = null,
    val watchedEpisodes: List<Int> = emptyList(),
    val selectedSeason: SeasonResponse? = null,
    val isSeasonLoading: Boolean = false,
    val customLists: Map<String, List<Int>> = emptyMap(),
    val showAddToListDialog: Boolean = false
) : BaseUiState

