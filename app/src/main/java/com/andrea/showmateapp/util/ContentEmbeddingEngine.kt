package com.andrea.showmateapp.util

import com.andrea.showmateapp.data.model.UserProfile
import com.andrea.showmateapp.data.model.MediaContent
import kotlin.math.sqrt

object ContentEmbeddingEngine {

    // IDs de géneros TMDB: orden fijo porque define la posición de cada dimensión en el vector
    private val GENRE_IDS = intArrayOf(
        10759, 16, 35, 80, 99, 18, 10751, 10762,
        9648, 10763, 10764, 10765, 10766, 10767, 10768, 37
    )

    // Límites empíricos: suficiente señal sin inflar la dimensión hasta degradar la similitud coseno
    private const val MAX_KEYWORDS = 30
    private const val MAX_ACTORS = 20
    private const val MAX_CREATORS = 10

    data class EmbeddingSpace(
        val topKeywords: List<String>,
        val topActors: List<String>,
        val topCreators: List<String>,
        val totalDim: Int
    )

    fun buildEmbeddingSpace(profile: UserProfile): EmbeddingSpace {
        val topKeywords = profile.preferredKeywords.entries
            .sortedByDescending { it.value }.take(MAX_KEYWORDS).map { it.key }
        val topActors = profile.preferredActors.entries
            .sortedByDescending { it.value }.take(MAX_ACTORS).map { it.key }
        val topCreators = profile.preferredCreators.entries
            .sortedByDescending { it.value }.take(MAX_CREATORS).map { it.key }
        val totalDim = GENRE_IDS.size + topKeywords.size + topActors.size + topCreators.size
        return EmbeddingSpace(topKeywords, topActors, topCreators, totalDim)
    }

    fun buildUserVector(profile: UserProfile, space: EmbeddingSpace): FloatArray {
        val vec = FloatArray(space.totalDim)

        // tanh comprime scores arbitrariamente grandes a [-1,1] sin saturar al nivel máximo observado
        fun normalize(score: Float, maxAbs: Float): Float = kotlin.math.tanh((score / maxAbs).toDouble()).toFloat()

        val maxAbsGenre = profile.genreScores.values.maxOfOrNull { kotlin.math.abs(it) }?.coerceAtLeast(1f) ?: 1f
        val maxAbsKw = profile.preferredKeywords.values.maxOfOrNull { kotlin.math.abs(it) }?.coerceAtLeast(1f) ?: 1f
        val maxAbsActor = profile.preferredActors.values.maxOfOrNull { kotlin.math.abs(it) }?.coerceAtLeast(1f) ?: 1f
        val maxAbsCreator = profile.preferredCreators.values
            .maxOfOrNull { kotlin.math.abs(it) }?.coerceAtLeast(1f) ?: 1f

        GENRE_IDS.forEachIndexed { i, genreId ->
            vec[i] = normalize(profile.genreScores[genreId.toString()] ?: 0f, maxAbsGenre).coerceAtLeast(0f)
        }
        val kwOffset = GENRE_IDS.size
        val actorOffset = kwOffset + space.topKeywords.size
        val creatorOffset = actorOffset + space.topActors.size

        space.topKeywords.forEachIndexed { i, kw ->
            vec[kwOffset + i] = normalize(profile.preferredKeywords[kw] ?: 0f, maxAbsKw).coerceAtLeast(0f)
        }
        space.topActors.forEachIndexed { i, actorId ->
            vec[actorOffset + i] = normalize(profile.preferredActors[actorId] ?: 0f, maxAbsActor).coerceAtLeast(0f)
        }
        space.topCreators.forEachIndexed { i, creatorId ->
            vec[creatorOffset + i] = normalize(profile.preferredCreators[creatorId] ?: 0f, maxAbsCreator).coerceAtLeast(0f)
        }
        return vec
    }

    fun buildShowVector(show: MediaContent, space: EmbeddingSpace): FloatArray {
        val vec = FloatArray(space.totalDim)
        val showGenres = show.safeGenreIds.toHashSet()
        val showKeywords = show.keywordNames.map { it.lowercase() }.toHashSet()
        val showActors = show.credits?.cast?.map { it.id.toString() }?.toHashSet() ?: emptySet<String>()
        val showCreators = show.creatorIds.map { it.toString() }.toHashSet()

        val kwOffset = GENRE_IDS.size
        val actorOffset = kwOffset + space.topKeywords.size
        val creatorOffset = actorOffset + space.topActors.size

        GENRE_IDS.forEachIndexed { i, genreId ->
            vec[i] = if (genreId in showGenres) 1f else 0f
        }
        space.topKeywords.forEachIndexed { i, kw ->
            vec[kwOffset + i] = if (kw.lowercase() in showKeywords) 1f else 0f
        }
        space.topActors.forEachIndexed { i, actorId ->
            vec[actorOffset + i] = if (actorId in showActors) 1f else 0f
        }
        space.topCreators.forEachIndexed { i, creatorId ->
            vec[creatorOffset + i] = if (creatorId in showCreators) 1f else 0f
        }
        return vec
    }

    fun cosineSimilarity(a: FloatArray, b: FloatArray): Float {
        var dot = 0.0
        var normA = 0.0
        var normB = 0.0
        for (i in a.indices) {
            dot += (a[i] * b[i]).toDouble()
            normA += (a[i] * a[i]).toDouble()
            normB += (b[i] * b[i]).toDouble()
        }
        val denom = sqrt(normA) * sqrt(normB)
        // Umbral 1e-9 evita división por cero con vectores casi nulos (usuario sin historial)
        return if (denom < 1e-9) 0f else (dot / denom).toFloat().coerceIn(-1f, 1f)
    }

    fun cosineSimilarityRatings(a: Map<Int, Float>, b: Map<Int, Float>): Float {
        if (a.isEmpty() || b.isEmpty()) return 0f
        var dot = 0.0
        for ((id, ratingA) in a) {
            val ratingB = b[id] ?: continue
            dot += ratingA * ratingB
        }
        var normA = 0.0
        var normB = 0.0
        a.values.forEach { normA += it * it }
        b.values.forEach { normB += it * it }
        val denom = sqrt(normA) * sqrt(normB)
        return if (denom == 0.0) 0f else (dot / denom).toFloat().coerceIn(-1f, 1f)
    }
}
