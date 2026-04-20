package com.andrea.showmateapp.ui.screens.profile

import com.andrea.showmateapp.data.model.MediaContent

data class WatchedShowItem(
    val show: MediaContent,
    val episodesWatched: Int
)

data class UserLevel(
    val label: String,
    val progress: Float,
    val nextLabel: String?
)
