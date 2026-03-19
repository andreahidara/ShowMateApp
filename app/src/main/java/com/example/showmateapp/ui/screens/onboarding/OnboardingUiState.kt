package com.example.showmateapp.ui.screens.onboarding

data class OnboardingUiState(
    val availableGenres: Map<String, String> = mapOf(
        "10759" to "Acción y Aventura",
        "16" to "Animación",
        "35" to "Comedia",
        "80" to "Crimen",
        "99" to "Documental",
        "18" to "Drama",
        "10751" to "Familiar",
        "10762" to "Infantil",
        "9648" to "Misterio",
        "10763" to "Noticias",
        "10764" to "Reality",
        "10765" to "Sci-Fi & Fantasía",
        "10766" to "Soap",
        "10767" to "Talk Show",
        "10768" to "War & Politics",
        "37" to "Western"
    ),
    val selectedGenres: Set<String> = emptySet(),
    val genrePosters: Map<String, String?> = emptyMap(), // genreId -> posterPath
    val isLoading: Boolean = false,
    val isComplete: Boolean = false
)
