package com.example.showmateapp.data.model

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
    val viewingHistory: List<String> = emptyList()
)
