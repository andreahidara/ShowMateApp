package com.andrea.showmateapp.ui.screens.home

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
