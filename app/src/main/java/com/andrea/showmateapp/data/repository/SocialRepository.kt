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
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SocialRepository @Inject constructor(
    private val db: FirebaseFirestore,
    private val auth: FirebaseAuth,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher
) : ISocialRepository {

    private val users          = db.collection("users")
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

    override suspend fun sendFriendRequest(toUid: String, toUsername: String): Boolean =
        withContext(ioDispatcher) {
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
                        "id"           to docRef.id,
                        "fromUid"      to me.uid,
                        "toUid"        to toUid,
                        "fromUsername" to myUsername,
                        "toUsername"   to toUsername,
                        "status"       to FriendRequest.STATUS_PENDING,
                        "createdAt"    to System.currentTimeMillis()
                    )
                ).await()

                users.document(toUid).collection("notifications").add(
                    mapOf(
                        "type"         to "friend_request",
                        "fromUid"      to me.uid,
                        "fromUsername" to myUsername,
                        "message"      to "$myUsername quiere ser tu amigo en ShowMate",
                        "createdAt"    to System.currentTimeMillis(),
                        "read"         to false
                    )
                ).await()
                true
            } catch (e: Exception) {
                if (e is CancellationException) throw e
                false
            }
        }

    override suspend fun acceptFriendRequest(requestId: String, fromUid: String) =
        withContext(ioDispatcher) {
            val myUid = auth.currentUser?.uid ?: return@withContext
            try {
                db.runTransaction { transaction ->
                    val requestRef = friendRequests.document(requestId)
                    transaction.update(requestRef, "status", FriendRequest.STATUS_ACCEPTED)

                    val myUserRef = users.document(myUid)
                    transaction.update(myUserRef, "friendIds", FieldValue.arrayUnion(fromUid))

                    val fromUserRef = users.document(fromUid)
                    transaction.update(fromUserRef, "friendIds", FieldValue.arrayUnion(myUid))
                }.await()

                val myUsername = resolveUsername(myUid)
                users.document(fromUid).collection("notifications").add(
                    mapOf(
                        "type"         to "request_accepted",
                        "fromUid"      to myUid,
                        "fromUsername" to myUsername,
                        "message"      to "$myUsername aceptó tu solicitud de amistad",
                        "createdAt"    to System.currentTimeMillis(),
                        "read"         to false
                    )
                ).await()
            } catch (e: Exception) {
                if (e is CancellationException) throw e
                Timber.e(e, "Error accepting friend request")
            }
        }

    override suspend fun rejectFriendRequest(requestId: String) = withContext(ioDispatcher) {
        try { friendRequests.document(requestId).delete().await() }
        catch (e: Exception) { if (e is CancellationException) throw e }
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
                val batch = db.batch()
                (q1.await().documents + q2.await().documents).forEach { batch.delete(it.reference) }
                batch.commit().await()
            }
        } catch (e: Exception) {
            if (e is CancellationException) throw e
        }
    }

    override suspend fun getFriends(): List<FriendInfo> = withContext(ioDispatcher) {
        val myUid = auth.currentUser?.uid ?: return@withContext emptyList()
        try {
            coroutineScope {
                val sentD      = async {
                    friendRequests.whereEqualTo("fromUid", myUid)
                        .whereEqualTo("status", FriendRequest.STATUS_ACCEPTED)
                        .get().await().toObjects(FriendRequest::class.java)
                }
                val receivedD  = async {
                    friendRequests.whereEqualTo("toUid", myUid)
                        .whereEqualTo("status", FriendRequest.STATUS_ACCEPTED)
                        .get().await().toObjects(FriendRequest::class.java)
                }
                val myProfileD = async {
                    users.document(myUid).get().await().toObject(UserProfile::class.java)
                }

                val sent      = sentD.await()
                val received  = receivedD.await()
                val myProfile = myProfileD.await()

                val friendUids = (sent.map { it.toUid } + received.map { it.fromUid }).distinct()
                friendUids.map { uid ->
                    async {
                        val p      = users.document(uid).get().await().toObject(UserProfile::class.java)
                        val compat = if (myProfile != null && p != null)
                            (GenreMapper.jaccardSimilarity(myProfile.genreScores, p.genreScores) * 100)
                                .toInt().coerceIn(0, 100)
                        else 0
                        FriendInfo(
                            uid               = uid,
                            username          = p?.username ?: "",
                            email             = p?.email    ?: "",
                            compatibilityScore = compat
                        )
                    }
                }.awaitAll()
            }
        } catch (e: Exception) {
            if (e is CancellationException) throw e
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
                .toObjects(FriendRequest::class.java)
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
                .toObjects(FriendRequest::class.java)
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

    override suspend fun getFriendActivityFeed(
        friendUids: List<String>, limit: Int
    ): List<ActivityEvent> = withContext(ioDispatcher) {
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
        type: String, mediaId: Int, mediaTitle: String, mediaPoster: String, score: Float
    ) = withContext(ioDispatcher) {
        val me = auth.currentUser ?: return@withContext
        try {
            val username = resolveUsername(me.uid)
            users.document(me.uid).collection("activity").add(
                mapOf(
                    "userId"      to me.uid,
                    "username"    to username,
                    "type"        to type,
                    "mediaId"     to mediaId,
                    "mediaTitle"  to mediaTitle,
                    "mediaPoster" to mediaPoster,
                    "score"       to score,
                    "timestamp"   to System.currentTimeMillis()
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
                val sentD     = async {
                    friendRequests.whereEqualTo("fromUid", myUid).get().await()
                        .toObjects(FriendRequest::class.java).map { it.toUid }
                }
                val receivedD = async {
                    friendRequests.whereEqualTo("toUid", myUid).get().await()
                        .toObjects(FriendRequest::class.java).map { it.fromUid }
                }
                (sentD.await() + receivedD.await()).toSet() + myUid
            }

            val myProfile  = users.document(myUid).get().await().toObject(UserProfile::class.java)
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

    override suspend fun saveDeviceToken(token: String) = withContext(ioDispatcher) {
        val uid = auth.currentUser?.uid ?: return@withContext
        try { users.document(uid).update("fcmToken", token).await() }
        catch (e: Exception) { if (e is CancellationException) throw e }
    }

    private suspend fun resolveUsername(uid: String): String {
        cachedUsername?.takeIf { uid == cachedUsernameUid }?.let { return it }
        return try {
            val profile = users.document(uid).get().await().toObject(UserProfile::class.java)
            val name = profile?.username?.takeIf { it.isNotBlank() }
                ?: auth.currentUser?.email
                ?: "Usuario"
            if (uid == auth.currentUser?.uid) {
                cachedUsername    = name
                cachedUsernameUid = uid
            }
            name
        } catch (e: Exception) { if (e is CancellationException) throw e;
            auth.currentUser?.email ?: "Usuario"
        }
    }
}
