package com.andrea.showmateapp.ui.screens.home

import com.andrea.showmateapp.data.network.MediaContent

data class HomeGenreShows(
    val action: List<MediaContent> = emptyList(),
    val comedy: List<MediaContent> = emptyList(),
    val drama: List<MediaContent> = emptyList()
)
