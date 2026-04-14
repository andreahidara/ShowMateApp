package com.andrea.showmateapp.domain.usecase

import com.andrea.showmateapp.data.model.MediaContent
import com.andrea.showmateapp.data.model.UserProfile
import com.andrea.showmateapp.domain.repository.IInteractionRepository
import com.andrea.showmateapp.domain.repository.IUserRepository
import io.mockk.mockk
import kotlin.math.abs
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class GetProfileStatsUseCaseTest {

    private val userRepository: IUserRepository = mockk(relaxed = true)
    private val interactionRepository: IInteractionRepository = mockk(relaxed = true)
    private val useCase = GetProfileStatsUseCase(userRepository, interactionRepository)

    @Test
    fun `returns zero stats for empty watched list and null profile`() {
        val stats = useCase.execute(emptyList(), null)
        assertEquals(0, stats.totalHours)
        assertEquals(0, stats.watchedCount)
        assertEquals("Ninguno", stats.topGenre)
    }

    @Test
    fun `calculates hours using watched episode count times episode runtime`() {
        val show = MediaContent(id = 1, name = "Show", episodeRunTime = listOf(45))
        val profile = UserProfile(watchedEpisodes = mapOf("1" to listOf(101, 102, 103, 104)))
        val stats = useCase.execute(listOf(show), profile)
        // 4 episodes × 45 min = 180 min = 3 hours
        assertEquals(3, stats.totalHours)
    }

    @Test
    fun `falls back to 45 min per episode when runtime list is empty`() {
        val show = MediaContent(id = 1, name = "Show", episodeRunTime = emptyList())
        val profile = UserProfile(watchedEpisodes = mapOf("1" to listOf(1, 2)))
        val stats = useCase.execute(listOf(show), profile)
        // 2 episodes × 45 min = 90 min = 1 hour
        assertEquals(1, stats.totalHours)
    }

    @Test
    fun `falls back to 45 min per episode when runtime is null`() {
        val show = MediaContent(id = 1, name = "Show", episodeRunTime = null)
        val profile = UserProfile(watchedEpisodes = mapOf("1" to listOf(1, 2)))
        val stats = useCase.execute(listOf(show), profile)
        assertEquals(1, stats.totalHours)
    }

    @Test
    fun `watchedCount equals the number of shows in the list`() {
        val shows = listOf(
            MediaContent(id = 1),
            MediaContent(id = 2),
            MediaContent(id = 3)
        )
        val stats = useCase.execute(shows, null)
        assertEquals(3, stats.watchedCount)
    }

    @Test
    fun `topGenre returns the genre with the highest score`() {
        // Genre 18 = Drama, Genre 35 = Comedia
        val profile = UserProfile(genreScores = mapOf("18" to 50f, "35" to 10f))
        val stats = useCase.execute(emptyList(), profile)
        assertEquals("Drama", stats.topGenre)
    }

    @Test
    fun `topGenres are max-normalized and top genre has proportion 1`() {
        val profile = UserProfile(genreScores = mapOf("18" to 30f, "35" to 20f, "28" to 10f))
        val stats = useCase.execute(emptyList(), profile)
        assertTrue("All proportions should be in [0,1]", stats.topGenres.all { it.second in 0f..1f })
        assertTrue("Top genre should have proportion 1.0", abs(stats.topGenres.first().second - 1.0f) < 0.01f)
    }

    @Test
    fun `show with zero watched episodes contributes zero minutes`() {
        val show = MediaContent(id = 1, episodeRunTime = listOf(60))
        val profile = UserProfile(watchedEpisodes = mapOf("1" to emptyList()))
        val stats = useCase.execute(listOf(show), profile)
        assertEquals(0, stats.totalHours)
    }

    @Test
    fun `favoriteActorId is the actor with the highest score`() {
        val profile = UserProfile(preferredActors = mapOf("42" to 100f, "7" to 20f))
        val stats = useCase.execute(emptyList(), profile)
        assertEquals("42", stats.favoriteActorId)
    }

    @Test
    fun `likeRate is proportion of liked over liked plus disliked`() {
        val profile = UserProfile(
            likedMediaIds = listOf(1, 2, 3),
            dislikedMediaIds = listOf(4)
        )
        val stats = useCase.execute(emptyList(), profile)
        // 3 liked / (3 + 1) = 0.75
        assertEquals(0.75f, stats.likeRate, 0.01f)
    }

    @Test
    fun `likeRate is zero when no interactions`() {
        val stats = useCase.execute(emptyList(), UserProfile())
        assertEquals(0f, stats.likeRate, 0.01f)
    }
}
