package com.andrea.showmateapp.domain.usecase

import com.andrea.showmateapp.data.model.UserProfile
import com.andrea.showmateapp.data.network.MediaContent
import com.andrea.showmateapp.domain.repository.IInteractionRepository
import com.andrea.showmateapp.domain.repository.IShowRepository
import com.andrea.showmateapp.domain.repository.IUserRepository
import com.andrea.showmateapp.util.ExplorationEngine
import com.andrea.showmateapp.util.MoodContextEngine
import com.andrea.showmateapp.util.MoodContextEngine.DayType
import com.andrea.showmateapp.util.MoodContextEngine.TimeSlot
import com.andrea.showmateapp.util.Resource
import com.andrea.showmateapp.util.TemporalPatternAnalyzer
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.unmockkAll
import java.time.LocalDate
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * Tests de regresión del pipeline completo de recomendación.
 *
 * Cada test define un fixture (perfil + candidatos) y verifica el ordenamiento resultante.
 * Si una modificación en el algoritmo altera el orden, estos tests fallan intencionalmente.
 * Para aprobar un cambio legítimo, actualizar las aserciones y documentar el motivo.
 *
 * Contexto fijo para todos los tests:
 *  - Contexto de ánimo: mañana entre semana → multiplicador 1.0 para todos los géneros
 *  - isWeekday = true, viewingHistory = [] → TemporalPattern(0.5, 2.0, false) → boost=1.0
 *  - explorationFactor = MIN_EXPLORATION = 0.10
 *  - Sin boost colaborativo
 */
class RecommendationRegressionTest {

    // ── Mocks ─────────────────────────────────────────────────────────────────

    private val userRepository = mockk<IUserRepository>()
    private val interactionRepository = mockk<IInteractionRepository>()
    private val showRepository = mockk<IShowRepository>()
    private val collabUseCase = mockk<GetCollaborativeBoostUseCase>()

    private lateinit var useCase: GetRecommendationsUseCase

    private val neutralMoodContext = MoodContextEngine.MoodContext(TimeSlot.MORNING, DayType.WEEKDAY)

    @Before
    fun setup() {
        mockkObject(MoodContextEngine, TemporalPatternAnalyzer, ExplorationEngine)
        every { MoodContextEngine.currentContext() } returns neutralMoodContext
        every { MoodContextEngine.getMoodMultiplier(any(), any()) } returns 1.0f
        every { TemporalPatternAnalyzer.isWeekday() } returns true
        every { ExplorationEngine.calculateFactor(any()) } returns ExplorationEngine.MIN_EXPLORATION
        every { ExplorationEngine.unexploredGenres(any(), any()) } returns emptySet()
        coEvery { collabUseCase.execute(any()) } returns emptyMap()
        coEvery { showRepository.getPopularShows(any()) } returns Resource.Success(emptyList())

        useCase = GetRecommendationsUseCase(
            userRepository, interactionRepository, showRepository, collabUseCase
        )
    }

    @After
    fun teardown() = unmockkAll()

    // ── Helpers ───────────────────────────────────────────────────────────────

    private fun show(
        id: Int,
        genreIds: List<Int> = emptyList(),
        voteAverage: Float = 7f,
        voteCount: Int = 500,
        status: String? = null,
        seasons: Int? = null,
        firstAirDate: String? = null
    ) = MediaContent(
        id = id,
        name = "Show $id",
        genreIds = genreIds,
        voteAverage = voteAverage,
        voteCount = voteCount,
        status = status,
        numberOfSeasons = seasons,
        firstAirDate = firstAirDate
    )

