package com.andrea.showmateapp.data.model

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

