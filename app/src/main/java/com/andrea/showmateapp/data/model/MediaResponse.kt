package com.andrea.showmateapp.data.model

import com.andrea.showmateapp.data.model.MediaContent
import com.google.gson.annotations.SerializedName

data class MediaResponse(
    @SerializedName("page") val page: Int,
    @SerializedName("results") val results: List<MediaContent>,
    @SerializedName("total_pages") val total_pages: Int,
    @SerializedName("total_results") val total_results: Int
)

