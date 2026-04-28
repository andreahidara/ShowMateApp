package com.andrea.showmateapp.data.model

import com.google.gson.annotations.SerializedName

data class PersonResponse(
    val id: Int = 0,
    val name: String = "",
    @SerializedName("profile_path") val profilePath: String? = null,
    val biography: String = "",
    val birthday: String? = null,
    @SerializedName("place_of_birth") val placeOfBirth: String? = null,
    @SerializedName("known_for_department") val knownForDepartment: String? = null,
    val popularity: Float = 0f
)
