package com.andrea.showmateapp.util

object GenreMapper {

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
        "16" -> "Animación"
        "35" -> "Comedia"
        "80" -> "Crimen"
        "99" -> "Documental"
        "18" -> "Drama"
        "10751" -> "Familiar"
        "10762" -> "Infantil"
        "9648" -> "Misterio"
        "10763" -> "Noticias"
        "10764" -> "Reality"
        "10765" -> "Sci-Fi y Fantasía"
        "10766" -> "Soap"
        "10767" -> "Talk"
        "10768" -> "Guerra y Política"
        "37" -> "Western"
        else -> "Series Variadas"
    }

    fun getGenreName(id: Int): String = getGenreName(id.toString())

    fun getGenreDescriptionKey(id: String): String = when (id) {
        "10759" -> "discover_genre_action"
        "16" -> "discover_genre_animation"
        "35" -> "discover_genre_comedy"
        "80" -> "discover_genre_crime"
        "99" -> "discover_genre_documentary"
        "18" -> "discover_genre_drama"
        "10751" -> "discover_genre_family"
        "10762" -> "discover_genre_kids"
        "9648" -> "discover_genre_mystery"
        "10763" -> "discover_genre_news"
        "10764" -> "discover_genre_reality"
        "10765" -> "discover_genre_scifi"
        "10766" -> "discover_genre_soap"
        "10767" -> "discover_genre_talk"
        "10768" -> "discover_genre_war"
        "37" -> "discover_genre_western"
        else -> "discover_genre_comedy"
    }
}
