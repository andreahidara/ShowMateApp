package com.andrea.showmateapp.domain.usecase

import com.andrea.showmateapp.data.model.MediaContent
import com.andrea.showmateapp.data.model.UserProfile
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

class RecommendationRegressionTest {

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
        coEvery { interactionRepository.getExcludedMediaIds() } returns emptySet()
        coEvery { showRepository.getPopularShows(any()) } returns Resource.Success(emptyList())

        useCase = GetRecommendationsUseCase(
            userRepository, interactionRepository, showRepository, collabUseCase
        )
    }

    @After
    fun teardown() = unmockkAll()

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

    @Test
    fun `FIXTURE1 drama expert - show1 ended ranks above show2 ongoing despite identical rating`() = runTest {
        val profile = UserProfile(
            userId = "expert",
            genreScores = mapOf("18" to 40f),
            genreScoreDates = mapOf("18" to System.currentTimeMillis())
        )
        val show1 = show(1, listOf(18), 8.5f, 3000, "Ended", 2)
        val show2 = show(2, listOf(18), 8.5f, 3000, "Returning Series", 4)
        stubPipeline(profile, listOf(show1, show2))

        val result = useCase.execute()

        assertEquals(1, result.first().id)
        assertTrue(
            result.first { it.id == 1 }.affinityScore > result.first { it.id == 2 }.affinityScore
        )
    }

    @Test
    fun `FIXTURE1 drama expert - comedy show ranks below all drama shows`() = runTest {
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

        val result = useCase.execute()

        assertEquals(4, result.last().id)
    }

    @Test
    fun `FIXTURE1 drama expert - show with hidden gem boost outranks show with equal rating but more votes`() =
        runTest {
            val profile = UserProfile(
                userId = "expert",
                genreScores = mapOf("18" to 40f),
                genreScoreDates = mapOf("18" to System.currentTimeMillis())
            )
            val hiddenGem = show(3, listOf(18), voteAverage = 7.5f, voteCount = 100)
            val popularShow = show(5, listOf(18), voteAverage = 7.5f, voteCount = 800)
            stubPipeline(profile, listOf(hiddenGem, popularShow))

            val result = useCase.execute()
            val gemScore = result.first { it.id == 3 }.affinityScore
            val popularScore = result.first { it.id == 5 }.affinityScore

            assertTrue(
                gemScore > popularScore
            )
        }

    @Test
    fun `FIXTURE2 novelty - show aired 2 weeks ago ranks above identical show from last year`() = runTest {
        val today = LocalDate.now()
        val profile = UserProfile(
            userId = "u",
            genreScores = mapOf("18" to 20f),
            genreScoreDates = mapOf("18" to System.currentTimeMillis())
        )
        val freshShow = show(
            1,
            listOf(18),
            7.5f,
            1000,
            firstAirDate = today.minusDays(14).toString()
        )
        val staleShow = show(
            2,
            listOf(18),
            7.5f,
            1000,
            firstAirDate = today.minusYears(1).toString()
        )
        stubPipeline(profile, listOf(freshShow, staleShow))

        val result = useCase.execute()

        assertEquals(1, result.first().id)
        val delta = result.first { it.id == 1 }.affinityScore - result.first { it.id == 2 }.affinityScore
        assertTrue(delta > 0.35f)
    }

    @Test
    fun `FIXTURE2 novelty - show aired 2 months ago ranks above show aired 5 months ago`() = runTest {
        val today = LocalDate.now()
        val profile = UserProfile(
            userId = "u",
            genreScores = mapOf("18" to 20f),
            genreScoreDates = mapOf("18" to System.currentTimeMillis())
        )
        val twoMonthShow = show(1, listOf(18), 7.5f, 1000, firstAirDate = today.minusMonths(2).toString())
        val fiveMonthShow = show(2, listOf(18), 7.5f, 1000, firstAirDate = today.minusMonths(5).toString())
        stubPipeline(profile, listOf(twoMonthShow, fiveMonthShow))

        val result = useCase.execute()

        assertEquals(1, result.first().id)
        val delta = result.first { it.id == 1 }.affinityScore - result.first { it.id == 2 }.affinityScore
        assertEquals(0.10f, delta, 0.005f)
    }

    @Test
    fun `FIXTURE3 saturation - drama shows are penalized when drama dominates profile`() = runTest {
        val saturatedProfile = UserProfile(
            userId = "u",
            genreScores = mapOf("18" to 45f, "35" to 44f, "80" to 2f),
            genreScoreDates = mapOf(
                "18" to System.currentTimeMillis(),
                "35" to System.currentTimeMillis(),
                "80" to System.currentTimeMillis()
            )
        )
        val dramaShow = show(1, listOf(18), 7.0f, 500)
        val comedyShow = show(2, listOf(35), 7.0f, 500)
        stubPipeline(saturatedProfile, listOf(dramaShow, comedyShow))

        val result = useCase.execute()
        val dramaScore = result.first { it.id == 1 }.affinityScore
        val comedyScore = result.first { it.id == 2 }.affinityScore

        assertTrue(
            comedyScore > dramaScore
        )
    }

    @Test
    fun `FIXTURE3 saturation - non-saturated profile applies no penalty to any genre`() = runTest {
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

        val result = useCase.execute()
        val dramaScore = result.first { it.id == 1 }.affinityScore
        val crimeScore = result.first { it.id == 2 }.affinityScore

        assertEquals(dramaScore, crimeScore, 0.001f)
    }

    @Test
    fun `FIXTURE4 binge watcher gets boost for ongoing 3-season show, casual watcher does not`() = runTest {
        val bingeHistory = listOf("2026-04-01:1:5", "2026-04-02:2:5", "2026-04-03:3:5")
        val bingeProfile = UserProfile(
            userId = "binge",
            genreScores = mapOf("18" to 20f),
            genreScoreDates = mapOf("18" to System.currentTimeMillis()),
            viewingHistory = bingeHistory
        )
        val casualHistory = listOf("2026-04-01:1:1", "2026-04-02:2:1", "2026-04-03:3:1")
        val casualProfile = UserProfile(
            userId = "casual",
            genreScores = mapOf("18" to 20f),
            genreScoreDates = mapOf("18" to System.currentTimeMillis()),
            viewingHistory = casualHistory
        )
        val ongoingLong = show(1, listOf(18), 7.5f, 1000, "Returning Series", 3)

        coEvery { userRepository.getUserProfile() } returns bingeProfile
        val bingeResult = useCase.scoreShows(listOf(ongoingLong)).first().affinityScore

        coEvery { userRepository.getUserProfile() } returns casualProfile
        val casualResult = useCase.scoreShows(listOf(ongoingLong)).first().affinityScore

        val diff = bingeResult - casualResult
        assertTrue(diff > 0.15f)
    }

    @Test
    fun `FIXTURE4 casual watcher gets boost for ended 2-season show, binge watcher does not`() = runTest {
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

        assertTrue(
            casualScore > bingeScore
        )
    }

    @Test
    fun `FIXTURE5 full pipeline - structural ordering holds with 8 candidate shows`() = runTest {
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
            show(1, listOf(18), 8.5f, 3000, "Ended", 2),
            show(2, listOf(18), 8.5f, 3000, "Returning Series", 4),
            show(3, listOf(18), 7.0f, 100),
            show(4, listOf(35), 9.5f, 5000, "Ended", 3),
            show(
                5,
                listOf(18),
                8.0f,
                500,
                firstAirDate = today.minusDays(20).toString()
            ),
            show(6, listOf(80), 8.0f, 800),
            show(7, listOf(18), 6.0f, 200),
            show(8, listOf(10765), 7.5f, 1000)
        )
        stubPipeline(profile, candidates)

        val result = useCase.execute()

        assertEquals(8, result.size)

        val show4Score = result.first { it.id == 4 }.affinityScore
        val show8Score = result.first { it.id == 8 }.affinityScore
        val show1Score = result.first { it.id == 1 }.affinityScore
        val show2Score = result.first { it.id == 2 }.affinityScore

        assertTrue(show1Score > show4Score)
        assertTrue(show2Score > show8Score)

        val show5Score = result.first { it.id == 5 }.affinityScore
        val show7Score = result.first { it.id == 7 }.affinityScore
        assertTrue(show5Score > show7Score)

        assertTrue(
            show1Score > show2Score
        )

        val show3Score = result.first { it.id == 3 }.affinityScore
        assertTrue(
            show3Score > show7Score
        )
    }
}
