package com.andrea.showmateapp.domain.repository

import com.andrea.showmateapp.data.model.NowWatching
import com.andrea.showmateapp.data.model.SharedList
import kotlinx.coroutines.flow.Flow

interface ISharedListRepository {
    fun observeMySharedLists(): Flow<List<SharedList>>
    suspend fun createSharedList(name: String, memberUids: List<String>, memberUsernames: List<String>): String?
    suspend fun addShowToSharedList(listId: String, showId: Int)
    suspend fun removeShowFromSharedList(listId: String, showId: Int)
    suspend fun deleteSharedList(listId: String)
    suspend fun setNowWatching(showId: Int, showName: String, posterPath: String?)
    suspend fun clearNowWatching()
    suspend fun getFriendsNowWatching(friendUids: List<String>): List<NowWatching>
}
