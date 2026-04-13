package com.andrea.showmateapp.data.repository

import com.andrea.showmateapp.data.model.ActivityEvent
import com.andrea.showmateapp.data.model.FriendInfo
import com.andrea.showmateapp.data.model.FriendRequest
import com.andrea.showmateapp.data.model.UserProfile
import com.andrea.showmateapp.di.IoDispatcher
import com.andrea.showmateapp.domain.repository.ISocialRepository
import com.andrea.showmateapp.util.GenreMapper
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.merge
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import timber.log.Timber

@Singleton
class SocialRepository @Inject constructor(
    private val db: FirebaseFirestore,
    private val auth: FirebaseAuth,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher
) : ISocialRepository {

    private val users = db.collection("users")
    private val friendRequests = db.collection("friendRequests")

    @Volatile private var cachedUsername: String? = null

    @Volatile private var cachedUsernameUid: String? = null

    override fun getCurrentUid(): String? = auth.currentUser?.uid

    override suspend fun searchByUsername(query: String): List<UserProfile> = withContext(ioDispatcher) {
        if (query.length < 2) return@withContext emptyList()
        val myUid = auth.currentUser?.uid
        try {
            users
                .whereGreaterThanOrEqualTo("username", query)
                .whereLessThanOrEqualTo("username", query + "\uf8ff")
                .limit(20)
                .get()
                .await()
                .toObjects(UserProfile::class.java)
                .filter { it.userId != myUid }
        } catch (e: Exception) {
            if (e is CancellationException) throw e
            emptyList()
        }
    }

    override suspend fun sendFriendRequest(toUid: String, toUsername: String): Boolean = withContext(ioDispatcher) {
        val me = auth.currentUser ?: return@withContext false
        try {
            val (fwd, rev) = coroutineScope {
                val a = async {
                    friendRequests.whereEqualTo("fromUid", me.uid).whereEqualTo("toUid", toUid)
                        .limit(1).get().await()
                }
                val b = async {
                    friendRequests.whereEqualTo("fromUid", toUid).whereEqualTo("toUid", me.uid)
                        .limit(1).get().await()
                }
                a.await() to b.await()
            }
            if (!fwd.isEmpty || !rev.isEmpty) return@withContext false

            val myUsername = resolveUsername(me.uid)
            val docRef = friendRequests.document()
            docRef.set(
                mapOf(
                    "id" to docRef.id,
                    "fromUid" to me.uid,
                    "toUid" to toUid,
                    "fromUsername" to myUsername,
                    "toUsername" to toUsername,
                    "status" to FriendRequest.STATUS_PENDING,
                    "createdAt" to System.currentTimeMillis()
                )
            ).await()

            users.document(toUid).collection("notifications").add(
                mapOf(
                    "type" to "friend_request",
                    "fromUid" to me.uid,
                    "fromUsername" to myUsername,
                    "message" to "$myUsername quiere ser tu amigo en ShowMate",
                    "createdAt" to System.currentTimeMillis(),
                    "read" to false
                )
            ).await()
            true
        } catch (e: Exception) {
            if (e is CancellationException) throw e
            false
        }
    }

    override suspend fun acceptFriendRequest(requestId: String, fromUid: String) = withContext(ioDispatcher) {
        val myUid = auth.currentUser?.uid ?: return@withContext
        try {
            // Step 1 — Transaction: delete the request + add fromUid to OWN friend list.
            // We only touch myUserRef here so the transaction doesn't conflict with
            // concurrent reads/writes on fromUserRef (avoids the size-check race condition
            // in the isValidFriendUpdate security rule).
            db.runTransaction { transaction ->
                val requestRef = friendRequests.document(requestId)
                val myUserRef = users.document(myUid)

                val myDoc = transaction.get(myUserRef)

                @Suppress("UNCHECKED_CAST")
                val myFriendIds = (myDoc.get("friendIds") as? List<String> ?: emptyList()).toMutableList()
                if (!myFriendIds.contains(fromUid)) myFriendIds.add(fromUid)

                transaction.delete(requestRef)
                transaction.update(myUserRef, "friendIds", myFriendIds)
            }.await()

            // Step 2 — Atomically add myUid to the sender's friend list.
            // arrayUnion is idempotent and requires only the isValidFriendUpdate rule
            // (no size read needed, so no race condition).
            users.document(fromUid)
                .update("friendIds", FieldValue.arrayUnion(myUid))
                .await()

            // Step 3 — Send notification. Wrapped in runCatching so a clock-skew
            // rejection (isRecentMs rule) or any other notification error never
            // bubbles up and makes the whole accept look failed to the user.
            runCatching {
                val myUsername = resolveUsername(myUid)
                users.document(fromUid).collection("notifications").add(
                    mapOf(
                        "type" to "request_accepted",
                        "fromUid" to myUid,
                        "fromUsername" to myUsername,
                        "message" to "$myUsername aceptó tu solicitud de amistad",
                        "createdAt" to System.currentTimeMillis(),
                        "read" to false
                    )
                ).await()
            }.onFailure { Timber.w(it, "Notification after accept failed (non-critical)") }
        } catch (e: Exception) {
            if (e is CancellationException) throw e
            Timber.e(e, "Error accepting friend request $requestId: ${e.message}")
            throw e
        }
    }

    override suspend fun rejectFriendRequest(requestId: String) = withContext(ioDispatcher) {
        try {
            friendRequests.document(requestId).delete().await()
        } catch (e: Exception) {
            if (e is CancellationException) throw e
        }
        Unit
    }

    override suspend fun removeFriend(friendUid: String) = withContext(ioDispatcher) {
        val myUid = auth.currentUser?.uid ?: return@withContext
        try {
            coroutineScope {
                val q1 = async {
                    friendRequests
                        .whereEqualTo("fromUid", myUid).whereEqualTo("toUid", friendUid)
                        .whereEqualTo("status", FriendRequest.STATUS_ACCEPTED).get().await()
                }
                val q2 = async {
                    friendRequests
                        .whereEqualTo("fromUid", friendUid).whereEqualTo("toUid", myUid)
                        .whereEqualTo("status", FriendRequest.STATUS_ACCEPTED).get().await()
                }

                val requests = q1.await().documents + q2.await().documents

                db.runTransaction { transaction ->
                    requests.forEach { transaction.delete(it.reference) }

                    val myUserRef = users.document(myUid)
                    transaction.update(myUserRef, "friendIds", FieldValue.arrayRemove(friendUid))

                    val friendUserRef = users.document(friendUid)
                    transaction.update(friendUserRef, "friendIds", FieldValue.arrayRemove(myUid))
                }.await()
            }
        } catch (e: Exception) {
            if (e is CancellationException) throw e
            Timber.e(e, "Error removing friend")
        }
    }

    override suspend fun getFriends(): List<FriendInfo> = withContext(ioDispatcher) {
        val myUid = auth.currentUser?.uid ?: return@withContext emptyList()
        try {
            val myProfile = users.document(myUid).get().await().toObject(UserProfile::class.java)
            val friendUids = myProfile?.friendIds ?: emptyList()

            if (friendUids.isEmpty()) return@withContext emptyList()

            coroutineScope {
                friendUids.map { uid ->
                    async {
                        val p = users.document(uid).get().await().toObject(UserProfile::class.java)
                        val compat = if (myProfile != null && p != null) {
                            (GenreMapper.jaccardSimilarity(myProfile.genreScores, p.genreScores) * 100)
                                .toInt().coerceIn(0, 100)
                        } else {
                            0
                        }
                        FriendInfo(
                            uid = uid,
                            username = p?.username ?: "",
                            email = p?.email ?: "",
                            compatibilityScore = compat
                        )
                    }
                }.awaitAll()
            }
        } catch (e: Exception) {
            if (e is CancellationException) throw e
            Timber.e(e, "Error getting friends")
            emptyList()
        }
    }

    override suspend fun getIncomingRequests(): List<FriendRequest> = withContext(ioDispatcher) {
        val myUid = auth.currentUser?.uid ?: return@withContext emptyList()
        try {
            friendRequests
                .whereEqualTo("toUid", myUid)
                .whereEqualTo("status", FriendRequest.STATUS_PENDING)
                .get().await()
                .documents
                .mapNotNull { doc ->
                    // Always use the actual document ID — the stored "id" field may be missing
                    // in older records and would default to "" causing the accept to fail.
                    doc.toObject(FriendRequest::class.java)?.copy(id = doc.id)
                }
        } catch (e: Exception) {
            if (e is CancellationException) throw e
            emptyList()
        }
    }

    override suspend fun getOutgoingRequests(): List<FriendRequest> = withContext(ioDispatcher) {
        val myUid = auth.currentUser?.uid ?: return@withContext emptyList()
        try {
            friendRequests
                .whereEqualTo("fromUid", myUid)
                .whereEqualTo("status", FriendRequest.STATUS_PENDING)
                .get().await()
                .documents
                .mapNotNull { doc ->
                    doc.toObject(FriendRequest::class.java)?.copy(id = doc.id)
                }
        } catch (e: Exception) {
            if (e is CancellationException) throw e
            emptyList()
        }
    }

    override suspend fun getPendingRequestCount(): Int = withContext(ioDispatcher) {
        val myUid = auth.currentUser?.uid ?: return@withContext 0
        try {
            friendRequests
                .whereEqualTo("toUid", myUid)
                .whereEqualTo("status", FriendRequest.STATUS_PENDING)
                .get().await().size()
        } catch (e: Exception) {
            if (e is CancellationException) throw e
            0
        }
    }

    override suspend fun getFriendActivityFeed(friendUids: List<String>, limit: Int): List<ActivityEvent> =
        withContext(ioDispatcher) {
            if (friendUids.isEmpty()) return@withContext emptyList()
            try {
                coroutineScope {
                    friendUids.take(20).map { uid ->
                        async {
                            users.document(uid)
                                .collection("activity")
                                .orderBy("timestamp", Query.Direction.DESCENDING)
                                .limit(10)
                                .get().await()
                                .toObjects(ActivityEvent::class.java)
                        }
                    }.awaitAll()
                        .flatten()
                        .sortedByDescending { it.timestamp }
                        .take(limit)
                }
            } catch (e: Exception) {
                if (e is CancellationException) throw e
                emptyList()
            }
        }

    override suspend fun postActivityEvent(
        type: String,
        mediaId: Int,
        mediaTitle: String,
        mediaPoster: String,
        score: Float
    ) = withContext(ioDispatcher) {
        val me = auth.currentUser ?: return@withContext
        try {
            val username = resolveUsername(me.uid)
            users.document(me.uid).collection("activity").add(
                mapOf(
                    "userId" to me.uid,
                    "username" to username,
                    "type" to type,
                    "mediaId" to mediaId,
                    "mediaTitle" to mediaTitle,
                    "mediaPoster" to mediaPoster,
                    "score" to score,
                    "timestamp" to System.currentTimeMillis()
                )
            ).await()
        } catch (e: Exception) {
            if (e is CancellationException) throw e
        }
    }

    override suspend fun getSuggestedFriends(): List<UserProfile> = withContext(ioDispatcher) {
        val myUid = auth.currentUser?.uid ?: return@withContext emptyList()
        try {
            val excluded = coroutineScope {
                val sentD = async {
                    friendRequests.whereEqualTo("fromUid", myUid).get().await()
                        .toObjects(FriendRequest::class.java).map { it.toUid }
                }
                val receivedD = async {
                    friendRequests.whereEqualTo("toUid", myUid).get().await()
                        .toObjects(FriendRequest::class.java).map { it.fromUid }
                }
                (sentD.await() + receivedD.await()).toSet() + myUid
            }

            val myProfile = users.document(myUid).get().await().toObject(UserProfile::class.java)
            val candidates = users.whereNotEqualTo("userId", myUid).limit(50).get().await()
                .toObjects(UserProfile::class.java)
                .filter { it.userId !in excluded }

            if (myProfile == null) return@withContext candidates.take(10)
            candidates
                .map { it to GenreMapper.jaccardSimilarity(myProfile.genreScores, it.genreScores) }
                .sortedByDescending { it.second }
                .take(10).map { it.first }
        } catch (e: Exception) {
            if (e is CancellationException) throw e
            emptyList()
        }
    }

    override fun observeFriendActivityFeed(friendUids: List<String>): Flow<List<ActivityEvent>> {
        if (friendUids.isEmpty()) return kotlinx.coroutines.flow.flowOf(emptyList())

        val flows = friendUids.take(10).map { uid ->
            callbackFlow {
                val listener = users.document(uid)
                    .collection("activity")
                    .orderBy("timestamp", Query.Direction.DESCENDING)
                    .limit(10)
                    .addSnapshotListener { snap, err ->
                        if (err != null) return@addSnapshotListener
                        trySend(snap?.toObjects(ActivityEvent::class.java) ?: emptyList())
                    }
                awaitClose { listener.remove() }
            }.catch { emit(emptyList()) }
        }

        return merge(*flows.toTypedArray()).flowOn(ioDispatcher)
    }

    override suspend fun getFriendProfile(friendUid: String): UserProfile? = withContext(ioDispatcher) {
        try {
            users.document(friendUid).get().await().toObject(UserProfile::class.java)
        } catch (e: Exception) {
            if (e is CancellationException) throw e
            null
        }
    }

    override suspend fun saveDeviceToken(token: String) = withContext(ioDispatcher) {
        val uid = auth.currentUser?.uid ?: return@withContext
        try {
            users.document(uid).update("fcmToken", token).await()
        } catch (e: Exception) {
            if (e is CancellationException) throw e
        }
    }

    private suspend fun resolveUsername(uid: String): String {
        cachedUsername?.takeIf { uid == cachedUsernameUid }?.let { return it }
        return try {
            val profile = users.document(uid).get().await().toObject(UserProfile::class.java)
            val name = profile?.username?.takeIf { it.isNotBlank() }
                ?: auth.currentUser?.email
                ?: "Usuario"
            if (uid == auth.currentUser?.uid) {
                cachedUsername = name
                cachedUsernameUid = uid
            }
            name
        } catch (e: Exception) {
            if (e is CancellationException) throw e
            auth.currentUser?.email ?: "Usuario"
        }
    }
}
