package com.andrea.showmateapp.data.model

import androidx.compose.runtime.Immutable
import com.andrea.showmateapp.util.UiText

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
    val description: UiText,
    val iconEmoji: String
)
