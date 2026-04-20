package com.andrea.showmateapp.ui.screens.home

import androidx.compose.runtime.Immutable
import com.andrea.showmateapp.data.model.MediaContent
import com.andrea.showmateapp.util.ErrorType
import com.andrea.showmateapp.util.UiText

data class HomeGenreShows(
    val action: List<MediaContent> = emptyList(),
    val comedy: List<MediaContent> = emptyList(),
    val drama: List<MediaContent> = emptyList(),
    val sciFi: List<MediaContent> = emptyList(),
    val mystery: List<MediaContent> = emptyList()
)

@Immutable
data class HomeUiState(
    val isLoading: Boolean = true,
    val isRefreshing: Boolean = false,
    val isFromCache: Boolean = false,
    val userName: String = "",
    val upNextShows: List<MediaContent> = emptyList(),
    val upNextProgress: Map<Int, Float> = emptyMap(),
    val trendingShows: List<MediaContent> = emptyList(),
    val isLoadingMoreTrending: Boolean = false,
    val top10Shows: List<MediaContent> = emptyList(),
    val newReleasesShows: List<MediaContent> = emptyList(),
    val genres: HomeGenreShows = HomeGenreShows(),
    val thisWeekShows: List<MediaContent> = emptyList(),
    val isLoadingMoreThisWeek: Boolean = false,
    val selectedPlatform: String? = null,
    val platformShows: Map<String, List<MediaContent>> = emptyMap(),
    val isPlatformLoading: Boolean = false,
    val whatToWatchToday: MediaContent? = null,
    val showContextSelector: Boolean = false,
    val criticalError: ErrorType? = null,
    val errorMessage: UiText? = null
)

sealed interface HomeAction {
    object LoadData : HomeAction
    object Refresh : HomeAction
    object RequestWhatToWatch : HomeAction
    object DismissContextSelector : HomeAction
    data class PickWhatToWatchToday(val mood: MoodOption? = null, val time: TimeOption? = null) : HomeAction
    object DismissWhatToWatch : HomeAction
    object LoadMoreTrending : HomeAction
    object LoadMoreThisWeek : HomeAction
    data class SelectPlatform(val platform: String?) : HomeAction
    data class SwipeLeft(val mediaId: Int) : HomeAction
    data class MarkAsWatched(val mediaId: Int) : HomeAction
}

enum class MoodOption(val label: String, val emoji: String, val genreIds: List<Int>) {
    RELAX("Relajante", "😌", listOf(35, 10751)),
    ACTION("Adrenalina", "⚡", listOf(10759)),
    EMOTIONAL("Emoción", "😢", listOf(18)),
    THRILLER("Suspense", "😰", listOf(9648, 80))
}

enum class TimeOption(val label: String, val maxRuntime: Int?) {
    SHORT("30 min", 32),
    MEDIUM("1 hora", 65),
    MARATHON("Maratón", null)
}

