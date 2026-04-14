package com.andrea.showmateapp.domain.repository

import com.andrea.showmateapp.data.model.UserProfile
import kotlinx.coroutines.flow.Flow

interface IUserRepository {
    suspend fun getUserProfile(): UserProfile?
    fun getUserProfileFlow(): Flow<UserProfile?>
    fun getCurrentUserEmail(): String?
    suspend fun initUserProfile(username: String)
    suspend fun saveOnboardingInterests(
        genres: List<String>,
        watchedShows: List<com.andrea.showmateapp.data.model.MediaContent> = emptyList(),
        lovedShows: List<com.andrea.showmateapp.data.model.MediaContent> = emptyList(),
        preferShortEpisodes: Boolean? = null,
        preferFinishedShows: Boolean? = null,
        preferDubbed: Boolean? = null
    )
    suspend fun updateProfile(username: String)
    suspend fun getSimilarUsers(limit: Long = 30): List<UserProfile>
    suspend fun userExists(email: String): Boolean
    suspend fun getFriendProfile(friendEmail: String): UserProfile?
    suspend fun compareWithFriend(friendEmail: String): List<Int>
    suspend fun recordViewingSession(showId: Int, episodeCount: Int)
    suspend fun resetAlgorithmData()
    suspend fun clearUserCache()
    suspend fun updateProfilePhoto(url: String)
    suspend fun deleteAccount()
    suspend fun restoreBackup(partial: UserProfile)
}

