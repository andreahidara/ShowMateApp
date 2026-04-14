package com.andrea.showmateapp.domain.usecase

import com.andrea.showmateapp.data.model.UserProfile
import com.andrea.showmateapp.domain.repository.IUserRepository
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class GetCollaborativeBoostUseCaseTest {

    private val userRepository = mockk<IUserRepository>()
    private lateinit var useCase: GetCollaborativeBoostUseCase

    @Before
    fun setup() {
        useCase = GetCollaborativeBoostUseCase(userRepository)
    }

    @Test
    fun `given watched episode entry, when buildRatingVector, then show gets SIGNAL_WATCHED=0_40`() {
        val profile = UserProfile(watchedEpisodes = mapOf("42" to listOf(1, 2, 3)))

        val vector = useCase.buildRatingVector(profile)

        assertEquals(0.40f, vector[42]!!, 0.001f)
    }

    @Test
    fun `given liked show, when buildRatingVector, then show gets SIGNAL_LIKED=0_80`() {
        val profile = UserProfile(likedMediaIds = listOf(7))

        val vector = useCase.buildRatingVector(profile)

        assertEquals(0.80f, vector[7]!!, 0.001f)
    }

    @Test
    fun `given essential show, when buildRatingVector, then show gets SIGNAL_ESSENTIAL=1_00`() {
        val profile = UserProfile(essentialMediaIds = listOf(5))

        val vector = useCase.buildRatingVector(profile)

        assertEquals(1.00f, vector[5]!!, 0.001f)
    }

    @Test
    fun `given explicit rating 8 out of 10, when buildRatingVector, then show gets 0_80`() {
        val profile = UserProfile(ratings = mapOf("15" to 8f))

        val vector = useCase.buildRatingVector(profile)

        assertEquals(0.80f, vector[15]!!, 0.001f)
    }

    @Test
    fun `given disliked show, when buildRatingVector, then show is absent from vector`() {
        val profile = UserProfile(dislikedMediaIds = listOf(99))

        val vector = useCase.buildRatingVector(profile)

        assertTrue(99 !in vector)
    }

    @Test
    fun `given empty profile, when buildRatingVector, then vector is empty`() {
        val profile = UserProfile()

        val vector = useCase.buildRatingVector(profile)

        assertTrue(vector.isEmpty())
    }

    @Test
    fun `given show that is both watched and liked, when buildRatingVector, then liked takes priority`() {
        val profile = UserProfile(
            watchedEpisodes = mapOf("10" to listOf(1)),
            likedMediaIds = listOf(10)
        )

        val vector = useCase.buildRatingVector(profile)

        assertEquals(0.80f, vector[10]!!, 0.001f)
    }

    @Test
    fun `given show with explicit rating 9 and liked signal, when buildRatingVector, then explicit rating wins`() {
        val profile = UserProfile(
            ratings = mapOf("20" to 9f),
            likedMediaIds = listOf(20)
        )

        val vector = useCase.buildRatingVector(profile)

        assertEquals(0.90f, vector[20]!!, 0.001f)
    }

    @Test
    fun `given essential show with lower explicit rating, when buildRatingVector, then_essential always wins`() {
        val profile = UserProfile(
            ratings = mapOf("30" to 9.5f),
            likedMediaIds = listOf(30),
            essentialMediaIds = listOf(30)
        )

        val vector = useCase.buildRatingVector(profile)

        assertEquals(1.00f, vector[30]!!, 0.001f)
    }

    @Test
    fun `given show that is disliked and watched, when buildRatingVector, then it is excluded`() {
        val profile = UserProfile(
            watchedEpisodes = mapOf("55" to listOf(1, 2)),
            dislikedMediaIds = listOf(55)
        )

        val vector = useCase.buildRatingVector(profile)

        assertTrue(55 !in vector)
    }

    @Test
    fun `given multiple shows with mixed signals, when buildRatingVector, then each gets correct value`() {
        val profile = UserProfile(
            watchedEpisodes = mapOf("1" to listOf(1), "2" to listOf(1)),
            ratings = mapOf("1" to 7f),
            likedMediaIds = listOf(2),
            essentialMediaIds = listOf(3),
            dislikedMediaIds = listOf(4)
        )

        val vector = useCase.buildRatingVector(profile)

        assertEquals(0.70f, vector[1]!!, 0.001f)
        assertEquals(0.80f, vector[2]!!, 0.001f)
        assertEquals(1.00f, vector[3]!!, 0.001f)
        assertTrue(4 !in vector)
    }

    @Test
    fun `given no similar users, when execute, then result is empty map`() = runTest {
        val myProfile = UserProfile(userId = "me", likedMediaIds = listOf(1, 2, 3))
        coEvery { userRepository.getSimilarUsers(limit = any()) } returns emptyList()

        val result = useCase.execute(myProfile)

        assertTrue(result.isEmpty())
    }

    @Test
    fun `given identical profiles, when execute, then cosine similarity is 1 and boost is recommended`() = runTest {
        val sharedShows = listOf(1, 2, 3)
        val myProfile = UserProfile(
            userId = "me",
            likedMediaIds = sharedShows,
            essentialMediaIds = listOf(10)
        )
        val neighborProfile = UserProfile(
            userId = "neighbor",
            likedMediaIds = sharedShows,
            essentialMediaIds = listOf(10),
            ratings = mapOf("20" to 9f)
        )
        coEvery { userRepository.getSimilarUsers(limit = any()) } returns listOf(neighborProfile)

        val result = useCase.execute(myProfile)

        assertTrue(
            result.isNotEmpty()
        )
        assertTrue(
            result.values.all { it in 0f..1.20f }
        )
    }

    @Test
    fun `given neighbor below MIN_SIMILARITY threshold, when execute, then result is empty`() = runTest {
        val myProfile = UserProfile(userId = "me", likedMediaIds = listOf(1))
        val unrelatedNeighbor = UserProfile(
            userId = "stranger",
            likedMediaIds = listOf(99, 100, 101)
        )
        coEvery { userRepository.getSimilarUsers(limit = any()) } returns listOf(unrelatedNeighbor)

        val result = useCase.execute(myProfile)

        assertTrue(
            result.isEmpty()
        )
    }

    @Test
    fun `given show already seen by current user, when execute, then it is excluded from boost map`() = runTest {
        val myProfile = UserProfile(
            userId = "me",
            likedMediaIds = listOf(1, 2, 3),
            watchedEpisodes = mapOf("5" to listOf(1, 2, 3))
        )
        val neighborProfile = UserProfile(
            userId = "neighbor",
            likedMediaIds = listOf(1, 2, 3, 5)
        )
        coEvery { userRepository.getSimilarUsers(limit = any()) } returns listOf(neighborProfile)

        val result = useCase.execute(myProfile)

        assertTrue(
            5 !in result
        )
    }

    @Test
    fun `given show disliked by current user, when execute, then it is excluded from boost map`() = runTest {
        val myProfile = UserProfile(
            userId = "me",
            likedMediaIds = listOf(1, 2, 3),
            dislikedMediaIds = listOf(7)
        )
        val neighborProfile = UserProfile(
            userId = "neighbor",
            likedMediaIds = listOf(1, 2, 3, 7)
        )
        coEvery { userRepository.getSimilarUsers(limit = any()) } returns listOf(neighborProfile)

        val result = useCase.execute(myProfile)

        assertTrue(7 !in result)
    }

    @Test
    fun `given neighbor recommends many shows, when execute, then results are capped at 50`() = runTest {
        val myProfile = UserProfile(userId = "me", likedMediaIds = listOf(1))
        val neighborIds = (100..199).toList()
        val neighborProfile = UserProfile(
            userId = "neighbor",
            likedMediaIds = listOf(1) + neighborIds
        )
        coEvery { userRepository.getSimilarUsers(limit = any()) } returns listOf(neighborProfile)

        val result = useCase.execute(myProfile)

        assertTrue(result.size <= 50)
    }

    @Test
    fun `given exception thrown by userRepository, when execute, then returns empty map without crashing`() = runTest {
        val myProfile = UserProfile(userId = "me", likedMediaIds = listOf(1))
        coEvery { userRepository.getSimilarUsers(limit = any()) } throws RuntimeException("DB error")

        val result = useCase.execute(myProfile)

        assertTrue(result.isEmpty())
    }

    @Test
    fun `given partially overlapping profiles, when execute, then similarity is between 0 and 1`() = runTest {
        val myProfile = UserProfile(
            userId = "me",
            likedMediaIds = listOf(1, 2, 3, 4)
        )
        val neighborProfile = UserProfile(
            userId = "neighbor",
            likedMediaIds = listOf(1, 2),
            ratings = mapOf("10" to 8f)
        )
        coEvery { userRepository.getSimilarUsers(limit = any()) } returns listOf(neighborProfile)

        val result = useCase.execute(myProfile)

        result.values.forEach { boost ->
            assertTrue(boost in 0f..1.20f)
        }
    }

    @Test
    fun `given multiple similar users recommending same show, when execute, then boost reflects weighted average`() =
        runTest {
            val myProfile = UserProfile(userId = "me", likedMediaIds = listOf(1, 2, 3))
            val neighbor1 = UserProfile(
                userId = "n1",
                likedMediaIds = listOf(1, 2, 3),
                essentialMediaIds = listOf(50)
            )
            val neighbor2 = UserProfile(
                userId = "n2",
                likedMediaIds = listOf(1, 2, 3),
                ratings = mapOf("50" to 9f)
            )
            coEvery { userRepository.getSimilarUsers(limit = any()) } returns listOf(neighbor1, neighbor2)

            val result = useCase.execute(myProfile)

            assertTrue(
                50 in result
            )
            val boost = result[50]!!
            assertTrue(
                boost > 0.5f
            )
        }
}
