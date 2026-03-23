package com.example.showmateapp.data.model

data class UserProfile(
    val userId: String = "",
    val username: String = "",
    val email: String = "",
    // Affinity scores
    val genreScores: Map<String, Float> = emptyMap(),
    val preferredKeywords: Map<String, Float> = emptyMap(),
    val preferredActors: Map<String, Float> = emptyMap(),
    // Narrative style scores — clusters: complejidad, protagonista, tono, ritmo
    val narrativeStyleScores: Map<String, Float> = emptyMap(),
    // Timestamps (epoch ms) for time-decay — key matches the score map key
    val genreScoreDates: Map<String, Long> = emptyMap(),
    val keywordScoreDates: Map<String, Long> = emptyMap(),
    val actorScoreDates: Map<String, Long> = emptyMap(),
    val narrativeStyleDates: Map<String, Long> = emptyMap(),
    // Creator (showrunner/director) preferences
    val preferredCreators: Map<String, Float> = emptyMap(),
    val creatorScoreDates: Map<String, Long> = emptyMap(),
    // Interaction lists
    val likedMediaIds: List<Int> = emptyList(),
    val essentialMediaIds: List<Int> = emptyList(),
    val dislikedMediaIds: List<Int> = emptyList(),
    val ratings: Map<String, Float> = emptyMap(),
    // Episodes: Map<showId, List<episodeId>>
    val watchedEpisodes: Map<String, List<Int>> = emptyMap(),
    // User-written reviews: Map<mediaId, reviewText>
    val mediaReviews: Map<String, String> = emptyMap(),
    // Custom lists: Map<listName, List<mediaId>>
    val customLists: Map<String, List<Int>> = emptyMap(),
    // Viewing history for stats: List of "YYYY-MM-DD:showId:episodeCount"
    val viewingHistory: List<String> = emptyList()
)
