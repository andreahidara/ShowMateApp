package com.andrea.showmateapp.domain.usecase

import com.andrea.showmateapp.data.model.UserProfile
import com.andrea.showmateapp.data.network.MediaContent
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.abs

/**
 * Tests adicionales de GetProfileStatsUseCase cubriendo edge cases no presentes en
 * GetProfileStatsUseCaseTest (los casos base están allí).
 */
class GetProfileStatsUseCaseExtendedTest {

    private val useCase = GetProfileStatsUseCase()

    // ═══════════════════════════════════════════════════════════════════════════
    // totalEpisodes
    // ═══════════════════════════════════════════════════════════════════════════

    @Test
    fun `given show with watched episodes, totalEpisodes is sum of watched counts`() {
        // Given
        val showA = MediaContent(id = 1, name = "A", episodeRunTime = listOf(45))
        val showB = MediaContent(id = 2, name = "B", episodeRunTime = listOf(30))
        val profile = UserProfile(watchedEpisodes = mapOf(
            "1" to listOf(101, 102, 103),  // 3 episodes
            "2" to listOf(201, 202)         // 2 episodes
        ))

        // When
        val stats = useCase.execute(listOf(showA, showB), profile)

        // Then — 3 + 2 = 5 total episodes
        assertEquals(5, stats.totalEpisodes)
    }

    @Test
    fun `given show not in watchedEpisodes, totalEpisodes falls back to seasons times 10`() {
        // Given — show without watchedEpisodes entry; 2 seasons × 10 = 20 estimated episodes
        val show = MediaContent(id = 1, name = "S", episodeRunTime = listOf(45), numberOfSeasons = 2)
        val profile = UserProfile(watchedEpisodes = emptyMap())

        // When
        val stats = useCase.execute(listOf(show), profile)

        // Then
        assertEquals(20, stats.totalEpisodes)
    }

