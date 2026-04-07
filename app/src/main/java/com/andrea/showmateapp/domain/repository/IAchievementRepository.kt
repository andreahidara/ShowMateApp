package com.andrea.showmateapp.domain.repository

interface IAchievementRepository {

    suspend fun getUnlockedIds(): List<String>

    suspend fun unlockAchievements(achievementIds: List<String>, xpToAdd: Int)

    suspend fun addXp(delta: Int)

    suspend fun getXp(): Int

    suspend fun getFriendLeaderboard(friendEmails: List<String>): List<LeaderboardEntry>

    suspend fun incrementAndGetGroupMatchCount(): Int

    data class LeaderboardEntry(
        val username: String,
        val email: String,
        val xp: Int,
        val levelName: String
    )
}
