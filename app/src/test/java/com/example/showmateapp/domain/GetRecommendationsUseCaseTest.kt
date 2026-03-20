package com.example.showmateapp.domain

import com.example.showmateapp.data.model.UserProfile
import com.example.showmateapp.data.network.MediaContent
import com.example.showmateapp.data.repository.ShowRepository
import com.example.showmateapp.data.repository.UserRepository
import com.example.showmateapp.domain.usecase.GetRecommendationsUseCase
import com.example.showmateapp.util.Resource
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever

class GetRecommendationsUseCaseTest {

    private lateinit var useCase: GetRecommendationsUseCase
    private val userRepository: UserRepository = mock()
    private val showRepository: ShowRepository = mock()

    private val dramaProfile = UserProfile(
        userId = "test",
        genreScores = mapOf("18" to 30f, "35" to 5f),      // strong Drama preference
        genreScoreDates = mapOf("18" to System.currentTimeMillis(), "35" to System.currentTimeMillis())
    )

    private fun makeShow(
        id: Int,
        genreIds: List<Int> = emptyList(),
        voteAverage: Float = 7f,
        voteCount: Int = 500,
        status: String? = null,
        seasons: Int? = null
    ) = MediaContent(
        id = id,
        name = "Show $id",
        genreIds = genreIds,
        voteAverage = voteAverage,
        voteCount = voteCount,
        status = status,
        numberOfSeasons = seasons
    )

    @Before
    fun setup() {
        useCase = GetRecommendationsUseCase(userRepository, showRepository)
    }

    @Test
    fun `drama show scores higher than comedy for drama-loving user`() = runTest {
        val dramaShow  = makeShow(1, genreIds = listOf(18))
        val comedyShow = makeShow(2, genreIds = listOf(35))

        whenever(userRepository.getUserProfile()).thenReturn(dramaProfile)
        whenever(userRepository.getWatchedMediaIds()).thenReturn(emptySet())
        whenever(showRepository.getDetailedRecommendations("18,35")).thenReturn(listOf(dramaShow, comedyShow))

        val result = useCase.execute()

        assertTrue("Drama show should rank first", result.first().id == 1)
        assertTrue("Drama show affinity > comedy", result[0].affinityScore > result[1].affinityScore)
    }

    @Test
    fun `ended show receives completeness boost`() = runTest {
        val endedShow   = makeShow(1, genreIds = listOf(18), seasons = 2, status = "Ended")
        val ongoingShow = makeShow(2, genreIds = listOf(18), voteAverage = 7f, voteCount = 500)

        whenever(userRepository.getUserProfile()).thenReturn(dramaProfile)
        whenever(userRepository.getWatchedMediaIds()).thenReturn(emptySet())
        whenever(showRepository.getDetailedRecommendations("18,35")).thenReturn(listOf(endedShow, ongoingShow))

        val result = useCase.execute()
        val ended   = result.first { it.id == 1 }
        val ongoing = result.first { it.id == 2 }

        assertTrue("Ended short show should score higher", ended.affinityScore > ongoing.affinityScore)
    }

    @Test
    fun `watched shows are excluded from recommendations`() = runTest {
        val watchedShow = makeShow(1, genreIds = listOf(18))
        val newShow     = makeShow(2, genreIds = listOf(18))

        whenever(userRepository.getUserProfile()).thenReturn(dramaProfile)
        whenever(userRepository.getWatchedMediaIds()).thenReturn(setOf(1))
        whenever(showRepository.getDetailedRecommendations("18,35")).thenReturn(listOf(watchedShow, newShow))

        val result = useCase.execute()

        assertTrue("Watched show should not appear", result.none { it.id == 1 })
        assertEquals(1, result.size)
    }

    @Test
    fun `diversity filter promotes genre variety in top results`() = runTest {
        val dramaShows  = (1..10).map { makeShow(it, genreIds = listOf(18)) }
        val comedyShows = (11..12).map { makeShow(it, genreIds = listOf(35)) }

        whenever(userRepository.getUserProfile()).thenReturn(dramaProfile)
        whenever(userRepository.getWatchedMediaIds()).thenReturn(emptySet())
        whenever(showRepository.getDetailedRecommendations("18,35"))
            .thenReturn(dramaShows + comedyShows)

        val result = useCase.execute()

        assertEquals("All shows should be returned", 12, result.size)

        val comedyCount = result.count { it.safeGenreIds.contains(35) }
        assertTrue("Comedy shows should be included in results", comedyCount > 0)

        val topSix = result.take(6)
        val dramaInTop = topSix.count { it.safeGenreIds.contains(18) }
        assertTrue("Drama should not fill all top-6 slots after diversity reorder", dramaInTop < 6)
    }

    @Test
    fun `scoreShows returns shows with affinity set`() = runTest {
        val shows = listOf(makeShow(1, genreIds = listOf(18)), makeShow(2, genreIds = listOf(35)))
        whenever(userRepository.getUserProfile()).thenReturn(dramaProfile)

        val result = useCase.scoreShows(shows)

        assertTrue("All shows should have affinity > 0", result.all { it.affinityScore > 0f })
    }

    @Test
    fun `fallback to popular when no user profile`() = runTest {
        val popular = listOf(makeShow(99))
        whenever(userRepository.getUserProfile()).thenReturn(null)
        whenever(showRepository.getPopularShows()).thenReturn(Resource.Success(popular))

        val result = useCase.execute()
        assertEquals(99, result.first().id)
    }

    @Test
    fun `time decay shifts relative genre weight for old interactions`() = runTest {
        val ninetyDaysAgo = System.currentTimeMillis() - (90L * 24 * 60 * 60 * 1000)
        val now = System.currentTimeMillis()
        val mixedProfile = UserProfile(
            userId = "test",
            genreScores = mapOf("18" to 30f, "35" to 30f),
            genreScoreDates = mapOf("18" to now, "35" to ninetyDaysAgo)
        )
        val dramaShow  = makeShow(1, genreIds = listOf(18))
        val comedyShow = makeShow(2, genreIds = listOf(35))

        whenever(userRepository.getUserProfile()).thenReturn(mixedProfile)

        val result = useCase.scoreShows(listOf(dramaShow, comedyShow))
        val dramaScore  = result.first { it.id == 1 }.affinityScore
        val comedyScore = result.first { it.id == 2 }.affinityScore

        assertTrue(
            "Recently-interacted genre (drama=$dramaScore) should score higher than stale genre (comedy=$comedyScore)",
            dramaScore > comedyScore
        )
    }

    @Test
    fun `bayesian rating penalizes show with few votes`() = runTest {
        val popularShow = makeShow(1, genreIds = listOf(18), voteAverage = 8f, voteCount = 1000)
        val obscureShow = makeShow(2, genreIds = listOf(18), voteAverage = 8f, voteCount = 10)

        whenever(userRepository.getUserProfile()).thenReturn(dramaProfile)
        whenever(userRepository.getWatchedMediaIds()).thenReturn(emptySet())
        whenever(showRepository.getDetailedRecommendations("18,35"))
            .thenReturn(listOf(popularShow, obscureShow))

        val result = useCase.execute()
        val popular = result.first { it.id == 1 }
        val obscure = result.first { it.id == 2 }

        assertTrue(
            "Show with more votes should score higher due to Bayesian adjustment",
            popular.affinityScore > obscure.affinityScore
        )
    }
}
