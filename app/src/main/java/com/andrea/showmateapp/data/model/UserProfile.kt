package com.andrea.showmateapp.data.model

import androidx.compose.runtime.Immutable

@Immutable
data class UserProfile(
    val userId: String = "",
    val username: String = "",
    val email: String = "",
    val genreScores: Map<String, Float> = emptyMap(),
    val preferredKeywords: Map<String, Float> = emptyMap(),
    val preferredActors: Map<String, Float> = emptyMap(),
    val narrativeStyleScores: Map<String, Float> = emptyMap(),
    val genreScoreDates: Map<String, Long> = emptyMap(),
    val keywordScoreDates: Map<String, Long> = emptyMap(),
    val actorScoreDates: Map<String, Long> = emptyMap(),
    val narrativeStyleDates: Map<String, Long> = emptyMap(),
    val preferredCreators: Map<String, Float> = emptyMap(),
    val creatorScoreDates: Map<String, Long> = emptyMap(),
    val likedMediaIds: List<Int> = emptyList(),
    val essentialMediaIds: List<Int> = emptyList(),
    val dislikedMediaIds: List<Int> = emptyList(),
    val ratings: Map<String, Float> = emptyMap(),
    val watchedEpisodes: Map<String, List<Int>> = emptyMap(),
    val mediaReviews: Map<String, String> = emptyMap(),
    val customLists: Map<String, List<Int>> = emptyMap(),
    val viewingHistory: List<String> = emptyList(),
    val fcmToken: String = "",
    val preferShortEpisodes: Boolean? = null,
    val preferFinishedShows: Boolean? = null,
    val preferDubbed: Boolean? = null,
    val xp: Int = 0,
    val unlockedAchievementIds: List<String> = emptyList(),
    val completedGroupMatches: Int = 0,
    val photoUrl: String? = null,
    val friendIds: List<String> = emptyList(),
    val onboardingCompleted: Boolean = false
)
