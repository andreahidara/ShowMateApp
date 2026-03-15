package com.example.showmateapp.data.model

data class UserProfile(
    val userId: String = "",
    val genreScores: Map<String, Float> = emptyMap(),
    val preferredKeywords: Map<String, Float> = emptyMap(),
    val preferredActors: Map<String, Float> = emptyMap(),
    val likedMediaIds: List<Int> = emptyList(),
    val dislikedMediaIds: List<Int> = emptyList(),
    val ratings: Map<String, Float> = emptyMap()
)