package com.andrea.showmateapp.ui.screens.home

import com.andrea.showmateapp.data.model.MediaContent

data class HomeGenreShows(
    val action: List<MediaContent> = emptyList(),
    val comedy: List<MediaContent> = emptyList(),
    val drama: List<MediaContent> = emptyList(),
    val sciFi: List<MediaContent> = emptyList(),
    val mystery: List<MediaContent> = emptyList()
)

