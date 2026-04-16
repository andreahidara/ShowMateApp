package com.andrea.showmateapp.domain.usecase

import com.andrea.showmateapp.data.model.ReasonType
import com.andrea.showmateapp.data.model.RecommendationReason
import com.andrea.showmateapp.data.model.UserProfile
import com.andrea.showmateapp.data.model.MediaContent
import com.andrea.showmateapp.domain.repository.IInteractionRepository
import com.andrea.showmateapp.domain.repository.IShowRepository
import com.andrea.showmateapp.domain.repository.IUserRepository
import com.andrea.showmateapp.util.ContentEmbeddingEngine
import com.andrea.showmateapp.util.ExplorationEngine
import com.andrea.showmateapp.util.GenreMapper
import com.andrea.showmateapp.util.MoodContextEngine
import com.andrea.showmateapp.util.NarrativeStyleMapper
import com.andrea.showmateapp.util.Resource
import com.andrea.showmateapp.util.TemporalPatternAnalyzer
import com.andrea.showmateapp.util.UiText
import com.andrea.showmateapp.R
import javax.inject.Inject
import kotlin.math.exp
import kotlin.math.log10
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import timber.log.Timber

class GetRecommendationsUseCase @Inject constructor(
    private val userRepository: IUserRepository,
    private val interactionRepository: IInteractionRepository,
    private val showRepository: IShowRepository,
    private val getCollaborativeBoostUseCase: GetCollaborativeBoostUseCase
) {
    companion object {
        private const val W_PERSONAL = 0.70f
        private const val W_GLOBAL = 0.30f
        private const val NARRATIVE_ADDITIVE_MAX = 1.50f
        private const val C = 6.5f
        private const val M = 50f
        private const val DECAY_LAMBDA = 0.0077f
        private const val MAX_GENRE_FRACTION = 0.35f
        private const val SERENDIPITY_FRACTION = 0.15f
        private const val ABANDONMENT_THRESHOLD = 0.20f
        private const val ABANDONMENT_PENALTY = 1.50f
        private const val AVG_EPISODES_PER_SEASON = 10
        private const val NOVELTY_BOOST_1M = 0.40f
        private const val NOVELTY_BOOST_3M = 0.20f
        private const val NOVELTY_BOOST_6M = 0.10f
        private const val GENRE_SATURATION_THRESHOLD = 0.45f
        private const val GENRE_SATURATION_PENALTY = 2.50f
        private const val HIDDEN_GEM_VOTE_THRESHOLD = 500
        private const val HIDDEN_GEM_MIN_AFFINITY = 6.5f
        private const val HIDDEN_GEM_BOOST = 0.35f
        private const val BINGE_THRESHOLD_EPS = 3.0f
        private const val BINGE_PROFILE_BOOST = 0.60f
        private const val EXPLORATION_BONUS = 0.30f

        private val ALL_GENRE_IDS = setOf(
            10759, 16, 35, 80, 99, 18, 10751, 10762,
            9648, 10763, 10764, 10765, 10766, 10767, 10768, 37
        )
    }

    private data class RecommendationContext(
        val decayedProfile: UserProfile,
        val weights: ScoringWeights,
        val temporalPattern: TemporalPatternAnalyzer.TemporalPattern,
        val isWeekday: Boolean,
        val today: java.time.LocalDate,
        val watchedEpisodesMap: Map<String, List<Int>>,
        val embeddingSpace: ContentEmbeddingEngine.EmbeddingSpace,
        val userVector: FloatArray,
        val explorationFactor: Float,
        val moodContext: MoodContextEngine.MoodContext,
        val unexploredGenres: Set<Int>
    )

    private data class ScoringWeights(
        val maxNarrative: Float,
        val saturatedGenreId: Int?,
        val isSaturated: Boolean
    )

    private fun buildRecommendationContext(profile: UserProfile, now: Long): RecommendationContext {
        val decayed = applyTimeDecay(profile, now)
        val positiveGenres = decayed.genreScores.filter { it.value > 0 }
        val saturatedGenreId: Int?
        val isSaturated: Boolean
        if (positiveGenres.size >= 3) {
            val total = positiveGenres.values.sum().coerceAtLeast(0.01f)
            val top = positiveGenres.maxByOrNull { it.value }
            isSaturated = top != null && top.value / total >= GENRE_SATURATION_THRESHOLD
            saturatedGenreId = top?.key?.toIntOrNull()
        } else {
            saturatedGenreId = null
            isSaturated = false
        }

        val weights = ScoringWeights(
            maxNarrative = decayed.narrativeStyleScores.values.maxOrNull()?.coerceAtLeast(1f) ?: 1f,
            saturatedGenreId = saturatedGenreId,
            isSaturated = isSaturated
        )

        val embeddingSpace = ContentEmbeddingEngine.buildEmbeddingSpace(decayed)
        val userVector = ContentEmbeddingEngine.buildUserVector(decayed, embeddingSpace)

        return RecommendationContext(
            decayedProfile = decayed,
            weights = weights,
            temporalPattern = TemporalPatternAnalyzer.analyze(profile.viewingHistory),
            isWeekday = TemporalPatternAnalyzer.isWeekday(),
            today = java.time.LocalDate.now(),
            watchedEpisodesMap = profile.watchedEpisodes,
            embeddingSpace = embeddingSpace,
            userVector = userVector,
            explorationFactor = ExplorationEngine.calculateFactor(profile),
            moodContext = MoodContextEngine.currentContext(),
            unexploredGenres = ExplorationEngine.unexploredGenres(profile, ALL_GENRE_IDS)
        )
    }

    suspend fun execute(): List<MediaContent> {
        return try {
            val userProfile = userRepository.getUserProfile()
            val excludedIds = interactionRepository.getExcludedMediaIds().toList()

            if (userProfile == null || userProfile.genreScores.all { it.value <= 0 }) {
                val popular = showRepository.getPopularShows(excludedIds)
                return if (popular is Resource.Success) popular.data else emptyList()
            }

            val genres = userProfile.genreScores
                .filter { it.value > 0 }
                .entries
                .sortedByDescending { it.value }
                .take(5)
                .joinToString("|") { it.key }
                .ifEmpty { null }

            val (candidates, collaborativeBoost) = coroutineScope {
                val candidatesJob = async { showRepository.getDetailedRecommendations(genres, excludedIds) }
                val collabJob = async {
                    try {
                        getCollaborativeBoostUseCase.execute(userProfile)
                    } catch (e: Exception) {
                        emptyMap<Int, Float>()
                    }
                }
                candidatesJob.await() to collabJob.await()
            }

            val candidatesById = candidates.associateBy { it.id }
            val collabOnlyIds = collaborativeBoost.entries
                .sortedByDescending { it.value }
                .take(15)
                .map { it.key }
                .filter { it !in candidatesById && it !in excludedIds }
            val allCandidates = if (collabOnlyIds.isEmpty()) {
                candidates
            } else {
                candidates + showRepository.getShowDetailsInParallel(collabOnlyIds)
            }

            val now = System.currentTimeMillis()
            val context = buildRecommendationContext(userProfile, now)

            val scored = allCandidates
                .filter { it.id !in excludedIds }
                .map { show ->
                    scoreShow(
                        show = show,
                        context = context,
                        abandonmentPenalty = calculateAbandonmentPenalty(show, context.watchedEpisodesMap),
                        noveltyBoost = calculateNoveltyBoost(show, context.today),
                        collabBoost = collaborativeBoost[show.id] ?: 0f
                    )
                }
                .sortedByDescending { it.affinityScore }

            val diversified = applyDiversityFilter(scored, context.explorationFactor)
            applySerendipity(diversified, context.explorationFactor)
        } catch (e: Exception) {
            if (e is CancellationException) throw e
            Timber.e(e, "Error executing recommendations")
            val popular = showRepository.getPopularShows()
            if (popular is Resource.Success) popular.data else emptyList()
        }
    }

    suspend fun scoreShows(shows: List<MediaContent>): List<MediaContent> {
        return try {
            val userProfile = userRepository.getUserProfile()
            val excludedIds = interactionRepository.getExcludedMediaIds().toSet()
            if (userProfile == null || userProfile.genreScores.isEmpty()) {
                return shows.filter { it.id !in excludedIds }
            }
            val context = buildRecommendationContext(userProfile, System.currentTimeMillis())

            shows.filter { it.id !in excludedIds }
                .map { show ->
                    scoreShow(
                        show = show,
                        context = context,
                        noveltyBoost = calculateNoveltyBoost(show, context.today)
                    )
                }.sortedByDescending { it.affinityScore }
        } catch (e: Exception) {
            if (e is CancellationException) throw e
            Timber.e(e, "Error scoring shows")
            shows
        }
    }

    private fun applyTimeDecay(profile: UserProfile, now: Long): UserProfile {
        fun decayMap(scores: Map<String, Float>, dates: Map<String, Long>): Map<String, Float> =
            scores.mapValues { (key, score) ->
                val interactionTime = dates[key] ?: now
                val daysSince = ((now - interactionTime) / 86_400_000L).toFloat().coerceAtLeast(0f)
                score * exp(-DECAY_LAMBDA * daysSince)
            }
        return profile.copy(
            genreScores = decayMap(profile.genreScores, profile.genreScoreDates),
            preferredKeywords = decayMap(profile.preferredKeywords, profile.keywordScoreDates),
            preferredActors = decayMap(profile.preferredActors, profile.actorScoreDates),
            narrativeStyleScores = decayMap(profile.narrativeStyleScores, profile.narrativeStyleDates),
            preferredCreators = decayMap(profile.preferredCreators, profile.creatorScoreDates)
        )
    }

    private fun scoreShow(
        show: MediaContent,
        context: RecommendationContext,
        abandonmentPenalty: Float = 0f,
        noveltyBoost: Float = 0f,
        collabBoost: Float = 0f
    ): MediaContent {
        val showVector = ContentEmbeddingEngine.buildShowVector(show, context.embeddingSpace)
        val cosineSim = ContentEmbeddingEngine.cosineSimilarity(context.userVector, showVector)
        val embeddingScore = cosineSim * 10f

        val showStyles = NarrativeStyleMapper.extractStyles(
            show.keywordNames,
            show.episodeRunTime?.firstOrNull()
        )
        val narrativeContrib = calculateNarrativeContrib(showStyles, context.decayedProfile, context.weights)

        val popBoost = if (show.popularity > 1f) {
            (log10(show.popularity.toDouble()).toFloat() * 0.5f).coerceIn(0f, 1.5f)
        } else {
            0f
        }

        val personalAffinity = embeddingScore + narrativeContrib + popBoost

        val moodMultiplier = MoodContextEngine.getMoodMultiplier(show, context.moodContext)
        val temporalMultiplier = TemporalPatternAnalyzer.getContextBoost(
            context.temporalPattern,
            show.episodeRunTime?.firstOrNull(),
            context.isWeekday
        )

        val global = calculateBayesianRating(show)
        val completeness = calculateCompletenessBoost(show)
        val saturationPenalty = calculateGenreSaturationPenalty(show, context.weights)
        val hiddenGemBoost = calculateHiddenGemBoost(show, personalAffinity)
        val bingeBoost = calculateBingeProfileBoost(show, context.temporalPattern.avgEpisodesPerSession)

        val explorationBonus = if (show.safeGenreIds.any { it in context.unexploredGenres }) {
            EXPLORATION_BONUS * context.explorationFactor
        } else {
            0f
        }

        val score = (
            (personalAffinity * moodMultiplier * temporalMultiplier * W_PERSONAL) +
                (global * W_GLOBAL) +
                completeness + noveltyBoost + collabBoost +
                hiddenGemBoost + bingeBoost + explorationBonus -
                abandonmentPenalty - saturationPenalty
            ).coerceIn(0f, 11f)

        val reasons = buildReasons(
            show = show,
            context = context,
            cosineSim = cosineSim,
            showStyles = showStyles,
            collabBoost = collabBoost,
            hiddenGemBoost = hiddenGemBoost,
            bingeBoost = bingeBoost
        )

        return show.copy(affinityScore = score, reasons = reasons)
    }

    private fun calculateNarrativeContrib(
        showStyles: Map<String, Float>,
        user: UserProfile,
        weights: ScoringWeights
    ): Float {
        if (showStyles.isEmpty()) return 0f
        val raw = showStyles.entries.fold(0f) { acc, (style, relevance) ->
            val userPref = (user.narrativeStyleScores[style] ?: 0f)
            val clampedPref = kotlin.math.tanh(userPref / 15.0).toFloat()
            acc + clampedPref * relevance
        }
        val normalized = (raw / showStyles.size).coerceIn(-1f, 1f)
        return normalized * NARRATIVE_ADDITIVE_MAX
    }

    private fun calculateBayesianRating(show: MediaContent): Float {
        val v = show.voteCount.toFloat()
        val r = show.voteAverage
        return if (v + M > 0) ((v * r) + (M * C)) / (v + M) else C
    }

    private fun calculateCompletenessBoost(show: MediaContent): Float {
        var boost = 0f
        if (show.status == "Ended" || show.status == "Canceled") boost += 0.5f
        val seasons = show.numberOfSeasons ?: Int.MAX_VALUE
        if (seasons in 1..3) boost += 0.3f
        return boost
    }

    private fun calculateAbandonmentPenalty(show: MediaContent, watchedEpisodes: Map<String, List<Int>>): Float {
        val watched = watchedEpisodes[show.id.toString()]?.size ?: return 0f
        if (watched == 0) return 0f
        val seasons = show.numberOfSeasons ?: 1
        val totalEst = (seasons * AVG_EPISODES_PER_SEASON).coerceAtLeast(1)
        val completionRate = watched.toFloat() / totalEst
        return if (completionRate < ABANDONMENT_THRESHOLD && seasons > 1) {
            ABANDONMENT_PENALTY
        } else {
            0f
        }
    }

    private fun calculateNoveltyBoost(show: MediaContent, today: java.time.LocalDate): Float {
        val dateStr = show.firstAirDate?.take(10) ?: return 0f
        return try {
            val months = java.time.temporal.ChronoUnit.MONTHS.between(
                java.time.LocalDate.parse(dateStr),
                today
            )
            when {
                months <= 1 -> NOVELTY_BOOST_1M
                months <= 3 -> NOVELTY_BOOST_3M
                months <= 6 -> NOVELTY_BOOST_6M
                else -> 0f
            }
        } catch (e: Exception) {
            0f
        }
    }

    private fun calculateGenreSaturationPenalty(show: MediaContent, weights: ScoringWeights): Float {
        if (!weights.isSaturated || weights.saturatedGenreId == null) return 0f
        return if (show.safeGenreIds.contains(weights.saturatedGenreId)) GENRE_SATURATION_PENALTY else 0f
    }

    private fun calculateHiddenGemBoost(show: MediaContent, personalAffinity: Float): Float {
        if (show.voteCount > HIDDEN_GEM_VOTE_THRESHOLD) return 0f
        if (personalAffinity < HIDDEN_GEM_MIN_AFFINITY) return 0f
        return HIDDEN_GEM_BOOST
    }

    private fun calculateBingeProfileBoost(show: MediaContent, avgEpsPerSession: Float): Float {
        val seasons = show.numberOfSeasons ?: 1
        val isOngoing = show.status in listOf("Returning Series", "In Production")
        val isEnded = show.status == "Ended" || show.status == "Canceled"
        return when {
            avgEpsPerSession >= BINGE_THRESHOLD_EPS && seasons >= 3 && (isOngoing || show.status == null) -> BINGE_PROFILE_BOOST
            avgEpsPerSession < BINGE_THRESHOLD_EPS && seasons <= 2 && (isEnded || show.status == null) -> BINGE_PROFILE_BOOST
            else -> 0f
        }
    }

    private fun applyDiversityFilter(scored: List<MediaContent>, explorationFactor: Float): List<MediaContent> {
        val effectiveFraction = MAX_GENRE_FRACTION * (1f - explorationFactor * 0.28f)
        val maxPerGenre = (scored.size * effectiveFraction).toInt().coerceAtLeast(3)
        val genreCount = mutableMapOf<Int, Int>()
        val result = mutableListOf<MediaContent>()

        for (show in scored) {
            val dominantGenre = show.safeGenreIds.firstOrNull() ?: -1
            val count = genreCount.getOrDefault(dominantGenre, 0)
            if (count < maxPerGenre) {
                result.add(show)
                genreCount[dominantGenre] = count + 1
            }
        }
        val included = result.map { it.id }.toHashSet()
        result.addAll(scored.filter { it.id !in included })
        return result
    }

    private fun buildReasons(
        show: MediaContent,
        context: RecommendationContext,
        cosineSim: Float,
        showStyles: Map<String, Float>,
        collabBoost: Float,
        hiddenGemBoost: Float,
        bingeBoost: Float
    ): List<RecommendationReason> {
        val profile = context.decayedProfile
        val personal = mutableListOf<RecommendationReason>()
        val systemic = mutableListOf<RecommendationReason>()

        val topGenreEntry = show.safeGenreIds
            .mapNotNull { id -> profile.genreScores[id.toString()]?.let { id to it } }
            .filter { it.second > 3f }
            .maxByOrNull { it.second }
        topGenreEntry?.let { (genreId, genreScore) ->
            val weight = kotlin.math.tanh(genreScore / 20.0).toFloat().coerceIn(0.15f, 1f)
            personal += RecommendationReason(
                ReasonType.GENRE, weight,
                UiText.StringResource(R.string.reason_genre, GenreMapper.getGenreName(genreId)),
                genreEmoji(genreId)
            )
        }

        val topActorEntry = show.credits?.cast
            ?.mapNotNull { actor -> profile.preferredActors[actor.id.toString()]?.let { actor to it } }
            ?.filter { it.second > 3f }
            ?.maxByOrNull { it.second }
        topActorEntry?.let { (actor, actorScore) ->
            val weight = kotlin.math.tanh(actorScore / 20.0).toFloat().coerceIn(0.15f, 1f)
            if (weight > 0.20f) {
                personal += RecommendationReason(
                    ReasonType.ACTOR, weight,
                    UiText.StringResource(R.string.reason_actor, actor.name), "🎬"
                )
            }
        }

        val topStyleEntry = showStyles.entries
            .mapNotNull { (style, rel) ->
                profile.narrativeStyleScores[style]?.takeIf { it > 3f }?.let { style to (it * rel) }
            }
            .maxByOrNull { it.second }
        topStyleEntry?.let { (style, rawScore) ->
            val weight = kotlin.math.tanh(rawScore / 15.0).toFloat().coerceIn(0.15f, 1f)
            if (weight > 0.15f) {
                personal += RecommendationReason(
                    ReasonType.NARRATIVE, weight,
                    narrativeDescription(style), narrativeEmoji(style)
                )
            }
        }

        val topCreatorEntry = show.credits?.crew
            ?.filter { it.job in MediaContent.CREATOR_JOBS }
            ?.mapNotNull { crew -> profile.preferredCreators[crew.id.toString()]?.let { crew to it } }
            ?.filter { it.second > 3f }
            ?.maxByOrNull { it.second }
        topCreatorEntry?.let { (crew, creatorScore) ->
            val weight = kotlin.math.tanh(creatorScore / 20.0).toFloat().coerceIn(0.15f, 1f)
            if (weight > 0.25f) {
                personal += RecommendationReason(
                    ReasonType.CREATOR, weight,
                    UiText.StringResource(R.string.reason_creator, crew.name), "🎯"
                )
            }
        }

        if (collabBoost > 0.30f) {
            val weight = (collabBoost / 1.20f).coerceIn(0f, 1f)
            personal += RecommendationReason(
                ReasonType.COLLABORATIVE, weight,
                UiText.StringResource(R.string.reason_collaborative), "👥"
            )
        }

        if (personal.size < 2) {
            if (hiddenGemBoost > 0f) {
                systemic += RecommendationReason(
                    ReasonType.HIDDEN_GEM, 0.70f,
                    UiText.StringResource(R.string.reason_hidden_gem, (cosineSim * 100).toInt()), "💎"
                )
            }
            if (bingeBoost > 0f) {
                val seasons = show.numberOfSeasons ?: 1
                systemic += RecommendationReason(
                    ReasonType.BINGE, 0.50f,
                    if (seasons >= 3) {
                        UiText.StringResource(R.string.reason_binge_long, seasons)
                    } else {
                        UiText.StringResource(R.string.reason_binge_short)
                    },
                    "🍿"
                )
            }
            if (personal.isEmpty()) {
                if (show.status == "Ended" || show.status == "Canceled") {
                    systemic += RecommendationReason(
                        ReasonType.COMPLETENESS, 0.40f,
                        UiText.StringResource(R.string.reason_completeness), "✅"
                    )
                }
                if (show.popularity > 100f && show.voteCount > 1000) {
                    systemic += RecommendationReason(
                        ReasonType.TRENDING,
                        (show.popularity / 1000f).coerceIn(0.10f, 0.80f),
                        UiText.StringResource(R.string.reason_trending), "🔥"
                    )
                }
            }
        }

        val sortedPersonal = personal.sortedByDescending { r: RecommendationReason -> r.weight }
        val sortedSystemic = systemic.sortedByDescending { r: RecommendationReason -> r.weight }
        return (sortedPersonal + sortedSystemic).take(3)
    }

    private fun genreEmoji(genreId: Int): String = when (genreId) {
        10759 -> "⚔️"
        16 -> "🎨"
        35 -> "😂"
        80 -> "🔫"
        99 -> "🎥"
        18 -> "🎭"
        10751 -> "👨‍👩‍👧"
        10762 -> "🧸"
        9648 -> "🔍"
        10765 -> "🚀"
        10768 -> "⚡"
        37 -> "🤠"
        else -> "🎬"
    }

    private fun narrativeDescription(style: String): UiText = when (style) {
        "narrativa_compleja" -> UiText.StringResource(R.string.narrative_desc_complex)
        "protagonista_detective" -> UiText.StringResource(R.string.narrative_desc_detective)
        "protagonista_antihero" -> UiText.StringResource(R.string.narrative_desc_antihero)
        "protagonista_genio" -> UiText.StringResource(R.string.narrative_desc_genius)
        "tono_oscuro" -> UiText.StringResource(R.string.narrative_desc_dark)
        "tono_emocional" -> UiText.StringResource(R.string.narrative_desc_emotional)
        "tono_ligero" -> UiText.StringResource(R.string.narrative_desc_light)
        "ritmo_intenso" -> UiText.StringResource(R.string.narrative_desc_intense)
        "ritmo_lento" -> UiText.StringResource(R.string.narrative_desc_slow)
        "ritmo_episodico" -> UiText.StringResource(R.string.narrative_desc_episodic)
        "ritmo_largo" -> UiText.StringResource(R.string.narrative_desc_long)
        else -> UiText.StringResource(R.string.narrative_desc_default)
    }

    private fun narrativeEmoji(style: String): String = when (style) {
        "narrativa_compleja" -> "🧩"
        "protagonista_detective" -> "🔎"
        "protagonista_antihero" -> "😈"
        "protagonista_genio" -> "🧠"
        "tono_oscuro" -> "🌑"
        "tono_emocional" -> "🥺"
        "tono_ligero" -> "😄"
        "ritmo_intenso" -> "⚡"
        "ritmo_lento" -> "🕯️"
        "ritmo_episodico" -> "⏱️"
        "ritmo_largo" -> "📺"
        else -> "📖"
    }

    private fun applySerendipity(scored: List<MediaContent>, explorationFactor: Float): List<MediaContent> {
        if (scored.size < 10) return scored

        val effectiveFraction = SERENDIPITY_FRACTION * (1f + explorationFactor * 1.1f)
        val serendipityCount = (scored.size * effectiveFraction).toInt().coerceAtLeast(1)
        val mainCount = scored.size - serendipityCount

        val main = scored.take(mainCount).toMutableList()
        val pool = scored.drop(mainCount)

        val serendipityCandidates = if (explorationFactor > 0.5f) {
            pool.sortedWith(
                compareByDescending<MediaContent> { show ->
                    val genreNovelty = if (show.safeGenreIds.firstOrNull() !in
                        main.take(10).flatMap { it.safeGenreIds }.toSet()
                    ) {
                        1
                    } else {
                        0
                    }
                    genreNovelty.toFloat() + calculateBayesianRating(show) * 0.1f
                }
            )
        } else {
            pool.sortedByDescending { calculateBayesianRating(it) }
        }

        serendipityCandidates.forEachIndexed { i, show ->
            val pos = ((i + 1) * 7).coerceAtMost(main.size)
            main.add(pos, show)
        }
        return main
    }
}

