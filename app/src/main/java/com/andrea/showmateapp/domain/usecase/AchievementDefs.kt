package com.andrea.showmateapp.domain.usecase

import com.andrea.showmateapp.data.model.Achievement
import com.andrea.showmateapp.data.model.AchievementCategory

object AchievementDefs {

    val all: List<Achievement> = listOf(
        Achievement("first_show",       AchievementCategory.EXPLORER,   "Primera serie",         "Añadiste tu primera serie a la biblioteca",        "📺", 50),
        Achievement("genre_explorer",   AchievementCategory.EXPLORER,   "Explorador de géneros", "Viste series de 10 géneros diferentes",             "🧭", 150),
        Achievement("korean_drama",     AchievementCategory.EXPLORER,   "Hallyu!",               "Tu primera serie coreana",                          "🇰🇷", 100),
        Achievement("marathon_day",     AchievementCategory.MARATHON,   "Maratón",               "5 episodios en un mismo día",                       "⚡", 100),
        Achievement("weekend_warrior",  AchievementCategory.MARATHON,   "Guerrero del finde",    "Completaste una serie entera en un fin de semana",  "🏆", 200),
        Achievement("first_review",     AchievementCategory.CRITIC,     "Primera opinión",       "Escribiste tu primera reseña",                      "✍️", 50),
        Achievement("prolific_critic",  AchievementCategory.CRITIC,     "Crítico prolífico",     "Escribiste 10 reseñas",                             "📝", 200),
        Achievement("popular_review",   AchievementCategory.CRITIC,     "Reseña viral",          "Una de tus reseñas recibió 50 likes",               "🔥", 300),
        Achievement("first_friend",     AchievementCategory.SOCIAL,     "Primer amigo",          "Añadiste tu primer amigo en ShowMate",              "🤝", 50),
        Achievement("social_butterfly", AchievementCategory.SOCIAL,     "Mariposa social",       "Tienes 5 amigos en ShowMate",                       "👥", 150),
        Achievement("group_matcher",    AchievementCategory.SOCIAL,     "Maestro del grupo",     "Completaste 3 Group Matches",                       "🎬", 200),
        Achievement("hidden_gem",       AchievementCategory.DISCOVERER, "Joya escondida",        "Viste una serie con menos de 1.000 votos en TMDB", "💎", 250),
    )

    val byId: Map<String, Achievement> = all.associateBy { it.id }

    data class XpLevel(val level: Int, val name: String, val minXp: Int, val maxXp: Int)

    val levels: List<XpLevel> = listOf(
        XpLevel(1, "Rookie",         0,    99),
        XpLevel(2, "Espectador",     100,  299),
        XpLevel(3, "Entusiasta",     300,  599),
        XpLevel(4, "Fan",            600,  999),
        XpLevel(5, "Cinéfilo",       1000, 1499),
        XpLevel(6, "Experto",        1500, 1999),
        XpLevel(7, "Maestro",        2000, 2999),
        XpLevel(8, "Leyenda",        3000, Int.MAX_VALUE),
    )

    fun levelForXp(xp: Int): XpLevel = levels.lastOrNull { xp >= it.minXp } ?: levels.first()

    fun progressInLevel(xp: Int): Float {
        val lvl = levelForXp(xp)
        if (lvl.maxXp == Int.MAX_VALUE) return 1f
        return ((xp - lvl.minXp).toFloat() / (lvl.maxXp - lvl.minXp + 1)).coerceIn(0f, 1f)
    }

    const val XP_WATCH_EPISODE = 5
    const val XP_LIKE_SHOW     = 10
    const val XP_RATE_SHOW     = 15
    const val XP_WRITE_REVIEW  = 20
    const val XP_ADD_FRIEND    = 25
    const val XP_GROUP_MATCH   = 50
}
