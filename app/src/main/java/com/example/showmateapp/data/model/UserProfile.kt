package com.example.showmateapp.data.model

data class UserProfile(
    val userId: String = "",
    val genreScores: Map<String, Float> = emptyMap(), // ID Género -> Puntos Dinámicos
    val preferredKeywords: Map<String, Float> = emptyMap(), // IDs o nombres de keywords -> Puntos
    val preferredActors: Map<String, Float> = emptyMap(), // IDs de actores de TMDB -> Puntos
    val likedMediaIds: List<Int> = emptyList(), // Para los Favoritos / "Me Gusta"
    val dislikedMediaIds: List<Int> = emptyList(), // Para el Feedback Loop explícito ("No me gusta")
    val ratings: Map<String, Float> = emptyMap() // ID del Show -> Puntuación (1 a 5)
)