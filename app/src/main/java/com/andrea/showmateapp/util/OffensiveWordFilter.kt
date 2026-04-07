package com.andrea.showmateapp.util

object OffensiveWordFilter {

    private val terms = setOf(
        "idiota", "imbécil", "mierda", "puta", "puto", "coño", "gilipollas",
        "cabrón", "capullo", "bastardo", "joder", "hostia", "maricón",
        "imbecil", "cabron", "gilipollas", "estupido",
        "fuck", "shit", "asshole", "bitch", "cunt", "faggot", "nigger",
        "bastard", "motherfucker"
    )

    fun containsOffensiveContent(text: String): Boolean {
        val lower = text.lowercase()
        return terms.any { term -> lower.contains(term) }
    }
}
