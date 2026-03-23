package com.example.showmateapp.util

object GenreMapper {

    /**
     * Jaccard ponderada continua sobre mapas de puntuaciones de género.
     * Devuelve un valor en [0, 1]. Usada para compatibilidad entre perfiles.
     */
    fun jaccardSimilarity(a: Map<String, Float>, b: Map<String, Float>): Float {
        val allKeys = a.keys union b.keys
        if (allKeys.isEmpty()) return 0f
        var intersection = 0f
        var union = 0f
        for (key in allKeys) {
            val aScore = (a[key] ?: 0f).coerceAtLeast(0f)
            val bScore = (b[key] ?: 0f).coerceAtLeast(0f)
            intersection += minOf(aScore, bScore)
            union += maxOf(aScore, bScore)
        }
        return if (union > 0f) intersection / union else 0f
    }

    fun getGenreName(id: String): String = when (id) {
        "10759" -> "Acción y Aventura"
        "16"    -> "Animación"
        "35"    -> "Comedia"
        "80"    -> "Crimen"
        "99"    -> "Documental"
        "18"    -> "Drama"
        "10751" -> "Familiar"
        "10762" -> "Infantil"
        "9648"  -> "Misterio"
        "10763" -> "Noticias"
        "10764" -> "Reality"
        "10765" -> "Sci-Fi y Fantasía"
        "10766" -> "Soap"
        "10767" -> "Talk"
        "10768" -> "Guerra y Política"
        "37"    -> "Western"
        else    -> "Series Variadas"
    }
}
