package com.example.showmateapp.data.model

data class UserProfile(
    val userId: String = "",
    val genreScores: Map<String, Int> = emptyMap(), // Ej: {"Terror": 5, "Acción": 2}
    val actorScores: Map<String, Int> = emptyMap()
)