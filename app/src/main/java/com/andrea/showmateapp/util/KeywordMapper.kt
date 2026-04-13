package com.andrea.showmateapp.util

object KeywordMapper {

    private val keywordMap = mapOf(
        "time travel" to Pair("4363", "Porque te gustan los viajes en el tiempo"),
        "dystopia" to Pair("318", "Porque te gustan las distopías"),
        "superhero" to Pair("9715", "Porque te gustan los superhéroes"),
        "supernatural" to Pair("9717", "Porque te gusta lo sobrenatural"),
        "based on novel" to Pair("818", "Porque te gustan las adaptaciones literarias"),
        "based on true story" to Pair("9672", "Porque te gustan las historias reales"),
        "serial killer" to Pair("189402", "Porque te gustan los thrillers de asesinos"),
        "vampire" to Pair("11", "Porque te gustan los vampiros"),
        "vampires" to Pair("11", "Porque te gustan los vampiros"),
        "zombie" to Pair("12377", "Porque te gustan los zombis"),
        "zombies" to Pair("12377", "Porque te gustan los zombis"),
        "space" to Pair("11075", "Porque te gusta el espacio"),
        "survival" to Pair("5765", "Porque te gustan las historias de supervivencia"),
        "spy" to Pair("10052", "Porque te gustan los espías"),
        "espionage" to Pair("10052", "Porque te gustan los espías"),
        "magic" to Pair("2343", "Porque te gusta la magia"),
        "heist" to Pair("9882", "Porque te gustan los atracos"),
        "robbery" to Pair("9882", "Porque te gustan los atracos"),
        "artificial intelligence" to Pair("9951", "Porque te gusta la inteligencia artificial"),
        "murder mystery" to Pair("207317", "Porque te gustan los misterios"),
        "mystery" to Pair("9648", "Porque te gustan los misterios"),
        "post-apocalyptic" to Pair("4565", "Porque te gustan las historias post-apocalípticas"),
        "post apocalyptic" to Pair("4565", "Porque te gustan las historias post-apocalípticas"),
        "conspiracy" to Pair("241527", "Porque te gustan las conspiraciones"),
        "alien" to Pair("9840", "Porque te gustan los extraterrestres"),
        "aliens" to Pair("9840", "Porque te gustan los extraterrestres"),
        "future" to Pair("2964", "Porque te gusta el futuro"),
        "anthology" to Pair("246332", "Porque te gustan las series de antología"),
        "animation" to Pair("210024", "Porque te gusta la animación"),
        "crime" to Pair("6075", "Porque te gustan los dramas de crimen"),
        "drugs" to Pair("4270", "Porque te gustan los thrillers de crimen"),
        "prison" to Pair("302", "Porque te gustan las historias de prisión"),
        "medical" to Pair("10292", "Porque te gustan las series médicas"),
        "lawyer" to Pair("5565", "Porque te gustan las series de abogados"),
        "detective" to Pair("9799", "Porque te gustan los detectives"),
        "witch" to Pair("2343", "Porque te gusta la magia"),
        "robot" to Pair("9951", "Porque te gusta la inteligencia artificial"),
        "war" to Pair("10769", "Porque te gustan las historias bélicas"),
        "historical" to Pair("256258", "Porque te gustan las series históricas"),
        "biography" to Pair("9672", "Porque te gustan las historias reales"),
        "politics" to Pair("10768", "Porque te gustan las series políticas"),
        "political" to Pair("10768", "Porque te gustan las series políticas")
    )

    fun getKeywordInfo(keywordName: String): Pair<String, String>? = keywordMap[keywordName.lowercase().trim()]

    fun getTopMappedKeyword(
        preferredKeywords: Map<String, Float>,
        excludeKeywordId: String = "4363"
    ): Triple<String, String, String>? {
        return preferredKeywords.entries
            .sortedByDescending { it.value }
            .firstNotNullOfOrNull { (name, _) ->
                val info = keywordMap[name.lowercase().trim()] ?: return@firstNotNullOfOrNull null
                if (info.first == excludeKeywordId) return@firstNotNullOfOrNull null
                Triple(name, info.first, info.second)
            }
    }
}
