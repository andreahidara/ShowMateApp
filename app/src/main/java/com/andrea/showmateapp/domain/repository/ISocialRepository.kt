package com.andrea.showmateapp.domain.repository

import com.andrea.showmateapp.data.model.FriendInfo
import com.andrea.showmateapp.data.model.FriendRequest
import com.andrea.showmateapp.data.model.UserProfile

interface ISocialRepository {
    fun getCurrentUid(): String?

    suspend fun searchByUsername(query: String): List<UserProfile>

    suspend fun sendFriendRequest(toUid: String, toUsername: String): Boolean

    suspend fun acceptFriendRequest(requestId: String, fromUid: String)
    suspend fun rejectFriendRequest(requestId: String)
    suspend fun removeFriend(friendUid: String)

    suspend fun getFriends(): List<FriendInfo>
    suspend fun getIncomingRequests(): List<FriendRequest>
    suspend fun getOutgoingRequests(): List<FriendRequest>
    suspend fun getPendingRequestCount(): Int

    suspend fun getFriendProfile(friendUid: String): UserProfile?

    suspend fun saveDeviceToken(token: String)
}
