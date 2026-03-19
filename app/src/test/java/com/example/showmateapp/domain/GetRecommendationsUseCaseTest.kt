package com.example.showmateapp.domain

import com.example.showmateapp.data.model.UserProfile
import com.example.showmateapp.data.network.CreditsResponse
import com.example.showmateapp.data.network.CastMember
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
    fun `diversity filter limits single genre dominance`() = runTest {
        // 10 drama shows + 2 comedy — drama should not take more than 35% of results
        val dramaShows  = (1..10).map { makeShow(it, genreIds = listOf(18)) }
        val comedyShows = (11..12).map { makeShow(it, genreIds = listOf(35)) }

        whenever(userRepository.getUserProfile()).thenReturn(dramaProfile)
        whenever(userRepository.getWatchedMediaIds()).thenReturn(emptySet())
        whenever(showRepository.getDetailedRecommendations("18,35"))
            .thenReturn(dramaShows + comedyShows)

        val result = useCase.execute()
        val dramaCount = result.count { it.safeGenreIds.contains(18) }
        val maxAllowed = (result.size * 0.35f).toInt().coerceAtLeast(3)

        assertTrue("Drama shows should not exceed $maxAllowed (35%)", dramaCount <= maxAllowed)
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
}
