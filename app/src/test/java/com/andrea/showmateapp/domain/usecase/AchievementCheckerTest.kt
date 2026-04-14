package com.andrea.showmateapp.domain.usecase

import com.andrea.showmateapp.data.model.FriendInfo
import com.andrea.showmateapp.data.model.UserProfile
import com.andrea.showmateapp.domain.repository.IAchievementRepository
import com.andrea.showmateapp.domain.repository.ISocialRepository
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import io.mockk.slot
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class AchievementCheckerTest {

    private val achievementRepository = mockk<IAchievementRepository>(relaxed = true)
    private val socialRepository = mockk<ISocialRepository>(relaxed = true)

    private lateinit var checker: AchievementChecker

    private fun baseProfile() = UserProfile(
        userId = "test_uid",
        genreScores = emptyMap(),
        likedMediaIds = emptyList(),
        watchedEpisodes = emptyMap(),
        viewingHistory = emptyList()
    )

    @Before
    fun setUp() {
        checker = AchievementChecker(achievementRepository, socialRepository)
    }

    @Test
    fun `first_show desbloqueado cuando likedMediaIds no está vacío`() = runTest {
        coEvery { achievementRepository.getUnlockedIds() } returns emptyList()
        coEvery { socialRepository.getFriends() } returns emptyList()

        val profile = baseProfile().copy(likedMediaIds = listOf(42))
        checker.evaluate(AchievementChecker.EvalContext(profile))

        val idsSlot = slot<List<String>>()
        coVerify { achievementRepository.unlockAchievements(capture(idsSlot), any()) }
        assertTrue("first_show" in idsSlot.captured)
    }

    @Test
    fun `first_show no se emite si ya estaba desbloqueado`() = runTest {
        coEvery { achievementRepository.getUnlockedIds() } returns listOf("first_show")
        coEvery { socialRepository.getFriends() } returns emptyList()

        val profile = baseProfile().copy(likedMediaIds = listOf(1, 2))
        checker.evaluate(AchievementChecker.EvalContext(profile))

        coVerify(exactly = 0) { achievementRepository.unlockAchievements(any(), any()) }
    }

    @Test
    fun `genre_explorer se desbloquea con 10 géneros distintos`() = runTest {
        coEvery { achievementRepository.getUnlockedIds() } returns emptyList()
        coEvery { socialRepository.getFriends() } returns emptyList()

        val genres = (1..10).associate { it.toString() to 1f }
        val profile = baseProfile().copy(genreScores = genres)
        checker.evaluate(AchievementChecker.EvalContext(profile))

        val idsSlot = slot<List<String>>()
        coVerify { achievementRepository.unlockAchievements(capture(idsSlot), any()) }
        assertTrue("genre_explorer" in idsSlot.captured)
    }

    @Test
    fun `genre_explorer no se desbloquea con menos de 10 géneros`() = runTest {
        coEvery { achievementRepository.getUnlockedIds() } returns emptyList()
        coEvery { socialRepository.getFriends() } returns emptyList()

        val genres = (1..9).associate { it.toString() to 1f }
        val profile = baseProfile().copy(genreScores = genres)
        checker.evaluate(AchievementChecker.EvalContext(profile))

        coVerify(exactly = 0) { achievementRepository.unlockAchievements(any(), any()) }
    }

    @Test
    fun `marathon_day se desbloquea con 5 o más episodios en el día`() = runTest {
        coEvery { achievementRepository.getUnlockedIds() } returns emptyList()
        coEvery { socialRepository.getFriends() } returns emptyList()

        checker.evaluate(AchievementChecker.EvalContext(baseProfile(), episodesToday = 5))

        val idsSlot = slot<List<String>>()
        coVerify { achievementRepository.unlockAchievements(capture(idsSlot), any()) }
        assertTrue("marathon_day" in idsSlot.captured)
    }

    @Test
    fun `marathon_day no se desbloquea con menos de 5 episodios`() = runTest {
        coEvery { achievementRepository.getUnlockedIds() } returns emptyList()
        coEvery { socialRepository.getFriends() } returns emptyList()

        checker.evaluate(AchievementChecker.EvalContext(baseProfile(), episodesToday = 4))

        coVerify(exactly = 0) { achievementRepository.unlockAchievements(any(), any()) }
    }

    @Test
    fun `first_friend se desbloquea con al menos 1 amigo`() = runTest {
        coEvery { achievementRepository.getUnlockedIds() } returns emptyList()
        coEvery { socialRepository.getFriends() } returns listOf(
            FriendInfo("uid1", "user1", "u1@x.com", 80)
        )

        checker.evaluate(AchievementChecker.EvalContext(baseProfile()))

        val idsSlot = slot<List<String>>()
        coVerify { achievementRepository.unlockAchievements(capture(idsSlot), any()) }
        assertTrue("first_friend" in idsSlot.captured)
    }

    @Test
    fun `social_butterfly requiere 5 amigos`() = runTest {
        coEvery { achievementRepository.getUnlockedIds() } returns listOf("first_friend")
        coEvery { socialRepository.getFriends() } returns List(5) {
            FriendInfo("uid$it", "user$it", "u$it@x.com", 60)
        }

        checker.evaluate(AchievementChecker.EvalContext(baseProfile()))

        val idsSlot = slot<List<String>>()
        coVerify { achievementRepository.unlockAchievements(capture(idsSlot), any()) }
        assertTrue("social_butterfly" in idsSlot.captured)
    }

    @Test
    fun `hidden_gem se desbloquea con un show de menos de 1000 votos`() = runTest {
        coEvery { achievementRepository.getUnlockedIds() } returns emptyList()
        coEvery { socialRepository.getFriends() } returns emptyList()

        checker.evaluate(AchievementChecker.EvalContext(baseProfile(), voteCount = 999))

        val idsSlot = slot<List<String>>()
        coVerify { achievementRepository.unlockAchievements(capture(idsSlot), any()) }
        assertTrue("hidden_gem" in idsSlot.captured)
    }

    @Test
    fun `korean_drama se desbloquea al ver contenido con origen KR`() = runTest {
        coEvery { achievementRepository.getUnlockedIds() } returns emptyList()
        coEvery { socialRepository.getFriends() } returns emptyList()

        checker.evaluate(
            AchievementChecker.EvalContext(
                profile = baseProfile(),
                countries = listOf("KR", "US")
            )
        )

        val idsSlot = slot<List<String>>()
        coVerify { achievementRepository.unlockAchievements(capture(idsSlot), any()) }
        assertTrue("korean_drama" in idsSlot.captured)
    }

    @Test
    fun `first_review se desbloquea con 1 reseña`() = runTest {
        coEvery { achievementRepository.getUnlockedIds() } returns emptyList()
        coEvery { socialRepository.getFriends() } returns emptyList()

        checker.evaluate(AchievementChecker.EvalContext(baseProfile(), reviewCount = 1))

        val idsSlot = slot<List<String>>()
        coVerify { achievementRepository.unlockAchievements(capture(idsSlot), any()) }
        assertTrue("first_review" in idsSlot.captured)
    }

    @Test
    fun `prolific_critic requiere 10 reseñas`() = runTest {
        coEvery { achievementRepository.getUnlockedIds() } returns listOf("first_review")
        coEvery { socialRepository.getFriends() } returns emptyList()

        checker.evaluate(AchievementChecker.EvalContext(baseProfile(), reviewCount = 10))

        val idsSlot = slot<List<String>>()
        coVerify { achievementRepository.unlockAchievements(capture(idsSlot), any()) }
        assertTrue("prolific_critic" in idsSlot.captured)
    }

    @Test
    fun `group_matcher se desbloquea al completar 3 sesiones grupales`() = runTest {
        coEvery { achievementRepository.getUnlockedIds() } returns emptyList()

        checker.onGroupMatchCompleted(3)

        val idsSlot = slot<List<String>>()
        coVerify { achievementRepository.unlockAchievements(capture(idsSlot), any()) }
        assertEquals(listOf("group_matcher"), idsSlot.captured)
    }

    @Test
    fun `group_matcher no se desbloquea con menos de 3 sesiones`() = runTest {
        coEvery { achievementRepository.getUnlockedIds() } returns emptyList()

        checker.onGroupMatchCompleted(2)

        coVerify(exactly = 0) { achievementRepository.unlockAchievements(any(), any()) }
    }
}
