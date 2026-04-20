package com.andrea.showmateapp.data.repository

import com.andrea.showmateapp.data.model.GroupSession
import com.andrea.showmateapp.data.model.MemberVoteDoc
import com.andrea.showmateapp.data.model.VoteType
import com.andrea.showmateapp.di.IoDispatcher
import com.andrea.showmateapp.domain.repository.IGroupSessionRepository
import com.andrea.showmateapp.util.safeFirestoreCall
import com.andrea.showmateapp.util.safeFirestoreRun
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.SetOptions
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.channels.awaitClose
import timber.log.Timber
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext

@Singleton
class GroupSessionRepository @Inject constructor(
    private val db: FirebaseFirestore,
    private val auth: FirebaseAuth,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher
) : IGroupSessionRepository {

    private val sessions = db.collection("groupSessions")

    override fun observeSession(sessionId: String): Flow<GroupSession?> = callbackFlow {
        val listener = sessions.document(sessionId)
            .addSnapshotListener { snap, error ->
                if (error != null) { close(error); return@addSnapshotListener }
                trySend(snap?.toObject(GroupSession::class.java))
            }
        awaitClose { listener.remove() }
    }.flowOn(ioDispatcher)

    override fun observeAllVotes(sessionId: String): Flow<Map<String, MemberVoteDoc>> = callbackFlow {
        val listener = sessions.document(sessionId).collection("votes")
            .addSnapshotListener { snap, error ->
                if (error != null) { close(error); return@addSnapshotListener }
                val votes = snap?.documents?.associate { doc ->
                    doc.id to (doc.toObject(MemberVoteDoc::class.java) ?: MemberVoteDoc(email = doc.id))
                } ?: emptyMap()
                trySend(votes)
            }
        awaitClose { listener.remove() }
    }.flowOn(ioDispatcher)

    override suspend fun createSession(memberEmails: List<String>): GroupSession = withContext(ioDispatcher) {
        val myEmail = auth.currentUser?.email ?: ""
        val docRef = sessions.document()
        val session = GroupSession(
            id = docRef.id,
            hostEmail = myEmail,
            memberEmails = (listOf(myEmail) + memberEmails).distinct(),
            status = GroupSession.STATUS_LOBBY,
            createdAt = System.currentTimeMillis()
        )
        try {
            docRef.set(session).await()
        } catch (e: Exception) {
            if (e is CancellationException) throw e
            Timber.e(e, "createSession failed for host $myEmail")
            throw e
        }
        session
    }

    override suspend fun updateCandidates(sessionId: String, candidateIds: List<Int>) = withContext(ioDispatcher) {
        safeFirestoreRun { sessions.document(sessionId).update("candidateIds", candidateIds).await() }
    }

    override suspend fun updateSessionStatus(sessionId: String, status: String) = withContext(ioDispatcher) {
        safeFirestoreRun { sessions.document(sessionId).update("status", status).await() }
    }

    override suspend fun submitVote(sessionId: String, email: String, mediaId: Int, voteType: VoteType) =
        withContext(ioDispatcher) {
            val voteRef = sessions.document(sessionId).collection("votes").document(email)
            safeFirestoreRun {
                db.runTransaction { tx ->
                    val current = tx.get(voteRef).toObject(MemberVoteDoc::class.java) ?: MemberVoteDoc(email = email)
                    val updated = when (voteType) {
                        VoteType.YES -> current.copy(yes = (current.yes + mediaId).distinct())
                        VoteType.NO -> current.copy(no = (current.no + mediaId).distinct())
                        VoteType.MAYBE -> current.copy(maybe = (current.maybe + mediaId).distinct())
                        VoteType.SUPER_LIKE -> current.copy(superLikeId = mediaId)
                    }
                    tx.set(voteRef, updated)
                }.await()
            }
        }

    override suspend fun submitVeto(sessionId: String, email: String, mediaId: Int) = withContext(ioDispatcher) {
        safeFirestoreRun { sessions.document(sessionId).update("vetoes.$email", mediaId).await() }
    }

    override suspend fun setMemberReady(sessionId: String, email: String) = withContext(ioDispatcher) {
        val voteRef = sessions.document(sessionId).collection("votes").document(email)
        safeFirestoreRun { voteRef.set(mapOf("ready" to true), SetOptions.merge()).await() }
    }

    override suspend fun setMatch(sessionId: String, mediaId: Int) = withContext(ioDispatcher) {
        safeFirestoreRun {
            sessions.document(sessionId).update(
                mapOf(
                    "matchedMediaId" to mediaId,
                    "status" to GroupSession.STATUS_FINISHED,
                    "finishedAt" to System.currentTimeMillis()
                )
            ).await()
        }
    }

    override suspend fun saveNightTitle(sessionId: String, title: String) = withContext(ioDispatcher) {
        safeFirestoreRun { sessions.document(sessionId).update("nightTitle", title).await() }
    }

    override suspend fun getPastNights(email: String, limit: Int): List<GroupSession> = withContext(ioDispatcher) {
        safeFirestoreCall(emptyList()) {
            sessions
                .whereArrayContains("memberEmails", email)
                .whereEqualTo("status", GroupSession.STATUS_FINISHED)
                .orderBy("finishedAt", Query.Direction.DESCENDING)
                .limit(limit.toLong())
                .get().await()
                .toObjects(GroupSession::class.java)
        }
    }
}
