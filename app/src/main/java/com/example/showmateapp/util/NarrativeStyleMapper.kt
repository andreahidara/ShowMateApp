package com.example.showmateapp.util

/**
 * Mapea palabras clave de TMDB y el tiempo de episodio a clústeres de estilo narrativo.
 *
 * Cada clúster captura una dimensión no cubierta por los géneros:
 *   - Complejidad narrativa  → "narrativa_compleja"
 *   - Arquetipo protagonista → "protagonista_detective", "protagonista_antihero", "protagonista_genio"
 *   - Tono / emoción        → "tono_oscuro", "tono_emocional", "tono_ligero"
 *   - Ritmo                 → "ritmo_intenso", "ritmo_lento", "ritmo_episodico", "ritmo_largo"
 */
object NarrativeStyleMapper {

    private val CLUSTERS: Map<String, List<String>> = mapOf(
        "narrativa_compleja" to listOf(
            "plot twist", "nonlinear timeline", "unreliable narrator", "conspiracy",
            "psychological", "mystery box", "multiple storylines", "complex",
            "mind bending", "time loop", "parallel universe", "mythology"
        ),
        "protagonista_detective" to listOf(
            "detective", "investigator", "crime solving", "procedural",
            "murder mystery", "police", "fbi", "crime investigation", "spy"
        ),
        "protagonista_antihero" to listOf(
            "anti-hero", "morally ambiguous", "villain protagonist",
            "dark side", "anti hero", "antihero", "outlaw"
        ),
        "protagonista_genio" to listOf(
            "genius", "prodigy", "intellectual", "hacker", "scientist",
            "doctor", "chess", "mastermind", "gifted"
        ),
        "tono_oscuro" to listOf(
            "dark", "cynical", "nihilistic", "dystopia", "gritty", "noir",
            "bleak", "disturbing", "violence", "dark humor", "dark comedy"
        ),
        "tono_emocional" to listOf(
            "emotional", "heartbreak", "grief", "family drama", "loss",
            "tearjerker", "tragedy", "romance", "love story", "coming of age"
        ),
        "tono_ligero" to listOf(
            "feel-good", "heartwarming", "optimistic", "wholesome", "uplifting",
            "comedy", "humorous", "light-hearted", "sitcom", "fun"
        ),
        "ritmo_intenso" to listOf(
            "suspense", "thriller", "tension", "fast paced",
            "action", "adrenaline", "chase", "survival"
        ),
        "ritmo_lento" to listOf(
            "slow burn", "character study", "slice of life", "atmospheric",
            "introspective", "contemplative", "drama"
        )
    )

    /**
     * Devuelve un mapa de clúster → puntuación de relevancia [0,1] para la serie dada.
     * La relevancia es la fracción de palabras clave del clúster que coinciden (máx. 1).
     *
     * También infiere el ritmo a partir de [episodeRuntime]:
     *  - ≤30 min → "ritmo_episodico" +1
     *  - >50 min → "ritmo_largo" +1
     */
    fun extractStyles(keywords: List<String>, episodeRuntime: Int?): Map<String, Float> {
        val lower = keywords.map { it.lowercase() }
        val result = mutableMapOf<String, Float>()

        for ((cluster, clusterKeywords) in CLUSTERS) {
            val matches = clusterKeywords.count { ck -> lower.any { kw -> kw.contains(ck) } }
            if (matches > 0) {
                result[cluster] = (matches.toFloat() / clusterKeywords.size).coerceIn(0f, 1f)
            }
        }

        // Ritmo según duración del episodio
        when {
            episodeRuntime != null && episodeRuntime <= 30 ->
                result["ritmo_episodico"] = 1f
            episodeRuntime != null && episodeRuntime > 50  ->
                result["ritmo_largo"] = 1f
        }

        return result
    }

    fun getStyleLabel(key: String): String = when (key) {
        "narrativa_compleja"      -> "Narrativa compleja"
        "protagonista_detective"  -> "Protagonista detective"
        "protagonista_antihero"   -> "Protagonista anti-héroe"
        "protagonista_genio"      -> "Protagonista genio"
        "tono_oscuro"             -> "Tono oscuro"
        "tono_emocional"          -> "Tono emocional"
        "tono_ligero"             -> "Tono ligero"
        "ritmo_intenso"           -> "Ritmo intenso"
        "ritmo_lento"             -> "Ritmo lento"
        "ritmo_episodico"         -> "Episodios cortos"
        "ritmo_largo"             -> "Episodios largos"
        else                      -> key
    }
}