    private fun stubPipeline(profile: UserProfile, candidates: List<MediaContent>) {
        coEvery { userRepository.getUserProfile() } returns profile
        coEvery { interactionRepository.getExcludedMediaIds() } returns emptySet()
        coEvery { showRepository.getDetailedRecommendations(any(), any()) } returns candidates
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // FIXTURE 1 — Drama expert: 5 drama shows de calidad variable
    // ═══════════════════════════════════════════════════════════════════════════

    /**
     * Fixture: drama expert + 5 candidates
     *
     * Expected score drivers (scores approximate, all with neutral mood×1.0 temporal×1.0):
     *
     * Show 1 (Drama, Ended, 2 seasons, voteAvg=8.5, votes=3000):
     *   - High cosine affinity (Drama profile), high Bayesian (3000 votes close to R=8.5)
     *   - completeness: +0.5 (Ended) + 0.3 (2 seasons) = +0.8 → WINNER
     *
     * Show 2 (Drama, Returning, 4 seasons, voteAvg=8.5, votes=3000):
     *   - Same rating/genre but no completeness boost → ranks below Show 1
     *
     * Show 3 (Drama, voteAvg=7.5, votes=100):
     *   - Hidden gem: personalAffinity≈10 ≥ 6.5, voteCount=100 ≤ 500 → +0.35
     *   - But low Bayesian (100 votes, prior dominates) → mid-rank
     *
     * Show 4 (Comedy, voteAvg=9.5, votes=5000):
     *   - Lowest affinity for drama user; high Bayesian doesn't compensate → last or near last
     *
     * Show 5 (Drama, voteAvg=6.0, votes=800):
     *   - Low rating + no special boosts → ranks last among Drama
     */
    @Test
    fun `FIXTURE1 drama expert - show1 ended ranks above show2 ongoing despite identical rating`() = runTest {
        // Given
        val profile = UserProfile(
            userId = "expert",
            genreScores = mapOf("18" to 40f),
            genreScoreDates = mapOf("18" to System.currentTimeMillis())
        )
        val show1 = show(1, listOf(18), 8.5f, 3000, "Ended", 2)
        val show2 = show(2, listOf(18), 8.5f, 3000, "Returning Series", 4)
        stubPipeline(profile, listOf(show1, show2))

        // When
        val result = useCase.execute()

        // Then — show1 wins by completeness boost (+0.8 vs 0)
        assertEquals("Show 1 (Ended+2seasons) should rank first", 1, result.first().id)
        assertTrue(
            "Show 1 score must be greater than Show 2",
            result.first { it.id == 1 }.affinityScore > result.first { it.id == 2 }.affinityScore
        )
    }

    @Test
    fun `FIXTURE1 drama expert - comedy show ranks below all drama shows`() = runTest {
        // Given
        val profile = UserProfile(
            userId = "expert",
            genreScores = mapOf("18" to 40f),
            genreScoreDates = mapOf("18" to System.currentTimeMillis())
        )
        val dramaShows = listOf(
            show(1, listOf(18), 8.5f, 3000, "Ended", 2),
            show(2, listOf(18), 8.5f, 3000, "Returning Series", 4),
            show(3, listOf(18), 7.5f, 100)
        )
        val comedyShow = show(4, listOf(35), 9.5f, 5000)
        stubPipeline(profile, dramaShows + listOf(comedyShow))

        // When
        val result = useCase.execute()

        // Then — comedy show should be last (zero genre affinity)
        assertEquals("Comedy show should rank last for drama expert", 4, result.last().id)
    }

    @Test
    fun `FIXTURE1 drama expert - show with hidden gem boost outranks show with equal rating but more votes`() =
        runTest {
            // Given
            val profile = UserProfile(
                userId = "expert",
                genreScores = mapOf("18" to 40f),
                genreScoreDates = mapOf("18" to System.currentTimeMillis())
            )
            // Both drama shows, same voteAverage; hiddenGem has far fewer votes
            val hiddenGem = show(3, listOf(18), voteAverage = 7.5f, voteCount = 100)
            val popularShow = show(5, listOf(18), voteAverage = 7.5f, voteCount = 800)
            stubPipeline(profile, listOf(hiddenGem, popularShow))

            // When
            val result = useCase.execute()
            val gemScore = result.first { it.id == 3 }.affinityScore
            val popularScore = result.first { it.id == 5 }.affinityScore

            // Then — voteCount=100 ≤ 500 AND personalAffinity≈10 ≥ 6.5 → +0.35 hidden gem boost
            // popularShow has higher Bayesian (800 votes better estimate) but no gem boost
            // hiddenGem: Bayesian(7.5, 100) = (100/250)*7.5 + (150/250)*6.5 = 3.0+3.9 = 6.9
            // popularShow: Bayesian(7.5, 800) = (800/950)*7.5 + (150/950)*6.5 = 6.32+1.03 = 7.35
            // Bayesian diff × 0.30 = (7.35-6.9)*0.30 = 0.135
            // hiddenGem boost = +0.35 > 0.135 → hiddenGem should still win
            assertTrue(
                "Hidden gem boost (+0.35) should overcome Bayesian advantage of popular show",
                gemScore > popularScore
            )
        }

    // ═══════════════════════════════════════════════════════════════════════════
    // FIXTURE 2 — Novelty: same show, different air dates
    // ═══════════════════════════════════════════════════════════════════════════

    @Test
    fun `FIXTURE2 novelty - show aired 2 weeks ago ranks above identical show from last year`() = runTest {
        // Given
        val today = LocalDate.now()
        val profile = UserProfile(
            userId = "u",
            genreScores = mapOf("18" to 20f),
            genreScoreDates = mapOf("18" to System.currentTimeMillis())
        )
        // Identical in every aspect except firstAirDate
        val freshShow = show(
            1,
            listOf(18),
            7.5f,
            1000,
            firstAirDate = today.minusDays(14).toString()
        ) // ≤1 month → +0.40
        val staleShow = show(
            2,
            listOf(18),
            7.5f,
            1000,
            firstAirDate = today.minusYears(1).toString()
        ) // >6 months → +0.00
        stubPipeline(profile, listOf(freshShow, staleShow))

        // When
        val result = useCase.execute()

        // Then
        assertEquals("Fresh show (novelty=0.40) must beat year-old show (novelty=0)", 1, result.first().id)
        val delta = result.first { it.id == 1 }.affinityScore - result.first { it.id == 2 }.affinityScore
        assertTrue("Score difference should be approximately 0.40 (novelty boost)", delta > 0.35f)
    }

    @Test
    fun `FIXTURE2 novelty - show aired 2 months ago ranks above show aired 5 months ago`() = runTest {
        // Given
        val today = LocalDate.now()
        val profile = UserProfile(
            userId = "u",
            genreScores = mapOf("18" to 20f),
            genreScoreDates = mapOf("18" to System.currentTimeMillis())
        )
        val twoMonthShow = show(1, listOf(18), 7.5f, 1000, firstAirDate = today.minusMonths(2).toString())
        val fiveMonthShow = show(2, listOf(18), 7.5f, 1000, firstAirDate = today.minusMonths(5).toString())
        stubPipeline(profile, listOf(twoMonthShow, fiveMonthShow))

        // When
        val result = useCase.execute()

        // Then — NOVELTY_BOOST_3M=0.20 > NOVELTY_BOOST_6M=0.10
        assertEquals("2-month show (novelty=0.20) must beat 5-month show (novelty=0.10)", 1, result.first().id)
        val delta = result.first { it.id == 1 }.affinityScore - result.first { it.id == 2 }.affinityScore
        assertEquals("Novelty difference should be 0.10", 0.10f, delta, 0.005f)
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // FIXTURE 3 — Saturation: genre concentration penalty
    // ═══════════════════════════════════════════════════════════════════════════

    @Test
    fun `FIXTURE3 saturation - drama shows are penalized when drama dominates profile`() = runTest {
        // Given — drama = 90/100 = 90% > threshold 45%
        val saturatedProfile = UserProfile(
            userId = "u",
            genreScores = mapOf("18" to 90f, "35" to 5f, "80" to 5f),
            genreScoreDates = mapOf(
                "18" to System.currentTimeMillis(),
                "35" to System.currentTimeMillis(),
                "80" to System.currentTimeMillis()
            )
        )
        val dramaShow = show(1, listOf(18), 7.5f, 800)
        val crimeShow = show(2, listOf(80), 7.5f, 800)
        stubPipeline(saturatedProfile, listOf(dramaShow, crimeShow))

        // When
        val result = useCase.execute()
        val dramaScore = result.first { it.id == 1 }.affinityScore
        val crimeScore = result.first { it.id == 2 }.affinityScore

        // Then — drama penalty = -0.20; crime has no penalty
        // Despite drama having higher affinity (from saturatedProfile), the penalty should make crime competitive
        assertTrue(
            "Saturation penalty should reduce drama score",
            dramaScore - crimeScore < 1.0f
        ) // Without penalty, delta would be much larger
    }

    @Test
    fun `FIXTURE3 saturation - non-saturated profile applies no penalty to any genre`() = runTest {
        // Given — genres balanced → max/total = 33% < 45%
        val balancedProfile = UserProfile(
            userId = "u",
            genreScores = mapOf("18" to 20f, "35" to 20f, "80" to 20f),
            genreScoreDates = mapOf(
                "18" to System.currentTimeMillis(),
                "35" to System.currentTimeMillis(),
                "80" to System.currentTimeMillis()
            )
        )
        val dramaShow = show(1, listOf(18), 7.5f, 800)
        val crimeShow = show(2, listOf(80), 7.5f, 800)
        stubPipeline(balancedProfile, listOf(dramaShow, crimeShow))

        // When
        val result = useCase.execute()
        val dramaScore = result.first { it.id == 1 }.affinityScore
        val crimeScore = result.first { it.id == 2 }.affinityScore

        // Then — same genre score for both → same embedding contribution → equal final scores
        assertEquals("Balanced genres: drama and crime should score equally", dramaScore, crimeScore, 0.001f)
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // FIXTURE 4 — Binge vs casual: same shows, different viewing patterns
    // ═══════════════════════════════════════════════════════════════════════════

    @Test
    fun `FIXTURE4 binge watcher gets boost for ongoing 3-season show, casual watcher does not`() = runTest {
        // Given
        val bingeHistory = listOf("2026-04-01:1:5", "2026-04-02:2:5", "2026-04-03:3:5")
        val bingeProfile = UserProfile(
            userId = "binge",
            genreScores = mapOf("18" to 20f),
            genreScoreDates = mapOf("18" to System.currentTimeMillis()),
            // avg = 15/3 = 5.0 ≥ 3.0 → binge
            viewingHistory = bingeHistory
        )
        val casualHistory = listOf("2026-04-01:1:1", "2026-04-02:2:1", "2026-04-03:3:1")
        val casualProfile = UserProfile(
            userId = "casual",
            genreScores = mapOf("18" to 20f),
            genreScoreDates = mapOf("18" to System.currentTimeMillis()),
            // avg = 3/3 = 1.0 < 3.0 → casual
            viewingHistory = casualHistory
        )
        // Show that qualifies for binge boost only
        val ongoingLong = show(1, listOf(18), 7.5f, 1000, "Returning Series", 3)

        // When
        coEvery { userRepository.getUserProfile() } returns bingeProfile
        val bingeResult = useCase.scoreShows(listOf(ongoingLong)).first().affinityScore

        coEvery { userRepository.getUserProfile() } returns casualProfile
        val casualResult = useCase.scoreShows(listOf(ongoingLong)).first().affinityScore

        // Then — binge watcher receives +0.20 boost that casual watcher does not
        val diff = bingeResult - casualResult
        assertTrue("Binge watcher score should be 0.20 higher for ongoing 3-season show", diff > 0.15f)
    }

    @Test
    fun `FIXTURE4 casual watcher gets boost for ended 2-season show, binge watcher does not`() = runTest {
        // Given
        val bingeHistory = listOf("2026-04-01:1:5", "2026-04-02:2:5", "2026-04-03:3:5")
        val casualHistory = listOf("2026-04-01:1:1", "2026-04-02:2:1", "2026-04-03:3:1")
        val endedShort = show(1, listOf(18), 7.5f, 1000, "Ended", 2)

        coEvery { userRepository.getUserProfile() } returns UserProfile(
            genreScores = mapOf("18" to 20f),
            genreScoreDates = mapOf("18" to System.currentTimeMillis()),
            viewingHistory = casualHistory
        )
        val casualScore = useCase.scoreShows(listOf(endedShort)).first().affinityScore

        coEvery { userRepository.getUserProfile() } returns UserProfile(
            genreScores = mapOf("18" to 20f),
            genreScoreDates = mapOf("18" to System.currentTimeMillis()),
            viewingHistory = bingeHistory
        )
        val bingeScore = useCase.scoreShows(listOf(endedShort)).first().affinityScore

        // Then — casual gets binge boost for ended 2-season; binge watcher does not
        // Note: endedShort also gets completeness boost (+0.8) for BOTH profiles
        // The difference in binge boost (+0.20) should still show
        assertTrue(
            "Casual watcher should score higher on ended 2-season show than binge watcher",
            casualScore > bingeScore
        )
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // FIXTURE 5 — Full pipeline: 8-show deterministic ordering
    // ═══════════════════════════════════════════════════════════════════════════

    /**
     * Full pipeline regression with 8 specific shows and a known profile.
     * Documents the expected relative ordering of key subsets.
     * Update these assertions intentionally when the algorithm is changed.
     */
    @Test
    fun `FIXTURE5 full pipeline - structural ordering holds with 8 candidate shows`() = runTest {
        // Given
        val today = LocalDate.now()
        val profile = UserProfile(
            userId = "regression",
            genreScores = mapOf("18" to 40f, "35" to 5f, "80" to 5f, "9648" to 3f),
            genreScoreDates = mapOf(
                "18" to System.currentTimeMillis(),
                "35" to System.currentTimeMillis(),
                "80" to System.currentTimeMillis(),
                "9648" to System.currentTimeMillis()
            )
        )

        val candidates = listOf(
            // Drama, Ended, 2 seasons, high rating
            show(1, listOf(18), 8.5f, 3000, "Ended", 2),
            // Drama, ongoing, high rating
            show(2, listOf(18), 8.5f, 3000, "Returning Series", 4),
            // Drama, hidden gem
            show(3, listOf(18), 7.0f, 100),
            // Comedy, top rating, no drama affinity
            show(4, listOf(35), 9.5f, 5000, "Ended", 3),
            // Drama, novelty boost
            show(
                5,
                listOf(18),
                8.0f,
                500,
                firstAirDate = today.minusDays(20).toString()
            ),
            // Crime
            show(6, listOf(80), 8.0f, 800),
            // Drama, low rating
            show(7, listOf(18), 6.0f, 200),
            // Sci-Fi
            show(8, listOf(10765), 7.5f, 1000)
        )
        stubPipeline(profile, candidates)

        // When
        val result = useCase.execute()

        assertEquals("All 8 candidates should be in result", 8, result.size)

        // ── Assert structural properties (not exact positions, since diversity reorders) ──

        // Show 4 (Comedy) and Show 8 (Sci-Fi) should score lower than Drama shows
        val show4Score = result.first { it.id == 4 }.affinityScore
        val show8Score = result.first { it.id == 8 }.affinityScore
        val show1Score = result.first { it.id == 1 }.affinityScore
        val show2Score = result.first { it.id == 2 }.affinityScore

        assertTrue("Drama show 1 should score higher than Comedy show 4", show1Score > show4Score)
        assertTrue("Drama show 2 should score higher than Sci-Fi show 8", show2Score > show8Score)

        // Show 5 (Drama, novelty) should outscore Show 7 (Drama, low rating)
        val show5Score = result.first { it.id == 5 }.affinityScore
        val show7Score = result.first { it.id == 7 }.affinityScore
        assertTrue("Show 5 (novelty + decent rating) should beat Show 7 (low rating)", show5Score > show7Score)

        // Show 1 (Ended + 2 seasons = completeness +0.8) should beat Show 2 (same rating, ongoing)
        assertTrue(
            "Show 1 (completeness boost) should beat Show 2 (no completeness boost)",
            show1Score > show2Score
        )

        // Both show 3 (voteCount=100) and show 7 (voteCount=200) receive the hidden gem boost
        // (both ≤ HIDDEN_GEM_VOTE_THRESHOLD=500, both Drama → personalAffinity ≥ 6.5).
        // Show 3 wins because its Bayesian rating is higher: 7.0 vs 6.0.
        //   bayesian(7.0, 100) ≈ 6.70 > bayesian(6.0, 200) ≈ 6.22
        val show3Score = result.first { it.id == 3 }.affinityScore
        assertTrue(
            "Show 3 (better Bayesian, same hidden gem boost as show 7) should score higher",
            show3Score > show7Score
        )
    }
}