    @Test
    fun `given mix of shows with and without watchedEpisodes, totalEpisodes combines both`() {
        // Given — show1: 3 tracked episodes; show2: no tracking → 1 season × 10 = 10 estimated
        val show1 = MediaContent(id = 1, episodeRunTime = listOf(45))
        val show2 = MediaContent(id = 2, episodeRunTime = listOf(45), numberOfSeasons = 1)
        val profile = UserProfile(watchedEpisodes = mapOf("1" to listOf(1, 2, 3)))

        // When
        val stats = useCase.execute(listOf(show1, show2), profile)

        // Then
        assertEquals(3 + 10, stats.totalEpisodes)
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // totalWatchedHours
    // ═══════════════════════════════════════════════════════════════════════════

    @Test
    fun `given two shows with different runtimes, totalWatchedHours sums correctly`() {
        // Given
        val show1 = MediaContent(id = 1, episodeRunTime = listOf(60))
        val show2 = MediaContent(id = 2, episodeRunTime = listOf(30))
        val profile = UserProfile(watchedEpisodes = mapOf(
            "1" to listOf(1, 2),  // 2 × 60 = 120 min
            "2" to listOf(1, 2, 3, 4) // 4 × 30 = 120 min
        ))

        // When
        val stats = useCase.execute(listOf(show1, show2), profile)

        // Then — 240 min = 4 hours
        assertEquals(4, stats.totalWatchedHours)
    }

    @Test
    fun `given show with zero runtime in list, runtime falls back to 45 min`() {
        // Given — episodeRunTime contains 0
        val show = MediaContent(id = 1, episodeRunTime = listOf(0))
        val profile = UserProfile(watchedEpisodes = mapOf("1" to listOf(1, 2)))

        // When
        val stats = useCase.execute(listOf(show), profile)

        // Then — 2 episodes × 45 min fallback = 90 min = 1 hour
        assertEquals(1, stats.totalWatchedHours)
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // avgRating
    // ═══════════════════════════════════════════════════════════════════════════

    @Test
    fun `given multiple ratings, avgRating is their average`() {
        // Given — ratings: 8.0, 6.0, 10.0 → avg = 8.0
        val profile = UserProfile(ratings = mapOf("1" to 8f, "2" to 6f, "3" to 10f))

        // When
        val stats = useCase.execute(emptyList(), profile)

        // Then
        assertEquals(8.0f, stats.avgRating, 0.01f)
    }

    @Test
    fun `given no ratings, avgRating is zero`() {
        // Given
        val profile = UserProfile(ratings = emptyMap())

        // When
        val stats = useCase.execute(emptyList(), profile)

        // Then
        assertEquals(0f, stats.avgRating, 0.001f)
    }

    @Test
    fun `given single rating, avgRating equals that rating`() {
        // Given
        val profile = UserProfile(ratings = mapOf("42" to 7.5f))

        // When
        val stats = useCase.execute(emptyList(), profile)

        // Then
        assertEquals(7.5f, stats.avgRating, 0.001f)
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // essentialCount & ratingsCount
    // ═══════════════════════════════════════════════════════════════════════════

    @Test
    fun `given 3 essential shows, essentialCount is 3`() {
        // Given
        val profile = UserProfile(essentialMediaIds = listOf(1, 2, 3))

        // When
        val stats = useCase.execute(emptyList(), profile)

        // Then
        assertEquals(3, stats.essentialCount)
    }

    @Test
    fun `given 5 rated shows, ratingsCount is 5`() {
        // Given
        val profile = UserProfile(ratings = mapOf("1" to 7f, "2" to 8f, "3" to 9f, "4" to 6f, "5" to 5f))

        // When
        val stats = useCase.execute(emptyList(), profile)

        // Then
        assertEquals(5, stats.ratingsCount)
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // likeRate edge cases
    // ═══════════════════════════════════════════════════════════════════════════

    @Test
    fun `given only likes and no dislikes, likeRate is 1_0`() {
        // Given
        val profile = UserProfile(likedMediaIds = listOf(1, 2, 3), dislikedMediaIds = emptyList())

        // When
        val stats = useCase.execute(emptyList(), profile)

        // Then
        assertEquals(1.0f, stats.likeRate, 0.001f)
    }

    @Test
    fun `given only dislikes and no likes, likeRate is 0_0`() {
        // Given
        val profile = UserProfile(likedMediaIds = emptyList(), dislikedMediaIds = listOf(1, 2))

        // When
        val stats = useCase.execute(emptyList(), profile)

        // Then
        assertEquals(0.0f, stats.likeRate, 0.001f)
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // topGenres
    // ═══════════════════════════════════════════════════════════════════════════

    @Test
    fun `given more than 5 positive genres, topGenres returns only top 5`() {
        // Given — 6 genres with positive scores
        val profile = UserProfile(genreScores = mapOf(
            "18" to 50f, "35" to 40f, "80" to 30f,
            "99" to 20f, "16" to 10f, "37" to 5f
        ))

        // When
        val stats = useCase.execute(emptyList(), profile)

        // Then
        assertEquals("topGenres should contain at most 5 entries", 5, stats.topGenres.size)
    }

    @Test
    fun `given genres with negative scores, topGenres filters them out`() {
        // Given
        val profile = UserProfile(genreScores = mapOf(
            "18" to 30f,    // positive → included
            "35" to -5f,    // negative → excluded
            "80" to 0f      // zero → excluded (filter { it.value > 0 })
        ))

        // When
        val stats = useCase.execute(emptyList(), profile)

        // Then — only Drama (18) should appear
        assertEquals(1, stats.topGenres.size)
    }

    @Test
    fun `given genres, topGenres are ordered from highest to lowest proportion`() {
        // Given
        val profile = UserProfile(genreScores = mapOf("18" to 10f, "35" to 30f, "80" to 20f))

        // When
        val stats = useCase.execute(emptyList(), profile)

        // Then — first genre should be Comedia (35=30f), then Crime/Thriller (80=20f), then Drama (18=10f)
        val proportions = stats.topGenres.map { it.second }
        assertTrue("Proportions should be in descending order",
            proportions.zipWithNext().all { (a, b) -> a >= b })
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // favoriteActorId
    // ═══════════════════════════════════════════════════════════════════════════

    @Test
    fun `given null profile, favoriteActorId is null`() {
        // Given / When
        val stats = useCase.execute(emptyList(), null)

        // Then
        assertNull(stats.favoriteActorId)
    }

    @Test
    fun `given profile with no actors, favoriteActorId is null`() {
        // Given
        val profile = UserProfile(preferredActors = emptyMap())

        // When
        val stats = useCase.execute(emptyList(), profile)

        // Then
        assertNull(stats.favoriteActorId)
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // watchedCount
    // ═══════════════════════════════════════════════════════════════════════════

    @Test
    fun `watchedCount reflects number of shows in list regardless of episode tracking`() {
        // Given — 3 shows; tracking does not affect watchedCount
        val shows = listOf(
            MediaContent(id = 1), MediaContent(id = 2), MediaContent(id = 3)
        )
        val profile = UserProfile(watchedEpisodes = mapOf("1" to listOf(1, 2)))

        // When
        val stats = useCase.execute(shows, profile)

        // Then
        assertEquals("watchedCount should equal the number of shows passed", 3, stats.watchedCount)
    }
}
