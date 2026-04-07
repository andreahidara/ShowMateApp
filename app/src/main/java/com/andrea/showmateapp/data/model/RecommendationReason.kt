package com.andrea.showmateapp.data.model

enum class ReasonType {
    GENRE, ACTOR, NARRATIVE, CREATOR,
    HIDDEN_GEM, COLLABORATIVE, BINGE, COMPLETENESS, TRENDING
}

data class RecommendationReason(
    val type: ReasonType,
    val weight: Float,
    val description: String,
    val iconEmoji: String
)
