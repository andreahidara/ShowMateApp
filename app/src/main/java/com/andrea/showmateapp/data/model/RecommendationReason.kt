package com.andrea.showmateapp.data.model

import androidx.compose.runtime.Immutable

enum class ReasonType {
    GENRE,
    ACTOR,
    NARRATIVE,
    CREATOR,
    HIDDEN_GEM,
    COLLABORATIVE,
    BINGE,
    COMPLETENESS,
    TRENDING
}

@Immutable
data class RecommendationReason(
    val type: ReasonType,
    val weight: Float,
    val description: String,
    val iconEmoji: String
)
