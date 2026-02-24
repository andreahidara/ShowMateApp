package com.example.showmateapp.domain

import com.google.gson.annotations.SerializedName

data class TvShow(
    val id: Int,
    val name: String,
    val overview: String, // La sinopsis
    @SerializedName("poster_path")
    val posterPath: String?, // La ruta de la imagen
    @SerializedName("vote_average")
    val voteAverage: Double // La nota media
)