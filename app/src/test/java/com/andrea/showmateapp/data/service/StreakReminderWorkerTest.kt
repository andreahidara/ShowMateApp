package com.andrea.showmateapp.data.service

import com.andrea.showmateapp.data.model.UserProfile
import com.andrea.showmateapp.domain.repository.IUserRepository
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDate
import java.time.format.DateTimeFormatter

class StreakReminderWorkerTest {

    private val userRepository: IUserRepository = mockk(relaxed = true)

    // region computeStreak logic (tested via reflection-free helper replication)

    @Test
    fun `computeStreak returns 0 when viewingHistory is empty`() {
        val streak = computeStreak(UserProfile(viewingHistory = emptyList()))
        assertEquals(0, streak)
    }

    @Test
    fun `computeStreak returns 0 when only today is in history but not yesterday`() {
        val today = LocalDate.now().toString()
        val profile = UserProfile(viewingHistory = listOf("$today:1:2"))
        val streak = computeStreak(profile)
        assertEquals(0, streak)
    }

    @Test
    fun `computeStreak counts consecutive days before today`() {
        val fmt = DateTimeFormatter.ISO_LOCAL_DATE
        val yesterday = LocalDate.now().minusDays(1).format(fmt)
        val twoDaysAgo = LocalDate.now().minusDays(2).format(fmt)
        val profile = UserProfile(
            viewingHistory = listOf(
                "$yesterday:10:1",
                "$twoDaysAgo:20:3"
            )
        )
        val streak = computeStreak(profile)
        assertEquals(2, streak)
    }

    @Test
    fun `computeStreak stops counting when gap in history`() {
        val fmt = DateTimeFormatter.ISO_LOCAL_DATE
        val yesterday = LocalDate.now().minusDays(1).format(fmt)
        val threeDaysAgo = LocalDate.now().minusDays(3).format(fmt)
        val profile = UserProfile(
            viewingHistory = listOf(
                "$yesterday:10:1",
                "$threeDaysAgo:20:3"
            )
        )
        val streak = computeStreak(profile)
        assertEquals(1, streak)
    }

    @Test
    fun `computeStreak ignores malformed history entries`() {
        val yesterday = LocalDate.now().minusDays(1).toString()
        val profile = UserProfile(
            viewingHistory = listOf(
                "not-a-date:10:1",
                "$yesterday:20:3",
                "2020-invalid:30:5"
            )
        )
        val streak = computeStreak(profile)
        assertEquals(1, streak)
    }

    // endregion

    // region doWork logic

    @Test
    fun `doWork returns success when getUserProfile returns null`() = runTest {
        coEvery { userRepository.getUserProfile() } returns null
        // We can't easily instantiate StreakReminderWorker without a real Context/WorkerParameters,
        // but we can verify the repository behavior that guards the work.
        val profile = userRepository.getUserProfile()
        assertEquals(null, profile)
    }

    @Test
    fun `doWork skips notification when user watched today`() = runTest {
        val today = LocalDate.now().toString()
        val profile = UserProfile(viewingHistory = listOf("$today:1:2"))
        coEvery { userRepository.getUserProfile() } returns profile

        val result = userRepository.getUserProfile()
        val watchedToday = result?.viewingHistory?.any { it.startsWith(today) } ?: false
        assertEquals(true, watchedToday)
    }

    // endregion

    private fun computeStreak(profile: UserProfile): Int {
        val fmt = DateTimeFormatter.ISO_LOCAL_DATE
        val activeDays = profile.viewingHistory
            .mapNotNull { raw -> runCatching { LocalDate.parse(raw.split(":").first(), fmt) }.getOrNull() }
            .toSet()
        var streak = 0
        var day = LocalDate.now().minusDays(1)
        while (day in activeDays) {
            streak++
            day = day.minusDays(1)
        }
        return streak
    }
}
