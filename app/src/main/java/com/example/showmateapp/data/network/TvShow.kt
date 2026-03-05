package com.example.showmateapp.data.network

import com.google.gson.annotations.SerializedName

data class TvShow(
    val id: Int,
    val name: String,
    @SerializedName("poster_path") val poster_path: String?,
    val overview: String,
    @SerializedName("genre_ids") val genre_ids: List<Int>? = null,
    @SerializedName("vote_average") val vote_average: Double? = null
)