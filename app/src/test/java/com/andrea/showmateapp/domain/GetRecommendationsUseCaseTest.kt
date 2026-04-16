package com.andrea.showmateapp.domain

import com.andrea.showmateapp.data.model.MediaContent
import com.andrea.showmateapp.data.model.UserProfile
import com.andrea.showmateapp.domain.repository.IInteractionRepository
import com.andrea.showmateapp.domain.repository.IShowRepository
import com.andrea.showmateapp.domain.repository.IUserRepository
import com.andrea.showmateapp.domain.usecase.GetCollaborativeBoostUseCase
import com.andrea.showmateapp.domain.usecase.GetRecommendationsUseCase
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
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class GetRecommendationsUseCaseTest {

    private val userRepository = mockk<IUserRepository>()
    private val interactionRepository = mockk<IInteractionRepository>()
    private val showRepository = mockk<IShowRepository>()
    private val collabUseCase = mockk<GetCollaborativeBoostUseCase>()

    private lateinit var useCase: GetRecommendationsUseCase

    private val neutralMoodContext = MoodContextEngine.MoodContext(TimeSlot.MORNING, DayType.WEEKDAY)

    @Before
    fun setup() {
        mockkObject(MoodContextEngine)
        mockkObject(TemporalPatternAnalyzer)
        mockkObject(ExplorationEngine)

        every { MoodContextEngine.currentContext() } returns neutralMoodContext
        every { MoodContextEngine.getMoodMultiplier(any(), any()) } returns 1.0f

        every { TemporalPatternAnalyzer.isWeekday() } returns true

        every { ExplorationEngine.calculateFactor(any()) } returns ExplorationEngine.MIN_EXPLORATION
        every { ExplorationEngine.unexploredGenres(any(), any()) } returns emptySet()

        coEvery { collabUseCase.execute(any()) } returns emptyMap()
        coEvery { interactionRepository.getExcludedMediaIds() } returns emptySet()

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
        firstAirDate: String? = null,
        popularity: Float = 0f,
        episodeRunTime: List<Int>? = null
    ) = MediaContent(
        id = id,
        name = "Show $id",
        genreIds = genreIds,
        voteAverage = voteAverage,
        voteCount = voteCount,
        status = status,
        numberOfSeasons = seasons,
        firstAirDate = firstAirDate,
        popularity = popularity,
        episodeRunTime = episodeRunTime
    )

    private val dramaProfile = UserProfile(
        userId = "u1",
        genreScores = mapOf("18" to 30f, "35" to 5f),
        genreScoreDates = mapOf(
            "18" to System.currentTimeMillis(),
            "35" to System.currentTimeMillis()
        )
    )

    private fun stubExecute(
        profile: UserProfile = dramaProfile,
        candidates: List<MediaContent>,
        watchedIds: Set<Int> = emptySet()
    ) {
        coEvery { userRepository.getUserProfile() } returns profile
        coEvery { interactionRepository.getExcludedMediaIds() } returns watchedIds
        coEvery { showRepository.getDetailedRecommendations(any(), any()) } returns candidates
        coEvery { showRepository.getPopularShows(any()) } returns Resource.Success(emptyList())
    }

    @Test
    fun `given null profile, when execute, then fallback to popular shows`() = runTest {
        val popular = listOf(show(99))
        coEvery { userRepository.getUserProfile() } returns null
        coEvery { interactionRepository.getExcludedMediaIds() } returns emptySet()
        coEvery { showRepository.getPopularShows(any()) } returns Resource.Success(popular)

        val result = useCase.execute()

        assertEquals(1, result.size)
        assertEquals(99, result.first().id)
    }

    @Test
    fun `given profile with empty genreScores, when execute, then fallback to popular shows`() = runTest {
        val emptyProfile = UserProfile(userId = "u1")
        val popular = listOf(show(99))
        coEvery { userRepository.getUserProfile() } returns emptyProfile
        coEvery { interactionRepository.getExcludedMediaIds() } returns emptySet()
        coEvery { showRepository.getPopularShows(any()) } returns Resource.Success(popular)

        val result = useCase.execute()

        assertEquals(99, result.first().id)
    }

    @Test
    fun `given profile with only negative genreScores, when execute, then fallback to popular`() = runTest {
        val negativeProfile = UserProfile(
            userId = "u1",
            genreScores = mapOf("18" to -5f, "35" to -3f)
        )
        val popular = listOf(show(99))
        coEvery { userRepository.getUserProfile() } returns negativeProfile
        coEvery { interactionRepository.getExcludedMediaIds() } returns emptySet()
        coEvery { showRepository.getPopularShows(any()) } returns Resource.Success(popular)
        coEvery { showRepository.getDetailedRecommendations(any(), any()) } returns emptyList()

        val result = useCase.execute()

        assertTrue(result.isNotEmpty())
    }

    @Test
    fun `given single candidate that is disliked, when execute, then result is empty`() = runTest {
        val profileWithDislike = dramaProfile.copy(dislikedMediaIds = listOf(1))
        stubExecute(profile = profileWithDislike, candidates = listOf(show(1, genreIds = listOf(18))), watchedIds = setOf(1))

        val result = useCase.execute()

        assertTrue(result.none { it.id == 1 })
    }

    @Test
    fun `given single valid candidate, when execute, then it is returned with affinity set`() = runTest {
        stubExecute(candidates = listOf(show(1, genreIds = listOf(18))))

        val result = useCase.execute()

        assertEquals(1, result.size)
        assertTrue(result.first().affinityScore > 0f)
    }

    @Test
    fun `given show with more than 50pct watched, when execute, then it is excluded`() = runTest {
        val profileWithWatched = dramaProfile.copy(
            watchedEpisodes = mapOf("1" to (1..6).toList())
        )
        val watchedShow = show(1, genreIds = listOf(18), seasons = 1)
        val newShow = show(2, genreIds = listOf(18))
        stubExecute(profile = profileWithWatched, candidates = listOf(watchedShow, newShow), watchedIds = setOf(1))

        val result = useCase.execute()

        assertTrue(result.none { it.id == 1 })
        assertEquals(1, result.size)
    }

    @Test
    fun `given show with exactly 50pct watched, when execute, then it is NOT excluded`() = runTest {
        val profileWithWatched = dramaProfile.copy(
            watchedEpisodes = mapOf("1" to (1..5).toList())
        )
        val borderlineShow = show(1, genreIds = listOf(18), seasons = 1)
        stubExecute(profile = profileWithWatched, candidates = listOf(borderlineShow), watchedIds = emptySet())

        val result = useCase.execute()

        assertTrue(result.any { it.id == 1 })
    }

    @Test
    fun `given two drama shows with same voteAverage, when scored, then higher voteCount wins`() = runTest {
        val popular = show(1, genreIds = listOf(18), voteAverage = 8f, voteCount = 2000)
        val obscure = show(2, genreIds = listOf(18), voteAverage = 8f, voteCount = 10)
        coEvery { userRepository.getUserProfile() } returns dramaProfile

        val result = useCase.scoreShows(listOf(popular, obscure))

        val popularScore = result.first { it.id == 1 }.affinityScore
        val obscureScore = result.first { it.id == 2 }.affinityScore
        assertTrue("Popular score ($popularScore) should be > obscure score ($obscureScore)",
            popularScore > obscureScore
        )
    }

    @Test
    fun `given show with very few votes, when scored, then Bayesian pulls score toward prior 6_5`() = runTest {
        val exaggerated = show(1, genreIds = listOf(18), voteAverage = 9f, voteCount = 5)
        val steady = show(2, genreIds = listOf(18), voteAverage = 7f, voteCount = 500)
        coEvery { userRepository.getUserProfile() } returns dramaProfile

        val result = useCase.scoreShows(listOf(exaggerated, steady))

        val scoreExaggerated = result.first { it.id == 1 }.affinityScore
        val scoreSteady = result.first { it.id == 2 }.affinityScore
        assertTrue("Steady score ($scoreSteady) should be >= exaggerated score ($scoreExaggerated)",
            scoreSteady >= scoreExaggerated
        )
    }

    @Test
    fun `given genre interaction 90 days old, when scored, then it decays below recent interaction`() = runTest {
        val ninetyDaysAgo = System.currentTimeMillis() - 90L * 24 * 60 * 60 * 1000
        val mixedProfile = UserProfile(
            userId = "u1",
            genreScores = mapOf("18" to 30f, "35" to 30f),
            genreScoreDates = mapOf("18" to System.currentTimeMillis(), "35" to ninetyDaysAgo)
        )
        val dramaShow = show(1, genreIds = listOf(18))
        val comedyShow = show(2, genreIds = listOf(35))
        coEvery { userRepository.getUserProfile() } returns mixedProfile

        val result = useCase.scoreShows(listOf(dramaShow, comedyShow))

        val dramaScore = result.first { it.id == 1 }.affinityScore
        val comedyScore = result.first { it.id == 2 }.affinityScore
        assertTrue("Drama score ($dramaScore) should be > comedy score ($comedyScore) due to decay",
            dramaScore > comedyScore
        )
    }

    @Test
    fun `given genre interaction 180 days old, when scored, then decay is stronger than at 90 days`() = runTest {
        val ninetyDays = System.currentTimeMillis() - 90L * 24 * 60 * 60 * 1000
        val hundredEightyDays = System.currentTimeMillis() - 180L * 24 * 60 * 60 * 1000

        val profile90 = UserProfile(
            "u1",
            genreScores = mapOf("18" to 20f, "35" to 50f),
            genreScoreDates = mapOf("18" to ninetyDays, "35" to System.currentTimeMillis())
        )

        val profile180 = UserProfile(
            "u2",
            genreScores = mapOf("18" to 20f, "35" to 50f),
            genreScoreDates = mapOf("18" to hundredEightyDays, "35" to System.currentTimeMillis())
        )

        val dramaShow = show(1, genreIds = listOf(18))

        coEvery { userRepository.getUserProfile() } returns profile90
        val score90 = useCase.scoreShows(listOf(dramaShow)).first().affinityScore

        coEvery { userRepository.getUserProfile() } returns profile180
        val score180 = useCase.scoreShows(listOf(dramaShow)).first().affinityScore

        assertTrue(
            score180 < score90
        )
    }

    @Test
    fun `given ended show with 2 seasons, when scored, then it beats identical ongoing show`() = runTest {
        val ended = show(
            1,
            genreIds = listOf(18),
            voteAverage = 7f,
            voteCount = 500,
            status = "Ended",
            seasons = 2
        )
        val ongoing = show(2, genreIds = listOf(18), voteAverage = 7f, voteCount = 500)
        coEvery { userRepository.getUserProfile() } returns dramaProfile

        val result = useCase.scoreShows(listOf(ended, ongoing))

        assertTrue(result.first().id == 1)
        assertTrue(
            result.first { it.id == 1 }.affinityScore > result.first { it.id == 2 }.affinityScore
        )
    }

    @Test
    fun `given canceled show with 5 seasons, when scored, then it gets only status boost not season boost`() = runTest {
        val canceledLong = show(
            1,
            genreIds = listOf(18),
            voteAverage = 7f,
            voteCount = 500,
            status = "Canceled",
            seasons = 5
        )
        val endedShort = show(
            2,
            genreIds = listOf(18),
            voteAverage = 7f,
            voteCount = 500,
            status = "Ended",
            seasons = 2
        )
        coEvery { userRepository.getUserProfile() } returns dramaProfile

        val result = useCase.scoreShows(listOf(canceledLong, endedShort))

        assertTrue(
            result.first { it.id == 2 }.affinityScore > result.first { it.id == 1 }.affinityScore
        )
    }

    @Test
    fun `given show aired less than 1 month ago, when scored, then it gets max novelty boost`() = runTest {
        val today = LocalDate.now()
        val recentStr = today.minusDays(15).toString()
        val oldStr = today.minusMonths(12).toString()
        val recentShow = show(
            1,
            genreIds = listOf(18),
            voteAverage = 7f,
            voteCount = 500,
            firstAirDate = recentStr
        )
        val oldShow = show(
            2,
            genreIds = listOf(18),
            voteAverage = 7f,
            voteCount = 500,
            firstAirDate = oldStr
        )
        coEvery { userRepository.getUserProfile() } returns dramaProfile

        val result = useCase.scoreShows(listOf(recentShow, oldShow))

        assertTrue(
            result.first { it.id == 1 }.affinityScore > result.first { it.id == 2 }.affinityScore
        )
    }

    @Test
    fun `given shows at 2 months and 4 months, when scored, then 2-month gets larger boost`() = runTest {
        val today = LocalDate.now()
        val twoMonths = show(
            1,
            genreIds = listOf(18),
            voteAverage = 7f,
            voteCount = 500,
            firstAirDate = today.minusMonths(2).toString()
        )
        val fourMonths = show(
            2,
            genreIds = listOf(18),
            voteAverage = 7f,
            voteCount = 500,
            firstAirDate = today.minusMonths(4).toString()
        )
        coEvery { userRepository.getUserProfile() } returns dramaProfile

        val result = useCase.scoreShows(listOf(twoMonths, fourMonths))

        assertTrue(
            result.first().id == 1
        )
    }

    @Test
    fun `given show aired more than 6 months ago, when scored, then no novelty boost`() = runTest {
        val today = LocalDate.now()
        val oldShow = show(
            1,
            genreIds = listOf(18),
            voteAverage = 7f,
            voteCount = 500,
            firstAirDate = today.minusMonths(8).toString()
        )
        val noDateShow = show(
            2,
            genreIds = listOf(18),
            voteAverage = 7f,
            voteCount = 500,
            firstAirDate = null
        )
        coEvery { userRepository.getUserProfile() } returns dramaProfile

        val result = useCase.scoreShows(listOf(oldShow, noDateShow))
        val scoreOld = result.first { it.id == 1 }.affinityScore
        val scoreNoDate = result.first { it.id == 2 }.affinityScore

        assertEquals(
            scoreOld,
            scoreNoDate,
            0.001f
        )
    }

    @Test
    fun `given show with less than 20pct watched and more than 1 season, when scored, then penalty applied`() =
        runTest {
            val abandonedShow = show(1, genreIds = listOf(18), voteAverage = 7f, voteCount = 500, seasons = 2)
            val freshShow = show(2, genreIds = listOf(18), voteAverage = 7f, voteCount = 500)
            val profileWithWatched = dramaProfile.copy(
                watchedEpisodes = mapOf("1" to listOf(1))
            )
            stubExecute(profile = profileWithWatched, candidates = listOf(abandonedShow, freshShow))

            val result = useCase.execute()

            val abandoned = result.first { it.id == 1 }
            val fresh = result.first { it.id == 2 }
            assertTrue(
                fresh.affinityScore > abandoned.affinityScore
            )
        }

    @Test
    fun `given show with less than 20pct watched but only 1 season, when scored, then no penalty`() = runTest {
        val singleSeasonPartial = show(1, genreIds = listOf(18), voteAverage = 7f, voteCount = 500, seasons = 1)
        val freshShow = show(2, genreIds = listOf(18), voteAverage = 7f, voteCount = 500, seasons = 1)
        val profileWithWatched = dramaProfile.copy(
            watchedEpisodes = mapOf("1" to listOf(1))
        )
        stubExecute(profile = profileWithWatched, candidates = listOf(singleSeasonPartial, freshShow))

        val result = useCase.execute()

        val s1 = result.first { it.id == 1 }.affinityScore
        val s2 = result.first { it.id == 2 }.affinityScore
        assertEquals(s1, s2, 0.001f)
    }

    @Test
    fun `given saturated drama profile, when scored, then drama show receives saturation penalty`() = runTest {
        val saturatedProfile = UserProfile(
            userId = "u1",
            genreScores = mapOf("18" to 45f, "35" to 44f, "80" to 2f),
            genreScoreDates = mapOf(
                "18" to System.currentTimeMillis(),
                "35" to System.currentTimeMillis(),
                "80" to System.currentTimeMillis()
            )
        )

        val dramaShow = show(1, genreIds = listOf(18), voteAverage = 7f, voteCount = 500)
        val comedyShow = show(2, genreIds = listOf(35), voteAverage = 7f, voteCount = 500)

        coEvery { userRepository.getUserProfile() } returns saturatedProfile
        val results = useCase.scoreShows(listOf(dramaShow, comedyShow))
        val scoreDrama = results.first { it.id == 1 }.affinityScore
        val scoreComedy = results.first { it.id == 2 }.affinityScore

        assertTrue(
            scoreComedy > scoreDrama
        )
    }

    @Test
    fun `given balanced genre profile, when scored, then no saturation penalty`() = runTest {
        val balancedProfile = UserProfile(
            userId = "u1",
            genreScores = mapOf("18" to 20f, "35" to 20f, "80" to 20f),
            genreScoreDates = mapOf(
                "18" to System.currentTimeMillis(),
                "35" to System.currentTimeMillis(),
                "80" to System.currentTimeMillis()
            )
        )
        val dramaShow = show(1, genreIds = listOf(18), voteAverage = 7f, voteCount = 500)
        val crimeShow = show(2, genreIds = listOf(80), voteAverage = 7f, voteCount = 500)
        coEvery { userRepository.getUserProfile() } returns balancedProfile

        val result = useCase.scoreShows(listOf(dramaShow, crimeShow))

        val scoreDrama = result.first { it.id == 1 }.affinityScore
        val scoreCrime = result.first { it.id == 2 }.affinityScore
        assertEquals(scoreDrama, scoreCrime, 0.001f)
    }

    @Test
    fun `given drama show with few votes and high affinity, when scored, then hidden gem boost applied`() = runTest {
        val hiddenGem = show(1, genreIds = listOf(18), voteAverage = 7f, voteCount = 100)
        val regularShow = show(2, genreIds = listOf(18), voteAverage = 7f, voteCount = 1000)
        coEvery { userRepository.getUserProfile() } returns dramaProfile

        val result = useCase.scoreShows(listOf(hiddenGem, regularShow))

        assertTrue(
            result.first().id == 1
        )
    }

    @Test
    fun `given show with more than 500 votes, when scored, then no hidden gem boost`() = runTest {
        val normalShow = show(1, genreIds = listOf(18), voteAverage = 7f, voteCount = 501)
        val hiddenGem = show(2, genreIds = listOf(18), voteAverage = 7f, voteCount = 500)
        coEvery { userRepository.getUserProfile() } returns dramaProfile

        val result = useCase.scoreShows(listOf(normalShow, hiddenGem))

        assertTrue(
            result.first { it.id == 2 }.affinityScore > result.first { it.id == 1 }.affinityScore
        )
    }

    @Test
    fun `given binge watcher profile, when scoring ongoing 3-season show, then binge boost applied`() = runTest {
        val bingeHistory = listOf(
            "2026-04-01:1:4",
            "2026-04-02:2:5",
            "2026-04-03:3:3"
        )
        val bingeProfile = dramaProfile.copy(viewingHistory = bingeHistory)

        val ongoingLong = show(
            1,
            genreIds = listOf(18),
            voteAverage = 7f,
            voteCount = 500,
            status = "Returning Series",
            seasons = 3
        )
        val endedShort = show(
            2,
            genreIds = listOf(18),
            voteAverage = 7f,
            voteCount = 500,
            status = "Ended",
            seasons = 2
        )
        coEvery { userRepository.getUserProfile() } returns bingeProfile

        val result = useCase.scoreShows(listOf(ongoingLong, endedShort))

        val bingeShowScore = result.first { it.id == 1 }.affinityScore
        val endedShowScore = result.first { it.id == 2 }.affinityScore
        assertTrue(bingeShowScore > endedShowScore)
    }

    @Test
    fun `given casual watcher profile, when scoring ended 2-season show, then casual boost applied`() = runTest {
        val casualHistory = listOf(
            "2026-04-01:1:1",
            "2026-04-02:2:2",
            "2026-04-03:3:1"
        )
        val casualProfile = dramaProfile.copy(viewingHistory = casualHistory)

        val endedShort = show(
            1,
            genreIds = listOf(18),
            voteAverage = 7f,
            voteCount = 1000,
            status = "Ended",
            seasons = 2
        )
        val ongoingLong = show(
            2,
            genreIds = listOf(18),
            voteAverage = 7f,
            voteCount = 1000,
            status = "Returning Series",
            seasons = 4
        )
        coEvery { userRepository.getUserProfile() } returns casualProfile

        val result = useCase.scoreShows(listOf(endedShort, ongoingLong))

        assertTrue(
            result.first { it.id == 1 }.affinityScore > result.first { it.id == 2 }.affinityScore
        )
    }

    @Test
    fun `given 10 drama and 2 comedy shows, when execute, then comedy appears in top half`() = runTest {
        val dramaShows = (1..10).map { show(it, genreIds = listOf(18), voteAverage = 7f, voteCount = 500) }
        val comedyShows = (11..12).map { show(it, genreIds = listOf(35), voteAverage = 7f, voteCount = 500) }
        stubExecute(candidates = dramaShows + comedyShows)

        val result = useCase.execute()

        assertEquals(12, result.size)
        val comedyPositions = result.mapIndexedNotNull { idx, s ->
            if (s.safeGenreIds.contains(35)) idx else null
        }
        val halfSize = result.size / 2
        assertTrue(
            comedyPositions.any { it < halfSize }
        )
    }

    @Test
    fun `given many same-genre shows, when execute, then no single genre fills more than 35pct of first block`() =
        runTest {
            val dramas = (1..20).map { show(it, genreIds = listOf(18), voteAverage = 8f, voteCount = 500) }
            val comedies = (21..25).map { show(it, genreIds = listOf(35), voteAverage = 6f, voteCount = 500) }
            stubExecute(candidates = dramas + comedies)

            val result = useCase.execute()
            val topHalf = result.take(result.size / 2)
            val dramaCount = topHalf.count { it.safeGenreIds.contains(18) }

            assertTrue(
                dramaCount <= 9
            )
        }

    @Test
    fun `given 10 same-genre shows sorted by rating, when execute, then lowest-rated is not last`() = runTest {
        val shows = (1..10).map { i ->
            show(i, genreIds = listOf(18), voteAverage = (11 - i).toFloat(), voteCount = 500)
        }
        stubExecute(candidates = shows)

        val result = useCase.execute()

        assertEquals(10, result.size)
        assertNotEquals(
            10,
            result.last().id
        )
    }

    @Test
    fun `given null profile, when scoreShows called, then original list is returned unchanged`() = runTest {
        val shows = listOf(show(1, genreIds = listOf(18)), show(2, genreIds = listOf(35)))
        coEvery { userRepository.getUserProfile() } returns null

        val result = useCase.scoreShows(shows)

        assertEquals(shows.map { it.id }, result.map { it.id })
    }

    @Test
    fun `given valid profile, when scoreShows called, then all shows have affinity score greater than zero`() =
        runTest {
            val shows = listOf(show(1, genreIds = listOf(18)), show(2, genreIds = listOf(35)))
            coEvery { userRepository.getUserProfile() } returns dramaProfile

            val result = useCase.scoreShows(shows)

            assertTrue(result.all { it.affinityScore > 0f })
        }
}
