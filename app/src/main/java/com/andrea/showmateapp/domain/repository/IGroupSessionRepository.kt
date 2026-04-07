package com.andrea.showmateapp.domain.repository

import com.andrea.showmateapp.data.model.GroupSession
import com.andrea.showmateapp.data.model.MemberVoteDoc
import com.andrea.showmateapp.data.model.VoteType
import kotlinx.coroutines.flow.Flow

interface IGroupSessionRepository {
    fun observeSession(sessionId: String): Flow<GroupSession?>

    fun observeAllVotes(sessionId: String): Flow<Map<String, MemberVoteDoc>>

    suspend fun createSession(memberEmails: List<String>): GroupSession

    suspend fun updateCandidates(sessionId: String, candidateIds: List<Int>)

    suspend fun updateSessionStatus(sessionId: String, status: String)

    suspend fun submitVote(sessionId: String, email: String, mediaId: Int, voteType: VoteType)

    suspend fun submitVeto(sessionId: String, email: String, mediaId: Int)

    suspend fun setMemberReady(sessionId: String, email: String)

    suspend fun setMatch(sessionId: String, mediaId: Int)

    suspend fun saveNightTitle(sessionId: String, title: String)

    suspend fun getPastNights(email: String, limit: Int = 20): List<GroupSession>
}
