package com.andrea.showmateapp.domain.usecase

import com.andrea.showmateapp.data.model.UserProfile
import com.andrea.showmateapp.data.network.MediaContent
import com.andrea.showmateapp.domain.repository.IInteractionRepository
import com.andrea.showmateapp.domain.repository.IUserRepository
import io.mockk.mockk
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class GetProfileStatsUseCaseExtendedTest {

    private val userRepository: IUserRepository = mockk(relaxed = true)
    private val interactionRepository: IInteractionRepository = mockk(relaxed = true)
    private val useCase = GetProfileStatsUseCase(userRepository, interactionRepository)

    @Test
    fun `given show with watched episodes, totalEpisodes is sum of watched counts`() {
        val showA = MediaContent(id = 1, name = "A", episodeRunTime = listOf(45))
        val showB = MediaContent(id = 2, name = "B", episodeRunTime = listOf(30))
        val profile = UserProfile(
            watchedEpisodes = mapOf(
                "1" to listOf(101, 102, 103),
                "2" to listOf(201, 202)
            )
        )

        val stats = useCase.execute(listOf(showA, showB), profile)

        assertEquals(5, stats.totalEpisodes)
    }

    @Test
    fun `given show not in watchedEpisodes, totalEpisodes falls back to seasons times 10`() {
        val show = MediaContent(id = 1, name = "S", episodeRunTime = listOf(45), numberOfSeasons = 2)
        val profile = UserProfile(watchedEpisodes = emptyMap())

        val stats = useCase.execute(listOf(show), profile)

        assertEquals(20, stats.totalEpisodes)
    }

    @Test
    fun `given mix of shows with and without watchedEpisodes, totalEpisodes combines both`() {
        val show1 = MediaContent(id = 1, episodeRunTime = listOf(45))
        val show2 = MediaContent(id = 2, episodeRunTime = listOf(45), numberOfSeasons = 1)
        val profile = UserProfile(watchedEpisodes = mapOf("1" to listOf(1, 2, 3)))

        val stats = useCase.execute(listOf(show1, show2), profile)

        assertEquals(3 + 10, stats.totalEpisodes)
    }

    @Test
    fun `given two shows with different runtimes, totalWatchedHours sums correctly`() {
        val show1 = MediaContent(id = 1, episodeRunTime = listOf(60))
        val show2 = MediaContent(id = 2, episodeRunTime = listOf(30))
        val profile = UserProfile(
            watchedEpisodes = mapOf(
                "1" to listOf(1, 2),
                "2" to listOf(1, 2, 3, 4)
            )
        )

        val stats = useCase.execute(listOf(show1, show2), profile)

        assertEquals(4, stats.totalHours)
    }

    @Test
    fun `given show with zero runtime in list, runtime falls back to 45 min`() {
        val show = MediaContent(id = 1, episodeRunTime = listOf(0))
        val profile = UserProfile(watchedEpisodes = mapOf("1" to listOf(1, 2)))

        val stats = useCase.execute(listOf(show), profile)

        assertEquals(1, stats.totalHours)
    }

    @Test
    fun `given multiple ratings, avgRating is their average`() {
        val profile = UserProfile(ratings = mapOf("1" to 8f, "2" to 6f, "3" to 10f))

        val stats = useCase.execute(emptyList(), profile)

        assertEquals(8.0f, stats.avgRating, 0.01f)
    }

    @Test
    fun `given no ratings, avgRating is zero`() {
        val profile = UserProfile(ratings = emptyMap())

        val stats = useCase.execute(emptyList(), profile)

        assertEquals(0f, stats.avgRating, 0.001f)
    }

    @Test
    fun `given single rating, avgRating equals that rating`() {
        val profile = UserProfile(ratings = mapOf("42" to 7.5f))

        val stats = useCase.execute(emptyList(), profile)

        assertEquals(7.5f, stats.avgRating, 0.001f)
    }

    @Test
    fun `given 3 essential shows, essentialCount is 3`() {
        val profile = UserProfile(essentialMediaIds = listOf(1, 2, 3))

        val stats = useCase.execute(emptyList(), profile)

        assertEquals(3, stats.essentialCount)
    }

    @Test
    fun `given 5 rated shows, ratingsCount is 5`() {
        val profile = UserProfile(ratings = mapOf("1" to 7f, "2" to 8f, "3" to 9f, "4" to 6f, "5" to 5f))

        val stats = useCase.execute(emptyList(), profile)

        assertEquals(5, stats.ratingsCount)
    }

    @Test
    fun `given only likes and no dislikes, likeRate is 1_0`() {
        val profile = UserProfile(likedMediaIds = listOf(1, 2, 3), dislikedMediaIds = emptyList())

        val stats = useCase.execute(emptyList(), profile)

        assertEquals(1.0f, stats.likeRate, 0.001f)
    }

    @Test
    fun `given only dislikes and no likes, likeRate is 0_0`() {
        val profile = UserProfile(likedMediaIds = emptyList(), dislikedMediaIds = listOf(1, 2))

        val stats = useCase.execute(emptyList(), profile)

        assertEquals(0.0f, stats.likeRate, 0.001f)
    }

    @Test
    fun `given more than 5 positive genres, topGenres returns only top 5`() {
        val profile = UserProfile(
            genreScores = mapOf(
                "18" to 50f,
                "35" to 40f,
                "80" to 30f,
                "99" to 20f,
                "16" to 10f,
                "37" to 5f
            )
        )

        val stats = useCase.execute(emptyList(), profile)

        assertEquals(5, stats.topGenres.size)
    }

    @Test
    fun `given genres with negative scores, topGenres filters them out`() {
        val profile = UserProfile(
            genreScores = mapOf(
                "18" to 30f,
                "35" to -5f,
                "80" to 0f
            )
        )

        val stats = useCase.execute(emptyList(), profile)

        assertEquals(1, stats.topGenres.size)
    }

    @Test
    fun `given genres, topGenres are ordered from highest to lowest proportion`() {
        val profile = UserProfile(genreScores = mapOf("18" to 10f, "35" to 30f, "80" to 20f))

        val stats = useCase.execute(emptyList(), profile)

        val proportions = stats.topGenres.map { it.second }
        assertTrue(
            proportions.zipWithNext().all { (a, b) -> a >= b }
        )
    }

    @Test
    fun `given null profile, favoriteActorId is null`() {
        val stats = useCase.execute(emptyList(), null)

        assertNull(stats.favoriteActorId)
    }

    @Test
    fun `given profile with no actors, favoriteActorId is null`() {
        val profile = UserProfile(preferredActors = emptyMap())

        val stats = useCase.execute(emptyList(), profile)

        assertNull(stats.favoriteActorId)
    }

    @Test
    fun `watchedCount reflects number of shows in list regardless of episode tracking`() {
        val shows = listOf(
            MediaContent(id = 1),
            MediaContent(id = 2),
            MediaContent(id = 3)
        )
        val profile = UserProfile(watchedEpisodes = mapOf("1" to listOf(1, 2)))

        val stats = useCase.execute(shows, profile)

        assertEquals(3, stats.watchedCount)
    }
}
