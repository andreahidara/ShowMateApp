package com.andrea.showmateapp.data.model

import androidx.compose.runtime.Immutable

@Immutable
data class FriendInfo(
    val uid: String = "",
    val username: String = "",
    val email: String = "",
    val compatibilityScore: Int = 0
)
