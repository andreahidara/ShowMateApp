package com.andrea.showmateapp.ui.screens.onboarding

import com.andrea.showmateapp.data.network.MediaContent

enum class EpisodeLengthPref(val label: String, val description: String) {
    SHORT("Cortos · ~20 min", "Perfectos para un descanso rápido"),
    LONG("Largos · ~1 hora", "Para sumergirme de verdad"),
    BOTH("Me da igual", "Lo importante es el contenido")
}

enum class StatusPref(val label: String, val description: String) {
    FINISHED("Finalizadas", "Sin riesgo de quedarse sin final"),
    ONGOING("En emisión", "Me gusta seguirlas semana a semana"),
    BOTH("Las dos", "El estado no importa")
}

enum class DubbedPref(val label: String, val description: String) {
    DUBBED("Dobladas", "Más cómodo en mi idioma"),
    VO("Versión original", "La experiencia auténtica"),
    BOTH("Indiferente", "Depende de la serie")
}

enum class OnboardingPersonalityType(
    val title: String,
    val emoji: String,
    val tagline: String,
    val description: String
) {
    DETECTIVE(
        "El Detective",
        "🔍",
        "Analítico · Intrigante · Perspicaz",
        "Las tramas complejas y los giros inesperados son tu pasión. Resuelves los misterios antes que los protagonistas."
    ),
    VISIONARY(
        "El Visionario",
        "🚀",
        "Creativo · Soñador · Explorador",
        "Mundos alternativos, futuros posibles, universos imposibles. Tu imaginación no conoce límites."
    ),
    OPTIMIST(
        "El Optimista",
        "😄",
        "Alegre · Desenfadado · Sociable",
        "La vida ya tiene suficiente drama. Tú eliges reír, disfrutar y desconectar con buenas historias."
    ),
    EMPATH(
        "El Empático",
        "💙",
        "Sensible · Reflexivo · Profundo",
        "Las emociones humanas te mueven. Buscas personajes complejos y tramas que te hagan pensar y sentir."
    ),
    ADRENALINE(
        "El Adrenalínico",
        "⚡",
        "Intenso · Apasionado · Veloz",
        "Necesitas acción desde el primer minuto. Los episodios lentos no van contigo."
    ),
    CURIOUS(
        "El Curioso",
        "📚",
        "Intelectual · Aprendiz · Analítico",
        "Cada serie es una oportunidad de aprender algo nuevo. El conocimiento es tu mayor entretenimiento."
    ),
    ECLECTIC(
        "El Ecléctico",
        "🌈",
        "Versátil · Abierto · Singular",
        "Tu gusto no conoce fronteras. Saltas de géneros con total naturalidad y siempre encuentras algo que te engancha."
    ),
    MARATHON(
        "El Maratonero",
        "🏃",
        "Intenso · Dedicado · Apasionado",
        "Una vez que empiezas no puedes parar. Los fines de semana son tuyos y de tus series."
    )
}

data class OnboardingUiState(
    val step: Int = 1,

    val availableGenres: Map<String, String> = mapOf(
        "10759" to "Acción y Aventura",
        "16"    to "Animación",
        "35"    to "Comedia",
        "80"    to "Crimen",
        "99"    to "Documental",
        "18"    to "Drama",
        "10751" to "Familiar",
        "9648"  to "Misterio",
        "10765" to "Sci-Fi y Fantasía",
        "37"    to "Western",
        "10768" to "Política",
        "10764" to "Reality"
    ),
    val genreEmojis: Map<String, String> = mapOf(
        "10759" to "⚔️",
        "16"    to "🎨",
        "35"    to "😂",
        "80"    to "🔫",
        "99"    to "📽️",
        "18"    to "🎭",
        "10751" to "👨‍👩‍👧",
        "9648"  to "🔍",
        "10765" to "🚀",
        "37"    to "🤠",
        "10768" to "🗳️",
        "10764" to "📺"
    ),
    val selectedGenres: Set<String> = emptySet(),
    val genrePosters: Map<String, String?> = emptyMap(),

    val popularShows: List<MediaContent> = emptyList(),
    val watchedShowIds: Set<Int> = emptySet(),
    val lovedShowIds: Set<Int> = emptySet(),
    val isLoadingShows: Boolean = false,

    val episodeLengthPref: EpisodeLengthPref? = null,
    val statusPref: StatusPref? = null,
    val dubbedPref: DubbedPref? = null,

    val analyzePhase: Int = 0,

    val personality: OnboardingPersonalityType? = null,

    val isLoading: Boolean = false,
    val isComplete: Boolean = false
)
