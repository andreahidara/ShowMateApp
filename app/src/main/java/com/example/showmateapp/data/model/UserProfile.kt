package com.example.showmateapp.data.model

data class UserProfile(
    val userId: String = "",
    val genreScores: Map<String, Int> = emptyMap(), // ID Género -> Puntos
    val preferredKeywords: Set<String> = emptySet(), // IDs o nombres de keywords
    val preferredActors: Set<Int> = emptySet(), // IDs de actores de TMDB
    val dislikedShowIds: Set<Int> = emptySet() // Para el Feedback Loop (opcional pero recomendado)
)