package com.andrea.showmateapp.data.model

import androidx.compose.runtime.Stable

@Stable
data class Achievement(
    val id: String,
    val category: AchievementCategory,
    val title: String,
    val description: String,
    val emoji: String,
    val xpReward: Int,
    val unlockedAt: Long? = null
) {
    val isUnlocked: Boolean get() = unlockedAt != null
}

enum class AchievementCategory(val label: String, val emoji: String) {
    EXPLORER("Explorador", "🧭"),
    MARATHON("Maratonero", "⚡"),
    CRITIC("Crítico", "✍️"),
    SOCIAL("Social", "👥"),
    DISCOVERER("Descubridor", "🔍")
}
