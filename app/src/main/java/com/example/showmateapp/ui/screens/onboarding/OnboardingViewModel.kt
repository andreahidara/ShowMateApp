package com.example.showmateapp.ui.screens.onboarding

import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject

data class Genre(val id: String, val name: String)

@HiltViewModel
class OnboardingViewModel @Inject constructor() : ViewModel() {
    val genres = listOf(
        Genre("10759", "Acción y Aventura"),
        Genre("16", "Animación"),
        Genre("35", "Comedia"),
        Genre("80", "Crimen"),
        Genre("99", "Documental"),
        Genre("18", "Drama"),
        Genre("10751", "Familia"),
        Genre("10762", "Kids"),
        Genre("9648", "Misterio"),
        Genre("10763", "Noticias"),
        Genre("10764", "Reality"),
        Genre("10765", "Sci-Fi & Fantasy"),
        Genre("10766", "Soap"),
        Genre("10767", "Talk"),
        Genre("10768", "War & Politics"),
        Genre("37", "Western")
    )

    private val _selectedGenres = MutableStateFlow<List<String>>(emptyList())
    val selectedGenres: StateFlow<List<String>> = _selectedGenres.asStateFlow()

    fun toggleGenre(genreId: String) {
        val current = _selectedGenres.value.toMutableList()
        if (current.contains(genreId)) {
            current.remove(genreId)
        } else {
            current.add(genreId)
        }
        _selectedGenres.value = current
    }
}
