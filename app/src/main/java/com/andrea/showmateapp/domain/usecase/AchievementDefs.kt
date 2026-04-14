package com.andrea.showmateapp.domain.usecase

import com.andrea.showmateapp.data.model.Achievement
import com.andrea.showmateapp.data.model.AchievementCategory

object AchievementDefs {

    val all: List<Achievement> = listOf(
        Achievement(
            "first_show",
            AchievementCategory.EXPLORER,
            "Primera serie",
            "Añadiste tu primera serie a la biblioteca",
            "📺",
            50
        ),
        Achievement("shows_10", AchievementCategory.EXPLORER, "Enganchado", "Viste 10 series", "🎯", 100),
        Achievement("shows_25", AchievementCategory.EXPLORER, "Aficionado", "Viste 25 series", "🌟", 200),
        Achievement("shows_50", AchievementCategory.EXPLORER, "Veterano", "Viste 50 series", "🏅", 350),
        Achievement("shows_100", AchievementCategory.EXPLORER, "Leyenda de las series", "Viste 100 series", "👑", 600),
        Achievement(
            "genre_explorer",
            AchievementCategory.EXPLORER,
            "Explorador de géneros",
            "Viste series de 10 géneros diferentes",
            "🧭",
            150
        ),
        Achievement("korean_drama", AchievementCategory.EXPLORER, "Hallyu!", "Tu primera serie coreana", "🇰🇷", 100),
        Achievement(
            "anime_fan",
            AchievementCategory.EXPLORER,
            "Otaku",
            "Viste tu primera serie de animación japonesa",
            "🍜",
            100
        ),
        Achievement(
            "doc_watcher",
            AchievementCategory.EXPLORER,
            "Curioso nato",
            "Viste tu primer documental",
            "🔍",
            75
        ),
        Achievement(
            "world_traveler",
            AchievementCategory.EXPLORER,
            "Viajero del mundo",
            "Viste series de 5 países distintos",
            "🌍",
            200
        ),

        Achievement("marathon_day", AchievementCategory.MARATHON, "Maratón", "5 episodios en un mismo día", "⚡", 100),
        Achievement(
            "weekend_warrior",
            AchievementCategory.MARATHON,
            "Guerrero del finde",
            "Completaste una serie entera en un fin de semana",
            "🏆",
            200
        ),
        Achievement(
            "night_owl",
            AchievementCategory.MARATHON,
            "Búho nocturno",
            "Episodios vistos pasada la medianoche",
            "🌙",
            75
        ),
        Achievement(
            "binge_king",
            AchievementCategory.MARATHON,
            "Rey del binge",
            "10 episodios en un mismo día",
            "🚀",
            250
        ),
        Achievement(
            "season_sweep",
            AchievementCategory.MARATHON,
            "Temporada completa",
            "Marcaste una temporada entera de golpe",
            "📦",
            150
        ),

        Achievement(
            "first_review",
            AchievementCategory.CRITIC,
            "Primera opinión",
            "Escribiste tu primera reseña",
            "✍️",
            50
        ),
        Achievement(
            "prolific_critic",
            AchievementCategory.CRITIC,
            "Crítico prolífico",
            "Escribiste 10 reseñas",
            "📝",
            200
        ),
        Achievement(
            "popular_review",
            AchievementCategory.CRITIC,
            "Reseña viral",
            "Una de tus reseñas recibió 50 likes",
            "🔥",
            300
        ),
        Achievement(
            "five_stars",
            AchievementCategory.CRITIC,
            "Exigente",
            "Diste una puntuación perfecta a 5 series",
            "⭐",
            100
        ),
        Achievement(
            "harsh_critic",
            AchievementCategory.CRITIC,
            "Crítico despiadado",
            "Pusiste una puntuación de 1 a 3 series",
            "💀",
            75
        ),

        Achievement(
            "first_friend",
            AchievementCategory.SOCIAL,
            "Primer amigo",
            "Añadiste tu primer amigo en ShowMate",
            "🤝",
            50
        ),
        Achievement(
            "social_butterfly",
            AchievementCategory.SOCIAL,
            "Mariposa social",
            "Tienes 5 amigos en ShowMate",
            "👥",
            150
        ),
        Achievement(
            "group_matcher",
            AchievementCategory.SOCIAL,
            "Maestro del grupo",
            "Completaste 3 Group Matches",
            "🎬",
            200
        ),
        Achievement("influencer", AchievementCategory.SOCIAL, "Influencer", "Tienes 10 amigos en ShowMate", "📣", 300),
        Achievement(
            "shared_list",
            AchievementCategory.SOCIAL,
            "Lista compartida",
            "Creaste tu primera lista colaborativa",
            "📋",
            100
        ),

        Achievement(
            "hidden_gem",
            AchievementCategory.DISCOVERER,
            "Joya escondida",
            "Viste una serie con menos de 1.000 votos en TMDB",
            "💎",
            250
        ),
        Achievement(
            "early_adopter",
            AchievementCategory.DISCOVERER,
            "Pionero",
            "Viste una serie antes de que fuera tendencia",
            "🚀",
            200
        ),
        Achievement(
            "swipe_master",
            AchievementCategory.DISCOVERER,
            "Maestro del swipe",
            "50 swipes en el modo descubrir",
            "👆",
            100
        ),
        Achievement(
            "liked_50",
            AchievementCategory.DISCOVERER,
            "Coleccionista",
            "Marcaste 50 series como favoritas",
            "❤️",
            200
        )
    )

    val byId: Map<String, Achievement> = all.associateBy { it.id }

    data class XpLevel(val level: Int, val name: String, val minXp: Int, val maxXp: Int)

    val levels: List<XpLevel> = listOf(
        XpLevel(1, "Rookie", 0, 99),
        XpLevel(2, "Espectador", 100, 299),
        XpLevel(3, "Entusiasta", 300, 599),
        XpLevel(4, "Fan", 600, 999),
        XpLevel(5, "Cinéfilo", 1000, 1499),
        XpLevel(6, "Experto", 1500, 1999),
        XpLevel(7, "Maestro", 2000, 2999),
        XpLevel(8, "Leyenda", 3000, Int.MAX_VALUE)
    )

    fun levelForXp(xp: Int): XpLevel = levels.lastOrNull { xp >= it.minXp } ?: levels.first()

    fun progressInLevel(xp: Int): Float {
        val lvl = levelForXp(xp)
        if (lvl.maxXp == Int.MAX_VALUE) return 1f
        return ((xp - lvl.minXp).toFloat() / (lvl.maxXp - lvl.minXp + 1)).coerceIn(0f, 1f)
    }

    const val XP_WATCH_EPISODE = 5
    const val XP_LIKE_SHOW = 10
    const val XP_RATE_SHOW = 15
    const val XP_WRITE_REVIEW = 20
    const val XP_ADD_FRIEND = 25
    const val XP_GROUP_MATCH = 50
}
