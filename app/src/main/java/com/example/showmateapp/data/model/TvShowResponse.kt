package com.example.showmateapp.data.model

import com.example.showmateapp.data.network.TvShow
import com.google.gson.annotations.SerializedName

data class TvShowResponse(
    @SerializedName("page") val page: Int,
    @SerializedName("results") val results: List<TvShow>,
    @SerializedName("total_pages") val total_pages: Int,
    @SerializedName("total_results") val total_results: Int
)