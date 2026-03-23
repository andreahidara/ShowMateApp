package com.example.showmateapp.domain.usecase

import com.example.showmateapp.data.model.UserProfile
import com.example.showmateapp.data.network.MediaContent
import com.example.showmateapp.data.repository.UserRepository
import com.example.showmateapp.data.repository.ShowRepository
import com.example.showmateapp.util.NarrativeStyleMapper
import com.example.showmateapp.util.TemporalPatternAnalyzer
import com.example.showmateapp.util.Resource
import javax.inject.Inject
import kotlin.math.exp
import kotlin.math.log10
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope

class GetRecommendationsUseCase @Inject constructor(
    private val userRepository: UserRepository,
    private val showRepository: ShowRepository,
    private val getCollaborativeBoostUseCase: GetCollaborativeBoostUseCase
) {
    companion object {
        // Puntuación final = 70% afinidad personal + 30% valoración global Bayesiana
        private const val W_PERSONAL = 0.7f
        private const val W_GLOBAL   = 0.3f

        // Subpesos de afinidad personal (deben sumar 1.0)
        private const val WG  = 0.37f  // genres
        private const val WK  = 0.22f  // keywords
        private const val WA  = 0.12f  // actors
        private const val WNS = 0.19f  // narrative style
        private const val WC  = 0.10f  // creators/showrunners

        // Parámetros de valoración Bayesiana
        private const val C  = 6.5f
        private const val M  = 150f

        // Decaimiento temporal: semivida ~90 días
        private const val DECAY_LAMBDA = 0.0077f

        // Diversidad: ningún género ocupa más del 35% de los resultados
        private const val MAX_GENRE_FRACTION = 0.35f

        // Serendipia: el 15% de los resultados son descubrimientos con alta valoración global y baja afinidad
        private const val SERENDIPITY_FRACTION = 0.15f

        // Abandono: penalización si el usuario vio menos del 20% de una serie con varias temporadas
        private const val ABANDONMENT_THRESHOLD     = 0.20f
        private const val ABANDONMENT_PENALTY       = 1.5f
        private const val AVG_EPISODES_PER_SEASON   = 10

        // Impulsos de novedad según los meses desde el estreno
        private const val NOVELTY_BOOST_1M = 0.4f
        private const val NOVELTY_BOOST_3M = 0.2f
        private const val NOVELTY_BOOST_6M = 0.1f

        // Saturación de género: si un género domina más del 45% de la puntuación acumulada, se penaliza
        private const val GENRE_SATURATION_THRESHOLD = 0.45f
        private const val GENRE_SATURATION_PENALTY   = 0.20f

        // Joya oculta: impulsa series que el usuario probablemente adorará pero muy pocas personas han valorado
        private const val HIDDEN_GEM_VOTE_THRESHOLD = 500
        private const val HIDDEN_GEM_MIN_AFFINITY   = 6.5f
        private const val HIDDEN_GEM_BOOST          = 0.35f

        // Perfil maratoniano: impulsa series largas en curso para bingewatchers y cortas finalizadas para casuales
        // — derivado de la media de episodios por sesión
        private const val BINGE_THRESHOLD_EPS  = 3.0f
        private const val BINGE_PROFILE_BOOST  = 0.20f
    }

    // Pre-calculado una vez por pasada de scoring para no iterar los mapas de puntuación por cada serie candidata.
    private data class ScoringWeights(
        val maxGenre: Float,
        val maxKeyword: Float,
        val maxActor: Float,
        val maxNarrative: Float,
        val maxCreator: Float,
        // Saturación de género — pre-calculada aquí para no recomputar filter/sum/maxBy por cada serie
        val saturatedGenreId: Int?,
        val isSaturated: Boolean
    )

    private fun buildScoringWeights(profile: UserProfile): ScoringWeights {
        val scores = profile.genreScores.filter { it.value > 0 }
        val saturatedGenreId: Int?
        val isSaturated: Boolean
        if (scores.size >= 3) {
            val total = scores.values.sum().coerceAtLeast(0.01f)
            val top   = scores.maxByOrNull { it.value }
            isSaturated      = top != null && top.value / total >= GENRE_SATURATION_THRESHOLD
            saturatedGenreId = top?.key?.toIntOrNull()
        } else {
            saturatedGenreId = null
            isSaturated      = false
        }
        return ScoringWeights(
            maxGenre         = profile.genreScores.values.maxOrNull()?.coerceAtLeast(1f) ?: 1f,
            maxKeyword       = profile.preferredKeywords.values.maxOrNull()?.coerceAtLeast(1f) ?: 1f,
            maxActor         = profile.preferredActors.values.maxOrNull()?.coerceAtLeast(1f) ?: 1f,
            maxNarrative     = profile.narrativeStyleScores.values.maxOrNull()?.coerceAtLeast(1f) ?: 1f,
            maxCreator       = profile.preferredCreators.values.maxOrNull()?.coerceAtLeast(1f) ?: 1f,
            saturatedGenreId = saturatedGenreId,
            isSaturated      = isSaturated
        )
    }

    suspend fun execute(): List<MediaContent> {
        return try {
            val userProfile = userRepository.getUserProfile()

            if (userProfile == null || userProfile.genreScores.isEmpty()) {
                val popular = showRepository.getPopularShows()
                return if (popular is Resource.Success) popular.data else emptyList()
            }

            // Usar "|" (OR) en lugar de "," (AND) — la coma exige que la serie tenga TODOS los géneros a la vez.
            val genres = userProfile.genreScores
                .filter { it.value > 0 }
                .entries
                .sortedByDescending { it.value }
                .take(5)
                .map { it.key }
                .joinToString("|")
                .ifEmpty { null }

            // Obtiene candidatos y boost colaborativo en paralelo — son llamadas de red independientes.
            val (candidates, collaborativeBoost) = coroutineScope {
                val candidatesJob = async { showRepository.getDetailedRecommendations(genres) }
                val collabJob     = async {
                    try { getCollaborativeBoostUseCase.execute(userProfile) ?: emptyMap() }
                    catch (e: Exception) { emptyMap<Int, Float>() }
                }
                candidatesJob.await() to collabJob.await()
            }

            val watchedIds       = userRepository.getWatchedMediaIds()
            val watchedEpisodesMap = userProfile.watchedEpisodes

            // Búsqueda O(1) en lugar de O(n) candidates.find() por cada serie vista.
            val candidatesById = candidates.associateBy { it.id }

            // Filtro inteligente de vistas: solo excluye series con más del 50% de completado.
            val trulyWatchedIds = watchedIds.filter { id ->
                val watched  = watchedEpisodesMap[id.toString()]?.size ?: 0
                val totalEst = ((candidatesById[id]?.numberOfSeasons ?: 1) * AVG_EPISODES_PER_SEASON).coerceAtLeast(1)
                watched.toFloat() / totalEst > 0.50f
            }.toSet()

            val excludedIds = userProfile.dislikedMediaIds.toSet() + trulyWatchedIds

            // Amplía el pool de candidatos con las series de mayor boost colaborativo que no estaban ya en él.
            // Sin esto, el boost colaborativo se calcula pero las series a las que apunta nunca aparecen.
            val collabOnlyIds = collaborativeBoost.entries
                .sortedByDescending { it.value }
                .take(15)
                .map { it.key }
                .filter { it !in candidatesById && it !in excludedIds }
            val allCandidates = if (collabOnlyIds.isEmpty()) candidates
                                else candidates + showRepository.getShowDetailsInParallel(collabOnlyIds)

            val now = System.currentTimeMillis()
            val decayedProfile = applyTimeDecay(userProfile, now)
            // Calcula los máximos de cada mapa una sola vez; se reutilizan en todas las series candidatas.
            val weights = buildScoringWeights(decayedProfile)

            val temporalPattern = TemporalPatternAnalyzer.analyze(userProfile.viewingHistory)
            val isWeekday = TemporalPatternAnalyzer.isWeekday()
            val today     = java.time.LocalDate.now()

            val scored = allCandidates
                .filter { it.id !in excludedIds }
                .map { show ->
                    val abandonmentPenalty = calculateAbandonmentPenalty(show, watchedEpisodesMap)
                    val noveltyBoost       = calculateNoveltyBoost(show, today)
                    val collabBoost        = collaborativeBoost[show.id] ?: 0f
                    val temporal           = TemporalPatternAnalyzer.getContextBoost(
                        temporalPattern, show.episodeRunTime?.firstOrNull(), isWeekday
                    )
                    scoreShow(
                        show, decayedProfile, weights, abandonmentPenalty, noveltyBoost,
                        collabBoost, temporal, temporalPattern.avgEpisodesPerSession
                    )
                }
                .sortedByDescending { it.affinityScore }

            val diversified = applyDiversityFilter(scored)
            applySerendipity(diversified)

        } catch (e: Exception) {
            android.util.Log.e("GetRecommendations", "Error executing recommendations", e)
            val popular = showRepository.getPopularShows()
            if (popular is Resource.Success) popular.data else emptyList()
        }
    }

    suspend fun scoreShows(shows: List<MediaContent>): List<MediaContent> {
        return try {
            val userProfile = userRepository.getUserProfile()
            if (userProfile == null || userProfile.genreScores.isEmpty()) return shows
            val now = System.currentTimeMillis()
            val decayedProfile = applyTimeDecay(userProfile, now)
            val weights = buildScoringWeights(decayedProfile)
            val isWeekday = TemporalPatternAnalyzer.isWeekday()
            val today     = java.time.LocalDate.now()
            val temporalPattern = TemporalPatternAnalyzer.analyze(userProfile.viewingHistory)
            shows.map { show ->
                scoreShow(
                    show, decayedProfile, weights,
                    noveltyBoost = calculateNoveltyBoost(show, today),
                    temporalMultiplier = TemporalPatternAnalyzer.getContextBoost(
                        temporalPattern, show.episodeRunTime?.firstOrNull(), isWeekday
                    ),
                    avgEpsPerSession = temporalPattern.avgEpisodesPerSession
                )
            }.sortedByDescending { it.affinityScore }
        } catch (e: Exception) {
            android.util.Log.e("GetRecommendations", "Error scoring shows", e)
            shows
        }
    }

    // ── Decaimiento temporal ──────────────────────────────────────────────────

    private fun applyTimeDecay(profile: UserProfile, now: Long): UserProfile {
        fun decayMap(scores: Map<String, Float>, dates: Map<String, Long>): Map<String, Float> {
            return scores.mapValues { (key, score) ->
                val lastUpdated = dates[key] ?: now
                val daysSince   = ((now - lastUpdated) / 86_400_000L).toFloat().coerceAtLeast(0f)
                score * exp(-DECAY_LAMBDA * daysSince)
            }
        }
        return profile.copy(
            genreScores          = decayMap(profile.genreScores,          profile.genreScoreDates),
            preferredKeywords    = decayMap(profile.preferredKeywords,    profile.keywordScoreDates),
            preferredActors      = decayMap(profile.preferredActors,      profile.actorScoreDates),
            narrativeStyleScores = decayMap(profile.narrativeStyleScores, profile.narrativeStyleDates),
            preferredCreators    = decayMap(profile.preferredCreators,    profile.creatorScoreDates)
        )
    }

    // ── Filtro de diversidad ──────────────────────────────────────────────────

    private fun applyDiversityFilter(scored: List<MediaContent>): List<MediaContent> {
        val maxPerGenre = (scored.size * MAX_GENRE_FRACTION).toInt().coerceAtLeast(3)
        val genreCount  = mutableMapOf<Int, Int>()
        val result      = mutableListOf<MediaContent>()

        for (show in scored) {
            val dominantGenre = show.safeGenreIds.firstOrNull() ?: -1
            val count         = genreCount.getOrDefault(dominantGenre, 0)
            if (count < maxPerGenre) {
                result.add(show)
                genreCount[dominantGenre] = count + 1
            }
        }
        if (result.size < scored.size) {
            val included = result.map { it.id }.toSet()
            result.addAll(scored.filter { it.id !in included })
        }
        return result
    }

    // ── Serendipia ────────────────────────────────────────────────────────────

    /**
     * Reemplaza el último [SERENDIPITY_FRACTION] de resultados con series de alta valoración
     * Bayesiana y baja afinidad — permitiendo descubrir contenido fuera de la zona de confort.
     * Los elementos de serendipia se intercalan cada ~7 posiciones.
     */
    private fun applySerendipity(scored: List<MediaContent>): List<MediaContent> {
        if (scored.size < 10) return scored
        val serendipityCount = (scored.size * SERENDIPITY_FRACTION).toInt().coerceAtLeast(1)
        val mainCount        = scored.size - serendipityCount

        val main = scored.take(mainCount).toMutableList()
        val serendipityCandidates = scored.drop(mainCount)
            .sortedByDescending { calculateBayesianRating(it) }

        serendipityCandidates.forEachIndexed { i, show ->
            val pos = ((i + 1) * 7).coerceAtMost(main.size)
            main.add(pos, show)
        }
        return main
    }

    // ── Puntuación ───────────────────────────────────────────────────────────

    private fun scoreShow(
        show: MediaContent,
        user: UserProfile,
        weights: ScoringWeights,
        abandonmentPenalty: Float = 0f,
        noveltyBoost: Float = 0f,
        collabBoost: Float = 0f,
        temporalMultiplier: Float = 1.0f,
        avgEpsPerSession: Float = 2.0f
    ): MediaContent {
        val personal     = calculatePersonalAffinity(show, user, weights) * temporalMultiplier
        val global       = calculateBayesianRating(show)
        val completeness = calculateCompletenessBoost(show)

        val saturationPenalty = calculateGenreSaturationPenalty(show, weights)
        val hiddenGemBoost    = calculateHiddenGemBoost(show, personal)
        val bingeBoost        = calculateBingeProfileBoost(show, avgEpsPerSession)

        val score = ((personal * W_PERSONAL) + (global * W_GLOBAL) + completeness +
                     noveltyBoost + collabBoost + hiddenGemBoost + bingeBoost -
                     abandonmentPenalty - saturationPenalty)
            .coerceIn(0f, 11f)
        return show.copy(affinityScore = score)
    }

    private fun calculatePersonalAffinity(show: MediaContent, user: UserProfile, weights: ScoringWeights): Float {
        val normalizedGenreScore = scoreByMap(
            keys       = show.safeGenreIds.map { it.toString() },
            userScores = user.genreScores,
            maxWeight  = weights.maxGenre
        )
        val normalizedKeywordScore = scoreByMap(
            keys       = show.keywordNames,
            userScores = user.preferredKeywords,
            maxWeight  = weights.maxKeyword,
            capCount   = 5f
        )
        val normalizedActorScore = scoreByMap(
            keys       = show.credits?.cast?.map { it.id.toString() } ?: emptyList(),
            userScores = user.preferredActors,
            maxWeight  = weights.maxActor,
            capCount   = 5f
        )
        val normalizedCreatorScore = scoreByMap(
            keys       = show.creatorIds.map { it.toString() },
            userScores = user.preferredCreators,
            maxWeight  = weights.maxCreator,
            capCount   = 3f
        )

        val showStyles = NarrativeStyleMapper.extractStyles(show.keywordNames, show.episodeRunTime?.firstOrNull())
        val normalizedNarrativeScore = if (showStyles.isNotEmpty()) {
            val raw = showStyles.entries.fold(0f) { acc, (style, relevance) ->
                val userPref = (user.narrativeStyleScores[style] ?: 0f) / weights.maxNarrative
                acc + (userPref * relevance).coerceIn(-1f, 1f)
            }
            (raw / showStyles.size).coerceIn(-1f, 1f)
        } else 0f

        val rawAffinity = (WG  * normalizedGenreScore)     +
                          (WK  * normalizedKeywordScore)   +
                          (WA  * normalizedActorScore)     +
                          (WNS * normalizedNarrativeScore) +
                          (WC  * normalizedCreatorScore)
        val baseScore = ((rawAffinity + 1f) / 2f) * 10f
        val popBoost  = if (show.popularity > 1f)
            (log10(show.popularity.toDouble()).toFloat() * 0.5f).coerceIn(0f, 1.5f) else 0f

        return (baseScore + popBoost).coerceIn(0f, 10f)
    }

    // Normaliza una lista de claves puntuadas contra el mapa de puntuaciones del usuario.
    // capCount limita el denominador para que pocos señales fuertes prevalezcan sobre muchas débiles.
    private fun scoreByMap(
        keys: List<String>,
        userScores: Map<String, Float>,
        maxWeight: Float,
        capCount: Float = Float.MAX_VALUE
    ): Float {
        if (keys.isEmpty()) return 0f
        val raw = keys.fold(0f) { acc, key ->
            acc + ((userScores[key] ?: 0f) / maxWeight).coerceIn(-1f, 1f)
        }
        return (raw / minOf(capCount, keys.size.toFloat())).coerceIn(-1f, 1f)
    }

    private fun calculateBayesianRating(show: MediaContent): Float {
        val v = show.voteCount.toFloat()
        val R = show.voteAverage
        return if (v + M > 0) ((v / (v + M)) * R) + ((M / (v + M)) * C) else C
    }

    private fun calculateCompletenessBoost(show: MediaContent): Float {
        var boost = 0f
        if (show.status == "Ended" || show.status == "Canceled") boost += 0.5f
        val seasons = show.numberOfSeasons ?: Int.MAX_VALUE
        if (seasons in 1..3) boost += 0.3f
        return boost
    }

    /** Penaliza series donde el usuario vio menos del 20% de una serie con varias temporadas (probablemente abandonada). */
    private fun calculateAbandonmentPenalty(show: MediaContent, watchedEpisodes: Map<String, List<Int>>): Float {
        val watched = watchedEpisodes[show.id.toString()]?.size ?: return 0f
        if (watched == 0) return 0f
        val totalEst = ((show.numberOfSeasons ?: 1) * AVG_EPISODES_PER_SEASON).coerceAtLeast(1)
        val completionRate = watched.toFloat() / totalEst
        return if (completionRate < ABANDONMENT_THRESHOLD && (show.numberOfSeasons ?: 1) > 1)
            ABANDONMENT_PENALTY else 0f
    }

    /** Impulsa series estrenadas recientemente para compensar el bajo número de votos Bayesianos.
     *  [today] debe calcularse una vez fuera del bucle de scoring y pasarse aquí para evitar
     *  crear N objetos LocalDate durante la misma pasada. */
    private fun calculateNoveltyBoost(show: MediaContent, today: java.time.LocalDate): Float {
        val dateStr = show.firstAirDate?.take(10) ?: return 0f
        return try {
            val release = java.time.LocalDate.parse(dateStr)
            val months  = java.time.temporal.ChronoUnit.MONTHS.between(release, today)
            when {
                months <= 1 -> NOVELTY_BOOST_1M
                months <= 3 -> NOVELTY_BOOST_3M
                months <= 6 -> NOVELTY_BOOST_6M
                else        -> 0f
            }
        } catch (e: Exception) { 0f }
    }

    /**
     * Penalización por saturación de género — evita el efecto "burbuja de gustos".
     * Si un género supera el 45% de la puntuación total acumulada del usuario,
     * las series de ese género reciben una pequeña penalización para fomentar la variedad.
     */
    private fun calculateGenreSaturationPenalty(show: MediaContent, weights: ScoringWeights): Float {
        if (!weights.isSaturated || weights.saturatedGenreId == null) return 0f
        return if (show.safeGenreIds.contains(weights.saturatedGenreId)) GENRE_SATURATION_PENALTY else 0f
    }

    /**
     * Impulso de joya oculta — destaca series de alta afinidad que todavía
     * no han sido ampliamente descubiertas (menos de 500 votos).
     */
    private fun calculateHiddenGemBoost(show: MediaContent, personalAffinityScore: Float): Float {
        if (show.voteCount > HIDDEN_GEM_VOTE_THRESHOLD) return 0f
        if (personalAffinityScore < HIDDEN_GEM_MIN_AFFINITY) return 0f
        return HIDDEN_GEM_BOOST
    }

    /**
     * Adaptación al perfil maratoniano — aprende si el usuario es bingewatcher
     * (media ≥ 3 ep/sesión) o espectador casual, y ajusta el impulso en consecuencia.
     */
    private fun calculateBingeProfileBoost(show: MediaContent, avgEpsPerSession: Float): Float {
        val seasons   = show.numberOfSeasons ?: 1
        val isOngoing = show.status in listOf("Returning Series", "In Production")
        val isEnded   = show.status == "Ended" || show.status == "Canceled"
        return when {
            avgEpsPerSession >= BINGE_THRESHOLD_EPS && seasons >= 3 && isOngoing -> BINGE_PROFILE_BOOST
            avgEpsPerSession <  BINGE_THRESHOLD_EPS && seasons <= 2 && isEnded   -> BINGE_PROFILE_BOOST
            else -> 0f
        }
    }
}
